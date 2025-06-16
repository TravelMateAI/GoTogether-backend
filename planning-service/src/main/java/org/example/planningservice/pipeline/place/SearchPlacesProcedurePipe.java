package org.example.planningservice.pipeline.place;

import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
import org.example.planningservice.dto.response.SearchPlacesResponseDTO;
import org.example.planningservice.framework.pipeline.Pipe;
import org.example.planningservice.pipeline.place.blocks.SearchPlacesStorageBlock;
import org.example.planningservice.pipeline.place.blocks.SearchPlacesTransformationBlock;
import org.example.planningservice.pipeline.place.blocks.SearchPlacesValidationBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchPlacesProcedurePipe {

    private final SearchPlacesValidationBlock validationBlock;
    private final SearchPlacesTransformationBlock transformationBlock;
    private final SearchPlacesStorageBlock storageBlock;

    @Autowired
    public SearchPlacesProcedurePipe(
            SearchPlacesValidationBlock validationBlock,
            SearchPlacesTransformationBlock transformationBlock,
            SearchPlacesStorageBlock storageBlock) {
        this.validationBlock = validationBlock;
        this.transformationBlock = transformationBlock;
        this.storageBlock = storageBlock;
    }

    public SearchPlacesResponseDTO execute(SearchPlacesRequestDTO input) {
        Pipe<SearchPlacesRequestDTO, SearchPlacesResponseDTO> pipeline =
                new Pipe<>(validationBlock)
                        .connect(transformationBlock)
                        .connect(storageBlock);

        return pipeline.execute(input);
    }
}
