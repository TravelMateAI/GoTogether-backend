# Authservice

The `Authservice` is responsible for user authentication and session management within the GoTogether application. It supports two primary modes of authentication:

1.  **OAuth2/OIDC via Keycloak:** For browser-based flows and primary SSO. `Authservice` acts as an OAuth2 client to Keycloak.
2.  **Traditional Username/Password:** For scenarios where direct credential login is preferred. `Authservice` orchestrates this by:
    *   Accepting username/password.
    *   Calling `Userservice` via gRPC to validate credentials and manage user account storage (including hashed passwords).
    *   Issuing its own local JWTs (access and refresh tokens) upon successful validation by `Userservice`.

`Authservice` is stateless regarding user data storage for both Keycloak-federated users and locally registered users; all user profile information and local account credentials reside in `Userservice`.

## Core Functionalities

*   **Keycloak OAuth2/OIDC Login:**
    *   Initiates OAuth2 Authorization Code Grant flow with Keycloak.
    *   Handles callbacks from Keycloak.
    *   After successful authentication, ensures a corresponding user profile exists in `Userservice` by making a gRPC call.
    *   Provides Keycloak tokens (ID, Access, Refresh) to the client via a REST endpoint.
*   **Local Username/Password Authentication:**
    *   **Registration (`POST /api/auth/register`):** Accepts user details, calls `Userservice` via gRPC to create the account (stores username, email, hashed password, roles in `Userservice` DB).
    *   **Login (`POST /api/auth/login`):** Accepts username/password, calls `Userservice` via gRPC to validate credentials. If valid, `Authservice` generates and returns its own local JWT access and refresh tokens.
    *   **Local JWT Refresh (`POST /api/auth/token/refresh/local`):** Accepts a locally issued refresh token and returns a new local access token.
*   **Keycloak Token Refresh (`POST /api/auth/token/refresh/keycloak`):**
    *   Accepts a Keycloak refresh token and calls Keycloak's token endpoint to get new tokens.
*   **User Information (`GET /api/users/me`):**
    *   For users logged in via Keycloak OIDC, returns user information derived from the OIDC principal/ID token.
*   **Logout (`POST /api/auth/logout_trigger` & OIDC Logout Flow):**
    *   Initiates local session termination.
    *   Handles OIDC logout, redirecting to Keycloak's end session endpoint.
*   **gRPC Services (for internal use, e.g., by API Gateway):**
    *   `AuthService.GetUserAuthInfoFromToken`: Introspects a Keycloak-issued JWT and returns user information.
    *   `AuthService.ValidateToken`: Validates a Keycloak-issued JWT and returns its status and key claims.
    *   These gRPC methods require the calling service to be authenticated (e.g., with a service account JWT).

## API Endpoints (REST)

*   **OAuth2/OIDC Flow:**
    *   `GET /login` or `GET /oauth2/authorization/keycloak-login`: Initiates Keycloak login flow (handled by Spring Security).
    *   `/login/oauth2/code/keycloak-login`: Callback URI for Keycloak (handled by Spring Security).
    *   `GET /api/auth/session`: (Default success URL after OIDC login) Retrieves Keycloak tokens (ID, Access, Refresh) and basic user claims for the frontend. Requires active OIDC session.
*   **Local Authentication Flow:**
    *   `POST /api/auth/register`: Registers a new user locally (user data stored in `Userservice`).
        *   Request Body: `UserRegistrationRequestDto` (`username`, `email`, `password`, `roles` (optional)).
    *   `POST /api/auth/login`: Logs in a local user.
        *   Request Body: `LoginRequestDto` (`username`, `password`).
        *   Response Body: `JwtAuthenticationResponseDto` (local `accessToken`, `refreshToken`).
    *   `POST /api/auth/token/refresh/local`: Refreshes a locally issued JWT.
        *   Request Body: `TokenRefreshRequestDto` (`refreshToken`).
        *   Response Body: `JwtAuthenticationResponseDto` (new `accessToken`).
