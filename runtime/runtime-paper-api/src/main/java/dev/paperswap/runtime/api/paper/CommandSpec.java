package dev.paperswap.runtime.api.paper;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Static properties of a module command.
 *
 * @param label       command label, lower case ({@code [a-z0-9_-]{1,32}})
 * @param description description shown in help
 * @param usage       usage message
 * @param aliases     aliases (same rules as the label)
 * @param permission  permission required to run the command, or {@code null}
 */
public record CommandSpec(String label, String description, String usage, List<String> aliases, String permission) {

    private static final Pattern LABEL = Pattern.compile("[a-z0-9_-]{1,32}");

    public CommandSpec {
        Objects.requireNonNull(label, "label");
        if (!LABEL.matcher(label).matches()) {
            throw new IllegalArgumentException("invalid command label: " + label);
        }
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        for (String a : aliases) {
            if (!LABEL.matcher(a).matches()) {
                throw new IllegalArgumentException("invalid command alias: " + a);
            }
        }
        description = description == null ? "" : description;
        usage = usage == null || usage.isEmpty() ? "/" + label : usage;
    }

    /** @param label label @return a spec without aliases and permission */
    public static CommandSpec of(String label) {
        return new CommandSpec(label, "", null, List.of(), null);
    }

    /** @param permission permission node @return a copy requiring the permission */
    public CommandSpec withPermission(String permission) {
        return new CommandSpec(this.label, this.description, this.usage, this.aliases, permission);
    }

    /** @param description description @return a copy with the description */
    public CommandSpec withDescription(String description) {
        return new CommandSpec(this.label, description, this.usage, this.aliases, this.permission);
    }

    /** @param aliases aliases @return a copy with the aliases */
    public CommandSpec withAliases(String... aliases) {
        return new CommandSpec(this.label, this.description, this.usage, List.of(aliases), this.permission);
    }
}
