package dev.paperswap.runtime.livepatch;

import dev.paperswap.runtime.security.Hashing;
import dev.paperswap.runtime.security.PathGuard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metadata of a live patch ({@code paperswap-patch.yml} inside the archive).
 *
 * <pre>
 * format: 1
 * id: fix-tps-output
 * description: "Fix TPS colours"
 * created: "2026-10-14T12:00:00Z"
 * tool: "paperswap-livepatch-tool 1"
 * target:
 *   build-id: "paperswap:paperswap/26.2-DEV-4eff375/1970-01-01T00:00:00Z"
 *   minecraft: "26.2"
 * classes:
 *   - name: org.spigotmc.TicksPerSecondCommand
 *     sha256: &lt;digest of the new class file&gt;
 *     base-sha256: &lt;digest of the class file the patch was built against&gt;
 *   - name: org.spigotmc.TpsFormat
 *     sha256: &lt;digest&gt;
 *     new: true
 * </pre>
 *
 * @param id               patch id
 * @param description      free text (single line)
 * @param created          creation time as written by the tool (informational)
 * @param tool             tool that created the patch (informational)
 * @param targetBuildId    {@link BuildIdentity} of the server build the patch was made for
 * @param targetMinecraft  Minecraft version of that build
 * @param classes          class entries
 */
