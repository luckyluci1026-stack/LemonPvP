package dev.paperswap.runtime.livepatch;

import java.util.EnumSet;
import java.util.Set;

/**
 * Category of a difference between the class running in the server and the class in a
 * patch. Each kind maps to the JVM capabilities it needs; kinds with no capability are
 * never applied by PaperSwap (the JVM spec forbids them or PaperSwap cannot test them safely).
 */
public enum ChangeKind {
    /** Body of an existing method changed. */
    METHOD_BODY("method body changed", EnumSet.of(Capability.REDEFINE_METHOD_BODY),
            "running invocations finish with the old code; new invocations use the new code"),
    /** Body of an existing constructor changed. */
    CONSTRUCTOR_BODY("constructor body changed", EnumSet.of(Capability.REDEFINE_METHOD_BODY),
            "only objects created after the patch run the new constructor; existing objects keep their state"),
    /** Static initializer changed. */
    STATIC_INITIALIZER("static initializer changed", EnumSet.of(Capability.REDEFINE_METHOD_BODY),
            "the static initializer is NOT executed again; static fields keep their current values"),
    /** Only attributes without bytecode impact changed (line numbers, annotations, ...). */
    ATTRIBUTES_ONLY("no bytecode change (debug information or attributes only)", EnumSet.of(Capability.REDEFINE_METHOD_BODY), null),
    /** Value of a constant ({@code static final}) field changed. */
    FIELD_CONSTANT_VALUE("constant value changed", EnumSet.of(Capability.REDEFINE_METHOD_BODY),
            "the field keeps its current value; other classes that inlined the constant only change if they are part of the patch"),
    /** Method added. */
    METHOD_ADDED("method added", EnumSet.of(Capability.ADD_METHOD), null),
    /** Method removed. */
    METHOD_REMOVED("method removed", EnumSet.of(Capability.REMOVE_METHOD), "running invocations of the removed method can still complete"),
    /** Constructor added. */
    CONSTRUCTOR_ADDED("constructor added", EnumSet.of(Capability.ADD_METHOD), null),
    /** Constructor removed. */
    CONSTRUCTOR_REMOVED("constructor removed", EnumSet.of(Capability.REMOVE_METHOD), null),
    /** Field added. */
    FIELD_ADDED("field added", EnumSet.of(Capability.ADD_FIELD), "existing objects get the default value (0/null/false) for the new field"),
    /** Field removed. */
    FIELD_REMOVED("field removed", EnumSet.of(Capability.REMOVE_FIELD), null),
    /** Field type or modifiers changed. */
    FIELD_CHANGED("field type or modifiers changed", EnumSet.of(Capability.ADD_FIELD, Capability.REMOVE_FIELD), null),
    /** Field declaration order changed. */
    FIELDS_REORDERED("field order changed", EnumSet.of(Capability.ADD_FIELD, Capability.REMOVE_FIELD), null),
    /** Method modifiers changed. */
    METHOD_MODIFIERS("method modifiers changed", EnumSet.of(Capability.CHANGE_METHOD_MODIFIERS), null),
    /** Class modifiers changed. */
    CLASS_MODIFIERS("class modifiers changed", EnumSet.noneOf(Capability.class), null),
    /** Super class changed. */
    SUPERCLASS_CHANGED("super class changed", EnumSet.of(Capability.CHANGE_SUPERCLASS), null),
    /** Implemented interfaces changed. */
    INTERFACES_CHANGED("implemented interfaces changed", EnumSet.of(Capability.CHANGE_INTERFACES), null),
    /** NestHost, NestMembers, PermittedSubclasses or Record changed (e.g. a new anonymous or inner class). */
    RESTRICTED_ATTRIBUTE("nest/sealed/record attribute changed", EnumSet.noneOf(Capability.class),
            "typically caused by adding or removing an inner, anonymous or local class"),
    /** The class does not exist in the running server. */
    NEW_CLASS("new class", EnumSet.of(Capability.DEFINE_NEW_CLASS),
            "a defined class cannot be removed again before a restart");

    private final String description;
    private final EnumSet<Capability> required;
    private final String note;

    ChangeKind(String description, EnumSet<Capability> required, String note) {
        this.description = description;
        this.required = required;
        this.note = note;
    }

    /** @return human readable description */
    public String description() {
        return this.description;
    }

    /** @return capabilities the JVM must support; empty = never applied by PaperSwap */
    public Set<Capability> required() {
        return EnumSet.copyOf(this.required);
    }

    /** @return semantic caveat shown to the operator, or {@code null} */
    public String note() {
        return this.note;
    }

    /** @return whether the change keeps the class schema (standard JVM redefinition can apply it) */
    public boolean bodyOnly() {
        return this.required.equals(EnumSet.of(Capability.REDEFINE_METHOD_BODY));
    }
}
