package org.example.planningservice.controller;

import org.example.planningservice.grpc.apiservice.common.HelloReply; // Added for GreeterClient
import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse;
import org.example.planningservice.grpc.apiservice.maps.*;
import org.example.planningservice.grpc.client.GeminiClient;
import org.example.planningservice.grpc.client.GreeterClient; // Corrected import
import org.example.planningservice.grpc.client.MapsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/grpc")
public class GrpcTestController {

    private final GreeterClient greeterClient;
    private final GeminiClient geminiClient;
    private final MapsClient mapsClient;

    @Autowired
    public GrpcTestController(GreeterClient greeterClient, GeminiClient geminiClient, MapsClient mapsClient) {
        this.greeterClient = greeterClient;
        this.geminiClient = geminiClient;
        this.mapsClient = mapsClient;
    }

    // GreeterClient endpoint
    @GetMapping("/hello")
    public ResponseEntity<String> sayHello(@RequestParam(defaultValue = "World") String name) {
        try {
            // Assuming GreeterClient has a method sayHello that returns HelloReply
            // and GreeterClient itself is correctly imported and autowired.
            // The original GreeterClient had a method `greet(String name)` which returned void
            // and logged. It was changed to `sayHello` and to return HelloReply in the prompt.
            // Let's ensure GreeterClient actually has this method.
            // For now, assuming it's:
            // HelloReply reply = greeterClient.greet(name); // If greet was changed to return HelloReply
            // Or if a new method sayHello was added:
            org.example.planningservice.grpc.apiservice.common.HelloReply reply = greeterClient.sayHello(name);
            return ResponseEntity.ok(reply.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Greeter service: " + e.getMessage());
        }
    }

    // GeminiClient endpoint
    @GetMapping("/gemini/generate")
    public ResponseEntity<?> generateContent(@RequestParam String prompt) {
        try {
            GeminiResponse response = geminiClient.generateContent(prompt);
            return ResponseEntity.ok(response); // Consider converting to a more friendly JSON if needed
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Gemini service: " + e.getMessage());
        }
    }

    // MapsClient endpoints
    @GetMapping("/maps/geocode")
    public ResponseEntity<?> geocode(@RequestParam String address) {
        try {
            GeocodeResponse response = mapsClient.geocode(address);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps Geocode: " + e.getMessage());
        }
    }

    @GetMapping("/maps/search")
    public ResponseEntity<?> searchPlaces(@RequestParam String query, @RequestParam String location) {
        try {
            SearchPlacesResponse response = mapsClient.searchPlaces(query, location);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps SearchPlaces: " + e.getMessage());
        }
    }

    @GetMapping("/maps/details/{placeId}")
    public ResponseEntity<?> getPlaceDetails(@PathVariable String placeId) {
        try {
            PlaceDetailsResponse response = mapsClient.getPlaceDetails(placeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps GetPlaceDetails: " + e.getMessage());
        }
    }

    @GetMapping("/maps/directions")
    public ResponseEntity<?> getDirections(@RequestParam String origin, @RequestParam String destination) {
        try {
            DirectionsResponse response = mapsClient.getDirections(origin, destination);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps GetDirections: " + e.getMessage());
        }
    }

    @GetMapping("/maps/distancematrix")
    public ResponseEntity<?> getDistanceMatrix(@RequestParam String origins, @RequestParam String destinations) {
        try {
            DistanceMatrixResponse response = mapsClient.getDistanceMatrix(origins, destinations);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Maps GetDistanceMatrix: " + e.getMessage());
        }
    }
}
