package dev.paperswap.runtime.api;

/**
 * Information about the runtime and the hosting server.
 *
 * @param runtimeVersion   version of the hot-deployment runtime
 * @param apiVersion       highest module API version supported by the runtime
 * @param minecraftVersion Minecraft version of the running server, e.g. {@code "26.2"}
 * @param serverBrand      server brand name, e.g. {@code "PaperSwap"}
 * @param serverVersion    full server version string
 */
public record RuntimeInfo(
        String runtimeVersion,
        int apiVersion,
        String minecraftVersion,
        String serverBrand,
        String serverVersion
) {
}
