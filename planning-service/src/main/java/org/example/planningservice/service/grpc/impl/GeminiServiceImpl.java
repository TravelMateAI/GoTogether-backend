package org.example.planningservice.service.grpc.impl;

import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse; // Corrected import from proto
import org.example.planningservice.grpc.client.GeminiClient;
import org.example.planningservice.dto.request.GeminiRequestDTO;
import org.example.planningservice.dto.response.GeminiResponseDTO;
import org.example.planningservice.service.grpc.GeminiService;
import org.springframework.stereotype.Service;

@Service
public class GeminiServiceImpl implements GeminiService {

    private final GeminiClient geminiClient;

    public GeminiServiceImpl(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    @Override
    public GeminiResponseDTO generateContent(GeminiRequestDTO request) {
        GeminiResponse response = geminiClient.generateContent(request.getPrompt()); // Corrected type
        // The existing mapping logic seems compatible with the proto structure.
        // For now, let's assume a simple getText() or similar method.
        // If the actual response is more complex, this mapping will need to be more sophisticated.
        String generatedText = "";
        if (response != null && response.getCandidatesCount() > 0 &&
            response.getCandidates(0).hasContent() && // Check if content exists
            response.getCandidates(0).getContent().getPartsCount() > 0) {
            generatedText = response.getCandidates(0).getContent().getParts(0).getText();
        }
        return new GeminiResponseDTO(generatedText);
    }
}
