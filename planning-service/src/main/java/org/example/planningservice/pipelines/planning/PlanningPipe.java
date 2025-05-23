package org.example.planningservice.pipelines.planning;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.frameworks.pipeline.Pipe;
import org.example.planningservice.pipelines.planning.blocks.DataTransformationBlock;
import org.example.planningservice.pipelines.planning.blocks.StorageBlock;
import org.example.planningservice.pipelines.planning.blocks.ValidationBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanningPipe {

    @Autowired
    private DataTransformationBlock dataTransformationBlock;

    @Autowired
    private StorageBlock storageBlock;

    @Autowired
    private ValidationBlock validationBlock;

    public String execute(String input) {
        Pipe<String, String> procedurePipe = createProcedurePipe();
        log.info("=== created the pipeline (not executed yet) ===");

        // Execute the pipeline
        return procedurePipe.execute(input);
    }

    private Pipe<String, String> createProcedurePipe() {
        return new Pipe<>(validationBlock)
                .connect(dataTransformationBlock)
                .connect(storageBlock);
    }
}
