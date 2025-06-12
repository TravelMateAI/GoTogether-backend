package org.example.planningservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dtos.RouteDTO;
import org.example.planningservice.dtos.RouteRequestDTO;
import org.example.planningservice.pipelines.path.PathFindingPipe;
import org.springframework.beans.factory.annotation.Autowired;
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
    public RouteDTO executePipeline(@RequestBody RouteRequestDTO routeRequest) {

        log.info("🚀 Starting Path Finding...\n");
        RouteDTO path = pathFindingPipe.execute(routeRequest);
        log.info("🎉 Found Path: " + path);

        return path;
    }
}