package dev.paperswap.runtime.api;

/** Constants of the module API. */
public final class RuntimeApi {

    /**
     * Current module API version. Incremented on incompatible API changes; modules declare
     * the version they were written against in {@code api-version}.
     */
    public static final int API_VERSION = 1;

    /** Name of the descriptor file at the root of every module jar. */
    public static final String DESCRIPTOR_FILE = "paperswap-module.yml";

    /** Short alternative name of the descriptor (a jar must contain only one descriptor). */
    public static final String DESCRIPTOR_FILE_SHORT = "module.yml";

    /** Descriptor name of modules built before the project was renamed to PaperSwap; still accepted. */
    public static final String DESCRIPTOR_FILE_LEGACY = "lemon-module.yml";

    private RuntimeApi() {
    }
}
