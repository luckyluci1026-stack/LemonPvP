package dev.paperswap.runtime.loader;

import dev.paperswap.runtime.api.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A module jar found in the module directory together with its parsed descriptor.
 *
 * @param file       absolute path of the jar inside the module directory
 * @param size       file size in bytes at discovery time
 * @param descriptor the validated descriptor
 */
public record ModuleArtifact(Path file, long size, ModuleDescriptor descriptor) {
    public ModuleArtifact {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    /** @return module id */
    public String id() {
        return this.descriptor.id();
    }
}
