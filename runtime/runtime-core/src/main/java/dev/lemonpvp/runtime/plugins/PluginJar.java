package dev.lemonpvp.runtime.plugins;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of inspecting a plugin jar without loading any of its classes.
 *
 * @param file                 the jar
 * @param sha256               SHA-256 of the jar
 * @param size                 size in bytes
 * @param info                 parsed descriptor, or {@code null} if it could not be read
 * @param problems             defects that prevent loading the plugin at all
 * @param lifecycleApiClasses  classes that use Paper's lifecycle API (first few)
 * @param internalsClasses     classes that use net.minecraft / CraftBukkit internals (first few)
 * @param maxClassVersion      highest class file major version found
 * @param classCount           number of class files
 */
public record PluginJar(Path file, String sha256, long size, PluginInfo info, List<String> problems,
                        List<String> lifecycleApiClasses, List<String> internalsClasses, int maxClassVersion, int classCount) {
    public PluginJar {
        problems = List.copyOf(problems);
        lifecycleApiClasses = List.copyOf(lifecycleApiClasses);
        internalsClasses = List.copyOf(internalsClasses);
    }
}
