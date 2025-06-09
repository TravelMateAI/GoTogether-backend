package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.FollowerInfo;
import com.example.socialmediaservice.dto.LoginRequestDTO;
import com.example.socialmediaservice.dto.UpdateProfileRequest;
import com.example.socialmediaservice.dto.UpdateProfileResponse;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        UserService.TokenResponse tokenDetails = userService.authenticateWithKeycloak(loginRequest.getUsername(), loginRequest.getPassword());
        String accessToken = tokenDetails.getAccessToken();
        String refreshToken = tokenDetails.getRefreshToken();
        long expiresIn = tokenDetails.getExpiresIn();

        User user = userService.getUserByEmailFromToken(accessToken); // use Keycloak userinfo based on access token
        user.setPosts(null);

        String accessTokenCookieHeader = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false) // IMPORTANT for localhost! Should be true in prod
                .path("/")
                .maxAge(expiresIn) // Use actual expiry from token response
                .build().toString() + "; SameSite=Lax";
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookieHeader);

        if (refreshToken != null && !refreshToken.isEmpty()) {
            // Max age for refresh token cookie should be longer, e.g., 30 days or Keycloak's offline session timeout
            // For example: 60 seconds * 60 minutes * 24 hours * 30 days = 2592000 seconds
            long refreshTokenMaxAge = 2592000; // 30 days
            String refreshTokenCookieHeader = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false) // IMPORTANT for localhost! Should be true in prod
                    .path("/api/users/auth") // More specific path for refresh token cookie
                    .maxAge(refreshTokenMaxAge)
                    .build().toString() + "; SameSite=Lax";
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieHeader);
            log.info("Refresh token cookie set.");
        } else {
            log.warn("Refresh token was null or empty. Refresh token cookie not set.");
        }

        String userCookieHeader = ResponseCookie.from("user", serializeUser(user))
                .httpOnly(false)
                .secure(false) // Same
                .path("/")
                .maxAge(expiresIn) // Align with access token expiry for simplicity, or longer
                .build().toString() + "; SameSite=Lax";
        response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);

        log.info("User {} login successful", user.getUsername());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "expiresIn", expiresIn, // Send expiresIn to the client
                "user", serializeUser(user)
        ));
    }


    @PostMapping("/register")
    public User registerUser(@RequestBody RegisterRequest request) {
        return userService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );
    }

    @PutMapping("/{userId}/avatar")
    public User updateAvatar(@PathVariable String userId, @RequestBody AvatarUpdateRequest request) {
        return userService.updateAvatar(userId, request.getAvatarUrl());
    }

