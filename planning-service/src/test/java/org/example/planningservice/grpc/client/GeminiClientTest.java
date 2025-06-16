package org.example.planningservice.grpc.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.example.planningservice.PlanningServiceApplication;
import org.example.planningservice.grpc.client.GeminiClient; // Updated import

@SpringBootTest(classes = PlanningServiceApplication.class)
public class GeminiClientTest {

    @Autowired
    private GeminiClient geminiClient;

    @Test
    void contextLoads() {
        assertNotNull(geminiClient, "GeminiClient should be loaded from Spring context");
    }

    @Test
    void testGenerateContent_Placeholder() {
        // This is a placeholder test.
        // In a real scenario, you would mock the gRPC service or use a test server.
        // For now, we just check if the client is available.
        assertNotNull(geminiClient, "GeminiClient is available");
        // Example of how you might call it (would require a running/mocked server):
        // String prompt = "Test prompt";
        // org.example.planningservice.grpc.apiservice.gemini.GeminiResponse response = geminiClient.generateContent(prompt);
        // assertNotNull(response);
    }
}
