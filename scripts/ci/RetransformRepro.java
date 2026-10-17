import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Standalone check of JVM retransformation behaviour, one scenario per JVM process, so a
 * native crash of one scenario is attributed to it. Used in CI to find out what a JVM
 * (e.g. JetBrains Runtime with -XX:+AllowEnhancedClassRedefinition) survives.
 *
 * <pre>
 * java -javaagent:lemon-livepatch-agent.jar RetransformRepro.java &lt;scenario&gt;
 * scenarios: recorder | modify | modify-unload | modify-then-redefine
 * </pre>
 *
 * Prints "RESULT &lt;scenario&gt; OK" when the JVM got through the scenario including several
 * full GCs (which free old class versions); a crash ends the process without that line.
 */
public class RetransformRepro {

    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        Instrumentation inst = (Instrumentation) ClassLoader.getSystemClassLoader()
                .loadClass("dev.lemonpvp.livepatch.agent.LemonLivePatchAgent").getMethod("instrumentation").invoke(null);
        byte[] v1 = compile("1");
        byte[] v2 = compile("2");
        byte[] v3 = compile("3");
        Loader loader = new Loader();
        Class<?> t = loader.define(v1);
        Map<Class<?>, byte[]> supply = new ConcurrentHashMap<>();
        ClassFileTransformer modifying = new ClassFileTransformer() {
            @Override
            public byte[] transform(Module m, ClassLoader l, String n, Class<?> redefined, ProtectionDomain pd, byte[] buf) {
                return redefined == null ? null : supply.get(redefined);
            }
        };
        ClassFileTransformer recorder = new ClassFileTransformer() {
            @Override
            public byte[] transform(Module m, ClassLoader l, String n, Class<?> redefined, ProtectionDomain pd, byte[] buf) {
                return null;
            }
        };
        switch (scenario) {
            case "recorder" -> {
                inst.addTransformer(recorder, true);
                for (int i = 0; i < 5; i++) {
                    inst.retransformClasses(t);
                }
                inst.redefineClasses(new ClassDefinition(t, v2));
                inst.retransformClasses(t);
                check(t, 2);
            }
            case "modify" -> {
                inst.addTransformer(modifying, true);
                supply.put(t, v2);
                for (int i = 0; i < 5; i++) {
                    inst.retransformClasses(t);
                    check(t, 2);
                    gc();
                }
                supply.remove(t);
                inst.retransformClasses(t);
                check(t, 1);
            }
            case "modify-unload" -> {
                inst.addTransformer(modifying, true);
                supply.put(t, v2);
                inst.retransformClasses(t);
                check(t, 2);
                supply.remove(t);
                inst.retransformClasses(t);
                check(t, 1);
                t = null;
                loader = null;
            }
            case "modify-then-redefine" -> {
                inst.addTransformer(modifying, true);
                supply.put(t, v2);
                inst.retransformClasses(t);
                check(t, 2);
                supply.remove(t);
                inst.redefineClasses(new ClassDefinition(t, v3));
                check(t, 3);
                inst.retransformClasses(t);
                check(t, 3);
            }
            default -> throw new IllegalArgumentException(scenario);
        }
        gc();
        System.out.println("RESULT " + scenario + " OK" + (loader == null ? "" : " (" + t.getName() + " still loaded)"));
    }

    static void gc() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(100);
            byte[][] churn = new byte[2000][];
            for (int j = 0; j < churn.length; j++) {
                churn[j] = new byte[1024];
            }
        }
    }

    static void check(Class<?> t, int expected) throws ReflectiveOperationException {
        int v = (int) t.getMethod("value").invoke(null);
        if (v != expected) {
            System.out.println("RESULT unexpected value " + v + ", expected " + expected);
            System.exit(3);
        }
    }

    static byte[] compile(String value) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        String src = "package repro; public class T { public static int value() { return " + value + "; } }";
        Map<String, java.io.ByteArrayOutputStream> out = new ConcurrentHashMap<>();
        var std = javac.getStandardFileManager(null, null, null);
        var fm = new javax.tools.ForwardingJavaFileManager<>(std) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location l, String name, JavaFileObject.Kind k, javax.tools.FileObject s) {
                return new SimpleJavaFileObject(URI.create("mem:///" + name + ".class"), k) {
                    @Override
                    public java.io.OutputStream openOutputStream() {
                        return out.computeIfAbsent(name, x -> new java.io.ByteArrayOutputStream());
                    }
                };
            }
        };
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///repro/T.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignore) {
                return src;
            }
        };
        if (!javac.getTask(null, fm, null, List.of("--release", "21"), null, List.of(file)).call()) {
            throw new IllegalStateException("compile failed");
        }
        return out.get("repro.T").toByteArray();
    }

    static final class Loader extends ClassLoader {
        Loader() {
            super(RetransformRepro.class.getClassLoader());
        }

        Class<?> define(byte[] b) {
            return defineClass("repro.T", b, 0, b.length);
        }
    }
}
