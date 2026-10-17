package dev.paperswap.runtime.livepatch;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Main class of the second JVM in {@link DynamicAttachTest}. Started WITHOUT the agent;
 * loads a class, waits until the agent is attached from outside, then patches that class
 * by retransformation through {@link PatchTransformer} and prints what runs.
 *
 * <p>Arguments: jar with {@code att.Greeter} (v1), class file of {@code att.Greeter} (v2).</p>
 */
public final class AttachTarget {

    private AttachTarget() {
    }

    public static void main(String[] args) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {Path.of(args[0]).toUri().toURL()}, AttachTarget.class.getClassLoader());
        Class<?> greeter = Class.forName("att.Greeter", true, loader);
        System.out.println("agent before attach: " + InstrumentationAccess.find().instrumentation().isPresent());
        System.out.println("loaded before attach: " + text(greeter));
        long deadline = System.nanoTime() + 60_000_000_000L;
        InstrumentationAccess.Lookup lookup;
        while ((lookup = InstrumentationAccess.find()).instrumentation().isEmpty()) {
            if (System.nanoTime() > deadline) {
                System.out.println("no agent attached within 60 s");
                System.exit(2);
            }
            Thread.sleep(50);
        }
        System.out.println("agent: " + lookup.detail());
        Instrumentation inst = lookup.instrumentation().get();
        System.out.println("redefine " + inst.isRedefineClassesSupported() + ", retransform " + inst.isRetransformClassesSupported()
                + ", modifiable " + inst.isModifiableClass(greeter));
        PatchTransformer t = PatchTransformer.INSTANCE;
        t.register(inst);
        t.push("attach-test", Map.of(greeter, Files.readAllBytes(Path.of(args[1]))));
        inst.retransformClasses(greeter);
        System.out.println("patched: " + text(greeter));
        inst.retransformClasses(greeter);
        System.out.println("after another retransformation: " + text(greeter) + ", supplied " + t.served(greeter)
                + ", displaced " + t.slot(greeter).orElseThrow().displaced());
        t.remove("attach-test", List.of(greeter));
        inst.retransformClasses(greeter);
        System.out.println("removed: " + text(greeter));
    }

    private static Object text(Class<?> c) throws ReflectiveOperationException {
        return c.getMethod("text").invoke(null);
    }
}
