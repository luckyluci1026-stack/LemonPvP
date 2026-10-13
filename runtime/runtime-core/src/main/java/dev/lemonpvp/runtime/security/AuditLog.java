package dev.lemonpvp.runtime.security;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Append-only audit log of administrative actions ({@code lemon/logs/audit.log}).
 *
 * <p>Format (one line per record, fields separated by {@code " | "}):
 * {@code timestamp | actor | action | target | result | details}. Untrusted values are
 * sanitized (control characters and the separator are replaced) so a crafted module id
 * or file name cannot forge additional records. File I/O happens on the runtime worker
 * thread, never on the server main thread.</p>
 */
public final class AuditLog {

    private final Path file;
    private final Executor writer;
    private final Logger logger;

    /**
     * @param file   audit log file
     * @param writer executor used for file writes
     * @param logger logger for write failures
     */
    public AuditLog(Path file, Executor writer, Logger logger) {
        this.file = file;
        this.writer = writer;
        this.logger = logger;
    }

    /**
     * Records an action.
     *
     * @param actor   who triggered it (player name, "console", "startup")
     * @param action  action name
     * @param target  module id or other target
     * @param success outcome
     * @param details free text
     */
    public void record(String actor, String action, String target, boolean success, String details) {
        String line = Instant.now() + " | " + field(actor) + " | " + field(action) + " | " + field(target) + " | "
                + (success ? "OK" : "FAILED") + " | " + field(details) + System.lineSeparator();
        try {
            this.writer.execute(() -> append(line));
        } catch (RejectedExecutionException e) {
            append(line); // runtime is shutting down; write synchronously
        }
    }

    private static String field(String value) {
        return PathGuard.sanitize(value == null ? "-" : value).replace('|', '/');
    }

    private synchronized void append(String line) {
        try {
            Files.createDirectories(this.file.toAbsolutePath().getParent());
            try (Writer w = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
                w.write(line);
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Could not write audit log " + this.file + ": " + e.getMessage());
        }
    }
}
