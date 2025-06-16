package org.example.planningservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectionsResponseDTO {
    private List<RouteDTO> routes;
    private List<GeocodedWaypointDTO> geocodedWaypoints;
    private String status;

    @JsonIgnore
    public boolean isBlank() {
        if (!"OK".equalsIgnoreCase(status)) return true;
        if (routes == null || routes.isEmpty()) return true;

        RouteDTO firstRoute = routes.get(0);
        if (firstRoute.getLegs() == null || firstRoute.getLegs().isEmpty()) return true;
        if (firstRoute.getOverviewPolyline() == null ||
                firstRoute.getOverviewPolyline().getPoints() == null ||
                firstRoute.getOverviewPolyline().getPoints().isBlank()) return true;

        return false; // Not blank — has valid content
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteDTO {
        private String summary;
        private List<LegDTO> legs;
        private PolylineDTO overviewPolyline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegDTO {
        private DistanceDTO distance;
        private DurationDTO duration;
        private String startAddress;
        private String endAddress;
        private List<StepDTO> steps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDTO {
        private String htmlInstructions;
        private DistanceDTO distance;
        private DurationDTO duration;
        private PolylineDTO polyline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolylineDTO {
        private String points;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistanceDTO {
        private String text;
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationDTO {
        private String text;
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodedWaypointDTO {
        private String geocoderStatus;
        private String placeId;
        private List<String> types;
    }
}