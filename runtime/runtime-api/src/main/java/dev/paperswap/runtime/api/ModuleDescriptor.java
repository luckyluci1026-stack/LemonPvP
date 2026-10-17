package dev.paperswap.runtime.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable metadata of a module, parsed from {@code paperswap-module.yml}.
 *
 * @param id                 unique module id ({@code [a-z][a-z0-9_-]{1,47}})
 * @param version            semantic version of the module ({@code MAJOR.MINOR.PATCH[-pre]})
 * @param name               display name
 * @param description        short description, may be empty
 * @param authors            author / developer names
 * @param mainClass          fully qualified class implementing {@link RuntimeModule}
 * @param apiVersion         runtime API version the module was written against
 * @param minecraftVersions  supported Minecraft version patterns ({@code "26.2"}, {@code "26.*"}, {@code "*"})
 * @param capabilities       runtime capabilities the module requests
 * @param permissions        permissions the module declares
 * @param dependencies       other runtime modules this module depends on
 * @param pluginDependencies names of server plugins whose classes the module needs
 * @param loadOrder          ordering hint for start-up, lower values start first
 * @param resources          files inside the jar copied to the data directory on first load
 */
public record ModuleDescriptor(
        String id,
        String version,
        String name,
        String description,
        List<String> authors,
        String mainClass,
        int apiVersion,
        List<String> minecraftVersions,
        Set<Capability> capabilities,
        List<PermissionDeclaration> permissions,
        List<ModuleDependency> dependencies,
        List<String> pluginDependencies,
        int loadOrder,
        List<String> resources
) {
    public ModuleDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mainClass, "mainClass");
        description = description == null ? "" : description;
        authors = List.copyOf(authors);
        minecraftVersions = List.copyOf(minecraftVersions);
        capabilities = Set.copyOf(capabilities);
        permissions = List.copyOf(permissions);
        dependencies = List.copyOf(dependencies);
        pluginDependencies = List.copyOf(pluginDependencies);
        resources = List.copyOf(resources);
    }

    /**
     * @param capability capability to check
     * @return whether the module declared the capability
     */
    public boolean has(Capability capability) {
        return this.capabilities.contains(capability);
    }

    /** @return {@code id@version} */
    public String coordinates() {
        return this.id + "@" + this.version;
    }
}
