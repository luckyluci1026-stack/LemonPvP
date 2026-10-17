package dev.paperswap.runtime.livepatch;

import java.util.Locale;
import java.util.Optional;

/**
 * How the class files of a patch are installed. Both take the same {@code .swappatch}
 * archive and pass the same checks (build, running bytecode, change kinds, probes).
 */
public enum Mechanism {
    /**
     * {@link java.lang.instrument.Instrumentation#redefineClasses}: the patch class file
     * becomes the class's new definition. Later retransformations (by any agent) start from
     * the patched bytes. Rollback redefines the class with the bytes that ran before.
     */
    REDEFINE("redefine"),
    /**
     * {@link java.lang.instrument.Instrumentation#retransformClasses} with PaperSwap's
     * {@link PatchTransformer}: the class keeps its original definition; on every
     * retransformation the transformer supplies the patch bytes again. Rollback removes the
     * patch from the transformer and retransforms, so the JVM falls back to the class file
     * it loaded. PaperSwap applies only method-body changes this way.
     */
    RETRANSFORM("retransform");

    private final String id;

    Mechanism(String id) {
        this.id = id;
    }

    /** @return name used in commands and logs */
    public String id() {
        return this.id;
    }

    /**
     * @param text command argument
     * @return the mechanism, if {@code text} names one
     */
    public static Optional<Mechanism> parse(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        for (Mechanism m : values()) {
            if (m.id.equals(t)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
