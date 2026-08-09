package de.lemonpvp.smpcontent.content;

import org.bukkit.Material;

import java.util.List;

/**
 * Ein eigenes Item oder ein eigener Block.
 *
 * @param id        eindeutiger Schlüssel aus der config.yml
 * @param block     true = Block (wird als Note-Block platziert)
 * @param material  Basis-Material des Items
 * @param name      Anzeigename (MiniMessage)
 * @param lore      Beschreibung (MiniMessage)
 * @param modelData CustomModelData für Geyser/Bedrock
 * @param state     Blockzustand (nur bei Blöcken), sonst null
 */
public record CustomEntry(
        String id,
        boolean block,
        Material material,
        String name,
        List<String> lore,
        int modelData,
        String state
) {
}
