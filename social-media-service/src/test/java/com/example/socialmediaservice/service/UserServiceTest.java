package com.example.socialmediaservice.service;

import com.example.socialmediaservice.dto.UpdateProfileRequest;
import com.example.socialmediaservice.dto.UpdateProfileResponse;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
// Removed HttpHeaders, HttpStatus, ClientResponse, WebClientResponseException, Mono, OAuth2User
// unless specific remaining UserService methods need them. For now, assuming only getAdminAccessToken needs some WebClient parts.

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
    private WebClient.Builder webClientBuilder;

    // Mocks for WebClient calls made by getAdminAccessToken and createUserInKeycloak
    @Mock
    private WebClient webClientMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;
    @Mock
    private WebClient.ResponseSpec responseSpecMock;


    @InjectMocks
    private UserService userService;

    // mockOAuth2User and userAttributes are removed as processGoogleLogin tests are moved.

    @BeforeEach
    void setUp() {
        // Setup for WebClient mocks if getAdminAccessToken or createUserInKeycloak are tested
        when(webClientBuilder.build()).thenReturn(webClientMock);
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock); // Common for POST
        when(requestBodySpecMock.contentType(any(MediaType.class))).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock); // For POST calls
        when(requestHeadersSpecMock.header(anyString(), anyString())).thenReturn(requestHeadersSpecMock); // For Authorization header in createUserInKeycloak
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
    }

    // Tests for processGoogleLogin, refreshAccessToken, authenticateWithKeycloak, getUserByEmailFromToken are MOVED to AuthServiceTest.

    // Example: Test for a method remaining in UserService, e.g., registerUser
    @Test
    void registerUser_success() {
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "password";
        String firstName = "New";
        String lastName = "User";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mocking for getAdminAccessToken() call within createUserInKeycloak()
        // This needs to return a mock AdminTokenResponse
        UserService.AdminTokenResponse adminTokenResponse = mock(UserService.AdminTokenResponse.class);
        when(adminTokenResponse.getAccess_token()).thenReturn("mock-admin-token");
        when(responseSpecMock.bodyToMono(eq(UserService.AdminTokenResponse.class))).thenReturn(Mono.just(adminTokenResponse));

        // Mocking for the createUserInKeycloak call itself (toBodilessEntity part)
        WebClient.ResponseSpec finalResponseSpec = mock(WebClient.ResponseSpec.class); // a new one for the second call
        when(requestHeadersSpecMock.retrieve()).thenReturn(finalResponseSpec); // retrieve for the second call
        when(finalResponseSpec.toBodilessEntity()).thenReturn(Mono.empty()); // Assuming success
        // This setup might need more refinement based on exact WebClient chaining in createUserInKeycloak

        User registeredUser = userService.registerUser(username, email, password, firstName, lastName);

        assertNotNull(registeredUser);
        assertEquals(username, registeredUser.getUsername());
        assertEquals(email, registeredUser.getEmail());

        verify(userRepository).save(any(User.class));
        // verify(webClientMock, times(2)).post(); // One for admin token, one for user creation
    }

    @Test
    void registerUser_usernameExists_throwsRuntimeException() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser("existinguser", "email", "pass", "first", "last");
        });
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void updateUserProfile_userExists_updatesProfile() {
        String userId = UUID.randomUUID().toString();
        User existingUser = new User();
        existingUser.setUserId(userId);
        existingUser.setUsername("testuser");
        existingUser.setFirstName("OldFirstName");
        existingUser.setBio("OldBio");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("NewDisplayName");
        request.setBio("NewBio");
        request.setAvatarUrl("http://example.com/newavatar.jpg");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileResponse response = userService.updateUserProfile(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("NewDisplayName", response.getFirstName());
        assertEquals("NewBio", response.getBio());
        assertEquals("http://example.com/newavatar.jpg", response.getAvatarUrl());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("NewDisplayName", userCaptor.getValue().getFirstName());
    }

    @Test
    void updateUserProfile_userNotFound_throwsEntityNotFoundException() {
        String userId = "nonexistentuser";
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("AnyName");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            userService.updateUserProfile(userId, request);
        });
    }


    // WebClient related mocks setupWebClientMocks and specific tests for getAdminAccessToken
    // and createUserInKeycloak might need more detailed separate tests if their logic is complex,
    // or be covered implicitly by tests like registerUser.
    // The current setupWebClientMocks in this file is a general one.
}
