package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.api.ModuleDependency;
import dev.paperswap.runtime.api.ModuleDescriptor;
import dev.paperswap.runtime.api.ModuleState;
import dev.paperswap.runtime.descriptor.VersionRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency checks between modules. Pure functions over snapshots of module state. */
final class DependencyResolver {

    private DependencyResolver() {
    }

    /**
     * Result of resolving the dependencies of a descriptor against loaded instances.
     *
     * @param problems blocking problems (missing or incompatible required dependencies)
     * @param warnings optional dependencies that are absent or incompatible
     * @param linked   instances whose class loaders the new module may use
     */
    record Resolution(List<String> problems, List<String> warnings, List<ModuleInstance> linked) {
    }

    /**
     * @param descriptor    module to resolve
     * @param loaded        currently loaded instances by id
     * @param requireEnabled whether required dependencies must be enabled (enable) or only loaded (load)
     * @return the resolution
     */
    static Resolution resolve(ModuleDescriptor descriptor, Map<String, ModuleInstance> loaded, boolean requireEnabled) {
        List<String> problems = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ModuleInstance> linked = new ArrayList<>();
        for (ModuleDependency dep : descriptor.dependencies()) {
            ModuleInstance inst = loaded.get(dep.id());
            boolean present = inst != null && isAlive(inst.state());
            if (!present) {
                if (dep.optional()) {
                    warnings.add("optional dependency '" + dep.id() + "' is not loaded");
                } else {
                    problems.add("requires module '" + dep.id() + "' (" + dep.versionRange() + ") which is not loaded");
                }
                continue;
            }
            if (!VersionRange.parse(dep.versionRange()).matches(inst.descriptor().version())) {
                String msg = "module '" + dep.id() + "' " + inst.descriptor().version() + " does not satisfy " + dep.versionRange();
                if (dep.optional()) {
                    warnings.add("optional dependency ignored: " + msg);
                } else {
                    problems.add(msg);
                }
                continue;
            }
            if (requireEnabled && !dep.optional() && inst.state() != ModuleState.ENABLED) {
                problems.add("required module '" + dep.id() + "' is " + inst.state() + ", not ENABLED");
                continue;
            }
            linked.add(inst);
        }
        return new Resolution(problems, warnings, linked);
    }

    /**
     * Checks that every linked dependency of an already loaded instance is enabled.
     *
     * @param instance instance about to be enabled
     * @param loaded   currently loaded instances
     * @return problems
     */
    static List<String> checkLinkedEnabled(ModuleInstance instance, Map<String, ModuleInstance> loaded) {
        List<String> problems = new ArrayList<>();
        Map<String, Boolean> optional = new HashMap<>();
        for (ModuleDependency d : instance.descriptor().dependencies()) {
            optional.put(d.id(), d.optional());
        }
        for (String id : instance.linkedDependencies()) {
            ModuleInstance dep = loaded.get(id);
            if (dep == null || !isAlive(dep.state())) {
                problems.add("linked dependency '" + id + "' was unloaded; reload this module");
            } else if (!Boolean.TRUE.equals(optional.get(id)) && dep.state() != ModuleState.ENABLED) {
                problems.add("required module '" + id + "' is " + dep.state() + ", not ENABLED");
            }
        }
        for (ModuleDependency d : instance.descriptor().dependencies()) {
            if (!d.optional() && !instance.linkedDependencies().contains(d.id())) {
                problems.add("required module '" + d.id() + "' is not linked; reload this module");
            }
        }
        return problems;
    }

    /**
     * @param id     module id
     * @param loaded loaded instances
     * @param onlyEnabled whether to return only enabled dependents
     * @return ids of instances that link to {@code id}
     */
    static List<String> dependents(String id, Collection<ModuleInstance> loaded, boolean onlyEnabled) {
        List<String> out = new ArrayList<>();
        for (ModuleInstance inst : loaded) {
            if (inst.id().equals(id) || !isAlive(inst.state())) {
                continue;
            }
            if (onlyEnabled && inst.state() != ModuleState.ENABLED && inst.state() != ModuleState.ENABLING) {
                continue;
            }
            if (inst.linkedDependencies().contains(id)) {
                out.add(inst.id());
            }
        }
        out.sort(null);
        return out;
    }

    /**
     * Orders descriptors so that dependencies come first. Ties are broken by
     * {@code load-order}, then by id. Modules on a dependency cycle are left out.
     *
     * @param descriptors descriptors to order
     * @param problems    receives cycle problems
     * @return ordered descriptors
     */
    static List<ModuleDescriptor> startupOrder(Collection<ModuleDescriptor> descriptors, List<String> problems) {
        Map<String, ModuleDescriptor> byId = new LinkedHashMap<>();
        descriptors.stream()
                .sorted(Comparator.comparingInt(ModuleDescriptor::loadOrder).thenComparing(ModuleDescriptor::id))
                .forEach(d -> byId.put(d.id(), d));
        List<ModuleDescriptor> out = new ArrayList<>();
        Set<String> done = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Set<String> cyclic = new HashSet<>();
        for (String id : byId.keySet()) {
            visit(id, byId, done, visiting, cyclic, out);
        }
        for (String id : cyclic) {
            problems.add("module '" + id + "' is part of, or depends on, a dependency cycle");
        }
        out.removeIf(d -> cyclic.contains(d.id()));
        return out;
    }

    private static boolean visit(String id, Map<String, ModuleDescriptor> byId, Set<String> done, Set<String> visiting,
                                 Set<String> cyclic, List<ModuleDescriptor> out) {
        if (done.contains(id)) {
            return !cyclic.contains(id);
        }
        if (!visiting.add(id)) {
            cyclic.add(id);
            return false;
        }
        ModuleDescriptor d = byId.get(id);
        boolean ok = true;
        for (ModuleDependency dep : d.dependencies()) {
            if (byId.containsKey(dep.id()) && !visit(dep.id(), byId, done, visiting, cyclic, out)) {
                ok = false;
                cyclic.add(id);
            }
        }
        visiting.remove(id);
        done.add(id);
        out.add(d);
        return ok;
    }

    static boolean isAlive(ModuleState state) {
        return state == ModuleState.LOADED || state == ModuleState.ENABLING || state == ModuleState.ENABLED
                || state == ModuleState.DISABLING || state == ModuleState.DISABLED;
    }
}
