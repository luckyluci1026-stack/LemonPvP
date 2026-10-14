package dev.lemonpvp.runtime.livepatch.classfile;

/** The bytes are not a well-formed class file (as far as Lemon's reader checks). */
public final class ClassFormatException extends Exception {

    /** @param message description */
    public ClassFormatException(String message) {
        super(message);
    }
}
