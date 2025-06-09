# 🚀 GoTogether: Backend Platform

**GoTogether** is a robust backend platform powering a collaborative application for social networking and activity planning. Built with a microservices architecture, it provides a scalable and resilient foundation for rich user experiences.

This repository contains the backend services, API gateway, and identity management infrastructure for the GoTogether application.

---

## ✨ Features

The GoTogether backend platform provides a rich set of capabilities to power a modern social and planning application:

*   🛡️ **Secure Identity & Access Management:**
    *   Robust user authentication using username/password and social login (Google implemented in `social-media-service`) via Keycloak.
    *   OAuth2-based security with JWTs and refresh token capabilities.
    *   Centralized user identity across services.
*   👥 **Social Networking Core (via `social-media-service`):**
    *   User profile management (avatars, bios, etc.).
    *   Content posting and interaction system (specifics depend on service implementation details).
    *   Follower/following relationships and social graph management.
*   📅 **Collaborative Planning Engine (via `planning-service`):**
    *   Tools for creating, managing, and coordinating events or activities (specifics depend on service implementation details).
*   ⚡ **High-Performance API Delivery:**
    *   Efficient API gateway (Kong) for routing, security, and traffic management.
    *   Optimized inter-service communication using gRPC.
*   🧩 **Microservices Architecture:**
    *   Modular design allowing for independent scaling, development, and deployment of services.
    *   Resilient and scalable system foundation.
*   ⚙️ **Developer-Friendly:**
    *   Containerized environment using Docker for easy setup and consistent deployments.
    *   Clear separation of concerns between services (e.g., `AuthService` within `social-media-service` for authentication).

---

## 🚀 Getting Started

Follow these steps to set up and run the GoTogether backend platform on your local machine.

### Prerequisites

