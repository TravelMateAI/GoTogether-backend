package org.example.planningservice.pipelines;

public interface PipelineStep<T> {
    T process(T input);
}