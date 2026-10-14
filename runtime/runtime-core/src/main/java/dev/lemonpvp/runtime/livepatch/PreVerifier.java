package dev.lemonpvp.runtime.livepatch;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the JVM's own bytecode verifier on the classes of a patch before they are applied,
 * for JVMs whose class redefinition does not verify (probed as
 * {@link Capability#VERIFY_REDEFINED_CLASSES}).
 *
 * <p>Every class of the patch is defined in a throw-away class loader whose parent is the
 * server class loader, then linked through {@link Class#getDeclaredMethods()}: HotSpot links
 * a class before reflecting on it, and linking runs the verifier. No static initializer
 * runs. The classes become garbage afterwards.</p>
 *
 * <p>Limits: the copies live in a different runtime package than the originals. A class
 * whose superclass is package-private, or code that accesses protected members across
 * that boundary, cannot be checked this way; such classes are reported as
 * {@link Outcome.Kind#UNVERIFIABLE}, never as verified.</p>
 */
final class PreVerifier {

    private PreVerifier() {
    }

    /**
     * Result for one class.
     *
     * @param kind   outcome
     * @param detail JVM message, or {@code null} if verified
     */
    record Outcome(Kind kind, String detail) {
        /** Outcome kinds. */
        enum Kind {
            /** The JVM verifier accepted the class. */
            VERIFIED,
            /** The JVM verifier (or class file parser) rejected the class. */
            INVALID,
            /** The class could not be verified outside its own runtime package. */
            UNVERIFIABLE
        }
    }

    /**
     * @param classes binary class name to class file of every class in the patch
     * @param server  server class loader (parent of the throw-away loader)
     * @return one outcome per class, in the order of {@code classes}
     */
    static Map<String, Outcome> verify(Map<String, byte[]> classes, ClassLoader server) {
        VerifyLoader loader = new VerifyLoader(server, classes);
        Map<String, Outcome> out = new LinkedHashMap<>();
        for (String name : classes.keySet()) {
            out.put(name, verifyOne(loader, name));
        }
        return out;
    }

    private static Outcome verifyOne(VerifyLoader loader, String name) {
        try {
            Class<?> c = loader.loadClass(name);
            c.getDeclaredMethods(); // links the class, which runs the verifier
            return new Outcome(Outcome.Kind.VERIFIED, null);
        } catch (VerifyError e) {
            String message = String.valueOf(e.getMessage());
            if (message.contains("Bad access to protected data")) {
                return new Outcome(Outcome.Kind.UNVERIFIABLE, firstLine(message));
            }
            return new Outcome(Outcome.Kind.INVALID, "VerifyError: " + firstLine(message));
        } catch (ClassFormatError e) {
            return new Outcome(Outcome.Kind.INVALID, e.getClass().getSimpleName() + ": " + firstLine(String.valueOf(e.getMessage())));
        } catch (ClassNotFoundException | LinkageError | SecurityException e) {
            return new Outcome(Outcome.Kind.UNVERIFIABLE, e.getClass().getSimpleName() + ": " + firstLine(String.valueOf(e.getMessage())));
        }
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl);
    }

    /** Defines the patch classes itself (child-first for exactly those names). */
    private static final class VerifyLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        VerifyLoader(ClassLoader parent, Map<String, byte[]> classes) {
            super("lemon-livepatch-verify", parent);
            this.classes = classes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            byte[] bytes = this.classes.get(name);
            if (bytes == null) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                return c != null ? c : defineClass(name, bytes, 0, bytes.length);
            }
        }
    }
}