*   **Docker & Docker Compose:** Essential for running the containerized services and infrastructure. Install from [Docker's official website](https://www.docker.com/products/docker-desktop/).
*   **Java JDK (e.g., JDK 17 or later):** Required to build and run the Java-based microservices (`planning-service`, `social-media-service`).
*   **Go (e.g., Go 1.18 or later):** Required to build and run the `api-service`.
*   **Git:** For cloning the repository.
*   **(Optional) IDE:** Your preferred Integrated Development Environment (e.g., IntelliJ IDEA for Java, GoLand or VS Code for Go).

### Setup & Running the Platform

1.  **Clone the Repository:**
    ```bash
    # Replace <your-repository-url> with the actual URL
    git clone <your-repository-url>
    cd GoTogether-backend # Or your repository's root folder name
    ```

2.  **Configure Keycloak (Initial Setup):**
    The following commands configure the Keycloak instance that runs via Docker Compose. Execute these from the root of the project *after* the Docker services are running (see next step).

    *   **Start Core Infrastructure (including Keycloak & Kong):**
        Navigate to the API gateway configuration directory and start the services:
        ```bash
        cd apigateway/kong
        docker-compose up -d
        cd ../.. # Return to project root
        ```
        Wait for Keycloak to be fully initialized. You can check logs using `docker-compose -f apigateway/kong/docker-compose.yml logs keycloak`.

    *   **Configure Keycloak Admin CLI Access:**
        This command sets up `kcadm.sh` to connect to your Keycloak instance.
        ```bash
        docker compose -f apigateway/kong/docker-compose.yml exec keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8081 --realm master --user admin --password admin
        ```
        *(Note: Default Keycloak admin: `admin`/`admin`. Adjust if your setup differs.)*

    *   **Set SSL Required to None (for Local Development):**
        ```bash
        docker compose -f apigateway/kong/docker-compose.yml exec keycloak /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE
        ```
        *(Production environments should always enforce SSL.)*

    *   **Access Keycloak Admin Console:**
        [http://localhost:8081/admin/](http://localhost:8081/admin/)
        Refer to `social-media-service/keycloak-config.md` for details on configuring Google social login and other client setups for the `kong` realm.

3.  **Build and Run Individual Services:**
    For each microservice (`api-service`, `planning-service`, `social-media-service`):

    *   **`api-service` (Go):**
        ```bash
        cd api-service
        # Ensure environment variables are set if required by the service
        go build ./...
        ./api-service # Or your compiled binary name
        cd ..
        ```
    *   **`planning-service` & `social-media-service` (Java/Gradle):**
        (Example for `social-media-service`)
        ```bash
        cd social-media-service
        # Ensure environment variables/configurations (e.g., in application.properties) are set
        ./gradlew bootRun
        cd ..
        ```
    *(Note: Ensure databases (e.g., PostgreSQL instances in `apigateway/kong/docker-compose.yml`) are accessible by services. Configure connection strings in service properties accordingly.)*

4.  **Accessing the Platform:**
    *   Once all services and Kong are running, APIs are accessible via Kong's port (typically `http://localhost:8000`). Check Kong's configuration for specific routes.

---

## 🏛️ Architecture Overview

GoTogether is built upon a microservices architecture, promoting modularity, scalability, and independent development. Key components include:

*   🌐 **API Service (`api-service` - Go):** Main entry point for client applications, handling API requests, data aggregation, and gRPC service definitions.
*   🛡️ **API Gateway & Identity Infrastructure (`apigateway`):** Configures and deploys:
    *   **Kong:** API Gateway for request routing, security, and traffic management.
    *   **Keycloak:** IAM solution for user authentication, token issuance, and authorization.
*   🔑 **Authentication Logic (`social-media-service/AuthService` & Keycloak):** The `AuthService` module within `social-media-service` orchestrates authentication flows (username/password, OAuth2 social login, token refresh) with Keycloak.
*   📅 **Planning Service (`planning-service` - Java/Spring Boot):** Manages activity planning, event creation, and coordination.
*   👥 **Social Media Service (`social-media-service` - Java/Spring Boot):** Powers social networking features like user profiles, content interactions, and follower graphs. (Core authentication logic is now in its `AuthService` module).
*   🗣️ **Inter-Service Communication:** Primarily via **gRPC** for efficient, typed data exchange.
*   🐳 **Containerization (`Docker`):** Services are containerized for consistent deployments and local setup via `docker-compose`.

*(The `auth_service` directory contains Docker Compose configurations, likely for deploying Keycloak or related components. The primary, active authentication logic is now coordinated by `AuthService` within `social-media-service` interacting with Keycloak deployed via the `apigateway` setup.)*

---

## 🧱 Tech Stack

*   **Programming Languages & Frameworks:**
    *   ☕ **Java**: Core language for several microservices.
        *   **Spring Boot**: Application framework for Java microservices.
        *   **Gradle**: Build automation for Java projects.
    *   🐹 **Go (Golang)**: Used for the high-performance `api-service`.
*   **API & Communication:**
    *   **gRPC**: For efficient inter-service communication.
    *   **RESTful APIs**: For external client-server interaction.
*   **API Gateway:**
    *   **Kong**: Manages and secures API traffic.
*   **Identity & Access Management:**
    *   **Keycloak**: Provides authentication (OAuth2, social login) and authorization.
*   **Containerization:**
    *   **Docker & Docker Compose**: For building, deploying, and running services.
*   **Data Storage:**
    *   Each microservice utilizes appropriate data stores (e.g., PostgreSQL, MongoDB - specific databases to be confirmed by developers for each service). The `apigateway/kong/docker-compose.yml` includes PostgreSQL instances.

---

## 🎨 Client-Side Features & Considerations

The GoTogether backend is designed as a flexible foundation for various client applications. Features typically handled by the frontend include:

*   📱 **Progressive Web App (PWA):** Backend APIs fully support PWA development.
*   📄 **Static Site Generation (SSG):** Backend data can be consumed by SSG frontends at build or client time.
*   🌗 **Dark Mode:** A client-side display preference; backend APIs are theme-agnostic.
*   🖌️ **Canvas & Rich UI:** Backend can provide data for rich UIs; implementation is a frontend task.
*   🌍 **Internationalization (i18n) & Localization (l10n):**
    The backend currently defaults to a single language for API responses. Full multi-language support would require backend enhancements for internationalized responses and significant frontend work for locale handling and rendering.

---

## 🧪 Running Tests

Each service contains its own set of unit and integration tests.

*   **Java Services (e.g., `social-media-service`):**
    ```bash
    cd social-media-service
    ./gradlew test
    cd ..
    ```
*   **Go Service (`api-service`):**
    ```bash
    cd api-service
    go test ./...
    cd ..
    ```

---

## 🤝 Contributing (Example)

Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License (Example)

This project is licensed under the MIT License - see the `LICENSE.md` file for details (if one exists, or choose a license).

*(Consider adding a `LICENSE.md` file if one is not present.)*
