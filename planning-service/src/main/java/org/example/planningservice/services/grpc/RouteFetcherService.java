package org.example.planningservice.services.grpc;

import io.grpc.ManagedChannel;
import org.example.planningservice.dtos.RouteDTO;
import org.example.planningservice.dtos.RouteRequestDTO;
import org.springframework.stereotype.Service;
import route.DirectionsServiceGrpc;
import route.Route;


@Service
public class RouteFetcherService {

    private final DirectionsServiceGrpc.DirectionsServiceBlockingStub directionsStub;

    public RouteFetcherService(ManagedChannel channel) {
        this.directionsStub = DirectionsServiceGrpc.newBlockingStub(channel);
    }

    public RouteDTO fetchRoute(RouteRequestDTO routeRequest) {
        Route.RouteRequest request = Route.RouteRequest.newBuilder()
                .setOrigin(routeRequest.getOrigin())
                .setDestination(routeRequest.getDestination())
                .build();

        Route.RouteResponse response = directionsStub.getRoute(request);

        return new RouteDTO(
                response.getPolyline(),
                response.getDistance(),
                response.getDuration()
        );
    }
}

