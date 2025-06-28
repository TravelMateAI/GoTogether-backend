# Architecture Comparison: Monolith vs. Microservices

This document outlines the differences between an assumed original monolithic architecture and the current microservices setup for User, Post, and Authentication functionalities. It also highlights key inconsistencies found within the microservices.

## 1. Monolithic Approach (Assumed/Inferred)

In a typical monolithic application, the components would likely be structured as follows:

*   **`UserController` & `UserService`:**
    *   Managed all user lifecycle events: registration, login (possibly with a dedicated `AuthService`), profile CRUD (updates, retrieval), and social features like follow/unfollow.
    *   Directly interacted with a unified database schema, likely a single `User` table with JPA relationships to posts, followers, etc.
    *   User data was consistent and readily available across features.
*   **`PostController` & `PostService`:**
    *   Managed all aspects of posts: CRUD for posts, comments, reactions, media attachments.
    *   Directly linked to `User` entities for authorship, interactions, and displaying user information alongside posts.
*   **Authentication & Authorization:**
    *   Often a centralized `AuthService` or embedded within `UserService`, integrated with Spring Security.
    *   Would issue and validate session cookies or self-contained JWTs.
    *   Role-based access control (RBAC) managed within the single application context.

## 2. Microservice Approach (Current Implementation)

The application has been refactored into several microservices, with an API Gateway (Kong with Keycloak introspection) as the entry point.

### 2.1. `user-service`

*   **Primary Responsibilities:**
    *   Manages user profile data (CRUD operations via REST and gRPC).
    *   Manages the social graph (follow/unfollow relationships using a dedicated `Follow` entity and repository).
    *   Intended as the **source of truth for user profiles and relationships**.
*   **User Identification:**
    *   Uses Keycloak `sub` (subject claim) as `userId` for users authenticated via Keycloak.
    *   Generates `local-<uuid>` for users created via the "local" registration flow (orchestrated by `auth-service`).
*   **Authentication Role:**
    *   **Does not perform password validation.** The `authenticateLocalAccount` gRPC method is called by `auth-service` but expects `auth-service` or the original password hash source to perform the check. `user-service` likely just confirms user existence and status for this flow.
    *   Provides user details to `auth-service` for local login flow.
*   **Data Store:** Owns its `users` and `follows` tables.

### 2.2. `auth-service`

