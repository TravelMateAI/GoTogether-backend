package org.example.planningservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.pipelines.planning.PlanningPipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/pipeline")
public class PipelineController {

    @Autowired
    private PlanningPipe planningPipe;

    @PostMapping("/execute")
    public String executePipeline(@RequestBody String input) {

        log.info("🚀 Starting Pipeline...210588U\n");
        String result = planningPipe.execute(input);
        log.info("\n🎉 Final Output: " + result);

        return result;
    }
}