//    @CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email path variable is missing");
        }
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId path variable is missing");
        }
        User user = userService.getUserById(userId); // ✅ now it's consistent
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<UpdateProfileResponse> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UpdateProfileRequest request
    ) {
        UpdateProfileResponse updated = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updated);
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    @Data
    static class AvatarUpdateRequest {
        private String avatarUrl;
    }

    @PostMapping("/{targetUserId}/followers")
    public ResponseEntity<Void> followUser(
            @PathVariable String targetUserId,
            @RequestParam String followerUserId
    ) {
        userService.followUser(followerUserId, targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetUserId}/followers")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable String targetUserId,
            @RequestParam String followerUserId
    ) {
        userService.unfollowUser(followerUserId, targetUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{targetUserId}/follower-info")
    public ResponseEntity<FollowerInfo> getFollowerInfo(
            @PathVariable String targetUserId,
            @RequestParam String currentUserId
    ) {
        FollowerInfo info = userService.getFollowerInfo(currentUserId, targetUserId);
        return ResponseEntity.ok(info);
    }
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
            throw new RuntimeException("Failed to serialize user for cookie", e);
        }
    }

    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<?> handleGoogleCallback(@AuthenticationPrincipal OAuth2User principal, HttpServletResponse response) {
        if (principal == null) {
            // Handle cases where the principal is null, perhaps redirect to an error page or return an error response
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: No principal found.");
        }
        // Extract necessary user information from the principal
        String email = principal.getAttribute("email");
        // You might want to extract other attributes like name, etc.

        // Call a new method in UserService to process the Google login
        // This method will handle user creation/linking and token generation
        Map<String, Object> authResponse = userService.processGoogleLogin(principal);
        String token = (String) authResponse.get("token");
        User user = (User) authResponse.get("user");

        // Set cookies as in the existing login method
        String accessTokenHeader = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(false) // IMPORTANT for localhost!
                .path("/")
                .maxAge(3600)
                .build().toString() + "; SameSite=Lax";

        String userCookieHeader = ResponseCookie.from("user", serializeUser(user)) // Ensure serializeUser is accessible or reimplement
                .httpOnly(false)
                .secure(false) // Same
                .path("/")
                .maxAge(3600)
                .build().toString() + "; SameSite=Lax";

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenHeader);
        response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);

        // Redirect to the frontend application, possibly with the token or user info
        // For now, just returning the auth response
        // Consider redirecting to a frontend URL: return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://localhost:3000/some-path")).build();
        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "user", serializeUser(user)
        ));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing refresh token"));
        }

        try {
            UserService.TokenResponse tokenDetails = userService.refreshAccessToken(refreshToken);
            String newAccessToken = tokenDetails.getAccessToken();
            long newExpiresIn = tokenDetails.getExpiresIn();
            // Keycloak might also return a new refresh token (if rotation is on).
            // For now, we assume the existing refresh token cookie remains valid or Keycloak doesn't rotate it aggressively.
            // If Keycloak *does* rotate refresh tokens and sends a new one in tokenDetails.getRefreshToken(),
            // we would need to update the refresh_token cookie here as well.

            String newAccessTokenCookieHeader = ResponseCookie.from("access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(false) // IMPORTANT for localhost! Should be true in prod
                    .path("/")
                    .maxAge(newExpiresIn)
                    .build().toString() + "; SameSite=Lax";
            response.addHeader(HttpHeaders.SET_COOKIE, newAccessTokenCookieHeader);

            // If a new refresh token is provided and different, update its cookie
            String newRefreshToken = tokenDetails.getRefreshToken();
            if (newRefreshToken != null && !newRefreshToken.isEmpty() && !newRefreshToken.equals(refreshToken)) {
                long refreshTokenMaxAge = 2592000; // 30 days, or align with Keycloak's policy
                String newRefreshTokenCookieHeader = ResponseCookie.from("refresh_token", newRefreshToken)
                        .httpOnly(true)
                        .secure(false) // IMPORTANT for localhost!
                        .path("/api/users/auth") // Consistent path
                        .maxAge(refreshTokenMaxAge)
                        .build().toString() + "; SameSite=Lax";
                response.addHeader(HttpHeaders.SET_COOKIE, newRefreshTokenCookieHeader);
                log.info("Refresh token was rotated. New refresh_token cookie set.");
            }


            // The user cookie also needs to be updated if its maxAge was tied to the old access token.
            // For simplicity, we can re-fetch the user or assume the client handles user info separately.
            // Let's re-serialize and set the user cookie with the new access token's expiry.
            // This requires fetching the user again, or having user details available.
            // To avoid fetching user again just for cookie, client should rely on initial user info
            // and only care about new access token from refresh.
            // However, to keep cookie maxAge consistent:
            User user = userService.getUserByEmailFromToken(newAccessToken); // Re-fetch user to ensure consistency if needed for user cookie
            if (user != null) {
                user.setPosts(null); // Avoid sending too much data
                 String userCookieHeader = ResponseCookie.from("user", serializeUser(user))
                    .httpOnly(false)
                    .secure(false) // Same
                    .path("/")
                    .maxAge(newExpiresIn) // Align with new access token expiry
                    .build().toString() + "; SameSite=Lax";
                response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);
            }


            log.info("Access token refreshed successfully.");
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", newExpiresIn
            ));

        } catch (RuntimeException e) {
            log.error("Error refreshing token: {}", e.getMessage());
            // Consider more specific error handling based on exception types
            // e.g., if refresh token is invalid/expired, Keycloak might return a specific error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token", "detail", e.getMessage()));
        }
    }
    }