*   **Keycloak Token Refresh:**
    *   `POST /api/auth/token/refresh/keycloak`: Refreshes Keycloak tokens.
        *   Request Body: `TokenRefreshRequestDto` (`refreshToken` - Keycloak's refresh token).
        *   Response Body: Map containing new Keycloak tokens.
*   **User Info & Logout:**
    *   `GET /api/users/me`: Retrieves information for the OIDC authenticated user.
    *   `POST /api/auth/logout_trigger`: A trigger point for logout. Spring Security's `logoutUrl` (`/api/auth/perform_logout`) handles the actual OIDC logout process.

## gRPC Services

Service: `com.example.Authservice.grpc.AuthService`

*   `rpc GetUserAuthInfoFromToken(GetUserAuthInfoFromTokenRequest) returns (UserAuthInfoResponse)`
    *   Validates a Keycloak JWT and extracts user details.
*   `rpc ValidateToken(ValidateTokenRequest) returns (ValidateTokenResponse)`
    *   Validates a Keycloak JWT and returns its status and key claims.

Both gRPC methods require the calling client to be authenticated.

## Environment Variables

*   `KEYCLOAK_ISSUER_URI`: Keycloak issuer URI (e.g., `http://keycloak:8080/realms/kong` for internal, or public URL).
*   `OAUTH_CLIENT_ID`: Client ID for `Authservice` registered in Keycloak for OIDC login.
*   `OAUTH_CLIENT_SECRET`: Client secret for `Authservice`'s OIDC client.
*   `OAUTH_REDIRECT_URI`: Redirect URI configured in Keycloak (e.g., `http://localhost:8000/login/oauth2/code/keycloak-login` if via Kong).
*   `JWT_SECRET`: Secret key for signing locally issued JWTs (at least 32 URL-safe characters). **MUST BE SET SECURELY IN PRODUCTION.**
*   `JWT_ACCESS_EXPIRATION_MS`: Expiration time for locally issued access tokens (default: 1 hour).
*   `JWT_REFRESH_EXPIRATION_MS`: Expiration time for locally issued refresh tokens (default: 24 hours).
*   `APP_PORT`: Port for the REST API (default: 8081).
*   `GRPC_SERVER_PORT`: Port for the gRPC server (default: 9091).
*   `USER_SERVICE_GRPC_HOST`: Hostname of the Userservice (e.g., `user-service`).
*   `USER_SERVICE_GRPC_PORT`: gRPC port of the Userservice (e.g., `9092`).

(See `application.properties` for defaults if environment variables are not set).

## Build & Run

The service is a Spring Boot application built with Gradle.

1.  **Build:**
    ```bash
    ./gradlew clean build
    ```
    This will also generate gRPC stubs.

2.  **Run (Standalone, not recommended for full flow):**
    ```bash
    java -jar build/libs/auth-service-0.0.1-SNAPSHOT.jar
    ```
    (Ensure all required environment variables are set).

3.  **Run (via Docker Compose):**
    The service is configured in the main `deploy/docker-compose.yml` file.
    ```bash
    docker-compose -f deploy/docker-compose.yml up --build auth-service
    ```
    This is the recommended way to run, as it includes dependencies like Keycloak and Userservice.

## Notes
*   **Token Types:**
    *   For OIDC logins via Keycloak, `Authservice` facilitates obtaining Keycloak-issued ID, Access, and Refresh tokens. Clients should use the Keycloak Access Token to call other resource servers (like `Userservice`).
    *   For traditional username/password logins, `Authservice` (after validating credentials via `Userservice`) issues its *own local JWTs* (Access and Refresh). These local JWTs are primarily intended for managing the session with `Authservice` itself. Other resource servers (like `Userservice`) are typically configured to validate Keycloak tokens, not these local `Authservice`-issued JWTs, unless explicitly configured to trust `Authservice` as an additional token issuer.
*   **Accessing Resource Servers:** To access APIs on `Userservice` or other resource servers, clients generally need a Keycloak-issued Access Token. If a user authenticated via the traditional local login flow, the client application might need to guide the user through a (potentially seamless) Keycloak OIDC flow if a Keycloak token is required for broader API access, or `Authservice` would need to implement a token exchange/delegation mechanism (which is currently out of scope).
*   Error handling and logging are implemented but can be further enhanced for production monitoring.
*   For the OIDC flow, the frontend is expected to handle the redirection to Keycloak (initiated by accessing a protected resource or a login trigger URL in `Authservice`) and then call `/api/auth/session` to retrieve Keycloak tokens after successful authentication.
```
