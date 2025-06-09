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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final WebClient.Builder webClientBuilder;

    private final String keycloakUrl = "http://localhost:8081";
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

    public TokenResponse authenticateWithKeycloak(String username, String password) { // Return type changed
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "kong-oidc");
        formData.add("client_secret", "fBHJFdikM0ERtTXnvebguHRz6iPUfJfV"); // Move to env or config!
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", "openid profile email offline_access"); // Added offline_access scope

        TokenResponse tokenResponse = webClient.post()
                .uri(keycloakUrl + "/realms/kong/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            // It's also good to check if refresh token is present if offline_access was requested
            // though Keycloak might not return it if the client isn't configured for it
            // or if the user session doesn't support offline tokens.
            throw new RuntimeException("Login failed: Unable to retrieve token details from Keycloak");
        }

        // Log if refresh token is missing, for debugging purposes
        if (tokenResponse.getRefreshToken() == null) {
            log.warn("Refresh token was not received from Keycloak for user {}. Check Keycloak client configuration for 'Use Refresh Tokens' and scope 'offline_access'.", username);
        }

        return tokenResponse; // Return the whole object
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
        private String refresh_token;
        private long expires_in;
        // private String id_token; // Optional, can be added if needed later

        public String getAccessToken() {
            return access_token;
        }

        public String getRefreshToken() {
            return refresh_token;
        }

        public long getExpiresIn() {
            return expires_in;
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

    public Map<String, Object> processGoogleLogin(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("OAuth2User cannot be null");
        }

        String email = oauth2User.getAttribute("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found in OAuth2 user attributes");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // User not found, create a new one
                    User newUser = new User();
                    newUser.setUserId(UUID.randomUUID().toString()); // Generate a new user ID
                    newUser.setEmail(email);
                    newUser.setUsername(oauth2User.getAttribute("email")); // Or use a generated username
                    newUser.setFirstName(oauth2User.getAttribute("given_name"));
                    newUser.setLastName(oauth2User.getAttribute("family_name"));
                    newUser.setAvatarUrl(oauth2User.getAttribute("picture"));
                    // Set other default fields as necessary
                    newUser.setEnabled(true); // Assuming new users are enabled by default

                    // Save the new user to your local database
                    // Also, consider if you need to create/link this user in Keycloak if it wasn't done automatically
                    // For now, focusing on local DB
                    return userRepository.save(newUser);
                });

        // At this point, 'user' is either an existing user or a newly created one.
        // Now, generate a token for this user.
        // This could be a Keycloak token if Spring Security OAuth2 client handles it,
        // or you might need to explicitly request one from Keycloak,
        // or generate your own application-specific JWT.

        // For simplicity, let's assume Spring Security handles the token from Keycloak.
        // If you have a direct way to get the token used by Spring Security, use that.
        // Otherwise, you might need to call your existing Keycloak authentication logic
        // or adapt it. The `authenticateWithKeycloak` method expects username/password.
        // For OAuth2, the token is usually managed by Spring Security's context.

        // Placeholder: Simulating token generation or retrieval.
        // In a real scenario, you'd integrate with Keycloak to get/validate the token
        // associated with this OAuth2 login. If Spring Security's @AuthenticationPrincipal
        // gives you an OAuth2AuthenticatedPrincipal, it might contain the token.
        // For now, we'll re-use parts of the existing login flow for consistency,
        // but this might need refinement based on how Spring Security OAuth2 client and Keycloak interact.

        // This part is tricky without knowing exactly how Spring Security OAuth2 client
        // makes the token available post-authentication or if a new token needs to be issued by Keycloak.
        // Let's assume for now that we need to "simulate" a login to get a Keycloak token
        // This is NOT ideal and should be replaced with proper token handling from Spring Security context if possible.
        // A better approach would be to configure Keycloak as an OAuth2 resource server
        // and use the token obtained by Spring Security OAuth2 client directly.

        // If the user is already authenticated via OAuth2, Spring Security might have the token.
        // However, the existing `authenticateWithKeycloak` is for password grants.
        // We need a way to get a token for the now-authenticated OAuth2 user.
        // This might involve an exchange or simply trusting the authentication done by Spring Security.

        // For now, let's assume the goal is to ensure the user exists in our DB and then
        // we'd typically rely on the security context established by Spring OAuth2.
        // The token returned to the client should be the one from Keycloak.

        // This is a simplified placeholder. The actual token should be the one from Keycloak.
        // If Spring Security is configured as an OAuth2 client, it should handle token retrieval.
        // The principal itself might be an instance of OidcUser which contains an ID token.
        String token;
        if (oauth2User instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authToken =
                    (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) oauth2User;
            // This is not standard, ID token or access token might be elsewhere or require specific config
            // For OIDC, if principal is OidcUser:
            // if (oauth2User instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser) {
            //    token = ((org.springframework.security.oauth2.core.oidc.user.OidcUser) oauth2User).getIdToken().getTokenValue();
            // } else {
            //    // Fallback or error, as we need a token to return
                   token = "dummy-oauth2-token-for-" + user.getEmail(); // Replace with actual token logic
            // }
            // This is a placeholder. In a real app, you would get the actual Keycloak access token
            // that Spring Security OAuth2 client has obtained.
            // It might be accessible via SecurityContextHolder or injected if configured correctly.
            // For now, we'll generate a placeholder or assume one is available.
            // This part needs to be robustly implemented based on Spring Security's capabilities.
            // A common pattern is that the Spring OAuth2 client itself doesn't issue a *new* token,
            // but rather uses the one from the IdP (Keycloak).
            // The challenge is making *that* token available to the client in the expected format.
            // If Keycloak is also the resource server, the token obtained by Spring client is what you need.

            // Let's assume for now the token is not directly available here in a simple way
            // and we'll log a warning. The client should ideally receive the token Keycloak issued.
            // The `UserController` might need to be adapted if the token is already in the security context.
            log.warn("Token generation/retrieval in processGoogleLogin is a placeholder. Integrate properly with Spring Security OAuth2 to get the Keycloak token.");
            token = "NEEDS-ACTUAL-KEYCLOAK-TOKEN-FOR-" + user.getEmail();
        } else {
            log.warn("OAuth2User is not an instance of OAuth2AuthenticationToken. Token retrieval might fail.");
            token = "FALLBACK-TOKEN-" + user.getEmail(); // Fallback placeholder
        }


        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("token", token); // This should be the Keycloak token
        authResponse.put("user", user); // The local user entity

        log.info("Processed Google login for user: {}", user.getEmail());
        return authResponse;
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty.");
        }

        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", "kong-oidc"); // Must match the client ID used in login
        formData.add("client_secret", "fBHJFdikM0ERtTXnvebguHRz6iPUfJfV"); // Client secret for confidential client

        log.info("Attempting to refresh access token using refresh token (first 10 chars): {}", refreshToken.substring(0, Math.min(refreshToken.length(), 10)));

        TokenResponse tokenResponse = webClient.post()
                .uri(keycloakUrl + "/realms/kong/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Keycloak refresh token request failed with status {}: {}", clientResponse.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Failed to refresh token. Status: " + clientResponse.statusCode() + ", Body: " + errorBody));
                        })
                )
                .bodyToMono(TokenResponse.class)
                .doOnError(error -> log.error("Error during token refresh WebClient call: {}", error.getMessage()))
                .block(); // Consider async handling if this service is called in a reactive flow

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            // This case might be covered by onStatus, but as a fallback
            throw new RuntimeException("Token refresh failed: Did not receive a new access token from Keycloak.");
        }

        log.info("Access token refreshed successfully. New access token expires in: {}s", tokenResponse.getExpiresIn());
        if (tokenResponse.getRefreshToken() != null) {
            log.info("Keycloak returned a new refresh token (rotation might be on).");
        } else {
            log.info("Keycloak did not return a new refresh token (existing one should still be valid if not expired).");
        }

        return tokenResponse;
    }
}
