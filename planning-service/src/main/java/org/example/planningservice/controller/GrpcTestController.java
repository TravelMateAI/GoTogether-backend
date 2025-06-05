package org.example.planningservice.controller;


import org.example.planningservice.dto.request.*;
import org.example.planningservice.dto.response.*;
import org.example.planningservice.service.grpc.MapService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.example.planningservice.grpc.client.GreeterClient;
import org.example.planningservice.grpc.client.GeminiClient;
import org.example.planningservice.grpc.apiservice.common.HelloReply;
import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse;


@RestController
@RequestMapping("/test/grpc")
public class GrpcTestController {

    private final GreeterClient greeterClient;
    private final GeminiClient geminiClient;
    private final MapService mapService;

    @Autowired
    public GrpcTestController(GreeterClient greeterClient, GeminiClient geminiClient, MapService mapService) {
        this.greeterClient = greeterClient;
        this.geminiClient = geminiClient;
        this.mapService = mapService;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello(@RequestParam(defaultValue = "World") String name) {
        try {
            HelloReply reply = greeterClient.sayHello(name);
            return ResponseEntity.ok(reply.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Greeter service: " + e.getMessage());
        }
    }

    @GetMapping("/gemini/generate")
    public ResponseEntity<?> generateContent(@RequestParam String prompt) {
        try {
            GeminiResponse response = geminiClient.generateContent(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Gemini service: " + e.getMessage());
        }
    }

    @GetMapping("/maps/geocode")
    public ResponseEntity<?> geocode(@RequestParam String address) {
        try {
            GeocodeResponseDTO response = mapService.geocode(new GeocodeRequestDTO(address));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps Geocode: " + e.getMessage());
        }
    }

    @GetMapping("/maps/search")
    public ResponseEntity<?> searchPlaces(@RequestParam String query, @RequestParam String location) {
        try {
            SearchPlacesResponseDTO response = mapService.searchPlaces(new SearchPlacesRequestDTO(query, location));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps SearchPlaces: " + e.getMessage());
        }
    }

    @GetMapping("/maps/details/{placeId}")
    public ResponseEntity<?> getPlaceDetails(@PathVariable String placeId) {
        try {
            PlaceDetailsResponseDTO response = mapService.getPlaceDetails(placeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps GetPlaceDetails: " + e.getMessage());
        }
    }

    @GetMapping("/maps/directions")
    public ResponseEntity<?> getDirections(@RequestParam String origin, @RequestParam String destination) {
        try {
            DirectionsResponseDTO response = mapService.getDirections(new DirectionsRequestDTO(origin, destination));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps GetDirections: " + e.getMessage());
        }
    }

    @GetMapping("/maps/distancematrix")
    public ResponseEntity<?> getDistanceMatrix(@RequestParam String origins, @RequestParam String destinations) {
        try {
            DistanceMatrixResponseDTO response = mapService.getDistanceMatrix(new DistanceMatrixRequestDTO(origins, destinations));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps GetDistanceMatrix: " + e.getMessage());
        }
    }
}
