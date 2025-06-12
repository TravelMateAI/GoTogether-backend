package org.example.planningservice.pipelines.planning.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.frameworks.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorageBlock implements Block<String, String> {

    @Override
    public String process(String input) {
        log.info("💾 Data Stored: " + input);
        return input;
    }
}
