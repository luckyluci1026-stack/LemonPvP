package dev.lemonpvp.runtime.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** SHA-256 helpers. Never call these on the server main thread for large files. */
public final class Hashing {

    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

    private Hashing() {
    }

    /**
     * @param file file to hash
     * @return lower-case hex SHA-256 of the file content
     * @throws IOException if the file cannot be read
     */
    public static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return sha256(in);
        }
    }

    /**
     * @param in stream to consume completely
     * @return lower-case hex SHA-256 of the stream content
     * @throws IOException if reading fails
     */
    public static String sha256(InputStream in) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * @param bytes data
     * @return lower-case hex SHA-256 of the data
     */
    public static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(newDigest().digest(bytes));
    }

    /**
     * @param value candidate
     * @return whether the value is a lower-case hex SHA-256 digest
     */
    public static boolean isSha256(String value) {
        return value != null && SHA256_HEX.matcher(value).matches();
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
