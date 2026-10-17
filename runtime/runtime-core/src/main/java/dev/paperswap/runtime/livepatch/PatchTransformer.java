package dev.paperswap.runtime.livepatch;

import dev.paperswap.runtime.security.Hashing;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * PaperSwap's only class file transformer; it installs {@link Mechanism#RETRANSFORM} patches.
 *
 * <p>How a retransformation patch reaches the JVM: {@link Instrumentation#retransformClasses}
 * does not read class files from disk. The JVM takes the class's <em>initial</em> class file
 * bytes (the bytes it loaded or last received through {@code redefineClasses}, after the
 * non-retransformable transformers) and passes them through every retransformation-capable
 * transformer. This transformer holds the patch bytes in memory, per {@link Class} object,
 * and returns them when the JVM asks for exactly that class. Because the JVM does not keep
 * the output of retransformation-capable transformers as the new initial bytes, the
 * transformer supplies the patch again on <em>every</em> later retransformation (for
 * example when another agent or PaperSwap's own read-back check retransforms the class);
 * removing the patch from the transformer and retransforming brings back the initial
 * bytes.</p>
 *
 * <p>Rules, each enforced here:</p>
 * <ul>
 *     <li>registered once, with {@code canRetransform = true}, and only if the JVM supports
 *     retransformation and does not run with enhanced class redefinition (see
 *     {@link JvmCapabilities#ENHANCED_RETRANSFORM_PROBLEM});</li>
 *     <li>never acts on class loading ({@code classBeingRedefined == null}): classes that
 *     are loaded or defined are never touched;</li>
 *     <li>only classes with a patch layer are changed; for every other class the method
 *     returns {@code null} (no change) without looking at the bytes;</li>
 *     <li>no double modification: the bytes returned are always the complete class file of
 *     the top layer, never applied on top of the incoming bytes;</li>
 *     <li>base check: the digest of the incoming bytes is recorded on the first
 *     retransformation. If later retransformations bring different incoming bytes, the class
 *     was redefined underneath (by another tool) or a transformer before this one changed
 *     its output. The patch was built for the old base, so it is not supplied any more and the
 *     class is marked as <em>displaced</em> (visible in status and history) instead of
 *     silently overwriting the other change;</li>
 *     <li>no recursion: a thread that is already inside this transformer gets {@code null};</li>
 *     <li>errors never escape: the JVM would ignore them anyway, so they are logged and the
 *     class stays unchanged.</li>
 * </ul>
 *
 * <p>Order: the JVM calls non-retransformable transformers first (their result is reused on
 * retransformation), then retransformation-capable ones in the order they were added. This
 * transformer therefore sees the output of capable transformers registered before it, and
 * transformers registered after it (for example PaperSwap's read-back recorder) see the patch.</p>
 *
 * <p>JVM-global like {@link LivePatchRegistry}: the classes stay patched when the PaperSwap
 * runtime restarts inside the same JVM.</p>
 */
public final class PatchTransformer implements ClassFileTransformer {

    /** The transformer of this JVM. */
    public static final PatchTransformer INSTANCE = new PatchTransformer();

    /**
     * One patch layer of one class.
     *
     * @param patchId patch that installed it
     * @param bytes   complete class file
     * @param sha256  digest of {@code bytes}
     */
    public record Layer(String patchId, byte[] bytes, String sha256) {
    }

    /**
     * What the transformer holds for one class.
     *
     * @param className  binary name
     * @param loader     name of the defining class loader
     * @param patchIds   layers, bottom first; the last one is what the JVM gets
     * @param baseSha256 digest of the incoming bytes on the first retransformation, or {@code null}
     * @param served     how often the transformer supplied the top layer
     * @param displaced  why the patch is not supplied any more, or {@code null}
     */
    public record SlotInfo(String className, String loader, List<String> patchIds, String baseSha256, long served,
                           String displaced) {
        public SlotInfo {
            patchIds = List.copyOf(patchIds);
        }
    }

    private static final class Slot {
        final Class<?> type;
        final Deque<Layer> layers = new ArrayDeque<>();
        String baseSha256;
        long served;
        String displaced;

        Slot(Class<?> type) {
            this.type = type;
        }

        SlotInfo info() {
            return new SlotInfo(this.type.getName(), loaderName(this.type.getClassLoader()),
                    this.layers.stream().map(Layer::patchId).toList(), this.baseSha256, this.served, this.displaced);
        }
    }

    private final Map<Class<?>, Slot> slots = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> inside = new ThreadLocal<>();
    private Instrumentation registeredWith;
    private Instant registeredAt;
    private volatile Consumer<String> warnings = m -> System.err.println("[PaperSwap] " + m);

    PatchTransformer() {
    }

    /** @param sink receives warnings (e.g. a displaced patch); called on the thread that retransforms */
    public void warnings(Consumer<String> sink) {
        this.warnings = sink;
    }

    /**
     * Registers the transformer with {@code canRetransform = true}. Does nothing if it is
     * already registered with this instance.
     *
     * @param inst instrumentation of this JVM
     * @return {@code true} if it was registered now
     * @throws IllegalStateException if the JVM or agent does not support retransformation
     */
    public synchronized boolean register(Instrumentation inst) {
        if (this.registeredWith == inst) {
            return false;
        }
        if (this.registeredWith != null) {
            throw new IllegalStateException("the patch transformer is registered with another Instrumentation instance");
        }
        if (!inst.isRetransformClassesSupported()) {
            throw new IllegalStateException("this JVM or agent does not support retransformation"
                    + " (Instrumentation.isRetransformClassesSupported() is false)");
        }
        if (JvmCapabilities.enhancedRedefinition()) {
            throw new IllegalStateException(JvmCapabilities.ENHANCED_RETRANSFORM_PROBLEM);
        }
        inst.addTransformer(this, true);
        this.registeredWith = inst;
        this.registeredAt = Instant.now();
        return true;
    }

    /** @return when the transformer was registered, if it is */
    public synchronized Optional<Instant> registeredAt() {
        return Optional.ofNullable(this.registeredAt);
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classBeingRedefined == null) {
            return null; // class loading or definition: never touched
        }
        Slot slot = this.slots.get(classBeingRedefined);
        if (slot == null || Boolean.TRUE.equals(this.inside.get())) {
            return null;
        }
        this.inside.set(Boolean.TRUE);
        String warning = null;
        try {
            synchronized (slot) {
                Layer top = slot.layers.peekLast();
                if (top == null || slot.displaced != null) {
                    return null;
                }
                String incoming = Hashing.sha256(classfileBuffer);
                if (slot.baseSha256 == null) {
                    slot.baseSha256 = incoming;
                } else if (!slot.baseSha256.equals(incoming)) {
                    slot.displaced = "the JVM passed different initial bytes (" + PatchPlanner.shortSha(incoming) + " instead of "
                            + PatchPlanner.shortSha(slot.baseSha256) + ") at " + Instant.now()
                            + ": the class was redefined by something other than this patch, or a transformer before"
                            + " PaperSwap's changed its output";
                    warning = "live patch " + top.patchId() + " is no longer applied to " + slot.type.getName() + ": "
                            + slot.displaced + ". The class now runs that other version; roll the patch back.";
                    return null;
                }
                slot.served++;
                return top.bytes().clone();
            }
        } catch (RuntimeException | LinkageError e) {
            warning = "live-patch transformer failed for " + className + " (class left unchanged): " + e;
            return null;
        } finally {
            this.inside.remove();
            if (warning != null) {
                warn(warning);
            }
        }
    }

    private void warn(String message) {
        try {
            this.warnings.accept(message);
        } catch (RuntimeException e) {
            System.err.println("[PaperSwap] " + message);
        }
    }

    /**
     * Adds a layer per class. The classes change only on the next retransformation.
     *
     * @param patchId patch id
     * @param classes class file per class
     * @throws IllegalStateException if a class is displaced (nothing is added then)
     */
    public synchronized void push(String patchId, Map<Class<?>, byte[]> classes) {
        for (Class<?> c : classes.keySet()) {
            Slot s = this.slots.get(c);
            if (s != null && s.displaced != null) {
                throw new IllegalStateException(c.getName() + " is displaced: " + s.displaced);
            }
        }
        for (Map.Entry<Class<?>, byte[]> e : classes.entrySet()) {
            Slot s = this.slots.computeIfAbsent(e.getKey(), Slot::new);
            synchronized (s) {
                byte[] copy = e.getValue().clone();
                s.layers.addLast(new Layer(patchId, copy, Hashing.sha256(copy)));
            }
        }
    }

    /**
     * Removes the top layer of each class if it belongs to {@code patchId}. A class without
     * layers is forgotten (the next retransformation brings back its initial bytes).
     *
     * @param patchId patch id
     * @param classes classes
     * @return removed layers, for {@link #restore}
     */
    public synchronized Map<Class<?>, Layer> remove(String patchId, Collection<Class<?>> classes) {
        Map<Class<?>, Layer> removed = new IdentityHashMap<>();
        for (Class<?> c : classes) {
            Slot s = this.slots.get(c);
            if (s == null) {
                continue;
            }
            synchronized (s) {
                Layer top = s.layers.peekLast();
                if (top != null && top.patchId().equals(patchId)) {
                    removed.put(c, s.layers.removeLast());
                }
                if (s.layers.isEmpty()) {
                    this.slots.remove(c);
                }
            }
        }
        return removed;
    }

    /**
     * Puts layers back after a failed retransformation.
     *
     * @param layers result of {@link #remove}
     */
    public synchronized void restore(Map<Class<?>, Layer> layers) {
        for (Map.Entry<Class<?>, Layer> e : layers.entrySet()) {
            Slot s = this.slots.computeIfAbsent(e.getKey(), Slot::new);
            synchronized (s) {
                s.layers.addLast(e.getValue());
            }
        }
    }

    /**
     * @param type class
     * @return how often the top layer of the class was supplied so far ({@code 0} without layers)
     */
    public long served(Class<?> type) {
        Slot s = this.slots.get(type);
        if (s == null) {
            return 0;
        }
        synchronized (s) {
            return s.served;
        }
    }

    /**
     * @param type class
     * @return the transformer's state for the class, if it has layers
     */
    public Optional<SlotInfo> slot(Class<?> type) {
        Slot s = this.slots.get(type);
        if (s == null) {
            return Optional.empty();
        }
        synchronized (s) {
            return Optional.of(s.info());
        }
    }

    /** @return state of every class with layers, sorted by name */
    public List<SlotInfo> slots() {
        List<SlotInfo> out = new ArrayList<>();
        for (Slot s : this.slots.values()) {
            synchronized (s) {
                out.add(s.info());
            }
        }
        out.sort(Comparator.comparing(SlotInfo::className));
        return out;
    }

    static String loaderName(ClassLoader loader) {
        if (loader == null) {
            return "bootstrap";
        }
        String name = loader.getName();
        return (name == null ? "" : name + " ") + "(" + loader.getClass().getName() + ")";
    }

    /** Test hook: forgets every layer and unregisters (the JVM keeps what it runs until the next retransformation). */
    synchronized void clearForTests() {
        this.slots.clear();
        if (this.registeredWith != null) {
            this.registeredWith.removeTransformer(this);
            this.registeredWith = null;
            this.registeredAt = null;
        }
    }
}
