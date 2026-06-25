package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * A recurring timer: {@code every 30s} followed by an indented body. The body
 * runs once per online player per interval tick.
 *
 * @param intervalMs the interval in milliseconds
 * @param body       the body to run each interval
 * @param line       the source line
 */
public record EveryDef(long intervalMs, List<Stmt> body, int line) implements Node {
}