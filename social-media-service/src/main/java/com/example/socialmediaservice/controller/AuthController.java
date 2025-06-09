package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.service.AuthService;
// import com.example.socialmediaservice.service.UserService; // Only needed if serializeUser were to call userService directly for some reason.
import com.example.socialmediaservice.dto.LoginRequestDTO;
import com.example.socialmediaservice.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*; // For CookieValue

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    // serializeUser method copied from UserController
    public String serializeUser(User user) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> minimalUser = new HashMap<>();
            minimalUser.put("userId", user.getUserId());
            minimalUser.put("username", user.getUsername());
            minimalUser.put("firstName", user.getFirstName());
            minimalUser.put("avatarUrl", user.getAvatarUrl());

            String json = objectMapper.writeValueAsString(minimalUser);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user for cookie", e);
            throw new RuntimeException("Failed to serialize user for cookie", e);
        }
    }

    @PostMapping("/login") // Was /api/users/auth/login
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        AuthService.TokenResponse tokenDetails = authService.loginWithPassword(loginRequest.getUsername(), loginRequest.getPassword());
        String accessToken = tokenDetails.getAccessToken();
        String refreshTokenValue = tokenDetails.getRefreshToken(); // Renamed to avoid conflict with method param name
        long expiresIn = tokenDetails.getExpiresIn();

        User user = authService.getUserByEmailFromToken(accessToken);
        if (user != null) {
            user.setPosts(null);
        } else {
            // Handle case where user might be null after token validation, though unlikely if token is valid
            log.error("User not found for a valid access token: {}", accessToken);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "User details not found after login."));
        }


        String accessTokenCookieHeader = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(expiresIn)
                .build().toString() + "; SameSite=Lax";
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookieHeader);

        if (refreshTokenValue != null && !refreshTokenValue.isEmpty()) {
            long refreshTokenMaxAge = 2592000; // 30 days
            String refreshTokenCookieHeader = ResponseCookie.from("refresh_token", refreshTokenValue)
                    .httpOnly(true)
                    .secure(false)
                    .path("/api/auth/refresh") // Path for refresh token cookie, specific to auth controller
                    .maxAge(refreshTokenMaxAge)
                    .build().toString() + "; SameSite=Lax";
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieHeader);
            log.info("Refresh token cookie set for user {}.", user.getUsername());
        } else {
            log.warn("Refresh token was null or empty for user {}. Refresh token cookie not set.", user.getUsername());
        }

        String userCookieHeader = ResponseCookie.from("user", serializeUser(user))
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(expiresIn)
                .build().toString() + "; SameSite=Lax";
        response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);

        log.info("User {} login successful", user.getUsername());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "expiresIn", expiresIn,
                "user", serializeUser(user)
        ));
    }

    @GetMapping("/login/oauth2/code/google") // Was /api/users/login/oauth2/code/google
    public ResponseEntity<?> handleGoogleCallback(@AuthenticationPrincipal OAuth2User principal, HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: No principal found.");
        }

        Map<String, Object> authProcessingResponse = authService.processOAuth2Login(principal);
        String token = (String) authProcessingResponse.get("token"); // This is the placeholder token from processOAuth2Login
        User user = (User) authProcessingResponse.get("user");

        if (user == null) {
            log.error("User object was null after OAuth2 processing for principal: {}", principal.getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "User processing failed after OAuth2 login."));
        }
        user.setPosts(null);


        // TODO: The token from processOAuth2Login is a placeholder.
        // In a real OAuth2 setup, this token would be the actual Keycloak session token or ID token.
        // For now, using a fixed maxAge as the placeholder token has no real expiry.
        long oauthTokenMaxAge = 3600; // Example: 1 hour for OAuth2 login session token

        String accessTokenCookieHeader = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(oauthTokenMaxAge) // Placeholder expiry
                .build().toString() + "; SameSite=Lax";
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookieHeader);

        String userCookieHeader = ResponseCookie.from("user", serializeUser(user))
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(oauthTokenMaxAge) // Placeholder expiry
                .build().toString() + "; SameSite=Lax";
        response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);

        log.info("OAuth2 callback for user {} successful. Placeholder token issued.", user.getEmail());
        return ResponseEntity.ok(Map.of(
                "accessToken", token, // Placeholder token
                "user", serializeUser(user)
        ));
    }

    @PostMapping("/refresh") // Was /api/users/auth/refresh
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshTokenValue, HttpServletResponse response) {
        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing refresh token"));
        }

        try {
            AuthService.TokenResponse tokenDetails = authService.refreshAccessToken(refreshTokenValue);
            String newAccessToken = tokenDetails.getAccessToken();
            long newExpiresIn = tokenDetails.getExpiresIn();
            String newRefreshToken = tokenDetails.getRefreshToken();

            String newAccessTokenCookieHeader = ResponseCookie.from("access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(newExpiresIn)
                    .build().toString() + "; SameSite=Lax";
            response.addHeader(HttpHeaders.SET_COOKIE, newAccessTokenCookieHeader);

            if (newRefreshToken != null && !newRefreshToken.isEmpty() && !newRefreshToken.equals(refreshTokenValue)) {
                long refreshTokenMaxAge = 2592000; // 30 days
                String newRefreshTokenCookieHeader = ResponseCookie.from("refresh_token", newRefreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .path("/api/auth/refresh") // Consistent path
                        .maxAge(refreshTokenMaxAge)
                        .build().toString() + "; SameSite=Lax";
                response.addHeader(HttpHeaders.SET_COOKIE, newRefreshTokenCookieHeader);
                log.info("Refresh token was rotated. New refresh_token cookie set.");
            }

            User user = authService.getUserByEmailFromToken(newAccessToken);
            if (user != null) {
                user.setPosts(null);
                 String userCookieHeader = ResponseCookie.from("user", serializeUser(user))
                    .httpOnly(false)
                    .secure(false)
                    .path("/")
                    .maxAge(newExpiresIn)
                    .build().toString() + "; SameSite=Lax";
                response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);
            } else {
                log.warn("User could not be fetched with new access token during refresh. User cookie not updated.");
            }

            log.info("Access token refreshed successfully.");
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", newExpiresIn
            ));

        } catch (RuntimeException e) {
            log.error("Error refreshing token: {}", e.getMessage(), e); // Added exception to log
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token", "detail", e.getMessage()));
        }
    }
}
