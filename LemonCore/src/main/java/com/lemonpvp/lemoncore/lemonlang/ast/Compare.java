package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * A comparison condition, e.g. {@code apples >= 1000}.
 *
 * @param var   the left-hand variable: one of
 *              apples|planks|coins|health|food|world|x|y|z|gamemode|name|online
 *              or a {placeholder} like {arg1}
 * @param op    the comparison operator: {@code == != >= <= > <}
 * @param value a literal word/number or a {placeholder}
 * @param line  the source line
 */
public record Compare(String var, String op, String value, int line) implements Condition {
}