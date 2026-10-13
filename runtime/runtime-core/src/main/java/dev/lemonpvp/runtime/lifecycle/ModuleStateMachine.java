package dev.lemonpvp.runtime.lifecycle;

import dev.lemonpvp.runtime.api.ModuleState;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Allowed lifecycle transitions. Every state change of a module instance is checked here. */
public final class ModuleStateMachine {

    private static final Map<ModuleState, Set<ModuleState>> ALLOWED = new EnumMap<>(ModuleState.class);

    static {
        ALLOWED.put(ModuleState.DISCOVERED, EnumSet.of(ModuleState.VALIDATED, ModuleState.FAILED));
        ALLOWED.put(ModuleState.VALIDATED, EnumSet.of(ModuleState.LOADED, ModuleState.FAILED, ModuleState.UNLOADED));
        ALLOWED.put(ModuleState.LOADED, EnumSet.of(ModuleState.ENABLING, ModuleState.FAILED, ModuleState.UNLOADED));
        ALLOWED.put(ModuleState.ENABLING, EnumSet.of(ModuleState.ENABLED, ModuleState.FAILED));
        ALLOWED.put(ModuleState.ENABLED, EnumSet.of(ModuleState.DISABLING));
        ALLOWED.put(ModuleState.DISABLING, EnumSet.of(ModuleState.DISABLED, ModuleState.FAILED));
        ALLOWED.put(ModuleState.DISABLED, EnumSet.of(ModuleState.ENABLING, ModuleState.UNLOADED));
        ALLOWED.put(ModuleState.FAILED, EnumSet.of(ModuleState.UNLOADED));
        ALLOWED.put(ModuleState.UNLOADED, EnumSet.noneOf(ModuleState.class));
    }

    private ModuleStateMachine() {
    }

    /**
     * @param from current state
     * @param to   target state
     * @return whether the transition is allowed
     */
    public static boolean isAllowed(ModuleState from, ModuleState to) {
        return ALLOWED.get(from).contains(to);
    }

    /**
     * @param from current state
     * @param to   target state
     * @throws IllegalStateException if the transition is not allowed
     */
    public static void check(ModuleState from, ModuleState to) {
        if (!isAllowed(from, to)) {
            throw new IllegalStateException("illegal lifecycle transition " + from + " -> " + to);
        }
    }
}
