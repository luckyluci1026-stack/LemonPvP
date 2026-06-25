package com.lemonpvp.lemoncore.lemonlang.ast;

import java.util.List;

/**
 * An event trigger: {@code on EVENT} followed by an indented body.
 *
 * @param event  one of: join quit death respawn chat move interact hit kill
 *               blockbreak blockplace rightclick leftclick run click
 * @param params optional parameters (e.g. command run targets)
 * @param body   the trigger body
 * @param line   the source line
 */
public record TriggerDef(String event, List<String> params, List<Stmt> body, int line) implements Node {
}