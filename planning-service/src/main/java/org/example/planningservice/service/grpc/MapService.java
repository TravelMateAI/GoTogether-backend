package org.example.planningservice.service.grpc;

import org.example.planningservice.dto.request.*;
import org.example.planningservice.dto.response.*;

public interface MapService {
    GeocodeResponseDTO geocode(GeocodeRequestDTO request);
    SearchPlacesResponseDTO searchPlaces(SearchPlacesRequestDTO request);
    PlaceDetailsResponseDTO getPlaceDetails(String placeId);
    DirectionsResponseDTO getDirections(DirectionsRequestDTO request);
    DistanceMatrixResponseDTO getDistanceMatrix(DistanceMatrixRequestDTO request);
}
