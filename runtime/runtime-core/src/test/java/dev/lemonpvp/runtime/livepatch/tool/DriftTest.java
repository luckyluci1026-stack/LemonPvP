package dev.lemonpvp.runtime.livepatch.tool;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The reference check of {@link LivePatchTool}: a remapped server jar names the declaring class
 * in member references where javac names the receiver's type (seen in the 1.21.11 paperclip
 * jar: {@code Entity.firstTick} vs {@code LivingEntity.firstTick}). That is the same member,
 * unless a class in between declares a member with the same name and type.
 */
class DriftTest {

    @TempDir
    Path dir;

    private Path jar(String name, Map<String, String> sources) throws Exception {
        ModuleJarBuilder b = ModuleJarBuilder.create().noDescriptor();
        sources.forEach(b::source);
        return b.writeTo(this.dir, name, false);
    }

    private static byte[] entry(Path jar, String cls) throws Exception {
        try (JarFile f = new JarFile(jar.toFile()); InputStream in = f.getInputStream(f.getEntry(cls.replace('.', '/') + ".class"))) {
            return in.readAllBytes();
        }
    }

    /** Points every Fieldref/Methodref of {@code bytes} whose class is {@code from} at the class constant {@code to}. */
    private static byte[] retarget(byte[] bytes, String from, String to) {
        byte[] b = bytes.clone();
        int count = u2(b, 8);
        int[] offset = new int[count];
        int[] tag = new int[count];
        int p = 10;
        for (int i = 1; i < count; i++) {
            offset[i] = p;
            tag[i] = b[p] & 0xFF;
            switch (tag[i]) {
                case 1 -> p += 3 + u2(b, p + 1);
                case 7, 8, 16, 19, 20 -> p += 3;
                case 15 -> p += 4;
                case 3, 4, 9, 10, 11, 12, 17, 18 -> p += 5;
                case 5, 6 -> {
                    p += 9;
                    i++;
                }
                default -> throw new IllegalStateException("tag " + tag[i]);
            }
        }
        int fromIndex = 0;
        int toIndex = 0;
        for (int i = 1; i < count; i++) {
            if (tag[i] == 7) {
                int nameAt = offset[u2(b, offset[i] + 1)];
                String name = new String(b, nameAt + 3, u2(b, nameAt + 1), StandardCharsets.UTF_8);
                fromIndex = name.equals(from) ? i : fromIndex;
                toIndex = name.equals(to) ? i : toIndex;
            }
        }
        assertTrue(fromIndex > 0 && toIndex > 0, "class constants present");
        for (int i = 1; i < count; i++) {
            if ((tag[i] == 9 || tag[i] == 10) && u2(b, offset[i] + 1) == fromIndex) {
                b[offset[i] + 1] = (byte) (toIndex >> 8);
                b[offset[i] + 2] = (byte) toIndex;
            }
        }
        return b;
    }

    private static int u2(byte[] b, int at) {
        return ((b[at] & 0xFF) << 8) | (b[at + 1] & 0xFF);
    }

    @Test
    void memberReferencesThroughTheDeclaringClassAreTheSameMember() throws Exception {
        Path jar = jar("inherited.jar", Map.of(
                "dt.Base", "package dt;\npublic class Base { protected int x = 1; protected int y() { return 2; } }\n",
                "dt.A", "package dt;\npublic class A extends Base { public int f() { return this.x + y(); } }\n"));
        byte[] javac = entry(jar, "dt.A");
        byte[] remapped = retarget(javac, "dt/A", "dt/Base");
        assertNull(LivePatchTool.drift(remapped, javac, new LivePatchTool.Hierarchy(jar)));
    }

    @Test
    void aShadowingDeclarationInBetweenIsARealDifference() throws Exception {
        Path jar = jar("shadowed.jar", Map.of(
                "dt.Base", "package dt;\npublic class Base { protected int x = 1; }\n",
                "dt.A", "package dt;\npublic class A extends Base { protected int x = 3; public int f() { return this.x; } }\n"));
        byte[] javac = entry(jar, "dt.A");
        byte[] remapped = retarget(javac, "dt/A", "dt/Base");
        String drift = LivePatchTool.drift(remapped, javac, new LivePatchTool.Hierarchy(jar));
        assertNotNull(drift);
        assertTrue(drift.contains("dt/Base.x"), drift);
    }
}
