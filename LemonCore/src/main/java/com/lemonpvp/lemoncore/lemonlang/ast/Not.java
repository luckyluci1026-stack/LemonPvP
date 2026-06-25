package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * Logical negation of a condition.
 *
 * @param inner the negated condition
 * @param line  the source line
 */
public record Not(Condition inner, int line) implements Condition {
}