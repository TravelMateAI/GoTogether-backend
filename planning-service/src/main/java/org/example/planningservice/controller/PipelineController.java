package org.example.planningservice.controller;

//import lombok.extern.slf4j.Slf4j;
import org.example.planningservice.pipelines.PipelineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@Slf4j
@RestController
@RequestMapping("/pipeline")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(org.example.planningservice.controller.PipelineController.class);
    private final PipelineManager<String> pipelineManager;

    public PipelineController(PipelineManager<String> pipelineManager) {
        this.pipelineManager = pipelineManager;
    }

    @PostMapping("/execute")
    public String executePipeline(@RequestBody String input) {

        log.info("🚀 Starting Pipeline...");
        String result = pipelineManager.execute(input);
        log.info("🎉 Final Output: " + result);

        return result;
    }
}
