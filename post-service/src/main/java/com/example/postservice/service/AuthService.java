package com.example.postservice.service;

import com.example.postservice.entity.User;
import com.example.postservice.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    private final UserService userService; // To interact with user data for non-auth specific lookups
    private final UserRepo userRepository; // For direct DB access for auth-related user processing
    private final WebClient.Builder webClientBuilder;

    // private final String keycloakUrl = "http://localhost:8081"; // Placeholder, ideally from config

    @lombok.Data
    public static class TokenResponse {
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

    public AuthService.TokenResponse loginWithPassword(String username, String password) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "kong-oidc");
        formData.add("client_secret", keycloakClientSecret); // Move to env or config!
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", "openid profile email offline_access");

        AuthService.TokenResponse tokenResponse = webClient.post()
                .uri(keycloakServerUrl + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(AuthService.TokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Login failed: Unable to retrieve token details from Keycloak");
        }

        if (tokenResponse.getRefreshToken() == null) {
            log.warn("Refresh token was not received from Keycloak for user {}. Check Keycloak client configuration for 'Use Refresh Tokens' and scope 'offline_access'.", username);
        }
        return tokenResponse;
    }

    public AuthService.TokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty.");
        }
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", "kong-oidc");
        formData.add("client_secret", keycloakClientSecret);

        log.info("Attempting to refresh access token using refresh token (first 10 chars): {}", refreshToken.substring(0, Math.min(refreshToken.length(), 10)));

        AuthService.TokenResponse tokenResponse = webClient.post()
                .uri(keycloakServerUrl + "/protocol/openid-connect/token")
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
                .bodyToMono(AuthService.TokenResponse.class)
                .doOnError(error -> log.error("Error during token refresh WebClient call: {}", error.getMessage()))
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
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

    public Map<String, Object> processOAuth2Login(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("OAuth2User cannot be null");
        }
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found in OAuth2 user attributes");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUserId(UUID.randomUUID().toString());
                    newUser.setEmail(email);
                    newUser.setUsername(oauth2User.getAttribute("email"));
                    newUser.setFirstName(oauth2User.getAttribute("given_name"));
                    newUser.setLastName(oauth2User.getAttribute("family_name"));
                    newUser.setAvatarUrl(oauth2User.getAttribute("picture"));
//                    newUser.setEnabled(true);
                    return userRepository.save(newUser);
                });

        String token; // Placeholder logic from original method
        if (oauth2User instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            log.warn("Token generation/retrieval in processOAuth2Login is a placeholder. Integrate properly with Spring Security OAuth2 to get the Keycloak token.");
            token = "NEEDS-ACTUAL-KEYCLOAK-TOKEN-FOR-" + user.getEmail();
        } else {
            log.warn("OAuth2User is not an instance of OAuth2AuthenticationToken. Token retrieval might fail.");
            token = "FALLBACK-TOKEN-" + user.getEmail();
        }

        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("token", token);
        authResponse.put("user", user);

        log.info("Processed OAuth2 login for user: {}", user.getEmail());
        return authResponse;
    }

    public User getUserByEmailFromToken(String accessToken) {
        WebClient webClient = webClientBuilder.build();
        Map<String, Object> userInfo = webClient.get()
                .uri(keycloakServerUrl + "/protocol/openid-connect/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (userInfo == null || !userInfo.containsKey("email")) {
            throw new RuntimeException("Failed to fetch user info from Keycloak");
        }
        String email = userInfo.get("email").toString();
        // Call UserService for the actual lookup, as getUserByEmail is a general user service method
        return userService.getUserByEmail(email);
    }
}
