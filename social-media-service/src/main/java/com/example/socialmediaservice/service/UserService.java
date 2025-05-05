package com.example.socialmediaservice.service;

import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final WebClient.Builder webClientBuilder;

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
}
