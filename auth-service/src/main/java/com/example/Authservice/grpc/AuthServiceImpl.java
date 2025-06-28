package com.example.Authservice.grpc; // Renamed

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.Authservice.service.TokenValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final TokenValidationService tokenValidationService;

    @Autowired
    public AuthServiceImpl(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }

    @Override
    @PreAuthorize("isAuthenticated()") // Secure this gRPC method
    public void getUserAuthInfoFromToken(GetUserAuthInfoFromTokenRequest request, StreamObserver<UserAuthInfoResponse> responseObserver) {
        logger.debug("gRPC getUserAuthInfoFromToken called for token: {}", request.getToken().substring(0, Math.min(request.getToken().length(), 20)) + "...");
        try {
            Jwt jwt = tokenValidationService.validateToken(request.getToken());
            if (jwt == null) {
                responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Invalid token.").asRuntimeException());
                return;
            }

            Map<String, Object> claims = jwt.getClaims();
            UserAuthInfoResponse.Builder responseBuilder = UserAuthInfoResponse.newBuilder()
                    .setUserId(jwt.getSubject())
                    .setUsername(jwt.getClaimAsString("preferred_username"))
                    .setActive(true); // If token is valid, user is considered active in this context

            // Extract roles - this depends on how Keycloak is configured to put roles in the token
            // Example: from realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof java.util.List) {
                ((java.util.List<?>) realmAccess.get("roles")).forEach(role -> responseBuilder.addRoles(String.valueOf(role)));
            }

            // Optionally add all string claims
            // claims.forEach((key, value) -> {
            //     if (value instanceof String) {
            //         responseBuilder.putClaims(key, (String) value);
            //     }
            // });

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error validating token in getUserAuthInfoFromToken: {}", e.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Token validation failed: " + e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()") // Secure this gRPC method
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        logger.debug("gRPC validateToken called for token: {}", request.getToken().substring(0, Math.min(request.getToken().length(), 20)) + "...");
        try {
            Jwt jwt = tokenValidationService.validateToken(request.getToken());
             if (jwt == null) { // Should not happen if validateToken throws on failure
                responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Invalid token.").asRuntimeException());
                return;
            }

            ValidateTokenResponse.Builder responseBuilder = ValidateTokenResponse.newBuilder()
                    .setUserId(jwt.getSubject())
                    .setUsername(jwt.getClaimAsString("preferred_username"))
                    .setValid(true)
                    .setActive(true) // If token is valid
                    .setIssuedAt(jwt.getIssuedAt() != null ? jwt.getIssuedAt().getEpochSecond() : 0)
                    .setExpiresAt(jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : 0);

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof java.util.List) {
                ((java.util.List<?>) realmAccess.get("roles")).forEach(role -> responseBuilder.addRoles(String.valueOf(role)));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.warn("Token validation failed in validateToken: {}", e.getMessage());
             // For a "validate" endpoint, we might want to return valid=false instead of an error
            responseObserver.onNext(ValidateTokenResponse.newBuilder().setValid(false).setActive(false).build());
            responseObserver.onCompleted();
            // Or, if any exception means it's an error for the caller:
            // responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Token validation failed: " + e.getMessage()).withCause(e).asRuntimeException());
        }
    }
}
