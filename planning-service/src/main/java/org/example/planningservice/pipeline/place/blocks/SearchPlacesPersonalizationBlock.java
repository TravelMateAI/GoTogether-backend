//package org.example.planningservice.pipeline.place.blocks;
//
//import lombok.extern.slf4j.Slf4j;
//import org.example.planningservice.dto.request.GeminiRequestDTO;
//import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
//import org.example.planningservice.dto.response.GeminiResponseDTO;
//import org.example.planningservice.framework.pipeline.Block;
//import org.example.planningservice.service.grpc.GeminiService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class SearchPlacesPersonalizationBlock implements Block<SearchPlacesRequestDTO, SearchPlacesRequestDTO> {
//
//    private final UserService userService; // To get user details
//    private final GeminiService geminiService;
//
//    @Autowired
//    public SearchPlacesPersonalizationBlock(UserService userService, GeminiService geminiService) {
//        this.userService = userService;
//        this.geminiService = geminiService;
//    }
//
//    @Override
//    public SearchPlacesRequestDTO process(SearchPlacesRequestDTO request) {
//        log.info("🤖 Personalizing search query with Gemini...");
//
//        // 1. Get user data
//        String userId = "123"; // 🔄 This could come from context/session/request
//        User user = userService.getUserWithHistory(userId);
//
//        // 2. Build prompt for Gemini
//        String prompt = String.format(
//                "The user previously visited: %s. They are currently searching in %s. Suggest a more specific or relevant query instead of '%s' for Google Maps Place Search API.",
//                String.join(", ", user.getPreviousPlaces()),
//                request.getLocation(),
//                request.getQuery()
//        );
//
//        // 3. Call Gemini AI
//        GeminiRequestDTO geminiRequest = new GeminiRequestDTO(prompt);
//        GeminiResponseDTO response = geminiService.generateContent(geminiRequest);
//
//        String customizedQuery = response.getGeneratedText();
//        log.info("🎯 Gemini-customized query: {}", customizedQuery);
//
//        // 4. Replace original query
//        request.setQuery(customizedQuery);
//        return request;
//    }
//}
