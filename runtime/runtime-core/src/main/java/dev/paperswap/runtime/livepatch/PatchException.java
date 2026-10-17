package dev.paperswap.runtime.livepatch;

/** A patch was rejected (invalid archive, failed check, unsupported change, ...). */
public final class PatchException extends Exception {

    /** @param message reason shown to the operator */
    public PatchException(String message) {
        super(message);
    }

    /**
     * @param message reason shown to the operator
     * @param cause   underlying problem
     */
    public PatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
