package org.example.planningservice.service.grpc.impl;

import org.example.planningservice.dto.request.*;
import org.example.planningservice.dto.response.*;
import org.example.planningservice.grpc.apiservice.maps.*;
import org.example.planningservice.grpc.client.MapsClient;
import org.example.planningservice.service.grpc.MapService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapServiceImpl implements MapService {

    private final MapsClient mapsClient;

    public MapServiceImpl(MapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public GeocodeResponseDTO geocode(GeocodeRequestDTO request) {
        GeocodeResponse response = mapsClient.geocode(request.getAddress());
        List<GeocodeResponseDTO.ResultDTO> results = response.getResultsList().stream()
                .map(r -> new GeocodeResponseDTO.ResultDTO(
                        r.getFormattedAddress(),
                        new GeocodeResponseDTO.LocationDTO(
                                r.getLocation().getLat(),
                                r.getLocation().getLng()
                        )
                )).collect(Collectors.toList());
        return new GeocodeResponseDTO(results, response.getStatus());
    }

    @Override
    public SearchPlacesResponseDTO searchPlaces(SearchPlacesRequestDTO request) {
        SearchPlacesResponse response = mapsClient.searchPlaces(request.getQuery(), request.getLocation());
        List<SearchPlacesResponseDTO.PlaceDTO> places = response.getResultsList().stream().map(place -> {
            SearchPlacesResponseDTO.LocationDTO loc = new SearchPlacesResponseDTO.LocationDTO(
                    place.getGeometryLocation().getLat(),
                    place.getGeometryLocation().getLng()
            );
            List<SearchPlacesResponseDTO.PhotoDTO> photos = place.getPhotosList().stream().map(photo ->
                    new SearchPlacesResponseDTO.PhotoDTO(
                            photo.getPhotoReference(),
                            photo.getHeight(),
                            photo.getWidth()
                    )).collect(Collectors.toList());
            SearchPlacesResponseDTO.OpeningHoursDTO openingHours = new SearchPlacesResponseDTO.OpeningHoursDTO(
                    place.getOpeningHours().getOpenNow()
            );
            return new SearchPlacesResponseDTO.PlaceDTO(
                    place.getPlaceId(),
                    place.getName(),
                    place.getVicinity(),
                    place.getRating(),
                    place.getUserRatingsTotal(),
                    loc,
                    openingHours,
                    photos,
                    place.getPhotoUrlsList(),
                    place.getTypesList()
            );
        }).collect(Collectors.toList());
        return new SearchPlacesResponseDTO(places, response.getStatus());
    }

    @Override
    public PlaceDetailsResponseDTO getPlaceDetails(String placeId) {
        PlaceDetailsResponse response = mapsClient.getPlaceDetails(placeId);
        Place place = response.getResult();
        SearchPlacesResponseDTO.PlaceDTO placeDTO = new SearchPlacesResponseDTO.PlaceDTO(
                place.getPlaceId(),
                place.getName(),
                place.getVicinity(),
                place.getRating(),
                place.getUserRatingsTotal(),
                new SearchPlacesResponseDTO.LocationDTO(
                        place.getGeometryLocation().getLat(),
                        place.getGeometryLocation().getLng()
                ),
                new SearchPlacesResponseDTO.OpeningHoursDTO(
                        place.getOpeningHours().getOpenNow()
                ),
                place.getPhotosList().stream().map(photo ->
                        new SearchPlacesResponseDTO.PhotoDTO(
                                photo.getPhotoReference(),
                                photo.getHeight(),
                                photo.getWidth()
                        )).collect(Collectors.toList()),
                place.getPhotoUrlsList(),
                place.getTypesList()
        );
        return new PlaceDetailsResponseDTO(placeDTO, response.getStatus());
    }

    @Override
    public DirectionsResponseDTO getDirections(DirectionsRequestDTO request) {
        DirectionsResponse response = mapsClient.getDirections(request.getOrigin(), request.getDestination());

        List<DirectionsResponseDTO.RouteDTO> routes = response.getRoutesList().stream().map(route -> {
            DirectionsResponseDTO.PolylineDTO overview = new DirectionsResponseDTO.PolylineDTO(route.getOverviewPolyline().getPoints());
            List<DirectionsResponseDTO.LegDTO> legs = route.getLegsList().stream().map(leg -> {
                List<DirectionsResponseDTO.StepDTO> steps = leg.getStepsList().stream().map(step ->
                        new DirectionsResponseDTO.StepDTO(
                                step.getHtmlInstructions(),
                                new DirectionsResponseDTO.DistanceDTO(step.getDistance().getText(), step.getDistance().getValue()),
                                new DirectionsResponseDTO.DurationDTO(step.getDuration().getText(), step.getDuration().getValue()),
                                new DirectionsResponseDTO.PolylineDTO(step.getPolyline().getPoints())
                        )).collect(Collectors.toList());
                return new DirectionsResponseDTO.LegDTO(
                        new DirectionsResponseDTO.DistanceDTO(leg.getDistance().getText(), leg.getDistance().getValue()),
                        new DirectionsResponseDTO.DurationDTO(leg.getDuration().getText(), leg.getDuration().getValue()),
                        leg.getStartAddress(),
                        leg.getEndAddress(),
                        steps
                );
            }).collect(Collectors.toList());
            return new DirectionsResponseDTO.RouteDTO(route.getSummary(), legs, overview);
        }).collect(Collectors.toList());

        List<DirectionsResponseDTO.GeocodedWaypointDTO> waypoints = response.getGeocodedWaypointsList().stream()
                .map(wp -> new DirectionsResponseDTO.GeocodedWaypointDTO(wp.getGeocoderStatus(), wp.getPlaceId(), wp.getTypesList()))
                .collect(Collectors.toList());

        return new DirectionsResponseDTO(routes, waypoints, response.getStatus());
    }

    @Override
    public DistanceMatrixResponseDTO getDistanceMatrix(DistanceMatrixRequestDTO request) {
        DistanceMatrixResponse response = mapsClient.getDistanceMatrix(request.getOrigins(), request.getDestinations());
        List<DistanceMatrixResponseDTO.RowDTO> rows = response.getRowsList().stream().map(row -> {
            List<DistanceMatrixResponseDTO.ElementDTO> elements = row.getElementsList().stream().map(el ->
                    new DistanceMatrixResponseDTO.ElementDTO(
                            el.getStatus(),
                            new DirectionsResponseDTO.DurationDTO(el.getDuration().getText(), el.getDuration().getValue()),
                            new DirectionsResponseDTO.DistanceDTO(el.getDistance().getText(), el.getDistance().getValue())
                    )).collect(Collectors.toList());
            return new DistanceMatrixResponseDTO.RowDTO(elements);
        }).collect(Collectors.toList());
        return new DistanceMatrixResponseDTO(
                response.getOriginAddressesList(),
                response.getDestinationAddressesList(),
                rows,
                response.getStatus()
        );
    }
}