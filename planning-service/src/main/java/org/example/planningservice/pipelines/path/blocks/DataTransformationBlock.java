package org.example.planningservice.pipelines.path.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dtos.RouteDTO;
import org.example.planningservice.dtos.RouteRequestDTO;
import org.example.planningservice.frameworks.pipeline.Block;
import org.example.planningservice.services.grpc.RouteFetcherService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class DataTransformationBlock implements Block<RouteRequestDTO, RouteDTO> {

    private final RouteFetcherService routeFetcherService;

    public DataTransformationBlock(RouteFetcherService routeFetcherService) {
        this.routeFetcherService = routeFetcherService;
    }

    @Override
    public RouteDTO process(RouteRequestDTO routeRequest) {
        RouteDTO routeDTO = routeFetcherService.fetchRoute(routeRequest);

        if (routeDTO == null || routeDTO.isBlank()) {
            throw new IllegalArgumentException("Invalid retrieved data!");
        }
        return routeDTO;
    }
}