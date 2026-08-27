package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;
import java.util.Map;

/**
 * A custom item definition.
 *
 * @param name     the item name (identifier)
 * @param props    raw string props (material, glow, cooldown, ...);
 *                 name prop is a MiniMessage template
 * @param lore     MiniMessage lore lines
 * @param triggers the item's triggers (e.g. rightclick)
 * @param line     the source line
 */
public record ItemDef(String name, Map<String, String> props, List<String> lore, List<TriggerDef> triggers, int line) implements Node {
}