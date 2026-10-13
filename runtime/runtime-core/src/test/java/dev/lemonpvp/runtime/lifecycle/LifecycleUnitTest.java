package dev.lemonpvp.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.api.ModuleDependency;
import dev.lemonpvp.runtime.api.ModuleDescriptor;
import dev.lemonpvp.runtime.api.ModuleState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LifecycleUnitTest {

    @Test
    void onlyDocumentedTransitionsAreAllowed() {
        assertTrue(ModuleStateMachine.isAllowed(ModuleState.LOADED, ModuleState.ENABLING));
        assertTrue(ModuleStateMachine.isAllowed(ModuleState.DISABLED, ModuleState.ENABLING));
        assertTrue(ModuleStateMachine.isAllowed(ModuleState.FAILED, ModuleState.UNLOADED));
        assertFalse(ModuleStateMachine.isAllowed(ModuleState.ENABLED, ModuleState.ENABLING), "no double enable");
        assertFalse(ModuleStateMachine.isAllowed(ModuleState.ENABLED, ModuleState.UNLOADED), "must disable first");
        assertFalse(ModuleStateMachine.isAllowed(ModuleState.FAILED, ModuleState.ENABLING), "failed instances are not reused");
        assertFalse(ModuleStateMachine.isAllowed(ModuleState.UNLOADED, ModuleState.LOADED));
        assertThrows(IllegalStateException.class, () -> ModuleStateMachine.check(ModuleState.DISCOVERED, ModuleState.ENABLED));
        for (ModuleState s : ModuleState.values()) {
            assertFalse(ModuleStateMachine.isAllowed(ModuleState.UNLOADED, s), "UNLOADED is terminal");
        }
    }

    private static ModuleDescriptor d(String id, int order, String... deps) {
        List<ModuleDependency> list = new ArrayList<>();
        for (String dep : deps) {
            list.add(new ModuleDependency(dep, "*", false));
        }
        return new ModuleDescriptor(id, "1.0.0", id, "", List.of(), "a.B", 1, List.of("*"), Set.of(), List.of(), list,
                List.of(), order, List.of());
    }

    @Test
    void startupOrderPutsDependenciesFirstAndDropsCycles() {
        List<String> problems = new ArrayList<>();
        List<ModuleDescriptor> order = DependencyResolver.startupOrder(List.of(
                d("app", 0, "lib", "db"), d("db", 5), d("lib", 1, "db"), d("cycle-a", 0, "cycle-b"),
                d("cycle-b", 0, "cycle-a"), d("first", -10)), problems);
        List<String> ids = order.stream().map(ModuleDescriptor::id).toList();
        assertEquals(List.of("first", "db", "lib", "app"), ids);
        assertEquals(2, problems.size(), problems.toString());
    }
}
