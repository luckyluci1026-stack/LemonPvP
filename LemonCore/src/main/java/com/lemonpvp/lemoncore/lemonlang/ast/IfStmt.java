package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * An {@code if}/{@code else} statement. An {@code else if} is represented as a
 * single {@link IfStmt} nested inside {@code elseBody}. {@code elseBody} is
 * empty when there is no else branch.
 *
 * @param condition the condition to test
 * @param thenBody  statements run when the condition is true
 * @param elseBody  statements run when the condition is false (possibly empty)
 * @param line      the source line
 */
public record IfStmt(Condition condition, List<Stmt> thenBody, List<Stmt> elseBody, int line) implements Stmt {
}