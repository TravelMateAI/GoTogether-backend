package com.example.planning.grpc.client;

import com.example.planning.grpc.apiservice.maps.*; // Import all maps grpc classes
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Service
public class MapsClient {

    private ManagedChannel channel;
    private MapsGrpc.MapsBlockingStub blockingStub;

    @Value("${api.service.host:localhost}")
    private String apiServiceHost;

    @Value("${api.service.port:50051}")
    private int apiServicePort;

    @PostConstruct
    private void init() {
        channel = ManagedChannelBuilder.forAddress(apiServiceHost, apiServicePort)
                .usePlaintext()
                .build();
        blockingStub = MapsGrpc.newBlockingStub(channel);
    }

    public GeocodeResponse geocode(String address) {
        GeocodeRequest request = GeocodeRequest.newBuilder().setAddress(address).build();
        return blockingStub.geocode(request);
    }

    public SearchPlacesResponse searchPlaces(String query, String location) {
        SearchPlacesRequest request = SearchPlacesRequest.newBuilder()
                                            .setQuery(query)
                                            .setLocation(location)
                                            .build();
        return blockingStub.searchPlaces(request);
    }

    public PlaceDetailsResponse getPlaceDetails(String placeId) {
        PlaceDetailsRequest request = PlaceDetailsRequest.newBuilder().setPlaceId(placeId).build();
        return blockingStub.getPlaceDetails(request);
    }

    public DirectionsResponse getDirections(String origin, String destination) {
        DirectionsRequest request = DirectionsRequest.newBuilder()
                                          .setOrigin(origin)
                                          .setDestination(destination)
                                          .build();
        return blockingStub.getDirections(request);
    }

    public DistanceMatrixResponse getDistanceMatrix(String origins, String destinations) {
        DistanceMatrixRequest request = DistanceMatrixRequest.newBuilder()
                                            .setOrigins(origins)
                                            .setDestinations(destinations)
                                            .build();
        return blockingStub.getDistanceMatrix(request);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
