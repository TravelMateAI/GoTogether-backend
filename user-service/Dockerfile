# Use an OpenJDK base image
FROM openjdk:17-jdk-slim

# Install curl (required for wait-for-Keycloak logic)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*


# Add a volume for logs (optional)
VOLUME /tmp

# Copy the built JAR file
COPY build/libs/*.jar app.jar

# Expose the port on which the service runs
EXPOSE 8082 9092 # Expose configured web and gRPC ports

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]