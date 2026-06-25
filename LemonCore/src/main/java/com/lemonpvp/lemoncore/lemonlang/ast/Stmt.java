package com.lemonpvp.lemoncore.lemonlang.ast;

/**
 * A single executable statement inside a body. Sealed over the concrete
 * statement kinds.
 */
public sealed interface Stmt extends Node
        permits ActionStmt, IfStmt, SetStmt, DefineStmt, RepeatStmt, ThreadStmt {
}