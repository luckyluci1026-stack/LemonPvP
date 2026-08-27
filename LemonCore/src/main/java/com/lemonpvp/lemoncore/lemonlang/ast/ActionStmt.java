package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * A plain action statement: a verb followed by either structured arguments or
 * a rest-of-line MiniMessage template.
 *
 * <p>Verb is the first word, e.g. message, give, take, sound, particle,
 * firework, effect, teleport, heal, feed, kick, broadcast, actionbar, title,
 * subtitle, perm, run, console, command, gui, cooldown, wait, stop, cancel.</p>
 *
 * <p>For TEXT verbs (message, actionbar, title, subtitle, broadcast, kick)
 * {@code text} = rest of line (template) and {@code args} is empty. Title text
 * may contain '|' to split main|sub|duration (handled at runtime).</p>
 *
 * <p>For STRUCTURED verbs (give, take, sound, particle, effect, teleport, perm,
 * run, console, command, gui, cooldown, wait) {@code args} = remaining words and
 * {@code text} is null.</p>
 *
 * @param verb the verb (first word)
 * @param args structured arguments, empty for text verbs
 * @param text rest-of-line template for text verbs, otherwise null
 * @param line the source line
 */
public record ActionStmt(String verb, List<String> args, String text, int line) implements Stmt {
}