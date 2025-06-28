package com.example.Authservice.controller; // Renamed

import com.example.Authservice.controller.dto.JwtAuthenticationResponseDto;
import com.example.Authservice.controller.dto.LoginRequestDto;
import com.example.Authservice.controller.dto.TokenRefreshRequestDto;
import com.example.Authservice.controller.dto.UserRegistrationRequestDto;
import com.example.Authservice.model.LocalUser;
import com.example.Authservice.service.Authservice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final Authservice authservice;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    public AuthController(Authservice authservice, OAuth2AuthorizedClientService authorizedClientService) {
        this.authservice = authservice;
        this.authorizedClientService = authorizedClientService;
    }

    // --- Local Authentication Endpoints ---
    @PostMapping("/register")
    public ResponseEntity<?> registerLocalUser(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        try {
            LocalUser user = authservice.registerLocalUser(registrationRequest);
            // Consider what to return. Maybe just a success message or user info without password.
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully!", "userId", user.getId(), "username", user.getUsername()));
        } catch (RuntimeException e) {
            logger.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginLocalUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            Map<String, String> tokens = authservice.loginLocalUser(loginRequest);
            return ResponseEntity.ok(new JwtAuthenticationResponseDto(tokens.get("accessToken"), tokens.get("refreshToken")));
        } catch (Exception e) { // Catch specific Spring Security AuthenticationException for better messages
            logger.error("Local login error for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login failed: Invalid credentials or user not found."));
        }
    }

    @PostMapping("/token/refresh/local")
    public ResponseEntity<?> refreshLocalUserToken(@Valid @RequestBody TokenRefreshRequestDto refreshRequest) {
        try {
            Map<String, String> tokens = authservice.refreshLocalUserToken(refreshRequest.getRefreshToken());
            return ResponseEntity.ok(new JwtAuthenticationResponseDto(tokens.get("accessToken"), tokens.get("refreshToken"))); // refreshToken might be null if not re-issued
        } catch (RuntimeException e) {
            logger.error("Local token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // --- OAuth2 / Keycloak Related Endpoints ---
    @GetMapping("/session") // Called after successful OIDC login redirect
    public ResponseEntity<Map<String, Object>> getOidcSessionInfo(@AuthenticationPrincipal OidcUser principal, OAuth2AuthenticationToken authenticationToken) {
        if (principal == null || authenticationToken == null) {
             logger.warn("/session endpoint called without OIDC principal or authentication token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated via OIDC."));
        }
        String clientRegistrationId = authenticationToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId,
                principal.getName());

        return ResponseEntity.ok(authservice.buildTokenResponse(principal, authorizedClient));
    }

    @GetMapping("/user/me") // For users logged in via OIDC
    public ResponseEntity<Map<String, Object>> getOidcUserMe(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            logger.warn("/user/me endpoint called without OIDC principal.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated via OIDC."));
        }
        return ResponseEntity.ok(authservice.buildUserInfoResponse(principal));
    }

    // Keycloak direct refresh (if still needed, or prefer local JWTs if mixing)
    // This endpoint was calling authservice.refreshTokens which is a direct keycloak call
    @PostMapping("/token/refresh/keycloak")
    public ResponseEntity<?> refreshKeycloakToken(@Valid @RequestBody TokenRefreshRequestDto refreshTokenRequest) {
        if (refreshTokenRequest == null || refreshTokenRequest.getRefreshToken() == null || refreshTokenRequest.getRefreshToken().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required."));
        }
        try {
            Map<String, Object> tokenResponse = authservice.refreshTokens(refreshTokenRequest.getRefreshToken()); // This is the Keycloak refresh
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            logger.error("Keycloak token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Keycloak refresh token or refresh failed", "details", e.getMessage()));
        }
    }

    // Logout is primarily handled by SecurityConfig redirecting to Keycloak's end_session_endpoint.
    // Client should call GET /api/auth/perform_logout (or the configured logoutUrl in SecurityConfig)
    // This POST endpoint can remain if a specific backend action is needed before initiating logout.
    @PostMapping("/logout_trigger")
    public ResponseEntity<String> triggerLogout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // The actual logout (session invalidation, OIDC logout) is configured in SecurityConfig
        // This endpoint might not be strictly necessary if clients directly use the Spring Security logout URL.
        logger.info("Logout trigger endpoint called. Spring Security logout handler should take over via configured logout URL.");
        return ResponseEntity.ok("Logout process initiated. Client will be redirected by Spring Security if configured, or should clear local tokens.");
    }
}
