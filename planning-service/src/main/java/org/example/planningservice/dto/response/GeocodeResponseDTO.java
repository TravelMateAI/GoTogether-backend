package org.example.planningservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponseDTO {
    private List<ResultDTO> results;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultDTO {
        private String formattedAddress;
        private LocationDTO location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private double lat;
        private double lng;
    }
}