package com.ISA.Restaurant.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import realTimeSuggestion.RealTimeSuggestionsServiceGrpc;
import restaurant.SearchServiceGrpc;

@Configuration
public class GrpcStubConfig {

    // gRPC channel configuration
    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder.forAddress("127.0.0.1", 9191)
                .usePlaintext()
                .build();
    }

    // Stub for SearchService
    @Bean
    public SearchServiceGrpc.SearchServiceBlockingStub searchServiceBlockingStub(ManagedChannel managedChannel) {
        return SearchServiceGrpc.newBlockingStub(managedChannel);
    }


    @Bean
    public RealTimeSuggestionsServiceGrpc.RealTimeSuggestionsServiceBlockingStub suggestionsServiceBlockingStub(ManagedChannel managedChannel) {
        return RealTimeSuggestionsServiceGrpc.newBlockingStub(managedChannel);
    }
}
