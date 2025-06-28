package com.example.Userservice.controller; // Renamed

import com.example.Userservice.model.User; // Renamed import
import com.example.Userservice.service.UserProfileService; // Renamed import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Define DTOs in a separate package e.g., com.example.Userservice.dto
// For now, keeping inline for brevity during this refactoring step.
class UserProfileUpdateRequestDto {
    public String firstName;
    public String lastName;
    public String avatarUrl;
    // Add other updatable fields, e.g., bio
}

class FollowResponseDto {
    public String message;
    public FollowResponseDto(String message) { this.message = message; }
}


@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserProfileService userProfileService;

    @Autowired
    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        logger.info("Fetching profile for current user: {}", userId);
        User user = userProfileService.getUserById(userId);
        if (user == null) {
            logger.warn("User profile not found for authenticated user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found.");
        }
        return ResponseEntity.ok(user); // Consider a UserProfileResponseDTO
    }

    @PutMapping("/me")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCurrentUserProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody UserProfileUpdateRequestDto updateRequest) {
        String userId = jwt.getSubject();
        logger.info("Updating profile for current user: {}", userId);
        try {
            User updatedUser = userProfileService.updateUserProfile(userId, updateRequest.firstName, updateRequest.lastName, updateRequest.avatarUrl);
            return ResponseEntity.ok(updatedUser); // Consider a UserProfileResponseDTO
        } catch (RuntimeException e) {
            logger.error("Error updating profile for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // Assuming RuntimeException for not found
        }
    }

    @GetMapping("/username/{username}")
//    @PreAuthorize("isAuthenticated()") // Or permitAll if public lookup is allowed
    public ResponseEntity<?> getUserProfileByUsername(@PathVariable String username) {
        logger.info("Fetching profile for username: {}", username);
        User user = userProfileService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found for username: " + username);
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
//    @PreAuthorize("isAuthenticated()") // Or permitAll if public lookup is allowed
    public ResponseEntity<?> getUserProfileByEmail(@PathVariable String email) {
        // Ensure email path variable is properly encoded/decoded if it contains special characters
        logger.info("Fetching profile for email: {}", email);
        User user = userProfileService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found for email: " + email);
        }
        return ResponseEntity.ok(user);
    }

    // Follow APIs
    @PostMapping("/{targetUserId}/follow")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> followUser(@AuthenticationPrincipal Jwt jwt, @PathVariable String targetUserId) {
        String currentUserId = jwt.getSubject();
        if (currentUserId.equals(targetUserId)) {
            return ResponseEntity.badRequest().body(new FollowResponseDto("Cannot follow yourself."));
        }
        try {
            userProfileService.followUser(currentUserId, targetUserId);
            return ResponseEntity.ok(new FollowResponseDto("Successfully followed user " + targetUserId));
        } catch (IllegalArgumentException e) { // Or custom exception for UserNotFound
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new FollowResponseDto(e.getMessage()));
        } catch (IllegalStateException e) { // Or custom exception for AlreadyFollowing
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new FollowResponseDto(e.getMessage()));
        }
    }

    @DeleteMapping("/{targetUserId}/follow")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unfollowUser(@AuthenticationPrincipal Jwt jwt, @PathVariable String targetUserId) {
        String currentUserId = jwt.getSubject();
        try {
            userProfileService.unfollowUser(currentUserId, targetUserId);
            return ResponseEntity.ok(new FollowResponseDto("Successfully unfollowed user " + targetUserId));
        } catch (IllegalArgumentException e) { // Or custom exception for UserNotFound
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new FollowResponseDto(e.getMessage()));
        } catch (IllegalStateException e) { // Or custom exception for NotFollowing
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new FollowResponseDto(e.getMessage()));
        }
    }

    @GetMapping("/{userId}/followers")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFollowers(@PathVariable String userId) {
        // Add pagination here in a real app
        return ResponseEntity.ok(userProfileService.getFollowers(userId));
    }

    @GetMapping("/{userId}/following")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFollowing(@PathVariable String userId) {
        // Add pagination here in a real app
        return ResponseEntity.ok(userProfileService.getFollowing(userId));
    }

    @GetMapping("/{userId}/follow-info")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFollowInfo(@PathVariable String userId) {
        return ResponseEntity.ok(userProfileService.getFollowCounts(userId));
    }

    // Placeholder for avatar update - would typically involve multipart file upload
    @PutMapping("/me/avatar")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateAvatar(@AuthenticationPrincipal Jwt jwt /*, @RequestParam("file") MultipartFile file */) {
        String userId = jwt.getSubject();
        // String avatarUrl = fileStorageService.storeFile(file, userId); // Example
        // userProfileService.updateAvatar(userId, avatarUrl);
        logger.info("Avatar update endpoint called for user: {}. (File upload not implemented in this step)", userId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Avatar upload not fully implemented in this step.");
    }
}
