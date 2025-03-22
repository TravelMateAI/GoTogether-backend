package org.example.planningservice.pipelines.steps;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.pipelines.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
public class DataTransformationStep implements PipelineStep<String> {

    @Override
    public String process(String input) {
        String transformedData = input.toUpperCase(); // Simulating transformation
        log.info("🔄 Data Transformed: " + transformedData);
        return transformedData;
    }
}
