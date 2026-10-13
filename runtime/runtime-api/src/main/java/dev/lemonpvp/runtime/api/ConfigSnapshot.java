package dev.lemonpvp.runtime.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, thread-safe view of a parsed configuration tree.
 *
 * <p>Paths use {@code .} as separator ({@code "messages.join"}). Getters with a default
 * value return the default when the path is missing <em>or</em> has an incompatible type;
 * use {@link #get(String)} to distinguish the cases.</p>
 */
public final class ConfigSnapshot {

    private static final ConfigSnapshot EMPTY = new ConfigSnapshot(Map.of());

    private final Map<String, Object> root;

    private ConfigSnapshot(Map<String, Object> root) {
        this.root = root;
    }

    /** @return an empty snapshot */
    public static ConfigSnapshot empty() {
        return EMPTY;
    }

    /**
     * Creates a deep, immutable copy of a raw configuration tree consisting of maps, lists,
     * strings, numbers and booleans (as produced by a YAML parser).
     *
     * @param raw raw tree, may be {@code null}
     * @return the snapshot
     */
    public static ConfigSnapshot of(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return EMPTY;
        }
        return new ConfigSnapshot(freezeMap(raw));
    }

    private static Map<String, Object> freezeMap(Map<?, ?> raw) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            copy.put(String.valueOf(e.getKey()), freeze(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object freeze(Object value) {
        if (value instanceof Map<?, ?> m) {
            return freezeMap(m);
        }
        if (value instanceof List<?> l) {
            List<Object> copy = new ArrayList<>(l.size());
            for (Object o : l) {
                copy.add(freeze(o));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    /**
     * @param path dotted path
     * @return the raw value (immutable map, list, string, number or boolean)
     */
    public Optional<Object> get(String path) {
        Object current = this.root;
        for (String part : path.split("\\.", -1)) {
            if (!(current instanceof Map<?, ?> map)) {
                return Optional.empty();
            }
            current = map.get(part);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    /** @param path dotted path @return whether a value exists at the path */
    public boolean contains(String path) {
        return get(path).isPresent();
    }

    /** @param path dotted path @param def default @return string value or default */
    public String getString(String path, String def) {
        return get(path).filter(v -> v instanceof String || v instanceof Number || v instanceof Boolean)
                .map(String::valueOf).orElse(def);
    }

    /** @param path dotted path @param def default @return int value or default */
    public int getInt(String path, int def) {
        return get(path).filter(v -> v instanceof Number).map(v -> ((Number) v).intValue()).orElse(def);
    }

    /** @param path dotted path @param def default @return long value or default */
    public long getLong(String path, long def) {
        return get(path).filter(v -> v instanceof Number).map(v -> ((Number) v).longValue()).orElse(def);
    }

    /** @param path dotted path @param def default @return double value or default */
    public double getDouble(String path, double def) {
        return get(path).filter(v -> v instanceof Number).map(v -> ((Number) v).doubleValue()).orElse(def);
    }

    /** @param path dotted path @param def default @return boolean value or default */
    public boolean getBoolean(String path, boolean def) {
        return get(path).filter(v -> v instanceof Boolean).map(v -> (Boolean) v).orElse(def);
    }

    /**
     * @param path dotted path
     * @return list of the string representations of scalar list elements, empty when missing
     */
    public List<String> getStringList(String path) {
        Optional<Object> v = get(path);
        if (v.isEmpty() || !(v.get() instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o instanceof String || o instanceof Number || o instanceof Boolean) {
                out.add(String.valueOf(o));
            }
        }
        return List.copyOf(out);
    }

    /**
     * @param path dotted path
     * @return the sub tree as snapshot, empty when missing or not a section
     */
    public ConfigSnapshot getSection(String path) {
        return get(path).filter(v -> v instanceof Map).map(v -> new ConfigSnapshot(castMap(v))).orElse(EMPTY);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object v) {
        return (Map<String, Object>) v;
    }

    /** @return the top level keys */
    public Set<String> keys() {
        return this.root.keySet();
    }

    /** @return the immutable raw tree */
    public Map<String, Object> asMap() {
        return this.root;
    }

    /** @return whether the snapshot has no entries */
    public boolean isEmpty() {
        return this.root.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConfigSnapshot other && this.root.equals(other.root);
    }

    @Override
    public int hashCode() {
        return this.root.hashCode();
    }

    @Override
    public String toString() {
        return "ConfigSnapshot" + this.root;
    }
}
