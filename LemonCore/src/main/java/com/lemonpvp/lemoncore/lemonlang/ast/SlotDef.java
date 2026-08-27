package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;
import java.util.Map;

/**
 * A GUI slot definition.
 *
 * @param slot    the inventory slot index
 * @param props   raw string props (e.g. props.get("material")="APPLE");
 *                name is a MiniMessage template
 * @param lore    MiniMessage lore lines
 * @param onClick the click trigger, or null if none
 * @param line    the source line
 */
public record SlotDef(int slot, Map<String, String> props, List<String> lore, TriggerDef onClick, int line) implements Node {
}