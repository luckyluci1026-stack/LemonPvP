package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * A variable assignment: {@code set NAME = EXPRESSION}.
 *
 * @param name       the variable name
 * @param expression the rest of line after '=', evaluated at runtime
 *                   (supports {placeholders} and simple + - * / math)
 * @param line       the source line
 */
public record SetStmt(String name, String expression, int line) implements Stmt {
}