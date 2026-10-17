package dev.paperswap.runtime.livepatch;

import dev.paperswap.runtime.config.YamlSupport;
import dev.paperswap.runtime.livepatch.classfile.ClassFileReader;
import dev.paperswap.runtime.livepatch.classfile.ClassFormatException;
import dev.paperswap.runtime.livepatch.classfile.ClassInfo;
import dev.paperswap.runtime.security.Hashing;
import dev.paperswap.runtime.security.PathGuard;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A live patch archive ({@code <id>.swappatch}): a zip file with {@code paperswap-patch.yml}
 * and the new class files under {@code classes/}.
 *
 * <p>The archive is parsed from bytes that were already hashed, so the bytes that were
 * checked are exactly the bytes that are used. Parsing enforces: only the manifest and
 * listed class files, no duplicates, no path tricks, size limits, every class file
 * matches its digest and declares the class name it is listed under.</p>
 *
 * @param manifest  validated manifest
 * @param classes   class file bytes by binary class name (manifest order)
 * @param infos     parsed structure of every class file
 * @param sha256    digest of the whole archive
 */
public record PatchArchive(PatchManifest manifest, Map<String, byte[]> classes, Map<String, ClassInfo> infos, String sha256) {

    /** Largest accepted archive. */
    public static final int MAX_ARCHIVE_BYTES = 16 * 1024 * 1024;
    /** Largest accepted class file. */
    public static final int MAX_CLASS_BYTES = 4 * 1024 * 1024;
    /** Largest accepted sum of uncompressed entries. */
    public static final long MAX_TOTAL_BYTES = 64L * 1024 * 1024;

    public PatchArchive {
        classes = Collections.unmodifiableMap(new LinkedHashMap<>(classes));
        infos = Collections.unmodifiableMap(new LinkedHashMap<>(infos));
    }

    /**
     * Parses an archive.
     *
     * @param bytes archive content (already read into memory)
     * @return the archive
     * @throws PatchException if the archive is invalid
     */
    public static PatchArchive parse(byte[] bytes) throws PatchException {
        if (bytes.length > MAX_ARCHIVE_BYTES) {
            throw new PatchException("patch archive is larger than " + MAX_ARCHIVE_BYTES + " bytes");
        }
        String sha = Hashing.sha256(bytes);
        byte[] manifestBytes = null;
        Map<String, byte[]> entries = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > PatchManifest.MAX_CLASSES + 64) {
                    throw new PatchException("patch archive has too many entries");
                }
                String name = entry.getName();
                if (!names.add(name)) {
                    throw new PatchException("duplicate archive entry " + PathGuard.sanitize(name));
                }
                if (entry.isDirectory()) {
                    if (!name.equals("classes/") && !(name.startsWith("classes/") && isSafeEntry(name.substring(0, name.length() - 1)))) {
                        throw new PatchException("unexpected directory entry " + PathGuard.sanitize(name));
                    }
                    continue;
                }
                if (!name.equals(PatchManifest.FILE_NAME) && !(name.startsWith("classes/") && name.endsWith(".class") && isSafeEntry(name))) {
                    throw new PatchException("unexpected archive entry " + PathGuard.sanitize(name)
                            + " (allowed: " + PatchManifest.FILE_NAME + ", classes/**.class)");
                }
                int limit = name.equals(PatchManifest.FILE_NAME) ? YamlSupport.MAX_DOCUMENT_BYTES : MAX_CLASS_BYTES;
                byte[] data = readLimited(zip, limit, name);
                total += data.length;
                if (total > MAX_TOTAL_BYTES) {
                    throw new PatchException("patch archive expands to more than " + MAX_TOTAL_BYTES + " bytes");
                }
                if (name.equals(PatchManifest.FILE_NAME)) {
                    manifestBytes = data;
                } else {
                    entries.put(name, data);
                }
            }
        } catch (IOException e) {
            throw new PatchException("not a valid zip archive: " + e.getMessage());
        }
        if (manifestBytes == null) {
            throw new PatchException("archive has no " + PatchManifest.FILE_NAME);
        }
        PatchManifest manifest;
        try {
            manifest = PatchManifest.parse(YamlSupport.load(new ByteArrayInputStream(manifestBytes), PatchManifest.FILE_NAME));
        } catch (YamlSupport.YamlFormatException e) {
            throw new PatchException("invalid " + PatchManifest.FILE_NAME + ": " + e.getMessage());
        }
        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, ClassInfo> infos = new LinkedHashMap<>();
        for (PatchManifest.Entry e : manifest.classes()) {
            byte[] data = entries.remove(e.archivePath());
            if (data == null) {
                throw new PatchException("class " + e.name() + " is listed but " + e.archivePath() + " is missing");
            }
            String actual = Hashing.sha256(data);
            if (!actual.equals(e.sha256())) {
                throw new PatchException("class " + e.name() + ": digest mismatch (manifest " + e.sha256() + ", file " + actual + ")");
            }
            ClassInfo info;
            try {
                info = ClassFileReader.read(data);
            } catch (ClassFormatException ex) {
                throw new PatchException("class " + e.name() + " is not a valid class file: " + ex.getMessage());
            }
            if (!info.javaName().equals(e.name())) {
                throw new PatchException("class file " + e.archivePath() + " declares " + info.javaName() + " instead of " + e.name());
            }
            classes.put(e.name(), data);
            infos.put(e.name(), info);
        }
        if (!entries.isEmpty()) {
            throw new PatchException("archive contains class files that are not listed in " + PatchManifest.FILE_NAME + ": "
                    + entries.keySet().stream().map(PathGuard::sanitize).limit(5).toList());
        }
        return new PatchArchive(manifest, classes, infos, sha);
    }

    private static boolean isSafeEntry(String name) {
        if (name.contains("\\") || name.startsWith("/") || name.contains("//")) {
            return false;
        }
        for (String segment : name.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }

    private static byte[] readLimited(InputStream in, int limit, String name) throws IOException, PatchException {
        byte[] data = in.readNBytes(limit + 1);
        if (data.length > limit) {
            throw new PatchException(PathGuard.sanitize(name) + " is larger than " + limit + " bytes");
        }
        return data;
    }

    /**
     * Writes an archive (used by the patch tool). Entries are written in a fixed order with
     * a fixed timestamp, so the same input produces the same bytes.
     *
     * @param manifest manifest
     * @param classes  class bytes by binary name, matching the manifest
     * @return archive bytes
     * @throws IOException never for in-memory output
     */
    public static byte[] write(PatchManifest manifest, Map<String, byte[]> classes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, PatchManifest.FILE_NAME, manifest.toYaml().getBytes(StandardCharsets.UTF_8));
            for (PatchManifest.Entry e : manifest.classes()) {
                byte[] data = classes.get(e.name());
                if (data == null) {
                    throw new IllegalArgumentException("no bytes for " + e.name());
                }
                put(zip, e.archivePath(), data);
            }
        }
        return bytes.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(315532800000L); // 1980-01-01, the earliest zip timestamp
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }
}
