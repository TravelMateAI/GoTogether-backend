package org.example.planningservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailsResponseDTO {
    private SearchPlacesResponseDTO.PlaceDTO result;
    private String status;
}