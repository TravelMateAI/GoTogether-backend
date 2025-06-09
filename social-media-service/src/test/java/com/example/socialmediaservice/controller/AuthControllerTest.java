package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.LoginRequestDTO;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class) // Test AuthController
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService; // Mock AuthService

    // login test
    @Test
    void login_success() throws Exception {
        AuthService.TokenResponse mockTokenDetails = mock(AuthService.TokenResponse.class);
        when(mockTokenDetails.getAccessToken()).thenReturn("test-access-token");
        when(mockTokenDetails.getRefreshToken()).thenReturn("test-refresh-token");
        when(mockTokenDetails.getExpiresIn()).thenReturn(3600L);

        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setUsername("testuser");
        mockUser.setFirstName("Test");
        mockUser.setAvatarUrl("http://example.com/avatar.jpg");

        when(authService.loginWithPassword(anyString(), anyString())).thenReturn(mockTokenDetails);
        when(authService.getUserByEmailFromToken("test-access-token")).thenReturn(mockUser);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(Map.of("username", "testuser", "password", "password"));

        mockMvc.perform(post("/api/auth/login") // Updated path
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().value("access_token", "test-access-token"))
                .andExpect(cookie().maxAge("access_token", 3600))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().path("access_token", "/"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().value("refresh_token", "test-refresh-token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/auth/refresh"))
                .andExpect(cookie().exists("user"))
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user").exists());
    }

    // handleGoogleCallback test
    @Test
    void handleGoogleCallback_shouldProcessLoginAndSetCookies() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@example.com");
        attributes.put("given_name", "Test");
        attributes.put("family_name", "User");
        attributes.put("picture", "http://example.com/pic.jpg");
        attributes.put("sub", "test-sub-123");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes, "sub");

        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("Test");
        mockUser.setAvatarUrl("http://example.com/pic.jpg");

        Map<String, Object> authProcessingResponse = new HashMap<>();
        authProcessingResponse.put("token", "mock-oauth-token");
        authProcessingResponse.put("user", mockUser);

        when(authService.processOAuth2Login(any(OAuth2User.class))).thenReturn(authProcessingResponse);

        mockMvc.perform(get("/api/auth/login/oauth2/code/google") // Updated path
                        .with(oauth2Login().oauth2User(oauth2User)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().value("access_token", "mock-oauth-token"))
                // .andExpect(cookie().maxAge("access_token", 3600)) // Max age was fixed in controller for this path
                .andExpect(cookie().exists("user"))
                .andExpect(jsonPath("$.accessToken").value("mock-oauth-token"))
                .andExpect(jsonPath("$.user").exists());
    }

    // refreshToken tests
    @Test
    void refreshToken_success() throws Exception {
        AuthService.TokenResponse mockTokenResponse = mock(AuthService.TokenResponse.class);
        when(mockTokenResponse.getAccessToken()).thenReturn("new-test-access-token");
        when(mockTokenResponse.getExpiresIn()).thenReturn(3600L);
        when(mockTokenResponse.getRefreshToken()).thenReturn(null);

        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setEmail("test@example.com");

        when(authService.refreshAccessToken("test-refresh-token")).thenReturn(mockTokenResponse);
        when(authService.getUserByEmailFromToken("new-test-access-token")).thenReturn(mockUser);

        mockMvc.perform(post("/api/auth/refresh") // Updated path
                        .cookie(new Cookie("refresh_token", "test-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().value("access_token", "new-test-access-token"))
                .andExpect(jsonPath("$.accessToken").value("new-test-access-token"));
    }

    @Test
    void refreshToken_success_withRotation() throws Exception {
        AuthService.TokenResponse mockTokenResponse = mock(AuthService.TokenResponse.class);
        when(mockTokenResponse.getAccessToken()).thenReturn("new-access-token-rotated");
        when(mockTokenResponse.getExpiresIn()).thenReturn(1800L);
        when(mockTokenResponse.getRefreshToken()).thenReturn("new-rotated-refresh-token");

        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID().toString());
        mockUser.setEmail("test@example.com");

        when(authService.refreshAccessToken("old-refresh-token")).thenReturn(mockTokenResponse);
        when(authService.getUserByEmailFromToken("new-access-token-rotated")).thenReturn(mockUser);

        mockMvc.perform(post("/api/auth/refresh") // Updated path
                        .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().value("access_token", "new-access-token-rotated"))
                .andExpect(cookie().maxAge("access_token", 1800))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().value("refresh_token", "new-rotated-refresh-token"))
                .andExpect(cookie().path("refresh_token", "/api/auth/refresh"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token-rotated"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void refreshToken_missingCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")) // Updated path
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing refresh token"));
    }

    @Test
    void refreshToken_serviceThrowsException() throws Exception {
        when(authService.refreshAccessToken("test-refresh-token"))
                .thenThrow(new RuntimeException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/auth/refresh") // Updated path
                        .cookie(new Cookie("refresh_token", "test-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"));
    }
}
