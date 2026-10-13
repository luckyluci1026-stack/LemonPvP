package dev.lemonpvp.runtime.descriptor;

import java.util.List;

/** Thrown when a module descriptor is missing or invalid. Carries every problem found. */
public final class DescriptorException extends Exception {

    private final List<String> problems;

    public DescriptorException(List<String> problems) {
        super(String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public DescriptorException(String problem) {
        this(List.of(problem));
    }

    /** @return all problems found in the descriptor */
    public List<String> problems() {
        return this.problems;
    }
}
