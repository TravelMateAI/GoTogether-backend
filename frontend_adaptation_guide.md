# Frontend API Adaptation Guide: Monolith to Microservices

This guide outlines how to adapt your existing frontend application, previously connected to a monolithic backend, to communicate with the new microservices architecture. The key changes involve routing API calls through an API Gateway (Kong), handling new authentication flows with `auth-service` and Keycloak, and interacting with specialized microservices like `user-service`.

## 1. Core Principles of a Microservices Frontend

*   **API Gateway as the Entry Point:** All frontend API calls should be directed to the API Gateway (Kong). The gateway then routes requests to the appropriate backend microservice. This provides a single, consistent interface for the frontend.
    *   *Example Base URL:* `http://localhost:8000` (or your configured Kong proxy URL).
*   **Token-Based Authentication:** Interactions with protected microservice APIs will require an `Authorization: Bearer <accessToken>` header. These tokens are obtained during the login process.
*   **Decoupled Data:** Data previously available from a single API call in the monolith might now be split across multiple microservices. The frontend may need to make multiple calls or rely on a Backend-for-Frontend (BFF) pattern (potentially implemented in `api-service`) to aggregate data.

## 2. Adapting Authentication (`AuthController` equivalent)

Your monolithic `AuthController` logic will be replaced by interactions with `auth-service` via the API Gateway, which also integrates with Keycloak.

### 2.1. Login

*   **Monolithic Flow (Example):**
    *   Frontend `POST`s to `/api/auth/login` with username/password.
    *   Monolith validates credentials, might set an HttpOnly session cookie or return a monolith-issued JWT.

