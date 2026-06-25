package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * A loop: {@code repeat N} followed by an indented body. The current iteration
 * (1-based) is exposed as {@code {loop}} in a child environment.
 *
 * @param times the number of iterations
 * @param body  the loop body
 * @param line  the source line
 */
public record RepeatStmt(int times, List<Stmt> body, int line) implements Stmt {
}