package org.example.planningservice.configs;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import route.DirectionsServiceGrpc;

@Configuration
public class GrpcClientConfig {

    @Bean
    ManagedChannel channel() {
        return ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();
    }

    @Bean
    DirectionsServiceGrpc.DirectionsServiceBlockingStub routeServiceStub(ManagedChannel channel) {
        return DirectionsServiceGrpc.newBlockingStub(channel);
    }
}

