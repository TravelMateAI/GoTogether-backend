# "GoTogether": Collaborative Trip Planning & Travel Journal

This document outlines a project idea, "GoTogether," designed to leverage the existing microservice deployment capabilities provided by `deploy.sh` and `deploy/docker-compose.yml`. It also explains how this project would be deployed using the provided scripts.

## 1. "GoTogether": Concept & Features

**Concept:**
"GoTogether" is envisioned as a **Collaborative Trip Planning & Travel Journal Platform**. It allows users to plan trips together, document their travels by creating rich media posts, and share their experiences.

**Core Features & Mapping to Services:**

*   **User Authentication & Management:**
    *   **Services:** `user-service`, `auth-service`, `keycloak`.
    *   **Functionality:** Secure user registration (email/password, potential for social login via Keycloak), login, profile management (travel history, preferences), and social connections (followers/following).
    *   **Deployment:** Leverages `keycloak` for identity management, `auth-service` for orchestrating authentication flows (including issuing local JWTs for non-Keycloak logins) and syncing users with `user-service`, and `user-service` as the master database for user profiles and relationships.

*   **Collaborative Trip Planning:**
    *   **Service:** `planning-service`.
    *   **Functionality:** Users can create trips, invite friends to collaborate, define destinations, set dates, build detailed itineraries (day-by-day activities, accommodations), manage shared budgets, and assign planning tasks.
    *   **Deployment:** `planning-service` (a Spring Boot application) would handle this logic. It would require its own database schema (potentially a new PostgreSQL instance on Supabase or a local Docker volume configured in `docker-compose.yml`).

*   **Travel Journal / Experience Sharing:**
    *   **Service:** `post-service`.
    *   **Functionality:** Users can create posts associated with their trips or as standalone travelogues. Posts can include text descriptions, uploaded photos/videos, location tags, and timestamps. Other users can react to and comment on these posts.
    *   **Deployment:** `post-service` (Spring Boot) would manage these journal entries. It would need to link posts to users (from `user-service`) and potentially to trips (from `planning-service`). *Critical Refinement: For consistency, this service should not manage its own user data or auth logic as identified in prior analysis; it should rely on `user-service` and tokens validated by the gateway.*

*   **Media Handling:**
    *   **Service:** Could be part of `post-service` or a dedicated `media-service`.
    *   **Functionality:** Uploading, storing, and retrieving images and videos for travel journal posts.
    *   **Deployment:** The existing `MediaController/MediaService` within `post-service` can be enhanced. For large scale, a dedicated service with object storage (e.g., S3) would be better.

*   **API Orchestration & Third-Party Integrations:**
    *   **Service:** `api-service` (Go-based).
    *   **Functionality:** Can serve as a Backend-for-Frontend (BFF) for client applications, aggregating data from various microservices. It can also integrate with external APIs like mapping services (Google Maps, Mapbox for destination search, displaying maps), weather APIs, currency converters, etc.
    *   **Deployment:** The existing Go `api-service` is well-suited for these tasks.

*   **API Gateway:**
    *   **Service:** `api-gateway` (Kong).
    *   **Functionality:** Single, secure entry point for all client requests. Handles request routing to appropriate microservices, rate limiting, and request authentication (by introspecting tokens with `keycloak`).
    *   **Deployment:** Already configured in `docker-compose.yml`.

## 2. Deploying "GoTogether" with `deploy.sh`

The `deploy.sh` script and `deploy/docker-compose.yml` file provide a robust system for deploying the "GoTogether" project.

### 2.1. Prerequisites for Deployment

*   **Docker & Docker Compose:** Installed on the deployment host.
*   **Project Code:** Cloned Git repository containing all service code, `deploy.sh`, and `docker-compose.yml`.
*   **Build Tools:** JDK & Maven/Gradle for Java services (if building from source). Go for Go services (often handled in Docker multi-stage builds).
*   **Configuration Files:**
    *   Correctly populated `.env` files for services like `post-service` and `api-service`.
    *   Accurate database credentials in `docker-compose.yml` for `keycloak` and `user-service` (pointing to Supabase).
    *   If `planning-service` requires a database, its connection details need to be added.
    *   Valid JWT secrets in `docker-compose.yml` for `auth-service`.
    *   Keycloak realm import file (`kong-realm.json`) and SSL certificates (if using HTTPS for Keycloak on a server) must be correctly placed and referenced.

