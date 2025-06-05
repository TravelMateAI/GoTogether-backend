package com.example.planning.grpc.client;

import com.example.planning.grpc.apiservice.common.ExampleGreeterGrpc;
import com.example.planning.grpc.apiservice.common.HelloRequest;
import com.example.planning.grpc.apiservice.common.HelloReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GreeterClient {
    private static final Logger logger = Logger.getLogger(GreeterClient.class.getName());

    private final ManagedChannel channel;
    private final ExampleGreeterGrpc.ExampleGreeterBlockingStub blockingStub;

    /** Construct client connecting to Greeter server at {@code host:port}. */
    public GreeterClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build());
    }

    /** Construct client for accessing Greeter server using the existing channel. */
    GreeterClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = ExampleGreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Say hello to server. */
    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "RPC failed: " + e.getMessage(), e);
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        // Access a service running on the local machine on port 50051.
        GreeterClient client = new GreeterClient("localhost", 50051);
        try {
            String user = "world";
            if (args.length > 0) {
                user = args[0]; /* Use the arg as the name to greet if provided */
            }
            client.greet(user);
        } finally {
            client.shutdown();
        }
    }
}
