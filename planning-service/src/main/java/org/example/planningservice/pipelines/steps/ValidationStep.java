package org.example.planningservice.pipelines.steps;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.pipelines.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class ValidationStep implements PipelineStep<String> {

    @Override
    public String process(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Invalid input data!");
        }
        log.info("✅ Validation Passed: " + input);
        return input;
    }
}
