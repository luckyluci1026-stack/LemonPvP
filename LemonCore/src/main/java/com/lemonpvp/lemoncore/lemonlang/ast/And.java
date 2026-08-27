package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * Logical AND of two conditions.
 *
 * @param left  the left operand
 * @param right the right operand
 * @param line  the source line
 */
public record And(Condition left, Condition right, int line) implements Condition {
}