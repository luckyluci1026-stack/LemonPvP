package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;
import java.util.Map;

/**
 * A custom command definition.
 *
 * @param name     the command name
 * @param props    raw string props (permission, description, ...)
 * @param triggers the run triggers (0-param and target variants)
 * @param line     the source line
 */
public record CommandDef(String name, Map<String, String> props, List<TriggerDef> triggers, int line) implements Node {
}