package dev.paperswap.runtime.livepatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Identity of a server build, derived from the manifest of the server jar
 * ({@code Brand-Id}, {@code Implementation-Version} = {@code <mc>-<build>-<git commit>},
 * {@code Build-Time}). The patch tool reads the same attributes from the baseline jar, so
 * both sides compute the same string for the same build.
 *
 * <p>The build id is a coarse check. The precise check is the per-class base digest: a
 * patch only applies to a class whose running bytecode has exactly the digest the patch
 * was built against.</p>
 */
public final class BuildIdentity {

    /** Value used when the manifest does not carry the attributes. */
    public static final String UNKNOWN = "unknown";

    private BuildIdentity() {
    }

    /**
     * @param manifest jar manifest (may be {@code null})
     * @return build id, e.g. {@code paperswap:paperswap/26.2-DEV-4eff375/1970-01-01T00:00:00Z}
     */
    public static String of(Manifest manifest) {
        if (manifest == null) {
            return UNKNOWN;
        }
        Attributes a = manifest.getMainAttributes();
        String brand = a.getValue("Brand-Id");
        String version = a.getValue("Implementation-Version");
        if (brand == null || version == null) {
            return UNKNOWN;
        }
        String time = a.getValue("Build-Time");
        return brand.strip() + "/" + version.strip() + "/" + (time == null ? "-" : time.strip());
    }

    /**
     * @param jar server jar
     * @return build id of that jar
     * @throws IOException if the jar cannot be read
     */
    public static String of(Path jar) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            return of(file.getManifest());
        }
    }

    /**
     * Reads the manifest of the jar (or directory) a class was loaded from.
     *
     * @param anyClassOfTheBuild e.g. the server implementation class
     * @return the build id, or {@link #UNKNOWN}
     */
    public static String ofCodeSource(Class<?> anyClassOfTheBuild) {
        try {
            var source = anyClassOfTheBuild.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return UNKNOWN;
            }
            Path location = Path.of(source.getLocation().toURI());
            if (location.toString().endsWith(".jar")) {
                return of(location);
            }
            Path mf = location.resolve("META-INF/MANIFEST.MF");
            try (InputStream in = java.nio.file.Files.newInputStream(mf)) {
                return of(new Manifest(in));
            }
        } catch (IOException | java.net.URISyntaxException | RuntimeException e) {
            return UNKNOWN;
        }
    }
}
