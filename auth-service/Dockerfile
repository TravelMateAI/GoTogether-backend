FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY build/libs/*.jar /app.jar
EXPOSE 8081 9091 # Expose configured web and gRPC ports
ENTRYPOINT ["/entrypoint.sh"]
