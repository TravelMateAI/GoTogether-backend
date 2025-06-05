package org.example.planningservice.grpc.client;

import org.example.planningservice.grpc.apiservice.common.ExampleGreeterGrpc;
import org.example.planningservice.grpc.apiservice.common.HelloRequest;
import org.example.planningservice.grpc.apiservice.common.HelloReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class GreeterClient {
    private static final Logger logger = Logger.getLogger(GreeterClient.class.getName());

    private ManagedChannel channel;
    private ExampleGreeterGrpc.ExampleGreeterBlockingStub blockingStub;

    @Value("${api.service.host:localhost}")
    private String apiServiceHost;

    @Value("${api.service.port:8001}") // Matches the updated port in application.properties
    private int apiServicePort;

    @PostConstruct
    private void init() {
        logger.info("Initializing GreeterClient for " + apiServiceHost + ":" + apiServicePort);
        channel = ManagedChannelBuilder.forAddress(apiServiceHost, apiServicePort)
                .usePlaintext() // For simplicity in dev; use SSL/TLS in production
                .build();
        blockingStub = ExampleGreeterGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (channel != null) {
            logger.info("Shutting down GreeterClient channel");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /** Say hello to server. */
    public HelloReply sayHello(String name) {
        logger.info("Attempting to greet " + name + " via gRPC...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        // The try-catch block can be here for logging, or removed if errors are to be handled by the caller (controller)
        try {
            HelloReply response = blockingStub.sayHello(request);
            logger.info("Successfully greeted " + name + ". Response: " + response.getMessage());
            return response;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "gRPC call to Greeter service failed for name: " + name, e);
            throw e; // Re-throw the exception to be handled by the controller
        }
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    // Main method removed as this class is now a Spring managed bean
}
