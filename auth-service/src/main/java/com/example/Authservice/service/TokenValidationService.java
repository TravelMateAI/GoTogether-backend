package com.example.Authservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TokenValidationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);
    private final JwtDecoder jwtDecoder;

    @Autowired
    public TokenValidationService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Validates the given JWT string.
     * @param tokenValue the JWT string.
     * @return Jwt object if valid.
     * @throws JwtException if token is invalid or validation fails.
     */
    public Jwt validateToken(String tokenValue) throws JwtException {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new JwtException("Token cannot be null or empty");
        }
        try {
            Jwt jwt = jwtDecoder.decode(tokenValue);
            // Perform additional checks if needed (e.g., audience, specific claims)
            // For example, Keycloak tokens might have an 'aud' claim.
            // if (!jwt.getAudience().contains("your-expected-audience")) {
            //     throw new JwtException("Invalid token audience");
            // }
            logger.debug("Token validated successfully for subject: {}", jwt.getSubject());
            return jwt;
        } catch (JwtException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            throw e; // Re-throw for the gRPC service to handle as UNAUTHENTICATED
        }
    }
}
