package org.example.planningservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteRequestDTO {

    private String origin;
    private String destination;

    public boolean isBlank() {
        return origin == null || origin.isBlank()
                || destination == null || destination.isBlank();
    }

}
