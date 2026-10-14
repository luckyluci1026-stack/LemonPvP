package dev.lemonpvp.runtime.livepatch;

/**
 * Kinds of class redefinition a JVM may or may not support. Every capability is
 * determined by a real redefinition of a throw-away probe class ({@link CapabilityProbe}),
 * never assumed from the JVM name.
 */
public enum Capability {
    /** Replace method bodies (standard HotSpot redefinition). */
    REDEFINE_METHOD_BODY("change existing method bodies"),
    /** Add methods or constructors. */
    ADD_METHOD("add methods/constructors"),
    /** Remove methods or constructors. */
    REMOVE_METHOD("remove methods/constructors"),
    /** Add fields. */
    ADD_FIELD("add fields"),
    /** Remove fields. */
    REMOVE_FIELD("remove fields"),
    /** Change method modifiers (e.g. public to private). */
    CHANGE_METHOD_MODIFIERS("change method modifiers"),
    /** Change the list of implemented interfaces. */
    CHANGE_INTERFACES("change implemented interfaces"),
    /** Change the super class. */
    CHANGE_SUPERCLASS("change the super class"),
    /** Define a class that does not exist yet, in the package of an existing class. */
    DEFINE_NEW_CLASS("define new classes in existing packages"),
    /**
     * The JVM runs its bytecode verifier on redefined classes and rejects invalid ones.
     * Standard HotSpot does; JetBrains Runtime with enhanced redefinition did not in Lemon's
     * CI. Where it does not, Lemon verifies patch classes itself before applying them.
     */
    VERIFY_REDEFINED_CLASSES("reject invalid bytecode when redefining (JVM verifier)");

    private final String description;

    Capability(String description) {
        this.description = description;
    }

    /** @return human readable description */
    public String description() {
        return this.description;
    }
}
