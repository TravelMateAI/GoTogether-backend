package org.example.planningservice.pipeline.path;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.DirectionsRequestDTO;
import org.example.planningservice.dto.response.DirectionsResponseDTO;
import org.example.planningservice.framework.pipeline.Pipe;
import org.example.planningservice.pipeline.path.blocks.DataTransformationBlock;
import org.example.planningservice.pipeline.path.blocks.StorageBlock;
import org.example.planningservice.pipeline.path.blocks.ValidationBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PathFindingPipe {

    @Autowired
    private DataTransformationBlock dataTransformationBlock;

    @Autowired
    private StorageBlock storageBlock;

    @Autowired
    private ValidationBlock validationBlock;

    public DirectionsResponseDTO execute(DirectionsRequestDTO routeRequest) {
        Pipe<DirectionsRequestDTO, DirectionsResponseDTO> pathFindingPipe = createPathFindingPipe();
        log.info("=== created the pipeline (not executed yet) ===");

        // Execute the pipeline
        return pathFindingPipe.execute(routeRequest);
    }

    private Pipe<DirectionsRequestDTO, DirectionsResponseDTO> createPathFindingPipe() {
        return new Pipe<>(validationBlock)
                .connect(dataTransformationBlock)
                .connect(storageBlock);
    }
}