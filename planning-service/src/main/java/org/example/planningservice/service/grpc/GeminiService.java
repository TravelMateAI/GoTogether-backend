package org.example.planningservice.service.grpc;

import org.example.planningservice.dto.request.GeminiRequestDTO;
import org.example.planningservice.dto.response.GeminiResponseDTO;

public interface GeminiService {
    GeminiResponseDTO generateContent(GeminiRequestDTO request);
}
