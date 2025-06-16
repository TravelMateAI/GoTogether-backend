package org.example.planningservice.pipeline.place.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.UserTripHistoryDTO;
import org.example.planningservice.dto.request.GeminiRequestDTO;
import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
import org.example.planningservice.dto.response.GeminiResponseDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.example.planningservice.service.grpc.GeminiService;
import org.example.planningservice.service.rest.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SearchPlacesPersonalizationBlock implements Block<SearchPlacesRequestDTO, SearchPlacesRequestDTO> {

    private final UserServiceClient userServiceClient;
    private final GeminiService geminiService;

    @Autowired
    public SearchPlacesPersonalizationBlock(UserServiceClient userServiceClient, GeminiService geminiService) {
        this.userServiceClient = userServiceClient;
        this.geminiService = geminiService;
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
                "User previously visited: %s. Current location is %s. Rewrite '%s' into " +
                        "a more customized place search query for Google Maps API.",
                String.join(", ", visited),
                request.getLocation(),
                request.getQuery()
        );

//        GeminiRequestDTO geminiRequest = new GeminiRequestDTO(prompt);
//        GeminiResponseDTO response = geminiService.generateContent(geminiRequest);
//
//        String customizedQuery = response.getGeneratedContent();
//        log.info("🤖 Gemini-customized query: {}", customizedQuery);
//
//        request.setQuery(customizedQuery);
        return request;
    }
}

