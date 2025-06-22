package org.example.planningservice.pipeline.place.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.GeminiHttpResponse;
import org.example.planningservice.dto.UserTripHistoryDTO;
import org.example.planningservice.dto.request.GeminiRequestDTO;
import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
import org.example.planningservice.dto.response.GeminiResponseDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.example.planningservice.service.grpc.GeminiService;
import org.example.planningservice.service.rest.GeminiServiceClient;
import org.example.planningservice.service.rest.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SearchPlacesPersonalizationBlock implements Block<SearchPlacesRequestDTO, SearchPlacesRequestDTO> {

    private final UserServiceClient userServiceClient;
    private final GeminiServiceClient geminiServiceClient;

    @Autowired
    public SearchPlacesPersonalizationBlock(UserServiceClient userServiceClient, GeminiServiceClient geminiServiceClient) {
        this.userServiceClient = userServiceClient;
        this.geminiServiceClient = geminiServiceClient;
    }

    @Override
    public SearchPlacesRequestDTO process(SearchPlacesRequestDTO request) {
        log.info("🔍 Fetching user history for personalization...");

        UserTripHistoryDTO history = userServiceClient.getUserTripHistory(request.getUserId());
        List<String> visited = history.getPreviouslyVisitedPlaces();
        log.info("📜 User previously visited: {}", String.join(", ", visited));

        if (visited.isEmpty()) {
            log.warn("⚠️ No previous places found for user. Skipping personalization.");
            return request; // No personalization possible
        }

        request.setQuery("visitplaces");
        // Use Gemini to generate a smarter query
        String prompt = String.format(
                "The user has previously visited: %s. They're currently searching in %s. " +
                        "Generate only one customized query string that can be used directly in the Google Maps Place Search API. " +
                        "Output only the query, with no explanation, markdown, or additional formatting.Do not URL encode it.",
                String.join(", ", visited),
                request.getLocation(),
                request.getQuery()
        );

        GeminiHttpResponse response = geminiServiceClient.getAIResponse(prompt);

        String customizedQuery = extractText(response);
        log.info("🤖 Gemini-customized query: {}", customizedQuery);

        request.setQuery(customizedQuery);
        return request;
    }


    private String extractText(GeminiHttpResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            return null;
        }

        GeminiHttpResponse.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
            return null;
        }

        return candidate.getContent().getParts().get(0).getText().trim().replaceAll("\\s+", "");
    }
}

