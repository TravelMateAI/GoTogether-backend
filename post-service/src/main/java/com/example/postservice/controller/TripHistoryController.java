package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.UserTripHistoryDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripHistoryController {

    @GetMapping("/history/{userId}")
    public UserTripHistoryDTO getUserTripHistory(@PathVariable String userId) {
        // 👉 Simulate history fetched from DB
        List<String> history = List.of("temples", "beaches", "mountains");

        return new UserTripHistoryDTO(userId, history);
    }
}