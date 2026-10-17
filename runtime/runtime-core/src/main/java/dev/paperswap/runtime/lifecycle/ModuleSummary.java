package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.api.ModuleState;
import java.time.Instant;

/**
 * Short status of a loaded module.
 *
 * @param id           module id
 * @param version      module version
 * @param name         display name
 * @param state        lifecycle state
 * @param enabledSince time of the last successful enable or {@code null}
 * @param lastError    last error or {@code null}
 */
public record ModuleSummary(String id, String version, String name, ModuleState state, Instant enabledSince,
                            String lastError) {
}
