package org.example.planningservice.pipeline.path.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.dto.response.DirectionsResponseDTO;
import org.example.planningservice.framework.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorageBlock implements Block<DirectionsResponseDTO, DirectionsResponseDTO> {

    @Override
    public DirectionsResponseDTO process(DirectionsResponseDTO path) {

        log.info("\uD83D\uDCBE Data Stored: {}", path);

        return path;
    }
}