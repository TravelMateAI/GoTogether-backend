package org.example.planningservice.pipelines.path.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dtos.RouteDTO;
import org.example.planningservice.dtos.RouteRequestDTO;
import org.example.planningservice.frameworks.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorageBlock implements Block<RouteDTO, RouteDTO> {

    @Override
    public RouteDTO process(RouteDTO path) {

        log.info("💾 Data Stored: " + path);

        return path;
    }
}