package com.example.socialmediaservice.service;

import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient; // Keep if other tests use it
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepository;

    @Mock
    private WebClient.Builder webClientBuilder; // Mock if other UserService methods use it directly

    @InjectMocks
    private UserService userService;

    @Mock
    private WebClient webClientMock; // Mock WebClient itself
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock; // For bodyValue
    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    private OAuth2User mockOAuth2User;
    private Map<String, Object> userAttributes;

    @BeforeEach
    void setUp() {
        userAttributes = new HashMap<>();
        userAttributes.put("email", "testuser@example.com");
        userAttributes.put("given_name", "Test");
        userAttributes.put("family_name", "User");
        userAttributes.put("picture", "http://example.com/avatar.jpg");

        mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute(anyString())).thenAnswer(invocation -> userAttributes.get(invocation.getArgument(0)));
        when(mockOAuth2User.getAttributes()).thenReturn(userAttributes); // In case getAttributes() is used
    }

    @Test
    void processGoogleLogin_whenUserDoesNotExist_createsNewUser() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // Ensure userId is set if generated within the method
            if (savedUser.getUserId() == null) {
                 savedUser.setUserId(UUID.randomUUID().toString());
            }
            return savedUser;
        });

        Map<String, Object> result = userService.processGoogleLogin(mockOAuth2User);

        assertNotNull(result);
        assertTrue(result.containsKey("token"));
        assertTrue(result.containsKey("user"));

        User resultUser = (User) result.get("user");
        assertEquals("testuser@example.com", resultUser.getEmail());
        assertEquals("Test", resultUser.getFirstName());
        assertEquals("User", resultUser.getLastName());
        assertEquals("http://example.com/avatar.jpg", resultUser.getAvatarUrl());
        assertNotNull(resultUser.getUserId()); // Ensure userId is set

        verify(userRepository, times(1)).findByEmail("testuser@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void processGoogleLogin_whenUserExists_returnsExistingUser() {
        String existingUserId = UUID.randomUUID().toString();
        User existingUser = new User();
        existingUser.setUserId(existingUserId);
        existingUser.setEmail("testuser@example.com");
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(existingUser));

        Map<String, Object> result = userService.processGoogleLogin(mockOAuth2User);

        assertNotNull(result);
        assertTrue(result.containsKey("token"));
        assertTrue(result.containsKey("user"));

        User resultUser = (User) result.get("user");
        assertEquals(existingUserId, resultUser.getUserId());
        assertEquals("testuser@example.com", resultUser.getEmail());
        assertEquals("Existing", resultUser.getFirstName()); // Existing data should be preserved or updated based on policy

        verify(userRepository, times(1)).findByEmail("testuser@example.com");
        verify(userRepository, never()).save(any(User.class)); // Should not save if user exists (unless updating)
    }

    @Test
    void processGoogleLogin_whenOAuth2UserIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.processGoogleLogin(null);
        });
    }

    @Test
    void processGoogleLogin_whenEmailIsNullInAttributes_throwsRuntimeException() {
        userAttributes.remove("email"); // Make email null
        // Need to re-mock as the attributes map was captured at mock creation in @BeforeEach
        OAuth2User userWithNullEmail = mock(OAuth2User.class);
        when(userWithNullEmail.getAttribute("email")).thenReturn(null);


        assertThrows(RuntimeException.class, () -> {
            userService.processGoogleLogin(userWithNullEmail);
        });
    }

    private void setupWebClientMocks() {
        // Common setup for WebClient mocks used by both authenticate and refresh
        when(webClientBuilder.build()).thenReturn(webClientMock);
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.contentType(any(MediaType.class))).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock); // If bodyValue is before retrieve
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
    }

    @Test
    void refreshAccessToken_success() {
        setupWebClientMocks(); // Ensure WebClient mocks are set up

        UserService.TokenResponse mockKeycloakResponse = new UserService.TokenResponse();
        // Can't directly set private fields, so we'd need a constructor or use reflection,
        // or assume Jackson populates it. For mocking, let's assume it's populated.
        // To actually set fields for the mockKeycloakResponse, you would typically:
        // 1. Add setters to TokenResponse (not ideal for a DTO used with Jackson)
        // 2. Use a library like Mockito to mock the object and its getters if it were an interface/mockable class.
        // 3. Construct it with a JSON string if you have ObjectMapper instance.
        // For simplicity here, we'll rely on what bodyToMono would produce.
        // We need to mock the getters of the TokenResponse that will be returned by bodyToMono.
        UserService.TokenResponse mockedResponse = mock(UserService.TokenResponse.class);
        when(mockedResponse.getAccessToken()).thenReturn("new-access-token");
        when(mockedResponse.getRefreshToken()).thenReturn("new-refresh-token"); // Simulating rotation
        when(mockedResponse.getExpiresIn()).thenReturn(3600L);

        when(responseSpecMock.bodyToMono(eq(UserService.TokenResponse.class))).thenReturn(Mono.just(mockedResponse));
        // Mock onStatus to prevent it from triggering for success cases
        when(responseSpecMock.onStatus(any(), any())).thenReturn(responseSpecMock);


        UserService.TokenResponse actualResponse = userService.refreshAccessToken("valid-refresh-token");

        assertNotNull(actualResponse);
        assertEquals("new-access-token", actualResponse.getAccessToken());
        assertEquals("new-refresh-token", actualResponse.getRefreshToken());
        assertEquals(3600L, actualResponse.getExpiresIn());

        verify(requestBodySpecMock).bodyValue(argThat(map ->
            map.containsKey("grant_type") && map.get("grant_type").equals("refresh_token") &&
            map.containsKey("refresh_token") && map.get("refresh_token").equals("valid-refresh-token") &&
            map.containsKey("client_id") && map.containsKey("client_secret")
        ));
    }

    @Test
    void refreshAccessToken_keycloakReturnsError_throwsRuntimeException() {
        setupWebClientMocks();

        // Simulate Keycloak returning a 400 Bad Request
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{"error":"invalid_grant","error_description":"Invalid refresh token"}")
            .build();

        // When onStatus is called, make it invoke the error handler function
        when(responseSpecMock.onStatus(any(), any())).thenAnswer(invocation -> {
            java.util.function.Predicate<HttpStatus> predicate = invocation.getArgument(0);
            if (predicate.test(HttpStatus.BAD_REQUEST)) {
                java.util.function.Function<ClientResponse, Mono<Throwable>> errorHandler = invocation.getArgument(1);
                // Directly invoke the error handler with our mock ClientResponse
                // This requires the errorHandler to produce a Mono<Throwable>
                 return responseSpecMock; // Return self to chain, then bodyToMono will be called
            }
            return responseSpecMock;
        });
        // This part is tricky to mock perfectly to trigger the exact onStatus path.
        // A simpler way for unit testing the service logic *around* WebClient:
        // Assume WebClient throws WebClientResponseException directly if onStatus is not perfectly mocked
        // or if bodyToMono itself fails after onStatus.

        // Let's simplify by directly mocking bodyToMono to throw an error,
        // assuming onStatus didn't convert it to our desired RuntimeException.
        // This tests the service's handling of WebClient failures more broadly.
        WebClientResponseException mockException = WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            null,
            "{"error":"invalid_grant"}".getBytes(),
            null
        );
        when(responseSpecMock.bodyToMono(eq(UserService.TokenResponse.class))).thenReturn(Mono.error(mockException));


        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.refreshAccessToken("invalid-refresh-token");
        });
        // The internal onStatus handler should catch this and wrap it, or if not, WebClientResponseException itself might propagate
        // Based on the current implementation, the onStatus converts it.
        assertTrue(exception.getMessage().contains("Failed to refresh token") || exception.getMessage().contains("Error during token refresh WebClient call"));
    }

    @Test
    void refreshAccessToken_nullToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.refreshAccessToken(null);
        });
    }

    @Test
    void refreshAccessToken_emptyToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.refreshAccessToken("");
        });
    }

}
