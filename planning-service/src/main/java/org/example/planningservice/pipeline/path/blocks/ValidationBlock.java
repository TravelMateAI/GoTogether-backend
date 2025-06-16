package org.example.planningservice.pipeline.path.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.DirectionsRequestDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidationBlock implements Block<DirectionsRequestDTO, DirectionsRequestDTO> {

    @Override
    public DirectionsRequestDTO process(DirectionsRequestDTO routeRequest) {
        if (routeRequest == null || routeRequest.isBlank()) {
            throw new IllegalArgumentException("Invalid input data!");
        }

        log.info("✅ Validation Passed: {}", routeRequest);
        return routeRequest;
    }
}