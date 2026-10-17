package dev.paperswap.runtime.api;

import java.util.Objects;

/**
 * Dependency on another runtime module.
 *
 * @param id           id of the required module
 * @param versionRange version constraint such as {@code ">=1.2.0 <2.0.0"}; {@code "*"} accepts any version
 * @param optional     whether the module can run without the dependency
 */
public record ModuleDependency(String id, String versionRange, boolean optional) {
    public ModuleDependency {
        Objects.requireNonNull(id, "id");
        versionRange = versionRange == null || versionRange.isBlank() ? "*" : versionRange.trim();
    }
}
