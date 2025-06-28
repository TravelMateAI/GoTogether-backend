package com.example.Authservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
// Duplicates removed, the ones below are sufficient
import com.example.Authservice.controller.dto.LoginRequestDto;
import com.example.Authservice.controller.dto.UserRegistrationRequestDto;
// LocalUser and LocalUserRepository are no longer used here
import com.example.Authservice.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Autowired and Value are already imported
// HttpEntity, HttpHeaders, MediaType, ResponseEntity are already imported
// AuthenticationManager, UsernamePasswordAuthenticationToken, Authentication, SecurityContextHolder are not directly used now
// PasswordEncoder is not used here anymore
// OAuth2AuthorizedClient, ClientRegistration, ClientRegistrationRepository are already imported
// OidcUser is already imported
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class Authservice {
    private static final Logger logger = LoggerFactory.getLogger(Authservice.class);

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;
    // private final LocalUserRepository localUserRepository; // Removed
    // private final PasswordEncoder passwordEncoder; // Removed, Userservice handles hashing
    private final JwtTokenProvider jwtTokenProvider; // Authservice still issues its own JWTs for local flow
    // private final AuthenticationManager authenticationManager; // Removed, Userservice authenticates
    private final UserserviceClient userserviceClient; // Added


    @Value("${spring.security.oauth2.client.registration.keycloak-login.provider}")
    private String keycloakProviderId;

    @Value("${spring.security.oauth2.client.registration.keycloak-login.client-id}")
    private String keycloakOauthClientId;

    @Value("${spring.security.oauth2.client.registration.keycloak-login.client-secret}")
    private String keycloakOauthClientSecret;

    @Autowired
    public Authservice(ClientRegistrationRepository clientRegistrationRepository,
                       JwtTokenProvider jwtTokenProvider,
                       UserserviceClient userserviceClient) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.restTemplate = new RestTemplate();
        this.jwtTokenProvider = jwtTokenProvider;
        this.userserviceClient = userserviceClient;
    }

    // --- OAuth2 / Keycloak related methods ---
    public Map<String, Object> buildTokenResponse(OidcUser principal, OAuth2AuthorizedClient authorizedClient) {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("user_id", principal.getSubject());
        tokenResponse.put("username", principal.getPreferredUsername());
        tokenResponse.put("email", principal.getEmail());
        tokenResponse.put("name", principal.getFullName());
        tokenResponse.put("id_token", principal.getIdToken().getTokenValue());

        if (authorizedClient != null) {
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            tokenResponse.put("access_token", accessToken.getTokenValue());
            tokenResponse.put("access_token_issued_at", accessToken.getIssuedAt());
            tokenResponse.put("access_token_expires_at", accessToken.getExpiresAt());

            OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
            if (refreshToken != null) {
                tokenResponse.put("refresh_token", refreshToken.getTokenValue());
                if (refreshToken.getIssuedAt() != null) {
                     tokenResponse.put("refresh_token_issued_at", refreshToken.getIssuedAt());
                }
            }
        } else {
            tokenResponse.put("warning", "OAuth2AuthorizedClient not found, token details might be limited.");
        }
        return tokenResponse;
    }

    public Map<String, Object> buildUserInfoResponse(OidcUser principal) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", principal.getSubject());
        userInfo.put("username", principal.getPreferredUsername());
        userInfo.put("email", principal.getEmail());
        userInfo.put("first_name", principal.getGivenName());
        userInfo.put("last_name", principal.getFamilyName());
        userInfo.put("full_name", principal.getFullName());
        return userInfo;
    }

    public Map<String, Object> refreshTokens(String refreshTokenValue) throws HttpClientErrorException {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("keycloak-login");
        if (clientRegistration == null) {
            throw new IllegalStateException("Client registration not found for keycloak-login");
        }
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshTokenValue);
        params.add("client_id", keycloakOauthClientId); // Corrected field name
        if (keycloakOauthClientSecret != null && !keycloakOauthClientSecret.isEmpty()) { // Corrected field name
             params.add("client_secret", keycloakOauthClientSecret); // Corrected field name
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers); // Renamed variable to avoid clash
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, requestEntity, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("access_token", responseBody.get("access_token"));
            tokenMap.put("id_token", responseBody.get("id_token"));
            tokenMap.put("refresh_token", responseBody.get("refresh_token"));
            tokenMap.put("expires_in", responseBody.get("expires_in"));
            tokenMap.put("refresh_expires_in", responseBody.get("refresh_expires_in"));
            tokenMap.put("token_type", responseBody.get("token_type"));
            return tokenMap;
        } else {
            throw new RuntimeException("Failed to refresh token from Keycloak, status: " + response.getStatusCode());
        }
    }

    // --- Local User Authentication methods (using Userservice via gRPC) ---

    @Transactional // This method itself might not need to be transactional if gRPC call is the main work
    public com.example.Userservice.grpc.UserDetailsResponse registerLocalUser(UserRegistrationRequestDto registrationRequest) {
        logger.info("Registering local user: {}", registrationRequest.getUsername());
        try {
            // Authservice sends plain password to Userservice; Userservice hashes it.
            return userserviceClient.createLocalAccountInUserservice(
                registrationRequest.getUsername(),
                registrationRequest.getEmail(),
                registrationRequest.getPassword(),
                registrationRequest.getRoles() // Assuming DTO has roles, or set default in Userservice
            );
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call to Userservice for registration failed for {}: {}",registrationRequest.getUsername(), e.getStatus());
            throw new RuntimeException("Registration failed: " + e.getStatus().getDescription(), e);
        }
    }

    public Map<String, String> loginLocalUser(LoginRequestDto loginRequest) {
        logger.info("Attempting local login for user: {}", loginRequest.getUsername());
        com.example.Userservice.grpc.AuthenticationResponse authResponse;
        try {
            authResponse = userserviceClient.authenticateLocalAccountInUserservice(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call to Userservice for authentication failed for {}: {}", loginRequest.getUsername(), e.getStatus());
            throw new RuntimeException("Authentication failed due to service error: " + e.getStatus().getDescription(), e);
        }

        if (!authResponse.getIsValid() || !authResponse.getActive()) {
            String errorMessage = authResponse.getErrorMessage().isEmpty() ? "Invalid credentials or user inactive." : authResponse.getErrorMessage();
            logger.warn("Local login failed for {}: {}", loginRequest.getUsername(), errorMessage);
            throw new RuntimeException(errorMessage);
        }

        com.example.Userservice.grpc.UserDetails userDetails = authResponse.getUserDetails();
        // userDetails.getUserId() is already a String (e.g., "local-uuid...")
        // JwtTokenProvider's generateAccessToken and generateRefreshToken now accept String userId.

        String accessToken = jwtTokenProvider.generateAccessToken(
            userDetails.getUsername(),
            userDetails.getUserId(), // Pass String ID directly
            userDetails.getEmail(),
            userDetails.getRoles()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            userDetails.getUsername(),
            userDetails.getUserId() // Pass String ID directly
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        logger.info("Local login successful for {}, JWTs issued.", loginRequest.getUsername());
        return tokens;
    }

    public Map<String, String> refreshLocalUserToken(String refreshTokenValue) {
        logger.info("Attempting to refresh local token");
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new RuntimeException("Invalid or expired refresh token!");
        }
        io.jsonwebtoken.Claims claims = jwtTokenProvider.getAllClaimsFromJWT(refreshTokenValue);
        if (!"REFRESH_TOKEN".equals(claims.get("type", String.class))) {
            throw new RuntimeException("Token is not a refresh token!");
        }

        String username = claims.getSubject();
        String userIdStr = claims.get("userId", String.class); // Retrieve as String
        String email = claims.get("email", String.class);
        String roles = claims.get("roles", String.class);

        // Optional: Call Userservice to confirm user ("userIdStr") still exists and is active.
        // This adds a DB lookup but increases security. For now, trusting the valid refresh token.

        String newAccessToken = jwtTokenProvider.generateAccessToken(username, userIdStr, email, roles);
        // To re-issue refresh tokens for sliding sessions:
        // String newRefreshToken = jwtTokenProvider.generateRefreshToken(username, userIdStr);
        // tokens.put("refreshToken", newRefreshToken);


        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        logger.info("Local token refreshed for user {}", username);
        return tokens;
    }
}
