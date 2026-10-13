package dev.lemonpvp.runtime.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Safe YAML parsing for runtime, descriptor and module configuration files.
 *
 * <p>Uses SnakeYAML's {@link SafeConstructor} (no arbitrary object construction), rejects
 * duplicate keys and limits aliases, nesting and document size to keep parsing cheap and
 * predictable. SnakeYAML is provided by the Paper server at runtime.</p>
 */
public final class YamlSupport {

    /** Upper bound for any YAML document the runtime reads (1 MiB). */
    public static final int MAX_DOCUMENT_BYTES = 1024 * 1024;

    private YamlSupport() {
    }

    private static Yaml newYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(10);
        options.setNestingDepthLimit(32);
        options.setCodePointLimit(MAX_DOCUMENT_BYTES);
        return new Yaml(new SafeConstructor(options));
    }

    /**
     * @param file YAML file
     * @return top-level mapping; empty map for an empty document
     * @throws IOException          if reading fails
     * @throws YamlFormatException if the document is invalid or not a mapping
     */
    public static Map<String, Object> load(Path file) throws IOException {
        if (Files.size(file) > MAX_DOCUMENT_BYTES) {
            throw new YamlFormatException(file.getFileName() + " is larger than " + MAX_DOCUMENT_BYTES + " bytes");
        }
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.getFileName().toString());
        }
    }

    /**
     * @param in     UTF-8 encoded YAML (consumed but not closed)
     * @param source name used in error messages
     * @return top-level mapping; empty map for an empty document
     * @throws YamlFormatException if the document is invalid or not a mapping
     */
    public static Map<String, Object> load(InputStream in, String source) {
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        Object document;
        try {
            document = newYaml().load(reader);
        } catch (YAMLException e) {
            throw new YamlFormatException(source + ": " + firstLine(e.getMessage()), e);
        }
        if (document == null) {
            return new LinkedHashMap<>();
        }
        if (!(document instanceof Map<?, ?> map)) {
            throw new YamlFormatException(source + ": top level element must be a mapping");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "invalid YAML";
        }
        int nl = message.indexOf('\n');
        return nl < 0 ? message : message.substring(0, nl);
    }

    /** Raised for syntactically or structurally invalid YAML. */
    public static final class YamlFormatException extends RuntimeException {
        public YamlFormatException(String message) {
            super(message);
        }

        public YamlFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
