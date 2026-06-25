package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * The root of a parsed LemonLang script: an ordered list of top-level nodes
 * (TriggerDef, EveryDef, ItemDef, CommandDef, GuiDef, and top-level
 * set/define statements).
 *
 * @param nodes the top-level nodes
 * @param line  the source line (typically 1)
 */
public record Program(List<Node> nodes, int line) implements Node {
}