package org.example.planningservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectionsRequestDTO {
    private String origin;
    private String destination;

    public boolean isBlank() {
        return origin == null || origin.isBlank()
                || destination == null || destination.isBlank();
    }
}