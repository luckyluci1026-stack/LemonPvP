package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * Thread-block statement: {@code thread main|async} followed by indented body.
 * Runs the body on the specified thread.
 */
public record ThreadStmt(String mode, List<Stmt> body, int line) implements Stmt {}
