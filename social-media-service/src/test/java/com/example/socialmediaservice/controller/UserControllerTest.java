package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import jakarta.servlet.http.Cookie; // For creating Cookie object

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService; // Mock UserService

    @Test
    void handleGoogleCallback_shouldProcessLoginAndSetCookies() throws Exception {
        // 1. Mock OAuth2User details
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@example.com");
        attributes.put("given_name", "Test");
        attributes.put("family_name", "User");
        attributes.put("picture", "http://example.com/pic.jpg");
        // The "name" attribute key used by DefaultOAuth2User for principal's name
        // It should be one of the attributes, typically 'sub' or 'email' if not specified otherwise
        attributes.put("sub", "test-sub-123");


        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"); // "sub" or "email" or any attribute Keycloak uses as the principal name identifier

        // 2. Mock UserService response
        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("Test");
        mockUser.setAvatarUrl("http://example.com/pic.jpg");

        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("token", "mock-jwt-token");
        authResponse.put("user", mockUser);

        when(userService.processGoogleLogin(any(OAuth2User.class))).thenReturn(authResponse);

        // 3. Perform GET request with oauth2Login()
        mockMvc.perform(get("/api/users/login/oauth2/code/google")
                        .with(oauth2Login().oauth2User(oauth2User)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().secure("access_token", false)) // As per controller
                .andExpect(cookie().path("access_token", "/"))
                .andExpect(cookie().value("access_token", "mock-jwt-token"))
                .andExpect(cookie().exists("user"))
                .andExpect(cookie().httpOnly("user", false)) // As per controller
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.user").exists()); // Further checks on user JSON can be added
    }

    @Test
    void refreshToken_success() throws Exception {
        UserService.TokenResponse mockTokenResponse = mock(UserService.TokenResponse.class);
        when(mockTokenResponse.getAccessToken()).thenReturn("new-test-access-token");
        when(mockTokenResponse.getExpiresIn()).thenReturn(3600L);
        when(mockTokenResponse.getRefreshToken()).thenReturn(null); // Assume no rotation for this test

        User mockUser = new User(); // For user cookie recreation
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setEmail("test@example.com");


        when(userService.refreshAccessToken("test-refresh-token")).thenReturn(mockTokenResponse);
        when(userService.getUserByEmailFromToken("new-test-access-token")).thenReturn(mockUser);


        mockMvc.perform(post("/api/users/auth/refresh")
                        .cookie(new Cookie("refresh_token", "test-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().value("access_token", "new-test-access-token"))
                .andExpect(cookie().maxAge("access_token", 3600))
                .andExpect(cookie().path("access_token", "/"))
                .andExpect(cookie().exists("user")) // Check user cookie is updated
                .andExpect(jsonPath("$.accessToken").value("new-test-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void refreshToken_success_withRotation() throws Exception {
        UserService.TokenResponse mockTokenResponse = mock(UserService.TokenResponse.class);
        when(mockTokenResponse.getAccessToken()).thenReturn("new-access-token-rotated");
        when(mockTokenResponse.getExpiresIn()).thenReturn(1800L);
        when(mockTokenResponse.getRefreshToken()).thenReturn("new-rotated-refresh-token"); // Rotated token

        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setEmail("test@example.com");

        when(userService.refreshAccessToken("old-refresh-token")).thenReturn(mockTokenResponse);
        when(userService.getUserByEmailFromToken("new-access-token-rotated")).thenReturn(mockUser);

        mockMvc.perform(post("/api/users/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().value("access_token", "new-access-token-rotated"))
                .andExpect(cookie().maxAge("access_token", 1800))
                .andExpect(cookie().exists("refresh_token")) // Check new refresh token cookie
                .andExpect(cookie().value("refresh_token", "new-rotated-refresh-token"))
                .andExpect(cookie().path("refresh_token", "/api/users/auth"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token-rotated"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void refreshToken_missingCookie() throws Exception {
        mockMvc.perform(post("/api/users/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing refresh token"));
    }

    @Test
    void refreshToken_serviceThrowsException() throws Exception {
        when(userService.refreshAccessToken("test-refresh-token"))
                .thenThrow(new RuntimeException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/users/auth/refresh")
                        .cookie(new Cookie("refresh_token", "test-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"));
    }

}
