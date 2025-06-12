package org.example.planningservice.pipelines.path;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dtos.RouteDTO;
import org.example.planningservice.dtos.RouteRequestDTO;
import org.example.planningservice.frameworks.pipeline.Pipe;
import org.example.planningservice.pipelines.path.blocks.DataTransformationBlock;
import org.example.planningservice.pipelines.path.blocks.StorageBlock;
import org.example.planningservice.pipelines.path.blocks.ValidationBlock;
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

    public RouteDTO execute(RouteRequestDTO routeRequest) {
        Pipe<RouteRequestDTO, RouteDTO> pathFindingPipe = createPathFindingPipe();
        log.info("=== created the pipeline (not executed yet) ===");

        // Execute the pipeline
        return pathFindingPipe.execute(routeRequest);
    }

    private Pipe<RouteRequestDTO, RouteDTO> createPathFindingPipe() {
        return new Pipe<>(validationBlock)
                .connect(dataTransformationBlock)
                .connect(storageBlock);
    }
}