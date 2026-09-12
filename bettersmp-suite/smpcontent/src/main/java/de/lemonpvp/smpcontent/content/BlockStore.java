package de.lemonpvp.smpcontent.content;

import de.lemonpvp.smpcontent.SMPContent;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merkt sich, welcher eigene Block wo steht - unabhängig vom Notenblock-Zustand.
 *
 * Warum: Bei reinen Note-Block-Zuständen ist das Aussehen gleichzeitig die
 * Identität. Ändert irgendetwas den Zustand (Rechtsklick, Redstone, WorldEdit,
 * ein anderes Plugin, /setblock), ist der Block nicht nur falsch texturiert -
 * das Plugin erkennt ihn gar nicht mehr und er droppt einen normalen
 * Notenblock.
 *
 * Darum steht die Id zusätzlich im Chunk (PersistentDataContainer). Der
 * Zustand ist dann nur noch das Aussehen und kann jederzeit wiederhergestellt
 * werden.
 *
 * Im Speicher liegt pro geladenem Chunk eine kleine Tabelle, damit die
 * Abfragen ohne Datei- oder NBT-Zugriff auskommen.
 */
public final class BlockStore {

    private final SMPContent plugin;
    private final NamespacedKey key;

    /** Chunk-Schlüssel -> (Position im Chunk -> Id) */
    private final Map<Long, Map<Integer, String>> loaded = new HashMap<>();

    public BlockStore(SMPContent plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "blocks");
    }

    // ------------------------------------------------------------------
    //  Abfragen
    // ------------------------------------------------------------------

    /** Die Id des eigenen Blocks an dieser Stelle, oder null. */
    public String idAt(Block block) {
        return plainId(rawAt(block));
    }

    /** Der gespeicherte Wert, also Id und - bei Möbeln - die Drehung. */
    public String rawAt(Block block) {
        Map<Integer, String> chunk = loaded.get(chunkKey(block.getChunk()));
        return chunk == null ? null : chunk.get(position(block));
    }

    /** Blickrichtung, mit der ein Möbelstück gesetzt wurde. */
    public float yawAt(Block block) {
        return yawOf(rawAt(block));
    }

    /** Aus "sofa@90" wird "sofa". */
    public static String plainId(String raw) {
        if (raw == null) {
            return null;
        }
        int at = raw.indexOf('@');
        return at < 0 ? raw : raw.substring(0, at);
    }

    /** Aus "sofa@90" wird 90. Ohne Drehung: 0. */
    public static float yawOf(String raw) {
        if (raw == null) {
            return 0f;
        }
        int at = raw.indexOf('@');
        if (at < 0) {
            return 0f;
        }
        try {
            return Float.parseFloat(raw.substring(at + 1));
        } catch (NumberFormatException ignored) {
            return 0f;
        }
    }

    public boolean isEmpty() {
        return loaded.isEmpty();
    }

    // ------------------------------------------------------------------
    //  Ändern
    // ------------------------------------------------------------------

    public void set(Block block, String id) {
        store(block, id.toLowerCase(java.util.Locale.ROOT));
    }

    /** Möbel merken sich zusätzlich, wie herum sie gesetzt wurden. */
    public void set(Block block, String id, float yaw) {
        store(block, id.toLowerCase(java.util.Locale.ROOT) + "@" + Math.round(yaw));
    }

    private void store(Block block, String value) {
        Map<Integer, String> chunk = loaded.computeIfAbsent(
                chunkKey(block.getChunk()), k -> new LinkedHashMap<>());
        chunk.put(position(block), value);
        save(block.getChunk(), chunk);
    }

    public void remove(Block block) {
        Long chunkKey = chunkKey(block.getChunk());
        Map<Integer, String> chunk = loaded.get(chunkKey);
        if (chunk == null || chunk.remove(position(block)) == null) {
            return;
        }
        save(block.getChunk(), chunk);
    }

    // ------------------------------------------------------------------
    //  Laden und Speichern
    // ------------------------------------------------------------------

    /** Liest die Tabelle eines Chunks in den Speicher. */
    public Map<Integer, String> load(Chunk chunk) {
        String raw = chunk.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        Map<Integer, String> table = new LinkedHashMap<>();
        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(";")) {
                int split = part.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                try {
                    table.put(Integer.parseInt(part.substring(0, split)),
                            part.substring(split + 1));
                } catch (NumberFormatException ignored) {
                    // kaputter Eintrag - überspringen
                }
            }
        }
        if (table.isEmpty()) {
            loaded.remove(chunkKey(chunk));
        } else {
            loaded.put(chunkKey(chunk), table);
        }
        return table;
    }

    /** Gibt den Speicher für einen Chunk frei; die Daten stehen im Chunk selbst. */
    public void unload(Chunk chunk) {
        loaded.remove(chunkKey(chunk));
    }

    public void clear() {
        loaded.clear();
    }

    private void save(Chunk chunk, Map<Integer, String> table) {
        if (table.isEmpty()) {
            chunk.getPersistentDataContainer().remove(key);
            loaded.remove(chunkKey(chunk));
            return;
        }
        StringBuilder out = new StringBuilder(table.size() * 24);
        for (Map.Entry<Integer, String> entry : table.entrySet()) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        chunk.getPersistentDataContainer().set(key, PersistentDataType.STRING, out.toString());
    }

    // ------------------------------------------------------------------

    private static Long chunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) ^ (chunk.getZ() & 0xFFFFFFFFL);
    }

    /** x und z innerhalb des Chunks (je 4 Bit), y darüber. */
    private static int position(Block block) {
        return ((block.getY() + 2048) << 8) | ((block.getZ() & 15) << 4) | (block.getX() & 15);
    }
}
