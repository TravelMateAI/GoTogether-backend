package org.example.planningservice.pipelines.steps;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.pipelines.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(3)
public class StorageStep implements PipelineStep<String> {

    @Override
    public String process(String input) {
        log.info("💾 Data Stored: " + input);
        return input;
    }
}
