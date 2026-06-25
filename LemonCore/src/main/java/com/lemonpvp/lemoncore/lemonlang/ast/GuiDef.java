package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;
import java.util.Map;

/**
 * A GUI definition.
 *
 * @param name  the GUI name (identifier)
 * @param props raw string props (title, rows, ...); title is a MiniMessage template
 * @param slots the slot definitions
 * @param line  the source line
 */
public record GuiDef(String name, Map<String, String> props, List<SlotDef> slots, int line) implements Node {
}