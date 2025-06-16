package org.example.planningservice.pipeline.path.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.DirectionsRequestDTO;
import org.example.planningservice.dto.response.DirectionsResponseDTO;
import org.example.planningservice.exception.DirectionsServiceException;
import org.example.planningservice.framework.pipeline.Block;
import org.example.planningservice.service.grpc.MapService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class DataTransformationBlock implements Block<DirectionsRequestDTO, DirectionsResponseDTO> {

    private final MapService mapService;

    public DataTransformationBlock(MapService mapService) {
        this.mapService = mapService;
    }

    @Override
    public DirectionsResponseDTO process(DirectionsRequestDTO routeRequest) {
        try {
            DirectionsResponseDTO response = mapService.getDirections(routeRequest);

            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("Invalid retrieved data!");
            }

            log.info("🚌 Data Transformation Completed: {}", response);

            return response;

        } catch (Exception e) {
            throw new DirectionsServiceException("Error calling Maps GetDirections: " + e.getMessage());
        }
    }
}