package dev.paperswap.runtime;

/** Version of the hot-deployment runtime (independent of the Minecraft version). */
public final class RuntimeVersion {

    /** Runtime version. Bumped together with {@code runtimeVersion} in {@code runtime/gradle.properties}. */
    public static final String VERSION = "0.1.0";

    private RuntimeVersion() {
    }
}
