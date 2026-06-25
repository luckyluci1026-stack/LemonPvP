package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * Top-level import: {@code import luckperms|placeholderapi|vault|...}
 * LemonLangManager checks that the plugin is loaded before running the script.
 */
public record ImportDecl(String pluginName, int line) implements Node {}
