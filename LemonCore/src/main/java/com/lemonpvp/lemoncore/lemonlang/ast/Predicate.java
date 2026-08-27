package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * A named predicate condition, e.g. {@code at -125 674 89} or {@code has perm lemon.vip}.
 *
 * @param name one of: at|near|chance|has
 *             <ul>
 *               <li>{@code at}: args = x y z</li>
 *               <li>{@code near}: args = x y z radius</li>
 *               <li>{@code chance}: args = [percent]</li>
 *               <li>{@code has}: args = ["perm","node"] or ["item","MATERIAL"]</li>
 *             </ul>
 * @param args the predicate arguments
 * @param line the source line
 */
public record Predicate(String name, List<String> args, int line) implements Condition {
}