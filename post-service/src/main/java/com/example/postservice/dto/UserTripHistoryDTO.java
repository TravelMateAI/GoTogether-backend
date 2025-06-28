package com.example.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTripHistoryDTO {
    private String userId;
    private List<String> previouslyVisitedPlaces;
}
