package org.example.planningservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchPlacesResponseDTO {
    private List<PlaceDTO> results;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDTO {
        private String placeId;
        private String name;
        private String vicinity;
        private double rating;
        private int userRatingsTotal;
        private LocationDTO geometryLocation;
        private OpeningHoursDTO openingHours;
        private List<PhotoDTO> photos;
        private List<String> photoUrls;
        private List<String> types;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoDTO {
        private String photoReference;
        private int height;
        private int width;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpeningHoursDTO {
        private boolean openNow;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private double lat;
        private double lng;
    }
}
