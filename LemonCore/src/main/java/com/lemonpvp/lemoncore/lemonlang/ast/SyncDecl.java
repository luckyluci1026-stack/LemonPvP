package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * Top-level sync declaration: {@code sync on|off|to server1 server2}
 */
public record SyncDecl(String mode, List<String> targets, int line) implements Node {}
