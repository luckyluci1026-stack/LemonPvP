package dev.lemonpvp.runtime.livepatch;

import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFormatException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks after a redefinition that the JVM really runs the bytecode that was applied.
 *
 * <p>The JVM is asked for the class file of the class as it runs now
 * ({@link Instrumentation#retransformClasses}: HotSpot reconstitutes it from the loaded
 * class). A transformer that only records the bytes it is given and never changes them is
 * registered for the duration of that call. The recorded class file is compared with the
 * class file that was applied ({@link ClassDiff}: members, modifiers, hierarchy and the
 * normalized bytecode of every method).</p>
 *
 * <p>Side effect: a retransformation is itself a redefinition with the same bytecode. On
 * HotSpot this costs another safepoint (usually below a millisecond to a few
 * milliseconds) and lets the JIT recompile the methods. Other agents with
 * retransformation-capable transformers see the class again, as on any retransformation.
 * If the call fails, no class is changed (JVMTI specification).</p>
 */
public final class AppliedBytecodeCheck {

    private AppliedBytecodeCheck() {
    }

    /** Overall outcome. */
    public enum Outcome {
        /** Every class runs bytecode equal to the expected class file. */
        VERIFIED,
        /** At least one class runs bytecode that differs from the expected class file. */
        DIFFERS,
        /** The running bytecode could not be read back (see the report lines). */
        NOT_VERIFIABLE
    }

    /**
     * @param outcome overall outcome
     * @param lines   one line per class or problem, for the administrator
     */
    public record Report(Outcome outcome, List<String> lines) {
        public Report {
            lines = List.copyOf(lines);
        }

        /** @return one-line summary */
        public String summary() {
            return switch (this.outcome) {
                case VERIFIED -> "verified: the JVM runs the expected bytecode";
                case DIFFERS -> "VERIFICATION FAILED: the JVM runs bytecode that differs from the expected class file";
                case NOT_VERIFIABLE -> "not verified: the running bytecode could not be read back";
            };
        }
    }

    /**
     * Expectation for one class.
     *
     * @param type     the loaded class
     * @param expected class file that should run now
     * @param before   class file that ran before the operation, or {@code null} for a class
     *                 that did not exist; used to tell whether the change is visible at all
     */
    public record Expectation(Class<?> type, byte[] expected, byte[] before) {
    }

    /**
     * @param inst         instrumentation of the agent
     * @param expectations classes to check
     * @return the report; never throws
     */
    public static Report check(Instrumentation inst, List<Expectation> expectations) {
        if (expectations.isEmpty()) {
            return new Report(Outcome.VERIFIED, List.of());
        }
        if (!inst.isRetransformClassesSupported()) {
            return new Report(Outcome.NOT_VERIFIABLE, List.of("the live-patch agent cannot read back running bytecode"
                    + " (Can-Retransform-Classes missing: use the lemon-livepatch-agent.jar of this build, agent version 2"
                    + " or newer, or the JVM does not support retransformation)"));
        }
        Map<Class<?>, byte[]> running;
        try {
            running = readRunning(inst, expectations.stream().map(Expectation::type).toList());
        } catch (Exception | LinkageError e) {
            return new Report(Outcome.NOT_VERIFIABLE, List.of("reading back the running bytecode failed: " + e
                    + " (per the JVMTI specification no class was changed by this check)"));
        }
        List<String> lines = new ArrayList<>();
        boolean differs = false;
        boolean unverifiable = false;
        for (Expectation e : expectations) {
            String name = e.type().getName();
            byte[] actual = running.get(e.type());
            if (actual == null) {
                lines.add(name + ": the JVM did not report its bytecode");
                unverifiable = true;
                continue;
            }
            try {
                List<ClassChange> diff = ClassDiff.compare(ClassFileReader.read(actual), ClassFileReader.read(e.expected()));
                if (!diff.isEmpty()) {
                    differs = true;
                    lines.add(name + ": DIFFERS from the expected class file: " + diff.stream().limit(5).map(Object::toString).toList()
                            + (diff.size() > 5 ? " and " + (diff.size() - 5) + " more" : ""));
                } else if (e.before() != null
                        && ClassDiff.compare(ClassFileReader.read(e.before()), ClassFileReader.read(e.expected())).isEmpty()) {
                    // The change is invisible at this level (debug information or annotations only).
                    unverifiable = true;
                    lines.add(name + ": the running bytecode equals the expected class file, but the change itself is not visible"
                            + " in the bytecode (only debug information or annotations differ), so this does not prove it");
                } else {
                    lines.add(name + ": runs the expected bytecode (read back from the JVM)");
                }
            } catch (ClassFormatException ex) {
                unverifiable = true;
                lines.add(name + ": the class file reported by the JVM could not be read: " + ex.getMessage());
            }
        }
        Outcome outcome = differs ? Outcome.DIFFERS : unverifiable ? Outcome.NOT_VERIFIABLE : Outcome.VERIFIED;
        return new Report(outcome, lines);
    }

    /**
     * Asks the JVM for the class files of the given classes as they run now.
     *
     * @param inst    instrumentation with retransformation support
     * @param classes loaded, modifiable classes
     * @return class file per class (a class is missing if the JVM did not report it)
     * @throws Exception if the JVM refuses the retransformation
     */
    static Map<Class<?>, byte[]> readRunning(Instrumentation inst, List<Class<?>> classes) throws Exception {
        Set<Class<?>> wanted = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        wanted.addAll(classes);
        Map<Class<?>, byte[]> captured = new ConcurrentHashMap<>();
        ClassFileTransformer recorder = new ClassFileTransformer() {
            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> redefined,
                                    ProtectionDomain domain, byte[] classfile) {
                if (redefined != null && wanted.contains(redefined)) {
                    captured.put(redefined, classfile.clone());
                }
                return null; // never changes a class
            }
        };
        inst.addTransformer(recorder, true);
        try {
            inst.retransformClasses(classes.toArray(Class<?>[]::new));
        } finally {
            inst.removeTransformer(recorder);
        }
        Map<Class<?>, byte[]> out = new LinkedHashMap<>();
        for (Class<?> c : classes) {
            byte[] b = captured.get(c);
            if (b != null) {
                out.put(c, b);
            }
        }
        return out;
    }
}
