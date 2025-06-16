package org.example.planningservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.DirectionsRequestDTO;
import org.example.planningservice.dto.request.SearchPlacesRequestDTO;
import org.example.planningservice.dto.response.DirectionsResponseDTO;
import org.example.planningservice.dto.response.SearchPlacesResponseDTO;
import org.example.planningservice.exception.DirectionsServiceException;
import org.example.planningservice.pipeline.path.PathFindingPipe;
import org.example.planningservice.pipeline.place.SearchPlacesProcedurePipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/pipeline")
public class PipelineController {


    private final PathFindingPipe pathFindingPipe;
    private final SearchPlacesProcedurePipe procedurePipe;

    @Autowired
    public PipelineController(PathFindingPipe pathFindingPipe, SearchPlacesProcedurePipe procedurePipe) {
        this.pathFindingPipe = pathFindingPipe;
        this.procedurePipe = procedurePipe;
    }

    @GetMapping("/path")
    public ResponseEntity<?> executePipeline(@RequestBody DirectionsRequestDTO routeRequest) {

        try {
            DirectionsResponseDTO response = pathFindingPipe.execute(routeRequest);
            return ResponseEntity.ok(response);
        } catch (DirectionsServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPlaces(@RequestParam String userId, @RequestParam String location) {
        try {
            SearchPlacesRequestDTO request = new SearchPlacesRequestDTO();
            request.setUserId(userId);
            request.setLocation(location);

            SearchPlacesResponseDTO response = procedurePipe.execute(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Error: " + e.getMessage());
        }
    }
}