public record PatchManifest(String id, String description, String created, String tool, String targetBuildId,
                            String targetMinecraft, List<Entry> classes) {

    /** Format version understood by this runtime. */
    public static final int FORMAT = 1;
    /** Name of the manifest inside the archive. */
    public static final String FILE_NAME = "paperswap-patch.yml";
    /** Upper bound for the number of classes in one patch. */
    public static final int MAX_CLASSES = 256;

    private static final Set<String> TOP_KEYS = Set.of("format", "id", "description", "created", "tool", "target", "classes");
    private static final Set<String> TARGET_KEYS = Set.of("build-id", "minecraft");
    private static final Set<String> CLASS_KEYS = Set.of("name", "sha256", "base-sha256", "new");

    public PatchManifest {
        classes = List.copyOf(classes);
    }

    /**
     * One class of the patch.
     *
     * @param name       binary class name
     * @param sha256     digest of the class file in the archive
     * @param baseSha256 digest of the class file the patch was built against; {@code null} for new classes
     * @param isNew      whether the class does not exist in the target build
     */
    public record Entry(String name, String sha256, String baseSha256, boolean isNew) {

        /** @return path of the class file inside the archive */
        public String archivePath() {
            return "classes/" + this.name.replace('.', '/') + ".class";
        }
    }

    /**
     * Validates parsed YAML.
     *
     * @param raw parsed {@code paperswap-patch.yml}
     * @return the manifest
     * @throws PatchException if anything is missing, unknown or malformed
     */
    public static PatchManifest parse(Map<String, Object> raw) throws PatchException {
        List<String> errors = new ArrayList<>();
        for (String key : raw.keySet()) {
            if (!TOP_KEYS.contains(key)) {
                errors.add("unknown key '" + PathGuard.sanitize(key) + "'");
            }
        }
        if (!(raw.get("format") instanceof Integer f) || f != FORMAT) {
            errors.add("format must be " + FORMAT + " (got " + PathGuard.sanitize(String.valueOf(raw.get("format"))) + ")");
        }
        String id = string(raw, "id", errors, true);
        if (id != null && PatchIds.problem(id) != null) {
            errors.add("id: " + PatchIds.problem(id));
        }
        String description = string(raw, "description", errors, false);
        String created = string(raw, "created", errors, false);
        String tool = string(raw, "tool", errors, false);
        String buildId = null;
        String minecraft = null;
        if (raw.get("target") instanceof Map<?, ?> target) {
            for (Object key : target.keySet()) {
                if (!TARGET_KEYS.contains(String.valueOf(key))) {
                    errors.add("target: unknown key '" + PathGuard.sanitize(String.valueOf(key)) + "'");
                }
            }
            buildId = target.get("build-id") instanceof String s ? s : null;
            minecraft = target.get("minecraft") instanceof String s ? s : null;
            if (buildId == null || buildId.isBlank()) {
                errors.add("target.build-id is required");
            }
            if (minecraft == null || minecraft.isBlank()) {
                errors.add("target.minecraft is required");
            }
        } else {
            errors.add("target is required (build-id, minecraft)");
        }
        List<Entry> entries = new ArrayList<>();
        if (raw.get("classes") instanceof List<?> list) {
            if (list.isEmpty()) {
                errors.add("classes must not be empty");
            }
            if (list.size() > MAX_CLASSES) {
                errors.add("a patch may contain at most " + MAX_CLASSES + " classes");
            }
            Set<String> seen = new HashSet<>();
            for (Object o : list) {
                Entry e = entry(o, errors);
                if (e != null && !seen.add(e.name())) {
                    errors.add("class " + e.name() + " is listed twice");
                }
                if (e != null) {
                    entries.add(e);
                }
            }
        } else {
            errors.add("classes must be a list");
        }
        if (!errors.isEmpty()) {
            throw new PatchException("invalid " + FILE_NAME + ": " + String.join("; ", errors));
        }
        return new PatchManifest(id, description == null ? "" : description, created == null ? "" : created,
                tool == null ? "" : tool, buildId, minecraft, entries);
    }

    private static Entry entry(Object o, List<String> errors) {
        if (!(o instanceof Map<?, ?> m)) {
            errors.add("every classes entry must be a map");
            return null;
        }
        for (Object key : m.keySet()) {
            if (!CLASS_KEYS.contains(String.valueOf(key))) {
                errors.add("classes: unknown key '" + PathGuard.sanitize(String.valueOf(key)) + "'");
            }
        }
        String name = m.get("name") instanceof String s ? s : null;
        String sha = m.get("sha256") instanceof String s ? s : null;
        String base = m.get("base-sha256") instanceof String s ? s : null;
        Object newValue = m.get("new");
        boolean isNew = Boolean.TRUE.equals(newValue);
        if (newValue != null && !(newValue instanceof Boolean)) {
            errors.add("classes: 'new' must be true or false");
        }
        String classProblem = PatchIds.classProblem(name);
        if (classProblem != null) {
            errors.add("class '" + PathGuard.sanitize(String.valueOf(name)) + "': " + classProblem);
            return null;
        }
        if (sha == null || !Hashing.isSha256(sha)) {
            errors.add("class " + name + ": sha256 must be a lower-case SHA-256 hex digest");
        }
        if (isNew && base != null) {
            errors.add("class " + name + ": a new class has no base-sha256");
        }
        if (!isNew && (base == null || !Hashing.isSha256(base))) {
            errors.add("class " + name + ": base-sha256 is required for existing classes");
        }
        return new Entry(name, sha, base, isNew);
    }

    private static String string(Map<String, Object> raw, String key, List<String> errors, boolean required) {
        Object v = raw.get(key);
        if (v == null) {
            if (required) {
                errors.add(key + " is required");
            }
            return null;
        }
        if (!(v instanceof String s)) {
            errors.add(key + " must be a string");
            return null;
        }
        if (s.length() > 500) {
            errors.add(key + " is longer than 500 characters");
        }
        return s;
    }

    /** @return the manifest as YAML (only safe, quoted scalars) */
    public String toYaml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# PaperSwap live patch. Created by the patch tool; do not edit by hand (digests must match).\n");
        sb.append("format: ").append(FORMAT).append('\n');
        sb.append("id: ").append(quote(this.id)).append('\n');
        sb.append("description: ").append(quote(this.description)).append('\n');
        sb.append("created: ").append(quote(this.created)).append('\n');
        sb.append("tool: ").append(quote(this.tool)).append('\n');
        sb.append("target:\n");
        sb.append("  build-id: ").append(quote(this.targetBuildId)).append('\n');
        sb.append("  minecraft: ").append(quote(this.targetMinecraft)).append('\n');
        sb.append("classes:\n");
        for (Entry e : this.classes) {
            sb.append("  - name: ").append(quote(e.name())).append('\n');
            sb.append("    sha256: ").append(quote(e.sha256())).append('\n');
            if (e.isNew()) {
                sb.append("    new: true\n");
            } else {
                sb.append("    base-sha256: ").append(quote(e.baseSha256())).append('\n');
            }
        }
        return sb.toString();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : (s == null ? "" : s).toCharArray()) {
            if (ch == '\\' || ch == '"') {
                sb.append('\\');
            }
            sb.append(Character.isISOControl(ch) ? '?' : ch);
        }
        return sb.append('"').toString();
    }
}
