package dev.lemonpvp.runtime.api;

import java.util.Objects;

/**
 * A permission node declared by a module. The node must start with the module id
 * ({@code <id>.}) so that modules cannot declare permissions of other components.
 *
 * @param node         permission node
 * @param description  description shown to administrators
 * @param defaultValue who has the permission when nothing else is configured
 */
public record PermissionDeclaration(String node, String description, PermissionDefault defaultValue) {
    public PermissionDeclaration {
        Objects.requireNonNull(node, "node");
        description = description == null ? "" : description;
        defaultValue = defaultValue == null ? PermissionDefault.OP : defaultValue;
    }

    /** Platform-neutral permission defaults (mirrors the Bukkit semantics). */
    public enum PermissionDefault {
        TRUE, FALSE, OP, NOT_OP
    }
}
