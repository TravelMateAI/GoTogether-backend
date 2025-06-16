package org.example.planningservice.pipeline.place.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
import org.example.planningservice.dto.response.SearchPlacesResponseDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.example.planningservice.service.grpc.MapService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchPlacesTransformationBlock implements Block<SearchPlacesRequestDTO, SearchPlacesResponseDTO> {

    private final MapService mapService;

    public SearchPlacesTransformationBlock(MapService mapService) {
        this.mapService = mapService;
    }

    @Override
    public SearchPlacesResponseDTO process(SearchPlacesRequestDTO request) {
        try {
            SearchPlacesResponseDTO response = mapService.searchPlaces(request);
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                throw new IllegalArgumentException("No places found.");
            }
            log.info("🔄 Places retrieved: {}", response.getResults().size());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error calling mapService.searchPlaces: " + e.getMessage(), e);
        }
    }
}
