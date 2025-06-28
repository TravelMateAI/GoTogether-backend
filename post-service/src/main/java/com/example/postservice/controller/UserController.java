package com.example.postservice.controller;

import com.example.postservice.dto.FollowerInfo;
import com.example.postservice.dto.LoginRequestDTO;
import com.example.postservice.dto.UpdateProfileRequest;
import com.example.postservice.dto.UpdateProfileResponse;
import com.example.postservice.entity.User;
import com.example.postservice.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins ={"http://localhost:3000", "https://go-together-uom.vercel.app"}, allowCredentials = "true")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        String token = userService.authenticateWithKeycloak(loginRequest.getUsername(), loginRequest.getPassword());
        User user = userService.getUserByEmailFromToken(token); // use Keycloak userinfo
        user.setPosts(null);
//         Set SameSite manually because Spring doesn't support it directly in ResponseCookie
        String accessTokenHeader = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(false) // <--- IMPORTANT for localhost!
                .path("/")
                .maxAge(3600)
                .build().toString() + "; SameSite=Lax";

        String userCookieHeader = ResponseCookie.from("user", serializeUser(user))
                .httpOnly(false)
                .secure(false) // <--- Same
                .path("/")
                .maxAge(3600)
                .build().toString() + "; SameSite=Lax";

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenHeader);
        response.addHeader(HttpHeaders.SET_COOKIE, userCookieHeader);

        log.info("User {} login successful", user.getUsername());
        log.info( serializeUser(user));

        return ResponseEntity.ok(Map.of(
                "accessToken", token,
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

    }




