package org.example.planningservice.frameworks.pipeline;

/**
 * Represents a processing block in a pipeline.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public interface Block<I, O> {
    O process(I input);
}