*   **Microservice Flow (Example - Local Username/Password via `auth-service`):**
    1.  **Frontend Call:** `POST` to `{API_GATEWAY_URL}/auth/login` (this path is routed by Kong to `auth-service`).
        ```javascript
        // Example:
        async function login(username, password) {
          const response = await fetch(`${API_GATEWAY_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
          });
          if (!response.ok) { /* Handle error */ }
          const data = await response.json(); // { accessToken: "...", refreshToken: "..." }
          localStorage.setItem('accessToken', data.accessToken);
          localStorage.setItem('refreshToken', data.refreshToken);
          // Fetch user profile separately if needed
        }
        ```
    2.  **Token Storage:** The frontend receives an `accessToken` and `refreshToken` from `auth-service`.
        *   `accessToken`: Store in memory or `localStorage`. Used in `Authorization` headers.
        *   `refreshToken`: Store more securely if possible (e.g., `localStorage` or an HttpOnly cookie set by `auth-service` if it supports this for the refresh endpoint).
*   **Microservice Flow (Example - OIDC/Keycloak):**
    1.  Frontend redirects the user's browser to Keycloak's authorization endpoint (this might be initiated by a call to a specific path on the API Gateway that triggers the OIDC flow configured in `auth-service`).
    2.  User authenticates with Keycloak.
    3.  Keycloak redirects back to a predefined `redirect_uri` in your frontend (or `auth-service`).
    4.  The frontend (or `auth-service`) exchanges the received authorization code for tokens (ID, access, refresh) from Keycloak's token endpoint.
    5.  Store these Keycloak tokens similarly to the local JWTs.

### 2.2. Registration

*   **Monolithic Flow (Example):**
    *   Frontend `POST`s to `/api/users/register` or `/api/auth/register`.

*   **Microservice Flow:**
    *   Frontend `POST`s to `{API_GATEWAY_URL}/auth/register` (routed to `auth-service`).
    *   `auth-service` then communicates with `user-service` via gRPC to create the user profile.
    ```javascript
    // Example:
    async function register(userData) {
      const response = await fetch(`${API_GATEWAY_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData), // { username, email, password, roles }
      });
      // Handle response (e.g., success message, auto-login)
    }
    ```

### 2.3. Logout

*   **Monolithic Flow (Example):**
    *   Frontend `POST`s to `/api/auth/logout`.

*   **Microservice Flow:**
    1.  **Clear Local State:** Remove tokens from frontend storage (`localStorage`, memory).
    2.  **Call Backend Logout (Optional but Recommended):**
        *   Make a `POST` request to an endpoint like `{API_GATEWAY_URL}/auth/logout_trigger` (routed to `auth-service`).
        *   `auth-service`'s `SecurityConfig` is set up to handle `/api/auth/perform_logout` and trigger an OIDC logout, redirecting to Keycloak's end session endpoint if the session was OIDC-based. This invalidates the Keycloak session.
    3.  Redirect the user to the login page.

### 2.4. Token Refresh

*   The `accessToken` will expire. Your frontend must handle this:
    1.  When an API call returns a 401 Unauthorized status:
    2.  Use the `refreshToken` to call a refresh endpoint on `auth-service` via the API Gateway (e.g., `POST {API_GATEWAY_URL}/auth/token/refresh/local` for local JWTs, or a similar one for Keycloak refresh if `auth-service` proxies it).
    3.  `auth-service` will return a new `accessToken` (and potentially a new `refreshToken`).
    4.  Update stored tokens.
    5.  Retry the original failed API request with the new `accessToken`.
*   This logic is often implemented in an HTTP client interceptor.

## 3. Adapting User Data Calls (`UserController` equivalent)

Your monolithic `UserController` endpoints will now be served by `user-service` via the API Gateway.

### 3.1. Fetching User Profile (e.g., `/api/users/me`)

*   **Monolithic Flow (Example):**
    *   Frontend `GET`s `/api/users/me`. (Auth might be cookie-based or monolith JWT).

*   **Microservice Flow:**
    *   Frontend `GET`s `{API_GATEWAY_URL}/users/me` (path configured in Kong to route to `user-service`).
    *   **Crucially, include the `Authorization` header:**
    ```javascript
    // Example:
    async function fetchMyProfile() {
      const accessToken = localStorage.getItem('accessToken');
      if (!accessToken) throw new Error('Not authenticated');

      const response = await fetch(`${API_GATEWAY_URL}/users/me`, {
        headers: { 'Authorization': `Bearer ${accessToken}` },
      });
      if (response.status === 401) { /* Handle token refresh */ }
      if (!response.ok) { /* Handle error */ }
      return response.json(); // Profile data from user-service
    }
    ```

### 3.2. Updating User Profile

*   **Monolithic Flow (Example):**
    *   Frontend `PUT`s to `/api/users/me` or `/api/users/{id}` with profile data.

*   **Microservice Flow:**
    *   Frontend `PUT`s to `{API_GATEWAY_URL}/users/me` (or similar, routed to `user-service`).
    *   Include the `Authorization: Bearer <accessToken>` header.
    *   The request body format should match what `user-service` expects.

## 4. General Strategy for Adapting Other API Calls

1.  **Inventory Existing Calls:** List all API endpoints your monolithic frontend currently calls.
2.  **Map to New Gateway Routes:** For each old endpoint, determine its new equivalent path on the API Gateway. This requires understanding your Kong configuration (how it routes to `post-service`, `planning-service`, etc.).
3.  **Update Base URL:** Change the global API base URL in your frontend to point to the API Gateway.
4.  **Add Authorization Headers:** Ensure all calls to protected resources through the gateway include the `Authorization: Bearer <accessToken>` header.
5.  **Review Request/Response Payloads:** Microservices might have different request or response structures than the monolith. Adjust your frontend code to match.
    *   **Data Aggregation Example:** A monolithic call `GET /api/posts/{postId_with_author_and_comments}` might have returned everything. In microservices:
        *   `GET {API_GATEWAY_URL}/posts/{postId}` (to `post-service`) might return basic post data.
        *   Author details might need a separate call `GET {API_GATEWAY_URL}/users/{authorId}` (to `user-service`) if not embedded by `post-service`.
        *   Comments might be another call or part of the post response.
        *   Consider if `api-service` should provide BFF endpoints to aggregate this data for the frontend.
6.  **Update Error Handling:** Be prepared for new error types (e.g., gateway errors, specific service errors like 503 if a microservice is down).

## 5. Key Frontend Implementation Considerations

*   **Centralized API Client/Service:** Create a wrapper around your HTTP fetching library (e.g., `fetch`, `axios`) to:
    *   Automatically set the API Gateway base URL.
    *   Inject the `Authorization` header.
    *   Handle 401 responses by initiating the token refresh flow and retrying requests.
*   **State Management (Tokens & User Info):** Use a robust state management solution (Context API in React, Vuex, Redux, etc.) to manage authentication status, tokens, and user information globally in your app.
*   **CORS:** Ensure the API Gateway (Kong) and individual microservices (if accessed directly during development, though not typical for primary flow) are configured with appropriate Cross-Origin Resource Sharing (CORS) headers to allow requests from your frontend's domain.

This transition requires careful planning and systematic updates to your frontend codebase. Start with the authentication flow, as it's foundational, and then incrementally update API calls for each feature.
