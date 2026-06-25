package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * Top-level system declaration: {@code system paper|velocity|folia}
 * Validates that the running server matches the declared type.
 */
public record SystemDecl(String type, int line) implements Node {}