### 2.2. Deployment Steps

The primary command for a full deployment (build and run) is:

```bash
./deploy.sh
# or
./deploy.sh --up
```

**This command triggers the following sequence orchestrated by `deploy.sh`:**

1.  **Application Pre-Compilation (Java Services):**
    *   `deploy.sh` iterates through Java services defined in its `SERVICES` array (e.g., `planning-service`, `post-service`, `auth-service`, `user-service`).
    *   For each, it runs `mvn clean package -DskipTests` or `./gradlew bootJar --no-daemon` to build the application JAR.

2.  **Docker Image Construction:**
    *   `deploy.sh` then invokes `docker-compose -f deploy/docker-compose.yml build [service-name]` for each targeted service.
    *   The `Dockerfile` for each service (e.g., `../deploy/Dockerfile.springboot` for Spring Boot apps, or service-specific Dockerfiles) copies the pre-built JARs or source code and sets up the runtime environment.

3.  **Service Startup:**
    *   `deploy.sh` executes `docker-compose -f deploy/docker-compose.yml up -d [services...]`.
    *   Docker Compose starts all defined services:
        *   `kong-database`, `kong-migrations`, `api-gateway` (Kong).
        *   `keycloak` (configured with Supabase DB, SSL).
        *   `planning-service`, `post-service`, `api-service`, `auth-service`, `user-service`.
    *   Services connect to the `kong-net` custom network.
    *   Environment variables are injected, and volumes are mounted.
    *   `depends_on` ensures services like Kong wait for their database. `post-service` has an additional script in its command to wait for Keycloak's realm to be active.

4.  **Port Mapping Display:**
    *   After startup, `deploy.sh` helpfully parses `docker-compose.yml` and prints the exposed host port mappings, e.g.:
        ```
        api-gateway:
            - "8000:8000"
            - "8443:8443"
        keycloak:
            - "8084:8080" # HTTP if enabled
            - "8446:8443" # HTTPS
        post-service:
            - "8080:8080"
        # ... and so on for other services
        ```

### 2.3. Managing the "GoTogether" Deployment

*   **Stopping the Application:**
    ```bash
    ./deploy.sh --down
    ```
    This stops and removes all containers, networks, etc.

*   **Building/Rebuilding Specific Services:**
    ```bash
    ./deploy.sh --build planning-service
    # To force a rebuild without cache:
    ./deploy.sh --force-rebuild post-service
    ```

*   **Starting Specific Services (if already built):**
    ```bash
    ./deploy.sh --run planning-service post-service
    ```

*   **Viewing Logs:**
    ```bash
    ./deploy.sh --logs # For all services
    ./deploy.sh --logs planning-service # For a specific service
    ```

*   **Checking Status:**
    ```bash
    ./deploy.sh --status
    ```

### 2.4. Configuration in `docker-compose.yml` for "GoTogether"

*   **`planning-service` Database:** Add a new PostgreSQL service (e.g., `planning-db`) or configure `planning-service` environment variables to point to an external database (like another Supabase instance).
*   **`post-service` Refinements:**
    *   Remove its local database dependency for user data.
    *   Ensure its `.env` file or `docker-compose.yml` environment variables are set for communication with `user-service` (if fetching user details) and any other dependencies.
    *   Its Keycloak client ID/secret should ideally be managed by `auth-service` or Kong, not directly used by `post-service` for login flows.
*   **Inter-Service Communication:** Service names (e.g., `user-service`, `planning-service`) are resolvable on the `kong-net` network. Ports for gRPC/REST communication are configured via environment variables (e.g., `auth-service` knows `user-service` is at `user-service:9092`).

By adapting the service configurations as needed within `docker-compose.yml` and leveraging `deploy.sh`, the "GoTogether" project can be effectively developed, deployed, and managed in a containerized environment.
