package com.example.Userservice.grpc; // Renamed

import com.example.Userservice.model.User; // Renamed import
import com.example.Userservice.service.UserProfileService; // Renamed import
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;


@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserProfileService userProfileService;

    @Autowired
    public UserServiceImpl(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    private com.example.Userservice.grpc.UserDetails toGrpcUserDetails(User user) {
        if (user == null) return com.example.Userservice.grpc.UserDetails.newBuilder().getDefaultInstanceForType();
        return com.example.Userservice.grpc.UserDetails.newBuilder()
                .setUserId(user.getUserId())
                .setUsername(user.getUsername() == null ? "" : user.getUsername())
                .setFirstName(user.getFirstName() == null ? "" : user.getFirstName())
                .setLastName(user.getLastName() == null ? "" : user.getLastName())
                .setEmail(user.getEmail() == null ? "" : user.getEmail())
                .setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl())
                .build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getUserDetails(GetUserDetailsRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        logger.info("gRPC getUserDetails called for userId: {}", request.getUserId());
        User user = userProfileService.getUserById(request.getUserId());
        if (user != null) {
            UserDetailsResponse response = UserDetailsResponse.newBuilder().setUserDetails(toGrpcUserDetails(user)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
             responseObserver.onError(Status.NOT_FOUND
                .withDescription("User not found with ID: " + request.getUserId())
                .asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()") // Or a specific role if only certain services can create users
    public void createUserProfile(CreateUserProfileRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        logger.info("gRPC createUserProfile called for userId: {}, username: {}", request.getUserId(), request.getUsername());
        try {
            User newUser = new User();
            newUser.setUserId(request.getUserId());
            newUser.setUsername(request.getUsername());
            newUser.setEmail(request.getEmail());
            newUser.setFirstName(request.getFirstName());
            newUser.setLastName(request.getLastName());
            newUser.setAvatarUrl(request.getAvatarUrl());

            User createdUser = userProfileService.createUser(newUser);
            UserDetailsResponse response = UserDetailsResponse.newBuilder().setUserDetails(toGrpcUserDetails(createdUser)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (DataIntegrityViolationException e) {
             responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("User profile already exists or constraint violation: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        } catch (Exception e) {
            logger.error("gRPC createUserProfile error for userId {}: {}", request.getUserId(), e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error creating user profile: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        logger.info("gRPC updateUserProfile called for userId: {}", request.getUserId());
        // Assuming the principal's ID (from token) should match request.getUserId() for self-updates,
        // or specific roles for admin updates. This logic should be in UserProfileService or handled by PreAuthorize.
        // For now, UserProfileService.updateUserProfile should handle ownership/permissions if necessary.
        try {
            User updatedUser = userProfileService.updateUserProfile(
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getAvatarUrl()
            );
            UserDetailsResponse response = UserDetailsResponse.newBuilder().setUserDetails(toGrpcUserDetails(updatedUser)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RuntimeException e) { // Catch specific exceptions like UserNotFound
            logger.error("gRPC updateUserProfile error for userId {}: {}", request.getUserId(), e.getMessage(), e);
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }


    @Override
    @PreAuthorize("isAuthenticated()")
    public void getUserBasicInfo(GetUserBasicInfoRequest request, StreamObserver<UserBasicInfoResponse> responseObserver) {
        logger.info("gRPC getUserBasicInfo called for userId: {}", request.getUserId());
        User user = userProfileService.getUserById(request.getUserId());
        if (user != null) {
            String displayName = user.getFirstName() != null && !user.getFirstName().isEmpty() ?
                                 user.getFirstName() + (user.getLastName() != null && !user.getLastName().isEmpty() ? " " + user.getLastName() : "") :
                                 user.getUsername();
            UserBasicInfoResponse response = UserBasicInfoResponse.newBuilder()
                    .setUserId(user.getUserId())
                    .setUsername(user.getUsername())
                    .setDisplayName(displayName)
                    .setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
             responseObserver.onError(Status.NOT_FOUND
                .withDescription("User not found with ID: " + request.getUserId())
                .asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getUserByUsername(GetUserByUsernameRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        logger.info("gRPC getUserByUsername called for username: {}", request.getUsername());
        User user = userProfileService.getUserByUsername(request.getUsername());
        if (user != null) {
            UserDetailsResponse response = UserDetailsResponse.newBuilder().setUserDetails(toGrpcUserDetails(user)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found: " + request.getUsername()).asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        logger.info("gRPC getUserByEmail called for email: {}", request.getEmail());
        User user = userProfileService.getUserByEmail(request.getEmail());
        if (user != null) {
            UserDetailsResponse response = UserDetailsResponse.newBuilder().setUserDetails(toGrpcUserDetails(user)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found: " + request.getEmail()).asRuntimeException());
        }
    }

    // Follow/Unfollow RPCs
    @Override
    @PreAuthorize("isAuthenticated()") // Principal's ID would be extracted from token by an interceptor
    public void followUser(FollowUserRequest request, StreamObserver<FollowUserResponse> responseObserver) {
        // String currentUserId = ... get from SecurityContext / gRPC Context attributes
        // For now, this requires requestorUserId to be passed in request if not using interceptors to inject it.
        // This is a design choice: either client sends its ID, or server infers it from auth context.
        // Assuming request.getRequestorUserId() is the authenticated user.
        logger.info("gRPC followUser called: {} wants to follow {}", request.getRequestorUserId(), request.getTargetUserId());
        try {
            userProfileService.followUser(request.getRequestorUserId(), request.getTargetUserId());
            responseObserver.onNext(FollowUserResponse.newBuilder().setSuccess(true).setMessage("Followed successfully.").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void unfollowUser(UnfollowUserRequest request, StreamObserver<UnfollowUserResponse> responseObserver) {
        logger.info("gRPC unfollowUser called: {} wants to unfollow {}", request.getRequestorUserId(), request.getTargetUserId());
         try {
            userProfileService.unfollowUser(request.getRequestorUserId(), request.getTargetUserId());
            responseObserver.onNext(UnfollowUserResponse.newBuilder().setSuccess(true).setMessage("Unfollowed successfully.").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getFollowers(GetFollowListRequest request, StreamObserver<GetFollowListResponse> responseObserver) {
        logger.info("gRPC getFollowers called for userId: {}", request.getUserId());
        try {
            java.util.List<User> followers = userProfileService.getFollowers(request.getUserId());
            java.util.List<com.example.Userservice.grpc.UserDetails> grpcFollowers = followers.stream()
                .map(this::toGrpcUserDetails)
                .collect(Collectors.toList());
            responseObserver.onNext(GetFollowListResponse.newBuilder().addAllUsers(grpcFollowers).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
             responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getFollowing(GetFollowListRequest request, StreamObserver<GetFollowListResponse> responseObserver) {
        logger.info("gRPC getFollowing called for userId: {}", request.getUserId());
         try {
            java.util.List<User> following = userProfileService.getFollowing(request.getUserId());
            java.util.List<com.example.Userservice.grpc.UserDetails> grpcFollowing = following.stream()
                .map(this::toGrpcUserDetails)
                .collect(Collectors.toList());
            responseObserver.onNext(GetFollowListResponse.newBuilder().addAllUsers(grpcFollowing).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
             responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getFollowCounts(GetFollowCountsRequest request, StreamObserver<GetFollowCountsResponse> responseObserver) {
        logger.info("gRPC getFollowCounts called for userId: {}", request.getUserId());
        try {
            UserProfileService.FollowCounts counts = userProfileService.getFollowCounts(request.getUserId());
            responseObserver.onNext(GetFollowCountsResponse.newBuilder()
                .setUserId(request.getUserId())
                .setFollowerCount(counts.getFollowerCount())
                .setFollowingCount(counts.getFollowingCount())
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    // --- RPCs for Local Account Management by Authservice ---

    @Override
    @PreAuthorize("hasAuthority('SCOPE_INTERNAL_SERVICE') or hasRole('SERVICE_ACCOUNT')") // Example of securing internal RPC
    // Alternatively, if Authservice calls this without its own auth but relies on network security:
    // @PreAuthorize("permitAll()") and ensure this endpoint is not publicly exposed by gateway.
    // For now, let's assume Authservice will need a specific authority to call this.
    public void createLocalAccount(CreateLocalAccountRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        logger.info("gRPC createLocalAccount called for username: {}", request.getUsername());
        try {
            User createdUser = userProfileService.createLocalAccount(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getRoles()
            );
            UserDetailsResponse response = UserDetailsResponse.newBuilder().setUserDetails(toGrpcUserDetails(createdUser)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (DataIntegrityViolationException e) {
            logger.warn("gRPC createLocalAccount failed due to data integrity: {}", e.getMessage());
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            logger.error("gRPC createLocalAccount failed for username {}: {}", request.getUsername(), e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to create local account: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    // This endpoint is effectively public from Authservice's perspective as it's for user login.
    // Authservice itself might be secured, but this specific call validates credentials.
    // No specific @PreAuthorize needed here unless we want to restrict which clients can call it.
    public void authenticateLocalAccount(AuthenticateLocalAccountRequest request, StreamObserver<AuthenticationResponse> responseObserver) {
        logger.info("gRPC authenticateLocalAccount called for username: {}", request.getUsername());
        try {
            Optional<User> userOptional = userProfileService.authenticateLocalAccount(request.getUsername(), request.getPassword());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                AuthenticationResponse response = AuthenticationResponse.newBuilder()
                    .setIsValid(true)
                    .setUserId(user.getUserId())
                    .setUsername(user.getUsername())
                    .setEmail(user.getEmail())
//                    .setRoles(user.getRoles() == null ? "" : user.getRoles())
                    .setActive(true) // Assuming user is active if authenticated
                    .setUserDetails(toGrpcUserDetails(user))
                    .build();
                responseObserver.onNext(response);
            } else {
                AuthenticationResponse response = AuthenticationResponse.newBuilder()
                    .setIsValid(false)
                    .setErrorMessage("Invalid username or password.")
                    .build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("gRPC authenticateLocalAccount failed for username {}: {}", request.getUsername(), e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Authentication failed: " + e.getMessage()).asRuntimeException());
        }
    }
}
