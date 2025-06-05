package com.example.planning.grpc.client;

import com.example.planning.grpc.apiservice.gemini.GeminiGrpc;
import com.example.planning.grpc.apiservice.gemini.GeminiRequest;
import com.example.planning.grpc.apiservice.gemini.GeminiResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiClient {

    private ManagedChannel channel;
    private GeminiGrpc.GeminiBlockingStub blockingStub;

    @Value("${api.service.host:localhost}")
    private String apiServiceHost;

    @Value("${api.service.port:50051}")
    private int apiServicePort;

    @PostConstruct
    private void init() {
        channel = ManagedChannelBuilder.forAddress(apiServiceHost, apiServicePort)
                .usePlaintext()
                .build();
        blockingStub = GeminiGrpc.newBlockingStub(channel);
    }

    public GeminiResponse generateContent(String prompt) {
        GeminiRequest request = GeminiRequest.newBuilder().setPrompt(prompt).build();
        return blockingStub.generateContent(request);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
