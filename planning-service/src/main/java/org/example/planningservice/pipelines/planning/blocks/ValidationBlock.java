package org.example.planningservice.pipelines.planning.blocks;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.frameworks.pipeline.Block;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidationBlock implements Block<String, String> {

    @Override
    public String process(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Invalid input data!");
        }
        log.info("✅ Validation Passed: " + input);
        return input;
    }
}