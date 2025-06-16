package org.example.planningservice.pipeline.place.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchPlacesValidationBlock implements Block<SearchPlacesRequestDTO, SearchPlacesRequestDTO> {

    @Override
    public SearchPlacesRequestDTO process(SearchPlacesRequestDTO request) {
        if (request == null || request.getLocation() == null || request.getLocation().isBlank()) {
            throw new IllegalArgumentException("Invalid search request: missing query or location.");
        }
        log.info("✅ Search request validated: {}", request);
        return request;
    }
}
