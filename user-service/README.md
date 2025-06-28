# Userservice

The `Userservice` is the central repository for user profile information and related social features within the GoTogether application. It serves as the single source of truth for all user data, whether users are onboarded via Keycloak (through `Authservice`) or register via a traditional username/password flow (also orchestrated by `Authservice`).

## Core Functionalities

*   **User Profile Management:**
    *   Stores comprehensive user profiles, including user ID (from Keycloak `sub` or generated for local accounts), username, email, first name, last name, avatar URL, roles, and hashed passwords (for locally registered accounts).
    *   Provides REST and gRPC APIs for CRUD operations on user profiles.
*   **Local Account Backend:**
    *   **Account Creation (gRPC):** Accepts requests from `Authservice` to create new user accounts with username, email, and password. Hashes and stores the password.
    *   **Authentication (gRPC):** Accepts requests from `Authservice` to validate username/password combinations against stored hashed passwords.
*   **Social Features:**
    *   **Follow/Unfollow System:** Manages relationships between users (who follows whom).
    *   Provides APIs to follow/unfollow users, and retrieve follower/following lists and counts.
*   **Security:**
    *   Acts as an OAuth2 Resource Server, validating Keycloak-issued JWTs for its REST and gRPC APIs.
    *   Uses Spring Security method-level authorization (`@PreAuthorize`) to protect endpoints.

## API Endpoints (REST)

All `/api/users` endpoints require a valid Keycloak-issued JWT (Access Token).

*   **Current User:**
    *   `GET /api/users/me`: Retrieves the profile of the currently authenticated user.
    *   `PUT /api/users/me`: Updates the profile of the currently authenticated user.
        *   Request Body: `UserProfileUpdateRequestDto` (`firstName`, `lastName`, `avatarUrl`).
    *   `PUT /api/users/me/avatar`: (Conceptual endpoint for file upload) Updates the current user's avatar. *Full file upload mechanism is not part of this service's current direct implementation; this endpoint would typically set an avatar URL provided by a file storage service.*
*   **User Lookups:**
    *   `GET /api/users/username/{username}`: Retrieves a user profile by username.
    *   `GET /api/users/email/{email}`: Retrieves a user profile by email.
    *   `GET /api/users/{userId}`: Retrieves a user profile by user ID. (Access might be restricted, e.g., to admins or self).
*   **Follow System:**
    *   `POST /api/users/{targetUserId}/follow`: Allows the authenticated user to follow `targetUserId`.
    *   `DELETE /api/users/{targetUserId}/follow`: Allows the authenticated user to unfollow `targetUserId`.
    *   `GET /api/users/{userId}/followers`: Retrieves a list of users following `userId`.
    *   `GET /api/users/{userId}/following`: Retrieves a list of users `userId` is following.
    *   `GET /api/users/{userId}/follow-info`: Retrieves follower and following counts for `userId`.

## gRPC Services

Service: `com.example.Userservice.grpc.UserService`

Most RPC methods require the calling client/service to be authenticated with a valid Keycloak-issued JWT. Exceptions are:
*   `AuthenticateLocalAccount`: This method performs authentication based on its request payload.
*   `CreateLocalAccount`: This method is called by `Authservice` and is secured by requiring `Authservice` to have a specific service role/authority (e.g., `SCOPE_INTERNAL_SERVICE` or `ROLE_SERVICE_ACCOUNT`) in its own authentication token when calling this RPC.

*   **User Profile Management (requiring Keycloak JWT for caller):**
    *   `rpc GetUserDetails(GetUserDetailsRequest) returns (UserDetailsResponse)`
    *   `rpc CreateUserProfile(CreateUserProfileRequest) returns (UserDetailsResponse)`: Typically called by `Authservice` for OIDC user profile sync.
    *   `rpc UpdateUserProfile(UpdateUserProfileRequest) returns (UserDetailsResponse)`
    *   `rpc GetUserBasicInfo(GetUserBasicInfoRequest) returns (UserBasicInfoResponse)`
    *   `rpc GetUserByUsername(GetUserByUsernameRequest) returns (UserDetailsResponse)`
    *   `rpc GetUserByEmail(GetUserByEmailRequest) returns (UserDetailsResponse)`
*   **Local Account Support (called by `Authservice`):**
    *   `rpc CreateLocalAccount(CreateLocalAccountRequest) returns (UserDetailsResponse)`: Creates a user account with a hashed password.
    *   `rpc AuthenticateLocalAccount(AuthenticateLocalAccountRequest) returns (AuthenticationResponse)`: Validates credentials for local accounts.
*   **Follow System:**
    *   `rpc FollowUser(FollowUserRequest) returns (FollowUserResponse)`
    *   `rpc UnfollowUser(UnfollowUserRequest) returns (UnfollowUserResponse)`
    *   `rpc GetFollowers(GetFollowListRequest) returns (GetFollowListResponse)`
    *   `rpc GetFollowing(GetFollowListRequest) returns (GetFollowListResponse)`
    *   `rpc GetFollowCounts(GetFollowCountsRequest) returns (GetFollowCountsResponse)`

## Environment Variables

*   `DB_HOST`: Database host.
*   `DB_PORT`: Database port.
*   `DB_NAME`: Database name.
*   `DB_USER`: Database username.
*   `DB_PASSWORD`: Database password. **MUST BE SET SECURELY IN PRODUCTION.**
*   `KEYCLOAK_ISSUER_URI`: Keycloak issuer URI for JWT validation (e.g., `http://keycloak:8080/realms/kong`).
*   `APP_PORT`: Port for the REST API (default: 8082).
*   `GRPC_SERVER_PORT`: Port for the gRPC server (default: 9092).

(See `application.properties` for defaults if environment variables are not set).

## Database Schema

*   **`user_profiles` table:**
    *   `id` (String, PK - Keycloak `sub` or generated "local-uuid")
    *   `username` (String, unique)
    *   `email` (String, unique)
    *   `first_name` (String, nullable)
    *   `last_name` (String, nullable)
    *   `avatar_url` (String, nullable)
    *   `hashed_password` (String, nullable - for local accounts)
    *   `roles` (String, nullable - for local accounts)
    *   `created_at`, `updated_at` (Timestamps)
*   **`user_follows` table:**
    *   `follower_id` (String, PK, FK to `user_profiles.id`)
    *   `following_id` (String, PK, FK to `user_profiles.id`)
    *   `created_at` (Timestamp)

## Build & Run

The service is a Spring Boot application built with Gradle.

1.  **Build:**
    ```bash
    ./gradlew clean build
    ```
    This will also generate gRPC stubs.

2.  **Run (Standalone, not recommended for full flow):**
    ```bash
    java -jar build/libs/user-service-0.0.1-SNAPSHOT.jar
    ```
    (Ensure all required environment variables are set, and the database is accessible).

3.  **Run (via Docker Compose):**
    The service is configured in the main `deploy/docker-compose.yml` file.
    ```bash
    docker-compose -f deploy/docker-compose.yml up --build user-service
    ```
    This is the recommended way to run, as it includes dependencies like Keycloak and the database.
```
