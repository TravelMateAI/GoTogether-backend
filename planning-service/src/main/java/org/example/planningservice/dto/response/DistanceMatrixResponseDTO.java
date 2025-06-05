package org.example.planningservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistanceMatrixResponseDTO {
    private List<String> originAddresses;
    private List<String> destinationAddresses;
    private List<RowDTO> rows;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowDTO {
        private List<ElementDTO> elements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElementDTO {
        private String status;
        private DirectionsResponseDTO.DurationDTO duration;
        private DirectionsResponseDTO.DistanceDTO distance;
    }
}
