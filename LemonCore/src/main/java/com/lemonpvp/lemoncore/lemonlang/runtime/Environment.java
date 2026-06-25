package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.lemonlang.ast.Stmt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A lexically-scoped environment for LemonLang. Holds variables (set NAME = ...)
 * and macros (define NAME ...). Lookups fall through to the parent environment.
 */
public class Environment {

    private final Environment parent;
    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, List<Stmt>> macros = new HashMap<>();

    public Environment(Environment parent) {
        this.parent = parent;
    }

    /** Convenience root constructor. */
    public Environment() {
        this(null);
    }

    public Environment getParent() {
        return parent;
    }

    // ---- Variables ----------------------------------------------------------

    public void setVar(String name, String value) {
        variables.put(name, value);
    }

    /** Returns the variable value, searching parents; null if not found anywhere. */
    public String getVar(String name) {
        Environment env = this;
        while (env != null) {
            String v = env.variables.get(name);
            if (v != null) {
                return v;
            }
            env = env.parent;
        }
        return null;
    }

    /** True if the variable is defined in this environment or any parent. */
    public boolean hasVar(String name) {
        Environment env = this;
        while (env != null) {
            if (env.variables.containsKey(name)) {
                return true;
            }
            env = env.parent;
        }
        return false;
    }

    // ---- Macros -------------------------------------------------------------

    public void defineMacro(String name, List<Stmt> body) {
        macros.put(name, body);
    }

    /** Returns the macro body, searching parents; null if not found anywhere. */
    public List<Stmt> getMacro(String name) {
        Environment env = this;
        while (env != null) {
            List<Stmt> body = env.macros.get(name);
            if (body != null) {
                return body;
            }
            env = env.parent;
        }
        return null;
    }

    /** True if the macro is defined in this environment or any parent. */
    public boolean hasMacro(String name) {
        Environment env = this;
        while (env != null) {
            if (env.macros.containsKey(name)) {
                return true;
            }
            env = env.parent;
        }
        return false;
    }

    // ---- Scoping ------------------------------------------------------------

    /** Creates a new child environment whose parent is this one. */
    public Environment child() {
        return new Environment(this);
    }
}