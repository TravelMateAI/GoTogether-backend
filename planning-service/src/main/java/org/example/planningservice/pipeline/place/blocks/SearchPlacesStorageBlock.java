package org.example.planningservice.pipeline.place.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.response.SearchPlacesResponseDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchPlacesStorageBlock implements Block<SearchPlacesResponseDTO, SearchPlacesResponseDTO> {

    @Override
    public SearchPlacesResponseDTO process(SearchPlacesResponseDTO response) {
        log.info("💾 Search results stored: {}", response.getResults().size());
        // Implement DB store logic if needed
        return response;
    }
}
