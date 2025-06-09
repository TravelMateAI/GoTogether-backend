package com.example.socialmediaservice.service;

import com.example.socialmediaservice.dto.FollowerInfo;
import com.example.socialmediaservice.dto.UpdateProfileRequest;
import com.example.socialmediaservice.dto.UpdateProfileResponse;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.core.ParameterizedTypeReference; // Will be removed if not used by remaining methods
import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap; // Will be removed
// import org.springframework.util.MultiValueMap; // Will be removed
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType; // Will be removed if not used by remaining methods (createUserInKeycloak uses it)
import org.springframework.http.HttpHeaders; // Will be removed if not used by remaining methods (createUserInKeycloak uses it)
// import org.springframework.security.oauth2.core.user.OAuth2User; // Will be removed
// import reactor.core.publisher.Mono; // Will be removed

// import java.util.HashMap; // Will be removed
// import java.util.Map; // Will be removed
// import java.util.UUID; // Will be removed

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final WebClient.Builder webClientBuilder; // Still needed for Keycloak admin operations

    // These fields remain as they are used by Keycloak user creation logic
    private final String keycloakUrl = "http://localhost:8081";
    private final String adminUsername = "admin";
    private final String adminPassword = "admin";


    @Transactional
    public User registerUser(String username, String email, String password,String firstName,String lastName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);

        userRepository.save(user);

        createUserInKeycloak(username, email, password,firstName,lastName);

        return user;
    }

    // This method remains as it's part of user registration, not direct auth flow for existing users
    private void createUserInKeycloak(String username, String email, String password, String firstName, String lastName) {
        String adminToken = getAdminAccessToken(); // This still needs TokenResponse temporarily or refactor getAdminAccessToken

        WebClient webClient = webClientBuilder.build();

        // Temporarily define a minimal TokenResponse here if getAdminAccessToken isn't refactored yet,
        // or ensure getAdminAccessToken is refactored to not depend on the shared TokenResponse if it was complex.
        // For now, assuming getAdminAccessToken is simple or will be adapted.
        // The original TokenResponse in UserService was used by getAdminAccessToken.
        // We need a local, minimal DTO for getAdminAccessToken's WebClient call if that call expects a body matching the old TokenResponse.
        // Or, ideally, getAdminAccessToken is refactored to use a more specific DTO or just map fields.

        var response = webClient.post()
                .uri(keycloakUrl + "/admin/realms/kong/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON) // MediaType is used here
                .bodyValue(new KeycloakUserRequest(username, email, password, firstName, lastName))
                .retrieve()
                .toBodilessEntity()
                .block();

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create user in Keycloak");
        }
    }

    // Minimal DTO for getAdminAccessToken if it relies on the structure previously in UserService.TokenResponse
    // This is a temporary measure if getAdminAccessToken isn't fully refactored in this step.
    @lombok.Data
    private static class AdminTokenResponse {
        private String access_token;
        // No other fields needed if only access_token is used from admin token response
    }


    // This method remains, but its call to bodyToMono(TokenResponse.class) needs to change
    // if TokenResponse was the one moved. It should use AdminTokenResponse or similar.
    private String getAdminAccessToken() {
        WebClient webClient = webClientBuilder.build();

        AdminTokenResponse tokenResponse = webClient.post() // Changed to AdminTokenResponse
                .uri(keycloakUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) // MediaType is used here
                .bodyValue("grant_type=password&client_id=admin-cli&username=" + adminUsername + "&password=" + adminPassword)
                .retrieve()
                .bodyToMono(AdminTokenResponse.class) // Changed to AdminTokenResponse
                .block();

        if (tokenResponse == null || tokenResponse.getAccess_token() == null) { // Adjusted to getter/field of AdminTokenResponse
            throw new RuntimeException("Failed to authenticate with Keycloak for admin token");
        }

        return tokenResponse.getAccess_token(); // Adjusted
    }


    public User updateAvatar(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    // This DTO remains as it's used by createUserInKeycloak
    @lombok.Data
    static class KeycloakUserRequest {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private boolean enabled = true;
        private boolean emailVerified = true;
        private Credential[] credentials;

        public KeycloakUserRequest(String username, String email, String password,String firstName,String lastName) {
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.credentials = new Credential[] { new Credential(password) };
        }

        @lombok.Data
        static class Credential {
            private String type = "password";
            private String value;
            private boolean temporary = false;

            public Credential(String value) {
                this.value = value;
            }
        }
    }

    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
        user.setPosts(null); // Ensure lazy loaded posts are not fetched if not needed
        return user;
    }

    public User getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        user.setPosts(null);
        return user;
    }


    public UpdateProfileResponse updateUserProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.getDisplayName() != null) {
            user.setFirstName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User saved = userRepository.save(user);
        return new UpdateProfileResponse(
                saved.getUserId(),
                saved.getUsername(),
                saved.getFirstName(),
                saved.getAvatarUrl(),
                saved.getBio()
        );
    }


    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public void followUser(String followerId, String targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        User follower = userRepository.findByUserId(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean added = follower.getFollowingIds().add(targetUserId);
        target.getFollowerIds().add(followerId);

        log.info("Added: {} | Target: {}", added, targetUserId);

        if (added) {
            userRepository.save(follower);
            userRepository.save(target);
        }
    }

    @Transactional
    public void unfollowUser(String followerId, String targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot unfollow yourself.");
        }

        User follower = userRepository.findByUserId(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean removedFromFollowing = follower.getFollowingIds().remove(targetUserId);
        boolean removedFromFollowers = target.getFollowerIds().remove(followerId);
        log.info("RemovedFromFollowing: {} | RemovedFromFollowers: {}", removedFromFollowing, removedFromFollowers);

        if (removedFromFollowing || removedFromFollowers) {
            userRepository.save(follower);
            userRepository.save(target);
        }
    }


    public FollowerInfo getFollowerInfo(String currentUserId, String targetUserId) {
        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean isFollowing = target.getFollowerIds().contains(currentUserId);
        int count = target.getFollowerIds().size();

        return new FollowerInfo(count, isFollowing);
    }
}