*   **Primary Responsibilities:**
    *   **OIDC/Keycloak Integration:** Manages login via Keycloak (OAuth2/OIDC).
        *   On successful OIDC login, it ensures the user's profile exists in `user-service` by calling `user-service` via gRPC (`UserserviceClient.ensureUserProfileExistsForOidcUser`).
    *   **Local Authentication Flow Orchestration:**
        *   **Registration:** Receives registration data, then calls `user-service` via gRPC (`UserserviceClient.createLocalAccountInUserservice`) to store the user profile and hash the password.
        *   **Login:** Receives credentials, calls `user-service` via gRPC (`UserserviceClient.authenticateLocalAccountInUserservice`) to validate credentials against data stored in `user-service`.
        *   **Issues its own JWTs (local JWTs)** for users authenticated through this local flow, after `user-service` confirms validity. These JWTs are distinct from Keycloak's tokens.
    *   **Token Management:**
        *   Handles refresh of its own local JWTs.
        *   Provides REST endpoints to refresh Keycloak tokens (by calling Keycloak's token endpoint).
        *   Provides gRPC endpoints (`AuthServiceImpl`) for other services to validate Keycloak-issued tokens (using a `JwtDecoder` configured for Keycloak's issuer URI).
*   **Data Store:** Does **not** store user profiles or passwords. Relies entirely on `user-service` for local user data persistence.

### 2.3. `post-service`

*   **Core Responsibilities (Intended):**
    *   Manages posts (CRUD), comments, reactions, bookmarks, and associated media.
*   **Overlapping/Inconsistent Responsibilities (Current State):**
    *   **Contains its own `UserController`, `UserService`, `AuthController`, and `AuthService`.**
    *   **Maintains its own `User` entity and `users` database table**, separate from and duplicating `user-service`.
    *   **Handles User Registration Independently:**
        *   Its `UserService.registerUser` method creates a user in its local DB **and** directly calls Keycloak's Admin API to create the user in Keycloak.
        *   This flow bypasses `auth-service` orchestration and `user-service` as the central user profile store.
    *   **Handles Password-Based Login Independently:**
        *   Its `UserService.authenticateWithKeycloak` method directly calls Keycloak's token endpoint (`grant_type=password`) to obtain Keycloak tokens.
    *   **Handles OAuth2 Callbacks (e.g., Google) Independently:** Its `AuthController` and `AuthService` have logic for processing OAuth2 callbacks, creating/updating users in its local DB, and issuing placeholder tokens.
    *   **Manages Follow/Unfollow Logic Independently:** Uses `Set<String>` for `followerIds` and `followingIds` on its local `User` entity, which conflicts with `user-service`'s `Follow` entity approach.
*   **Data Store:** Owns tables for posts, comments, reactions, media, **and its own `users` table.**
*   **Security:** Its Spring Security configuration is set to `permitAll()`, relying on the API Gateway for external request authentication.

### 2.4. `api-gateway` (Kong)

*   Acts as the public entry point to the system.
*   Routes requests to the appropriate downstream microservices.
*   Expected to perform authentication for external requests by introspecting JWTs (likely Keycloak tokens) with Keycloak.

## 3. Key Differences & Inconsistencies

This section highlights the major deviations from a clean microservice architecture and potential issues.

### 3.1. User Data Management & Duplication
*   **Massive Inconsistency:** The most significant issue is that **`post-service` maintains its own separate `User` database table and associated entities/logic.** This directly conflicts with `user-service` being the intended source of truth for user profiles.
    *   **Consequences:** User data (profile information, avatar, bio, potentially username/email if not carefully managed via Keycloak as single source) will be inconsistent across services. Updates made via `user-service` (e.g., profile update) will not reflect in `post-service`'s user records, and vice-versa.
*   **Follow/Unfollow Logic:** Duplicated and implemented differently in `user-service` (using a `Follow` relational entity) and `post-service` (using `Set<String>` on its local `User` entity). This means the social graph is fragmented and inconsistent.

### 3.2. Authentication and Registration Flows
*   **Multiple Conflicting Flows:**
    1.  **Via `auth-service` (Cleaner for Local Users):**
        *   Registration: `auth-service` -> `user-service` (gRPC for storage).
        *   Login: `auth-service` -> `user-service` (gRPC for validation) -> `auth-service` issues local JWT.
    2.  **Via `post-service` (Problematic):**
        *   Registration: `post-service` -> local DB & direct to Keycloak Admin API. Bypasses `user-service` and `auth-service`.
        *   Login (Password): `post-service` -> direct to Keycloak token endpoint. Bypasses `auth-service`.
        *   Login (OAuth2): `post-service` handles callback, local user creation/update.
*   **Ambiguity for Clients:** It's unclear which service is authoritative for registration and login. Different flows will result in different states across the system.
*   **Token Strategy:**
    *   `auth-service` issues its own JWTs for local users.
    *   Keycloak issues its own JWTs for OIDC logins and password grants (when `post-service` calls it).
    *   The API Gateway likely only validates Keycloak tokens. Can local JWTs from `auth-service` be used at the gateway? This needs clarification.

### 3.3. Service Responsibilities & Coupling
*   **`post-service` Overreach:** `post-service` has taken on responsibilities that should belong to `user-service` (profile management, follow graph) and `auth-service` (authentication orchestration, Keycloak interaction). This makes `post-service` a "mini-monolith" for these features.
*   **Direct Keycloak Coupling in `post-service`:** `post-service` directly interacts with Keycloak for token exchange and user creation (admin API). This increases its complexity and couples it tightly to Keycloak's specifics, a role better suited for `auth-service`.
*   **Missing `post-service` -> `user-service` Interaction:** When displaying posts, `post-service` appears to use its local user data. It should ideally call `user-service` (e.g., via gRPC) to fetch up-to-date user information (author details, etc.) to ensure consistency.

### 3.4. Data Consistency
*   This is the overarching problem resulting from the above points. User profiles, follow relationships, and potentially even user existence will be inconsistent between `user-service` and `post-service`.

## 4. Recommendations for Consistency (Implied)

To resolve these inconsistencies, the following should be considered:

1.  **Single Source of Truth for Users:** `user-service` should be the sole owner of user profile data and the social graph. `post-service` must not maintain its own user table or follow logic.
2.  **Centralized Authentication Logic:** `auth-service` should be the sole orchestrator of all authentication flows (local and OIDC).
    *   `post-service` should not handle registration or login directly. These requests should route to `auth-service`.
    *   `post-service` should not directly call Keycloak for tokens or user creation.
3.  **Decouple `post-service`:** Remove user management and authentication code from `post-service`. When `post-service` needs user information, it should query `user-service` (e.g., via a gRPC client).
4.  **Clarify Token Strategy:** Define clearly how Keycloak tokens and local JWTs (from `auth-service`) are used, validated, and which services/endpoints expect which type of token, especially concerning the API Gateway.
5.  **Data Migration:** Plan for migrating user data from `post-service`'s local tables to `user-service` and reconciling inconsistencies.

This refactoring effort has made good progress in separating concerns (e.g., `auth-service`'s reliance on `user-service` for local user data), but `post-service` remains a significant area of inconsistency.
