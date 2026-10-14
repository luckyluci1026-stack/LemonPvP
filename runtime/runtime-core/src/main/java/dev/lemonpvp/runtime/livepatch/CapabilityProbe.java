package dev.lemonpvp.runtime.livepatch;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.Map;

/**
 * Determines what the running JVM can redefine by actually redefining small throw-away
 * classes, each in its own class loader. The results are facts about this JVM process, not
 * assumptions based on vendor names or flags.
 *
 * <p>Cost: a successful redefinition is a JVM safepoint operation (all Java threads pause
 * briefly). On JVMs with enhanced redefinition (e.g. JetBrains Runtime with
 * {@code -XX:+AllowEnhancedClassRedefinition}) a structural redefinition can take longer
 * because the JVM updates existing objects. Probes run only on explicit request
 * ({@code /fork patch capabilities probe}) or when a patch needs a capability that has not
 * been probed yet.</p>
 */
public final class CapabilityProbe {

    private static final String PACKAGE = "lemonprobe";
    private static final String PROBE = PACKAGE + "/Probe";
    private static final String BASE = PACKAGE + "/ProbeBase";
    private static final String OBJECT = "java/lang/Object";

    private CapabilityProbe() {
    }

    /**
     * Outcome of one probe.
     *
     * @param supported whether the JVM accepted (and, for bodies, executed) the change
     * @param detail    explanation, e.g. the JVM's error message
     */
    public record Result(boolean supported, String detail) {
    }

    /**
     * Runs all probes.
     *
     * @param instrumentation instrumentation of this JVM
     * @return one result per capability
     */
    public static Map<Capability, Result> probeAll(Instrumentation instrumentation) {
        Map<Capability, Result> out = new EnumMap<>(Capability.class);
        for (Capability c : Capability.values()) {
            out.put(c, probe(instrumentation, c));
        }
        return out;
    }

    /**
     * Runs one probe.
     *
     * @param instrumentation instrumentation of this JVM
     * @param capability      what to test
     * @return the result; never throws
     */
    public static Result probe(Instrumentation instrumentation, Capability capability) {
        if (!instrumentation.isRedefineClassesSupported()) {
            return new Result(false, "Instrumentation.isRedefineClassesSupported() is false");
        }
        try {
            return switch (capability) {
                case REDEFINE_METHOD_BODY -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 2), true);
                case ADD_METHOD -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1)
                                .staticIntMethod("extra", ProbeClassWriter.ACC_PUBLIC, 3), false);
                case REMOVE_METHOD -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1)
                                .staticIntMethod("extra", ProbeClassWriter.ACC_PUBLIC, 3),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1), false);
                case ADD_FIELD -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1)
                                .staticIntField("added"), false);
                case REMOVE_FIELD -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1)
                                .staticIntField("removed"),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1), false);
                case CHANGE_METHOD_MODIFIERS -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PRIVATE, 1), false);
                case CHANGE_INTERFACES -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1)
                                .implement("java/io/Serializable"), false);
                case CHANGE_SUPERCLASS -> redefine(instrumentation,
                        new ProbeClassWriter(PROBE, BASE).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1),
                        new ProbeClassWriter(PROBE, OBJECT).staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1), false);
                case DEFINE_NEW_CLASS -> defineNewClass();
            };
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError vme && !(vme instanceof StackOverflowError)) {
                throw vme;
            }
            return new Result(false, "probe failed unexpectedly: " + t);
        }
    }

    private static Result redefine(Instrumentation inst, ProbeClassWriter before, ProbeClassWriter after, boolean verifyBody)
            throws ReflectiveOperationException {
        ProbeLoader loader = new ProbeLoader();
        loader.define(BASE, new ProbeClassWriter(BASE, OBJECT).toBytes());
        Class<?> probe = loader.define(PROBE, before.toBytes());
        int beforeValue = (int) probe.getMethod("value").invoke(null);
        try {
            inst.redefineClasses(new ClassDefinition(probe, after.toBytes()));
        } catch (UnsupportedOperationException e) {
            return new Result(false, "JVM refused: " + e.getMessage());
        } catch (ClassFormatError | VerifyError | java.lang.instrument.UnmodifiableClassException | ClassNotFoundException e) {
            return new Result(false, "JVM error: " + e);
        }
        if (!verifyBody) {
            return new Result(true, "redefinition accepted by the JVM");
        }
        int afterValue = (int) probe.getMethod("value").invoke(null);
        if (beforeValue == 1 && afterValue == 2) {
            return new Result(true, "redefinition accepted; the new method body executed (1 -> 2)");
        }
        return new Result(false, "redefinition returned but the method still returns " + afterValue);
    }

    private static Result defineNewClass() throws ReflectiveOperationException {
        ProbeLoader loader = new ProbeLoader();
        Class<?> host = loader.define(PROBE, new ProbeClassWriter(PROBE, OBJECT)
                .staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1).toBytes());
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(host, MethodHandles.lookup());
        Class<?> added = lookup.defineClass(new ProbeClassWriter(PACKAGE + "/ProbeAdded", OBJECT)
                .staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 4).toBytes());
        int v = (int) added.getMethod("value").invoke(null);
        if (added.getClassLoader() == loader && v == 4) {
            return new Result(true, "class defined in the host's class loader and executed");
        }
        return new Result(false, "defined class is in an unexpected class loader");
    }

    /** Throw-away class loader; becomes garbage after the probe. */
    private static final class ProbeLoader extends ClassLoader {
        ProbeLoader() {
            super(CapabilityProbe.class.getClassLoader());
        }

        Class<?> define(String internalName, byte[] bytes) {
            return defineClass(internalName.replace('/', '.'), bytes, 0, bytes.length);
        }
    }
}
