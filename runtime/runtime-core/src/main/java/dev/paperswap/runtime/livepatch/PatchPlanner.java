package dev.paperswap.runtime.livepatch;

import dev.paperswap.runtime.livepatch.classfile.ClassFileReader;
import dev.paperswap.runtime.livepatch.classfile.ClassFormatException;
import dev.paperswap.runtime.livepatch.classfile.ClassInfo;
import dev.paperswap.runtime.security.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns a verified {@link PatchArchive} into a {@link PatchPlan} without changing anything.
 *
 * <p>Checks, in this order: target build and Minecraft version; for every existing class
 * that it is loaded (or loadable) by the server class loader, modifiable, and that its
 * running bytecode has the digest the patch was built against; the kind of every change;
 * whether this JVM supports all of them (probed, see {@link CapabilityProbe}); and, if this
 * JVM does not verify redefined classes itself, the bytecode verifier on every class
 * ({@link PreVerifier}). Any failed check rejects the whole patch: a patch is applied
 * completely or not at all.</p>
 *
 * <p>{@link Mechanism#RETRANSFORM} plans accept only method-body changes (probed through
 * a real retransformation) and are refused for classes whose retransformation patch was
 * displaced; {@link Mechanism#REDEFINE} plans are refused for classes that currently run a
 * retransformation patch, because the redefinition would silently push that patch out.</p>
 *
 * <p>Side effect: target classes that were not loaded yet are loaded (not initialized) by
 * the server class loader. Runs on the worker thread.</p>
 */
public final class PatchPlanner {

    /**
     * Everything the planner needs from the running server.
     *
     * @param buildId          build id of the running server ({@link BuildIdentity})
     * @param minecraftVersion running Minecraft version
     * @param loader           server class loader (the only loader whose classes are patched)
     * @param instrumentation  instrumentation of this JVM
     * @param registry         record of earlier patches
     * @param development      development profile: an unknown build id and classes that cannot be pre-verified
     *                         are warnings instead of errors
     * @param mechanism        how the classes would be installed
     * @param transformer      PaperSwap's retransformation patches in this JVM
     */
    public record Context(String buildId, String minecraftVersion, ClassLoader loader, Instrumentation instrumentation,
                          LivePatchRegistry registry, boolean development, Mechanism mechanism, PatchTransformer transformer) {
    }

    private PatchPlanner() {
    }

    /**
     * @param archive verified archive
     * @param ctx     server context
     * @return the plan
     */
    public static PatchPlan plan(PatchArchive archive, Context ctx) {
        long registryVersion = ctx.registry().version();
        PatchManifest m = archive.manifest();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (!m.targetMinecraft().equals(ctx.minecraftVersion())) {
            errors.add("patch targets Minecraft " + m.targetMinecraft() + " but the server runs " + ctx.minecraftVersion());
        }
        if (BuildIdentity.UNKNOWN.equals(ctx.buildId())) {
            if (ctx.development()) {
                warnings.add("the server's build id is unknown (no build manifest); only the per-class digests are checked");
            } else {
                errors.add("the server's build id is unknown (no build manifest), so the patch cannot be matched to this build");
            }
        } else if (!m.targetBuildId().equals(ctx.buildId())) {
            errors.add("patch was built for " + m.targetBuildId() + " but the server is " + ctx.buildId()
                    + " (wrong build id; create the patch against the jar that runs on this server)");
        }
        if (ctx.mechanism() == Mechanism.RETRANSFORM && !ctx.instrumentation().isRetransformClassesSupported()) {
            errors.add("retransformation is not available (Instrumentation.isRetransformClassesSupported() is false: agent"
                    + " without Can-Retransform-Classes, or the JVM cannot retransform); use the mechanism 'redefine'");
        } else if (ctx.mechanism() == Mechanism.RETRANSFORM && JvmCapabilities.enhancedRedefinition()) {
            errors.add("retransformation patches are disabled: " + JvmCapabilities.ENHANCED_RETRANSFORM_PROBLEM);
        }

        List<PatchPlan.Entry> entries = new ArrayList<>();
        Set<Capability> needed = EnumSet.noneOf(Capability.class);
        Set<ChangeKind> kinds = new LinkedHashSet<>();
        for (PatchManifest.Entry e : m.classes()) {
            PatchPlan.Entry entry = e.isNew() ? planNew(archive, e, ctx) : planExisting(archive, e, ctx);
            entries.add(entry);
            for (ClassChange c : entry.changes()) {
                kinds.add(c.kind());
                needed.addAll(required(c.kind(), ctx.mechanism()));
            }
        }

        if (entries.stream().anyMatch(e -> e.action() == PatchPlan.Action.REDEFINE || e.action() == PatchPlan.Action.DEFINE)) {
            needed.add(Capability.VERIFY_REDEFINED_CLASSES);
        }
        Map<Capability, CapabilityProbe.Result> caps = needed.isEmpty() ? Map.of()
                : JvmCapabilities.ensure(ctx.instrumentation(), needed);
        List<PatchPlan.Entry> checked = new ArrayList<>(entries.size());
        for (PatchPlan.Entry entry : entries) {
            checked.add(checkCapabilities(entry, caps, ctx.mechanism()));
        }
        CapabilityProbe.Result verifies = caps.get(Capability.VERIFY_REDEFINED_CLASSES);
        if (verifies != null && !verifies.supported()) {
            checked = preVerify(archive, checked, ctx, warnings);
        }
        for (PatchPlan.Entry entry : checked) {
            if (entry.action() == PatchPlan.Action.REJECT) {
                errors.add(entry.className() + ": " + entry.problem());
            }
        }
        if (errors.isEmpty() && checked.stream().noneMatch(x -> x.action() == PatchPlan.Action.REDEFINE
                || x.action() == PatchPlan.Action.DEFINE)) {
            errors.add("every class of the patch is identical to the running version; nothing to do");
        }
        for (ChangeKind k : kinds) {
            if (k.note() != null) {
                warnings.add(k.description() + ": " + k.note());
            }
        }
        if (ctx.mechanism() == Mechanism.RETRANSFORM) {
            warnings.add("retransformation: PaperSwap's transformer supplies the patch on every later retransformation; if"
                    + " another tool redefines a patched class, the patch is marked displaced and no longer supplied");
        }
        warnings.add("PaperSwap checks bytecode compatibility, not behaviour: the patch is technically applied but not"
                + " semantically validated. Existing objects, static fields and caches keep their state.");
        return new PatchPlan(m.id(), archive.sha256(), m.description(), ctx.mechanism(), checked, errors, warnings, caps,
                registryVersion);
    }

    /**
     * Capabilities a change needs with a mechanism; empty = never applied that way.
     *
     * @param kind      change
     * @param mechanism mechanism
     * @return required capabilities
     */
    static Set<Capability> required(ChangeKind kind, Mechanism mechanism) {
        if (mechanism == Mechanism.REDEFINE || kind == ChangeKind.NEW_CLASS) {
            return kind.required();
        }
        return kind.bodyOnly() ? EnumSet.of(Capability.RETRANSFORM_METHOD_BODY) : EnumSet.noneOf(Capability.class);
    }

    private static PatchPlan.Entry reject(PatchManifest.Entry e, String runningSha, List<ClassChange> changes, String problem) {
        return new PatchPlan.Entry(e.name(), PatchPlan.Action.REJECT, changes, runningSha, e.sha256(), null, null, problem);
    }

    private static PatchPlan.Entry planExisting(PatchArchive archive, PatchManifest.Entry e, Context ctx) {
        Class<?> cls;
        try {
            cls = Class.forName(e.name(), false, ctx.loader());
        } catch (ClassNotFoundException | LinkageError ex) {
            return reject(e, null, List.of(), "class does not exist in this server (" + ex.getClass().getSimpleName()
                    + "); mark it as 'new' if the patch adds it");
        }
        if (cls.getClassLoader() != ctx.loader()) {
            return reject(e, null, List.of(), "class is loaded by " + cls.getClassLoader()
                    + ", not by the server class loader; only server classes can be live-patched");
        }
        if (!ctx.instrumentation().isModifiableClass(cls)) {
            return reject(e, null, List.of(), "the JVM reports this class as not modifiable");
        }
        Optional<PatchTransformer.SlotInfo> slot = ctx.transformer().slot(cls);
        if (slot.isPresent() && slot.get().displaced() != null) {
            return reject(e, null, List.of(), "PaperSwap's retransformation patch " + slot.get().patchIds() + " of this class was"
                    + " displaced (" + slot.get().displaced() + "); roll it back first");
        }
        if (slot.isPresent() && ctx.mechanism() == Mechanism.REDEFINE) {
            return reject(e, null, List.of(), "the class runs retransformation patch " + slot.get().patchIds()
                    + "; a redefinition would push it out. Apply this patch with 'retransform' or roll that patch back first");
        }
        Optional<LivePatchRegistry.ClassVersion> patched = ctx.registry().current(e.name());
        byte[] running;
        if (patched.isPresent()) {
            running = patched.get().bytes();
        } else {
            try {
                running = readResource(ctx.loader(), e.name());
            } catch (IOException ex) {
                return reject(e, null, List.of(), "cannot read the running class file: " + ex.getMessage());
            }
        }
        String runningSha = Hashing.sha256(running);
        String replaces = null;
        if (!runningSha.equals(e.baseSha256())) {
            Optional<String> original = ctx.registry().originalSha(e.name());
            if (patched.isPresent() && original.isPresent() && original.get().equals(e.baseSha256())) {
                replaces = patched.get().installedBy();
            } else {
                return reject(e, runningSha, List.of(), "patch was built against " + shortSha(e.baseSha256())
                        + " but the server runs " + shortSha(runningSha) + " (different source or build; build the patch"
                        + " against the server jar that runs - versions/<mc>/*.jar in the server directory, -PserverJar)");
            }
        }
        if (runningSha.equals(e.sha256())) {
            return new PatchPlan.Entry(e.name(), PatchPlan.Action.UNCHANGED, List.of(), runningSha, e.sha256(), null, null, null);
        }
        ClassInfo before;
        try {
            before = ClassFileReader.read(running);
        } catch (ClassFormatException ex) {
            return reject(e, runningSha, List.of(), "cannot parse the running class file: " + ex.getMessage());
        }
        List<ClassChange> changes = new ArrayList<>(ClassDiff.compare(before, archive.infos().get(e.name())));
        if (changes.isEmpty()) {
            changes.add(new ClassChange(ChangeKind.ATTRIBUTES_ONLY, ""));
        }
        return new PatchPlan.Entry(e.name(), PatchPlan.Action.REDEFINE, changes, runningSha, e.sha256(), replaces, null, null);
    }

    private static PatchPlan.Entry planNew(PatchArchive archive, PatchManifest.Entry e, Context ctx) {
        try {
            Class.forName(e.name(), false, ctx.loader());
            return reject(e, null, List.of(), "class already exists in the server; a patch cannot define it again");
        } catch (ClassNotFoundException ex) {
            // expected
        } catch (LinkageError ex) {
            return reject(e, null, List.of(), "class name is already in use (" + ex + ")");
        }
        String pkg = archive.infos().get(e.name()).packageName().replace('/', '.');
        String host = null;
        for (Class<?> c : ctx.instrumentation().getAllLoadedClasses()) {
            if (c.getClassLoader() == ctx.loader() && !c.isHidden() && !c.isArray() && pkg.equals(c.getPackageName())) {
                host = c.getName();
                break;
            }
        }
        List<ClassChange> changes = List.of(new ClassChange(ChangeKind.NEW_CLASS, e.name()));
        if (host == null) {
            return reject(e, null, changes, "no class of package " + (pkg.isEmpty() ? "<unnamed>" : pkg)
                    + " is loaded by the server; new classes can only be added to packages that already exist");
        }
        return new PatchPlan.Entry(e.name(), PatchPlan.Action.DEFINE, changes, null, e.sha256(), null, host, null);
    }

    /**
     * This JVM applies redefined classes without running the bytecode verifier (probed), so
     * invalid bytecode could reach the running server. Every class is verified in a
     * throw-away class loader first.
     */
    private static List<PatchPlan.Entry> preVerify(PatchArchive archive, List<PatchPlan.Entry> entries, Context ctx,
                                                   List<String> warnings) {
        Map<String, byte[]> classes = new java.util.LinkedHashMap<>();
        for (PatchPlan.Entry e : entries) {
            if (e.action() == PatchPlan.Action.REDEFINE || e.action() == PatchPlan.Action.DEFINE) {
                classes.put(e.className(), archive.classes().get(e.className()));
            }
        }
        if (classes.isEmpty()) {
            return entries;
        }
        Map<String, PreVerifier.Outcome> outcomes = PreVerifier.verify(classes, ctx.loader());
        warnings.add("this JVM does not verify redefined classes (probed); PaperSwap ran the JVM verifier on "
                + classes.size() + " class(es) of the patch in a throw-away class loader first");
        List<PatchPlan.Entry> out = new ArrayList<>(entries.size());
        for (PatchPlan.Entry e : entries) {
            PreVerifier.Outcome o = outcomes.get(e.className());
            String problem = null;
            if (o != null && o.kind() == PreVerifier.Outcome.Kind.INVALID) {
                problem = "the class file fails bytecode verification (" + o.detail() + ")";
            } else if (o != null && o.kind() == PreVerifier.Outcome.Kind.UNVERIFIABLE) {
                if (ctx.development()) {
                    warnings.add(e.className() + " could not be verified before the redefinition (" + o.detail()
                            + "); the development profile applies it unverified");
                } else {
                    problem = "the class could not be verified before the redefinition (" + o.detail() + ") and this JVM"
                            + " does not verify redefined classes; the production profile does not apply unverified bytecode";
                }
            }
            out.add(problem == null ? e : new PatchPlan.Entry(e.className(), PatchPlan.Action.REJECT, e.changes(),
                    e.runningSha256(), e.newSha256(), e.replaces(), e.hostClass(), problem));
        }
        return out;
    }

    private static PatchPlan.Entry checkCapabilities(PatchPlan.Entry entry, Map<Capability, CapabilityProbe.Result> caps,
                                                     Mechanism mechanism) {
        if (entry.action() == PatchPlan.Action.REJECT || entry.action() == PatchPlan.Action.UNCHANGED) {
            return entry;
        }
        List<String> unsupported = new ArrayList<>();
        for (ClassChange c : entry.changes()) {
            Set<Capability> required = required(c.kind(), mechanism);
            if (required.isEmpty()) {
                unsupported.add(c + (c.kind().required().isEmpty() ? " (never applied by PaperSwap)"
                        : " (PaperSwap applies only method-body changes by retransformation; use 'redefine' on a JVM with"
                        + " enhanced class redefinition)"));
                continue;
            }
            for (Capability cap : required) {
                CapabilityProbe.Result r = caps.get(cap);
                if (r == null || !r.supported()) {
                    unsupported.add(c + " (this JVM cannot " + cap.description()
                            + (r == null ? "" : ": " + r.detail()) + ")");
                    break;
                }
            }
        }
        if (unsupported.isEmpty()) {
            return entry;
        }
        return new PatchPlan.Entry(entry.className(), PatchPlan.Action.REJECT, entry.changes(), entry.runningSha256(),
                entry.newSha256(), entry.replaces(), entry.hostClass(),
                "unsupported change(s): " + String.join("; ", unsupported) + ". A restart with a new server jar is required.");
    }

    static byte[] readResource(ClassLoader loader, String className) throws IOException {
        String path = className.replace('.', '/') + ".class";
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " not found by the server class loader");
            }
            byte[] data = in.readNBytes(PatchArchive.MAX_CLASS_BYTES + 1);
            if (data.length > PatchArchive.MAX_CLASS_BYTES) {
                throw new IOException(path + " is larger than " + PatchArchive.MAX_CLASS_BYTES + " bytes");
            }
            return data;
        }
    }

    /** @param sha digest @return first 12 characters, for messages */
    static String shortSha(String sha) {
        return sha == null ? "-" : sha.substring(0, Math.min(12, sha.length()));
    }
}
