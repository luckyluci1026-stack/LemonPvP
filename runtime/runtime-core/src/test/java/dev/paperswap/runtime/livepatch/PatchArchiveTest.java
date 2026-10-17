package dev.paperswap.runtime.livepatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.paperswap.runtime.security.Hashing;
import dev.paperswap.runtime.testsupport.ModuleJarBuilder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Archive and manifest validation. */
class PatchArchiveTest {

    @TempDir
    Path dir;

    private byte[] classBytes() throws Exception {
        Path jar = ModuleJarBuilder.create().noDescriptor().source("pa.C", "package pa; public class C { }").writeTo(this.dir, "c.jar", false);
        try (JarFile f = new JarFile(jar.toFile()); InputStream in = f.getInputStream(f.getEntry("pa/C.class"))) {
            return in.readAllBytes();
        }
    }

    private static PatchManifest manifest(String name, byte[] bytes) {
        return new PatchManifest("p1", "", "", "", "b", "26.2",
                List.of(new PatchManifest.Entry(name, Hashing.sha256(bytes), "0".repeat(64), false)));
    }

    private static byte[] zip(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (var e : entries.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue());
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    void roundTrip() throws Exception {
        byte[] c = classBytes();
        PatchArchive a = PatchArchive.parse(PatchArchive.write(manifest("pa.C", c), Map.of("pa.C", c)));
        assertEquals("p1", a.manifest().id());
        assertEquals("pa.C", a.infos().get("pa.C").javaName());
        assertEquals(PatchArchive.write(manifest("pa.C", c), Map.of("pa.C", c)).length, PatchArchive.write(manifest("pa.C", c),
                Map.of("pa.C", c)).length, "deterministic output");
    }

    @Test
    void rejectsPathTricksExtraFilesDuplicatesAndMismatches() throws Exception {
        byte[] c = classBytes();
        byte[] yml = manifest("pa.C", c).toYaml().getBytes(StandardCharsets.UTF_8);
        Map<String, byte[]> slip = new LinkedHashMap<>();
        slip.put(PatchManifest.FILE_NAME, yml);
        slip.put("classes/../pa/C.class", c);
        assertTrue(assertThrows(PatchException.class, () -> PatchArchive.parse(zip(slip))).getMessage().contains("unexpected archive entry"));

        Map<String, byte[]> extra = new LinkedHashMap<>();
        extra.put(PatchManifest.FILE_NAME, yml);
        extra.put("classes/pa/C.class", c);
        extra.put("run.sh", new byte[] {1});
        assertTrue(assertThrows(PatchException.class, () -> PatchArchive.parse(zip(extra))).getMessage().contains("run.sh"));

        Map<String, byte[]> unlisted = new LinkedHashMap<>();
        unlisted.put(PatchManifest.FILE_NAME, yml);
        unlisted.put("classes/pa/C.class", c);
        unlisted.put("classes/pa/D.class", c);
        assertTrue(assertThrows(PatchException.class, () -> PatchArchive.parse(zip(unlisted))).getMessage().contains("not listed"));

        Map<String, byte[]> wrongName = new LinkedHashMap<>();
        wrongName.put(PatchManifest.FILE_NAME, manifest("pa.D", c).toYaml().getBytes(StandardCharsets.UTF_8));
        wrongName.put("classes/pa/D.class", c);
        assertTrue(assertThrows(PatchException.class, () -> PatchArchive.parse(zip(wrongName))).getMessage().contains("declares pa.C"));

        Map<String, byte[]> tampered = new LinkedHashMap<>();
        tampered.put(PatchManifest.FILE_NAME, yml);
        byte[] changed = c.clone();
        changed[changed.length - 1] ^= 1;
        tampered.put("classes/pa/C.class", changed);
        assertTrue(assertThrows(PatchException.class, () -> PatchArchive.parse(zip(tampered))).getMessage().contains("digest mismatch"));

        Map<String, byte[]> missing = new LinkedHashMap<>();
        missing.put(PatchManifest.FILE_NAME, yml);
        assertTrue(assertThrows(PatchException.class, () -> PatchArchive.parse(zip(missing))).getMessage().contains("is missing"));
    }

    @Test
    void manifestIsStrict() {
        PatchException e = assertThrows(PatchException.class, () -> PatchManifest.parse(Map.of("format", 2, "id", "Bad Id",
                "unknown", 1, "classes", List.of(Map.of("name", "java.lang.String", "sha256", "x")))));
        String m = e.getMessage();
        assertTrue(m.contains("format must be 1") && m.contains("id:") && m.contains("unknown key") && m.contains("cannot be live-patched")
                && m.contains("target is required"), m);
        e = assertThrows(PatchException.class, () -> PatchManifest.parse(Map.of("format", 1, "id", "ok-id",
                "target", Map.of("build-id", "b", "minecraft", "26.2"),
                "classes", List.of(Map.of("name", "a.B", "sha256", "0".repeat(64), "new", true, "base-sha256", "0".repeat(64)),
                        Map.of("name", "a.C", "sha256", "0".repeat(64))))));
        assertTrue(e.getMessage().contains("a new class has no base-sha256") && e.getMessage().contains("base-sha256 is required"), e.getMessage());
        assertTrue(PatchIds.classProblem("dev.paperswap.runtime.livepatch.LivePatchService") != null, "the patcher cannot patch itself");
    }
}
