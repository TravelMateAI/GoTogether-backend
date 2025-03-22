package org.example.planningservice.pipelines;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PipelineManager<T> {

    private final List<PipelineStep<T>> steps;

    public PipelineManager(List<PipelineStep<T>> steps) {
        this.steps = steps;
    }


    public T execute(T input) {
        T result = input;
        for (PipelineStep<T> step : steps) {
            result = step.process(result);
        }
        return result;
    }
}
