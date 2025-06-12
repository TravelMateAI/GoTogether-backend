package org.example.planningservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteDTO {

    private String polyline;
    private String distance;
    private String duration;

    @JsonIgnore
    public boolean isBlank() {
        return polyline == null || polyline.isBlank()
                || distance == null || distance.isBlank()
                || duration == null || duration.isBlank();
    }

}

