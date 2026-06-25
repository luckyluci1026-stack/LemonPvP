package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * Logical OR of two conditions.
 *
 * @param left  the left operand
 * @param right the right operand
 * @param line  the source line
 */
public record Or(Condition left, Condition right, int line) implements Condition {
}