package com.example.Authservice.security;

import com.example.Authservice.model.LocalUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long jwtAccessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long jwtRefreshTokenExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            logger.warn("JWT secret is not configured or is too short. Using a default insecure key. PLEASE CONFIGURE app.jwt.secret PROPERLY.");
            // Generate a default key for safety if not configured, but this is not secure for production
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
    }

    // LocalUser variants are removed as LocalUser entity is removed from Authservice
    // public String generateAccessToken(LocalUser localUser) { ... }
    // public String generateRefreshToken(LocalUser localUser) { ... }

    public String generateAccessToken(String username, String userId, String email, String roles) { // userId changed to String
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpirationMs);

        Claims claims = Jwts.claims().setSubject(username);
        claims.put("userId", userId); // Store as String
        if (email != null) claims.put("email", email);
        if (roles != null && !roles.isEmpty()) {
             claims.put("roles", roles);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, String userId) { // userId changed to String
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshTokenExpirationMs);

        Claims claims = Jwts.claims().setSubject(username);
        claims.put("userId", userId); // Store as String
        claims.put("type", "REFRESH_TOKEN");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public String getUserIdFromJWT(String token) { // Changed return type to String
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.get("userId", String.class); // Retrieve as String
    }

    public Claims getAllClaimsFromJWT(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }


    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}
