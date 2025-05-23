package org.example.planningservice.pipelines.planning.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.frameworks.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataTransformationBlock implements Block<String, String> {

    @Override
    public String process(String input) {
        String transformedData = input.toUpperCase(); // Simulating transformation
        log.info("🔄 Data Transformed: " + transformedData);
        return transformedData;
    }
}
