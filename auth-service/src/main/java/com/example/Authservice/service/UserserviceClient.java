package com.example.Authservice.service;

// Imports for Userservice gRPC stubs
import com.example.Userservice.grpc.UserServiceGrpc;
import com.example.Userservice.grpc.CreateUserProfileRequest; // For OIDC profile sync
import com.example.Userservice.grpc.UserDetailsResponse;    // For OIDC profile sync & local create
import com.example.Userservice.grpc.CreateLocalAccountRequest; // For local registration
import com.example.Userservice.grpc.AuthenticateLocalAccountRequest;
import com.example.Userservice.grpc.AuthenticationResponse;


import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserserviceClient { // Renamed class

    private static final Logger logger = LoggerFactory.getLogger(UserserviceClient.class);

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    // Method for OIDC flow: ensures user profile exists in Userservice
    public void ensureUserProfileExistsForOidcUser(String userId, String username, String email, String firstName, String lastName, String avatarUrl) {
        logger.info("Ensuring user profile exists in Userservice for OIDC user: userId={}, username={}", userId, username);
        try {
            CreateUserProfileRequest.Builder requestBuilder = CreateUserProfileRequest.newBuilder()
                .setUserId(userId) // This ID comes from Keycloak (subject)
                .setUsername(username)
                .setEmail(email == null ? "" : email);

            if (firstName != null) requestBuilder.setFirstName(firstName);
            if (lastName != null) requestBuilder.setLastName(lastName);
            // Avatar URL from OIDC token might be available via 'picture' claim
            if (avatarUrl != null) requestBuilder.setAvatarUrl(avatarUrl);


            UserDetailsResponse response = userServiceBlockingStub.createUserProfile(requestBuilder.build());
            logger.info("User profile creation/check in Userservice successful for OIDC user {}: ID {}, Username {}",
                        userId, response.getUserDetails().getUserId(), response.getUserDetails().getUsername());

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                logger.info("User profile in Userservice already exists for OIDC user ID: {}. No action needed.", userId);
            } else {
                logger.error("gRPC error while ensuring user profile exists in Userservice for OIDC user {}: Status={}", userId, e.getStatus(), e);
                // Optionally re-throw or handle as a critical failure if profile sync is mandatory
            }
        } catch (Exception e) {
            logger.error("Unexpected error while ensuring user profile exists in Userservice for OIDC user {}: {}", userId, e.getMessage(), e);
        }
    }

    // Method for traditional registration: creates a local account in Userservice
    public UserDetailsResponse createLocalAccountInUserservice(String username, String email, String password, String roles) throws StatusRuntimeException {
        logger.info("Attempting to create local account in Userservice: username={}", username);
        CreateLocalAccountRequest request = CreateLocalAccountRequest.newBuilder()
            .setUsername(username)
            .setEmail(email)
            .setPassword(password) // Userservice will hash this
            .setRoles(roles == null ? "ROLE_USER" : roles)
            .build();
        try {
            UserDetailsResponse response = userServiceBlockingStub.createLocalAccount(request);
            logger.info("Local account created in Userservice for username {}: UserID {}", username, response.getUserDetails().getUserId());
            return response;
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error creating local account in Userservice for username {}: {}", username, e.getStatus(), e);
            throw e; // Re-throw for Authservice to handle (e.g., return 400 or 409 to client)
        }
    }

    // Method for traditional login: authenticates against Userservice
    public AuthenticationResponse authenticateLocalAccountInUserservice(String username, String password) throws StatusRuntimeException {
        logger.info("Attempting to authenticate local account in Userservice: username={}", username);
        AuthenticateLocalAccountRequest request = AuthenticateLocalAccountRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build();
        try {
            AuthenticationResponse response = userServiceBlockingStub.authenticateLocalAccount(request);
            if (response.getIsValid()) {
                logger.info("Local account authentication successful in Userservice for username {}", username);
            } else {
                logger.warn("Local account authentication failed in Userservice for username {}: {}", username, response.getErrorMessage());
            }
            return response;
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error authenticating local account in Userservice for username {}: {}", username, e.getStatus(), e);
            throw e; // Re-throw for Authservice to handle
        }
    }
}
