package dev.lemonpvp.runtime.api;

/** Constants of the module API. */
public final class RuntimeApi {

    /**
     * Current module API version. Incremented on incompatible API changes; modules declare
     * the version they were written against in {@code api-version}.
     */
    public static final int API_VERSION = 1;

    /** Name of the descriptor file at the root of every module jar. */
    public static final String DESCRIPTOR_FILE = "lemon-module.yml";

    /** Short alternative name of the descriptor (a jar must contain only one of the two). */
    public static final String DESCRIPTOR_FILE_SHORT = "module.yml";

    private RuntimeApi() {
    }
}
