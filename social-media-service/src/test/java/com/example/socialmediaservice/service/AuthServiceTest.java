package com.example.socialmediaservice.service;

import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepo userRepository;
    @Mock
    private UserService userService; // For testing calls from AuthService to UserService (e.g. in getUserByEmailFromToken)
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClientMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock; // For POST requests with .bodyValue()
    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    @InjectMocks
    private AuthService authService;

    private OAuth2User mockOAuth2User;
    private Map<String, Object> userAttributes;

    @BeforeEach
    void setUp() {
        // Common setup for WebClient mocks
        when(webClientBuilder.build()).thenReturn(webClientMock);

        // Setup for POST chain
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock); // Changed from requestHeadersSpecMock to requestBodySpecMock
        when(requestBodySpecMock.contentType(any(MediaType.class))).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock); // This now correctly returns RequestHeadersSpec for POST

        // Setup for GET chain (used by getUserByEmailFromToken)
        when(webClientMock.get()).thenReturn(requestHeadersSpecMock); // GET starts with RequestHeadersUriSpec, simplified to RequestHeadersSpec after uri()
        when(requestHeadersSpecMock.uri(anyString())).thenReturn(requestHeadersSpecMock); // GET .uri() leads to RequestHeadersSpec
        when(requestHeadersSpecMock.header(anyString(), anyString())).thenReturn(requestHeadersSpecMock); // For Authorization header

        // Common retrieve chain
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.onStatus(any(), any())).thenReturn(responseSpecMock); // Default: do not trigger error handler

        // OAuth2User mock setup (for processOAuth2Login)
        userAttributes = new HashMap<>();
        userAttributes.put("email", "testuser@example.com");
        userAttributes.put("given_name", "Test");
        userAttributes.put("family_name", "User");
        userAttributes.put("picture", "http://example.com/avatar.jpg");
        mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute(anyString())).thenAnswer(invocation -> userAttributes.get(invocation.getArgument(0)));
        when(mockOAuth2User.getAttributes()).thenReturn(userAttributes);
    }

    // Test for loginWithPassword (adapted from authenticateWithKeycloak)
    @Test
    void loginWithPassword_success() {
        AuthService.TokenResponse mockedResponse = mock(AuthService.TokenResponse.class);
        when(mockedResponse.getAccessToken()).thenReturn("access-token");
        when(mockedResponse.getRefreshToken()).thenReturn("refresh-token");
        when(mockedResponse.getExpiresIn()).thenReturn(3600L);
        when(responseSpecMock.bodyToMono(eq(AuthService.TokenResponse.class))).thenReturn(Mono.just(mockedResponse));

        AuthService.TokenResponse actual = authService.loginWithPassword("user", "pass");
        assertEquals("access-token", actual.getAccessToken());
        assertEquals("refresh-token", actual.getRefreshToken());
        verify(requestBodySpecMock).bodyValue(argThat(map -> map.get("grant_type").equals("password")));
    }

    // Tests for refreshAccessToken (adapted from UserServiceTest)
    @Test
    void refreshAccessToken_success() {
        AuthService.TokenResponse mockedResponse = mock(AuthService.TokenResponse.class);
        when(mockedResponse.getAccessToken()).thenReturn("new-access-token");
        when(mockedResponse.getRefreshToken()).thenReturn("new-refresh-token"); // Simulating rotation
        when(mockedResponse.getExpiresIn()).thenReturn(3600L);
        when(responseSpecMock.bodyToMono(eq(AuthService.TokenResponse.class))).thenReturn(Mono.just(mockedResponse));

        AuthService.TokenResponse actualResponse = authService.refreshAccessToken("valid-refresh-token");
        assertEquals("new-access-token", actualResponse.getAccessToken());
        assertEquals("new-refresh-token", actualResponse.getRefreshToken());
        verify(requestBodySpecMock).bodyValue(argThat(map -> map.get("grant_type").equals("refresh_token")));
    }

    @Test
    void refreshAccessToken_keycloakReturnsError_throwsRuntimeException() {
        WebClientResponseException mockException = WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", null, "{\"error\":\"invalid_grant\"}".getBytes(), null);

        // Ensure onStatus invokes the error handler
        when(responseSpecMock.onStatus(any(), any())).thenAnswer(invocation -> {
            java.util.function.Predicate<HttpStatus> predicate = invocation.getArgument(0);
            if (predicate.test(HttpStatus.BAD_REQUEST)) { // Assuming the predicate will match this
                java.util.function.Function<ClientResponse, Mono<Throwable>> errorHandler = invocation.getArgument(1);
                // Simulate ClientResponse for the error handler
                ClientResponse mockClientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"invalid_grant\"}").build();
                // The error handler should return Mono.error(...)
                // We want bodyToMono to eventually throw the exception produced by onStatus
                when(responseSpecMock.bodyToMono(eq(AuthService.TokenResponse.class)))
                    .thenReturn(errorHandler.apply(mockClientResponse).cast(AuthService.TokenResponse.class)); // This is a bit of a hack to make bodyToMono throw
            }
            return responseSpecMock; // Return self to allow chaining
        });
         // We expect the exception to be the one created by our onStatus handler
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshAccessToken("invalid-refresh-token");
        });
        assertTrue(exception.getMessage().contains("Failed to refresh token"));
    }

    @Test
    void refreshAccessToken_nullToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(null));
    }

    @Test
    void refreshAccessToken_emptyToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(""));
    }

    // Tests for processOAuth2Login (adapted from UserServiceTest)
    @Test
    void processOAuth2Login_whenUserDoesNotExist_createsNewUser() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authService.processOAuth2Login(mockOAuth2User);
        User resultUser = (User) result.get("user");
        assertEquals("testuser@example.com", resultUser.getEmail());
        assertEquals("Test", resultUser.getFirstName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void processOAuth2Login_whenUserExists_returnsExistingUser() {
        User existingUser = new User();
        existingUser.setUserId(UUID.randomUUID().toString());
        existingUser.setEmail("testuser@example.com");
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(existingUser));

        Map<String, Object> result = authService.processOAuth2Login(mockOAuth2User);
        User resultUser = (User) result.get("user");
        assertEquals(existingUser.getUserId(), resultUser.getUserId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processOAuth2Login_whenOAuth2UserIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> authService.processOAuth2Login(null));
    }

    @Test
    void processOAuth2Login_whenEmailIsNullInAttributes_throwsRuntimeException() {
        userAttributes.remove("email");
        // Re-mock as attributes are captured during initial mock setup if not careful
        OAuth2User userWithNullEmail = mock(OAuth2User.class);
        when(userWithNullEmail.getAttribute("email")).thenReturn(null);
        // when(userWithNullEmail.getAttributes()).thenReturn(userAttributes); // if getAttributes() is used by method

        assertThrows(RuntimeException.class, () -> authService.processOAuth2Login(userWithNullEmail));
    }

    // Test for getUserByEmailFromToken
    @Test
    void getUserByEmailFromToken_success() {
        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("email", "test@example.com");

        when(responseSpecMock.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(userInfoMap));

        User mockUser = new User();
        mockUser.setEmail("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(mockUser);

        User actualUser = authService.getUserByEmailFromToken("valid-access-token");
        assertEquals("test@example.com", actualUser.getEmail());
        verify(userService).getUserByEmail("test@example.com");
        verify(requestHeadersSpecMock).header("Authorization", "Bearer valid-access-token");
    }

    @Test
    void getUserByEmailFromToken_keycloakReturnsNoEmail_throwsRuntimeException() {
        Map<String, Object> userInfoMap = new HashMap<>(); // Empty map, no email
        when(responseSpecMock.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(userInfoMap));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.getUserByEmailFromToken("token-no-email");
        });
        assertEquals("Failed to fetch user info from Keycloak", exception.getMessage());
    }
}
