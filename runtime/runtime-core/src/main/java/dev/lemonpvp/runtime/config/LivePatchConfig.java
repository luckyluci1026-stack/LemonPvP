package dev.lemonpvp.runtime.config;

import java.util.Locale;

/**
 * Settings of core live patching ({@code live-patching:} section of
 * {@code lemon-runtime.yml}). Separate from module hot deployment.
 *
 * @param enabled               allow {@code /fork patch apply|rollback} (inspection works regardless)
 * @param profile               production or development rules
 * @param patchDirectory        directory with uploaded patch archives, relative to the server directory
 * @param trustedHashesFile     allow-list of approved archive digests (required in production)
 * @param confirmTimeoutSeconds how long a plan waits for {@code confirm}
 */
public record LivePatchConfig(boolean enabled, Profile profile, String patchDirectory, String trustedHashesFile,
                              int confirmTimeoutSeconds) {

    /** System property that must be {@code true} before the development profile is honoured. */
    public static final String DEVELOPMENT_PROPERTY = "lemon.livepatch.development";

    public LivePatchConfig {
        trustedHashesFile = trustedHashesFile == null ? "" : trustedHashesFile;
    }

    /** Rules for applying patches. */
    public enum Profile {
        /** Allow-list required, no shortcuts. Default. */
        PRODUCTION,
        /** Allow-list optional, more diagnostics; only on JVMs started with {@link #DEVELOPMENT_PROPERTY}. */
        DEVELOPMENT;

        /** @return the YAML spelling */
        public String yamlName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** @return the documented defaults (live patching disabled, production profile) */
    public static LivePatchConfig defaults() {
        return new LivePatchConfig(false, Profile.PRODUCTION, "lemon/patches/inbox", "", 120);
    }
}
