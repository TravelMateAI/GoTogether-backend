package org.example.planningservice.framework.pipeline;

/**
 * Represents a pipeline.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public class Pipe<I, O> {

    public final Block<I, O> current;

    public Pipe(Block<I, O> current) {
        this.current = current;
    }

    public <N0> Pipe<I, N0> connect(Block<O, N0> next) {
        return new Pipe<>(input -> next.process(current.process(input)));
    }

    public O execute(I input) {
        return current.process(input);
    }
}