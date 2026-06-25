package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * A boolean condition used by {@code if}/{@code else if} statements and
 * predicate verbs. Sealed over the concrete condition kinds.
 */
public sealed interface Condition extends Node
        permits Compare, Predicate, And, Or, Not {
}