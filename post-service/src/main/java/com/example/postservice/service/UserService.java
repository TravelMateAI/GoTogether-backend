package com.example.postservice.service;

import com.example.postservice.dto.FollowerInfo;
import com.example.postservice.dto.UpdateProfileRequest;
import com.example.postservice.dto.UpdateProfileResponse;
import com.example.postservice.entity.User;
import com.example.postservice.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final WebClient.Builder webClientBuilder;

    private final String keycloakUrl = "http://auth-service:8080";
    private final String adminUsername = "admin";
    private final String adminPassword = "admin";

    public User getUserByEmailFromToken(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        Map<String, Object> userInfo = webClient.get()
                .uri(keycloakUrl + "/realms/kong/protocol/openid-connect/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (userInfo == null || !userInfo.containsKey("email")) {
            throw new RuntimeException("Failed to fetch user info from Keycloak");
        }

        String email = userInfo.get("email").toString();
        return getUserByEmail(email);
    }

    public String authenticateWithKeycloak(String username, String password) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "kong-oidc");
        formData.add("client_secret", "fBHJFdikM0ERtTXnvebguHRz6iPUfJfV"); // Move to env or config!
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", "openid profile email");

        TokenResponse tokenResponse = webClient.post()
                .uri(keycloakUrl + "/realms/kong/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Login failed: Unable to retrieve access token from Keycloak");
        }

        return tokenResponse.getAccessToken();
    }


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

    private void createUserInKeycloak(String username, String email, String password, String firstName, String lastName) {
        String adminToken = getAdminAccessToken();

        WebClient webClient = webClientBuilder.build();

        var response = webClient.post()
                .uri(keycloakUrl + "/admin/realms/kong/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new KeycloakUserRequest(username, email, password, firstName, lastName))
                .retrieve()
                .toBodilessEntity()
                .block();

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create user in Keycloak");
        }
    }

    public User updateAvatar(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    private String getAdminAccessToken() {
        WebClient webClient = webClientBuilder.build();

        var tokenResponse = webClient.post()
                .uri(keycloakUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=password&client_id=admin-cli&username=" + adminUsername + "&password=" + adminPassword)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Failed to authenticate with Keycloak");
        }

        return tokenResponse.getAccessToken();
    }

    @lombok.Data
    static class TokenResponse {
        private String access_token;
        public String getAccessToken() {
            return access_token;
        }
    }

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
        user.setPosts(null);
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
            user.setFirstName(request.getDisplayName()); // ✅ assuming 'firstName' is your displayName
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
        log.info("Follower: {} | Target: {}", followerId, targetUserId);
        log.info("Follower: {} | Target: {}", follower.getFollowingIds().toString(), target.getFollowerIds().toString() );

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
        log.info("Follower: {} | Target: {}", followerId, targetUserId);
        log.info("Follower: {} | Target: {}", follower.getFollowingIds().toString(), target.getFollowerIds().toString() );

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
