package org.example.planningservice.pipelines.path.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dtos.RouteRequestDTO;
import org.example.planningservice.frameworks.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidationBlock implements Block<RouteRequestDTO, RouteRequestDTO> {

    @Override
    public RouteRequestDTO process(RouteRequestDTO routeRequest) {
        if (routeRequest == null || routeRequest.isBlank()) {
            throw new IllegalArgumentException("Invalid input data!");
        }

        log.info("✅ Validation Passed: " + routeRequest);
        return routeRequest;
    }
}