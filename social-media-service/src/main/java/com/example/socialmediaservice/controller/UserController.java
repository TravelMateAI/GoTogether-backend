package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.FollowerInfo;
import com.example.socialmediaservice.dto.UpdateProfileRequest;
import com.example.socialmediaservice.dto.UpdateProfileResponse;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // <-- Allow your frontend to call backend
public class UserController {

    private final UserService userService;

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
