package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * A macro definition: {@code define NAME} followed by an indented body.
 *
 * @param name the macro name
 * @param body the macro body
 * @param line the source line
 */
public record DefineStmt(String name, List<Stmt> body, int line) implements Stmt {
}