package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * Base type for every node in the LemonLang abstract syntax tree.
 * Every node knows the source line on which it was declared.
 */
public interface Node {
    int line();
}