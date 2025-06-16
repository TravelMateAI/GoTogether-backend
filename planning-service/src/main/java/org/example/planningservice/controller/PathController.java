package org.example.planningservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.request.DirectionsRequestDTO;
import org.example.planningservice.dto.response.DirectionsResponseDTO;
import org.example.planningservice.exception.DirectionsServiceException;
import org.example.planningservice.pipeline.path.PathFindingPipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/pipeline")
public class PathController {

    @Autowired
    private PathFindingPipe pathFindingPipe;

    @PostMapping("/path")
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
}