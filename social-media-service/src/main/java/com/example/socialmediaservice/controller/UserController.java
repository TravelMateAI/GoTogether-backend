package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.FollowerInfo;
// import com.example.socialmediaservice.dto.LoginRequestDTO; // Removed
import com.example.socialmediaservice.dto.UpdateProfileRequest;
import com.example.socialmediaservice.dto.UpdateProfileResponse;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.service.UserService;
// import com.fasterxml.jackson.core.JsonProcessingException; // Removed
// import com.fasterxml.jackson.databind.ObjectMapper; // Removed
// import jakarta.servlet.http.HttpServletResponse; // Removed (unless other endpoints need it)
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpHeaders; // Removed
// import org.springframework.http.HttpStatus; // Removed
// import org.springframework.http.ResponseCookie; // Removed
import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal; // Removed
// import org.springframework.security.oauth2.core.user.OAuth2User; // Removed
// import org.springframework.web.bind.annotation.CookieValue; // Removed
import org.springframework.web.bind.annotation.*;

// import java.nio.charset.StandardCharsets; // Removed
// import java.util.Base64; // Removed
// import java.util.HashMap; // Removed
// import java.util.Map; // Removed

@Slf4j
@RestController
@RequestMapping("/api/users") // This base path remains for user-profile related endpoints
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    private final UserService userService;

    // Login, handleGoogleCallback, and refreshToken methods have been moved to AuthController.
    // serializeUser method has been moved to AuthController.

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
        User user = userService.getUserById(userId);
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
}
