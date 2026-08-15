package de.lemonpvp.smpcontent.furniture;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Möbel als Anzeige-Objekte statt als Notenblöcke.
 *
 * Warum überhaupt: Note-Block-Zustände sind irgendwann alle (23 Instrumente
 * mal 25 Noten), und ein Notenblock kann sich nicht drehen. Ein Möbelstück
 * besteht darum aus
 *
 *   - einem unsichtbaren Platzhalterblock (Barriere bzw. Licht), damit alles
 *     Übliche weiter funktioniert: Schutz-Plugins, Kolben, Explosionen, und
 *     damit die Id wie bei jedem anderen eigenen Block im Chunk steht,
 *   - einem ItemDisplay, das das Modell zeigt und beim Setzen in die
 *     Blickrichtung gedreht wird,
 *   - einem Interaction-Feld zum Anklicken (abbauen, hinsetzen).
 *
 * Davon gibt es <b>beliebig viele</b> - die Zahl der eigenen Blöcke ist damit
 * nach oben offen.
 */
public final class FurnitureManager {

    private final SMPContent plugin;
    private final NamespacedKey posKey;
    private final NamespacedKey idKey;

    public FurnitureManager(SMPContent plugin) {
        this.plugin = plugin;
        this.posKey = new NamespacedKey(plugin, "furniture_at");
        this.idKey = new NamespacedKey(plugin, "furniture_id");
    }

    // ------------------------------------------------------------------
    //  Setzen
    // ------------------------------------------------------------------

    /**
     * Setzt ein Möbelstück. Die Drehung kommt aus der Blickrichtung des
     * Spielers und wird auf die eingestellten Schritte gerundet - bei
     * {@code rotate: 4} also auf die vier Himmelsrichtungen.
     */
    public void place(Player player, CustomEntry entry, Block block) {
        float yaw = snap(player.getLocation().getYaw() + 180f, entry.furniture().rotate());
        host(block, entry);
        plugin.blocks().set(block, entry.id(), yaw);
        spawn(block, entry, yaw);
    }

    /**
     * Der Platzhalter unter dem Möbel.
     *
     * Steht in der config.yml ein {@code state:}, wird der genommen - dann
     * sehen Bedrock-Spieler den Block genau so, wie er gemeint ist, denn
     * Geyser bildet Notenblock-Zustände auf echte Bedrock-Blöcke ab. Auf Java
     * ist dieser Zustand im Pack unsichtbar, dort zeigt das Anzeige-Objekt das
     * gedrehte Modell.
     *
     * Ohne {@code state:} ist es eine Barriere (bzw. Licht) - dafür gibt es
     * davon unbegrenzt viele.
     */
    private void host(Block block, CustomEntry entry) {
        CustomEntry.Furniture furniture = entry.furniture();
        if (entry.state() != null) {
            var data = plugin.registry().blockDataFor(entry);
            if (data != null) {
                block.setBlockData(data.clone(), false);
                return;
            }
        }
        if (furniture.solid()) {
            block.setType(Material.BARRIER, false);
            return;
        }
        block.setType(Material.LIGHT, false);
        if (block.getBlockData() instanceof Levelled light) {
            light.setLevel(Math.max(0, Math.min(15, furniture.light())));
            block.setBlockData(light, false);
        }
    }

    /** Modell und Klickfeld erzeugen. */
    public void spawn(Block block, CustomEntry entry, float yaw) {
        CustomEntry.Furniture furniture = entry.furniture();
        Location center = block.getLocation().add(0.5, 0, 0.5);
        String tag = tagOf(block);

        ItemStack model = plugin.registry().create(entry, 1);
        block.getWorld().spawn(center, ItemDisplay.class, display -> {
            display.setItemStack(model);
            display.setRotation(yaw, 0f);
            display.setPersistent(true);
            display.setInvulnerable(true);
            if (furniture.scale() != 1.0) {
                float scale = (float) furniture.scale();
                Transformation t = display.getTransformation();
                display.setTransformation(new Transformation(t.getTranslation(),
                        t.getLeftRotation(), new Vector3f(scale, scale, scale),
                        t.getRightRotation()));
            }
            mark(display, tag, entry.id());
        });

        block.getWorld().spawn(center, Interaction.class, hitbox -> {
            hitbox.setInteractionWidth((float) Math.max(0.1, furniture.width()));
            hitbox.setInteractionHeight((float) Math.max(0.1, furniture.height()));
            hitbox.setResponsive(true);
            hitbox.setPersistent(true);
            mark(hitbox, tag, entry.id());
        });
    }

    /**
     * Alle Stellen im Chunk, an denen schon ein Modell steht.
     *
     * Einmal über die Entities des Chunks statt einmal pro Möbelstück - bei
     * hundert Möbeln in einem Chunk ist das der Unterschied zwischen einem
     * kurzen Blick und hundert Umkreissuchen.
     */
    public Set<String> present(Chunk chunk) {
        Set<String> tags = new HashSet<>();
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof ItemDisplay)) {
                continue;
            }
            String tag = entity.getPersistentDataContainer()
                    .get(posKey, PersistentDataType.STRING);
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * Nach dem Laden eines Chunks: fehlt das Modell (jemand hat /kill benutzt
     * oder die Entities sind sonstwie weg), wird es neu gesetzt.
     */
    public void ensure(Block block, CustomEntry entry, float yaw, Set<String> present) {
        if (!isHost(block, entry)) {
            host(block, entry);
        }
        if (present.contains(tagOf(block))) {
            return;
        }
        removeEntities(block);
        spawn(block, entry, yaw);
    }

    // ------------------------------------------------------------------
    //  Abbauen
    // ------------------------------------------------------------------

    /** Entfernt Platzhalter und Anzeige. Die Drops macht der BlockListener. */
    public void clear(Block block) {
        removeEntities(block);
        if (block.getType() == Material.BARRIER || block.getType() == Material.LIGHT
                || block.getType() == Material.NOTE_BLOCK) {
            block.setType(Material.AIR, false);
        }
    }

    /** Steht dort noch der richtige Platzhalter? */
    public static boolean isHost(Block block, CustomEntry entry) {
        if (entry.state() != null) {
            return block.getType() == Material.NOTE_BLOCK;
        }
        return block.getType() == Material.BARRIER || block.getType() == Material.LIGHT;
    }

    public void removeEntities(Block block) {
        for (Entity entity : around(block)) {
            if (belongsTo(entity, block)
                    && (entity instanceof ItemDisplay || entity instanceof Interaction)) {
                entity.remove();
            }
        }
    }

    // ------------------------------------------------------------------
    //  Hinsetzen
    // ------------------------------------------------------------------

    /**
     * Setzt den Spieler auf das Möbelstück. Der Sitz ist ein unsichtbarer
     * Marker, auf dem der Spieler reitet - beim Aufstehen verschwindet er
     * wieder ({@code SeatListener}).
     */
    public boolean sit(Player player, Block block, CustomEntry entry) {
        if (entry.furniture() == null || !entry.furniture().seat()) {
            return false;
        }
        if (!player.getPassengers().isEmpty() || player.getVehicle() != null) {
            return false;
        }
        // Schon jemand drauf?
        for (Entity entity : around(block)) {
            if (entity instanceof ArmorStand && belongsTo(entity, block)
                    && !entity.getPassengers().isEmpty()) {
                return false;
            }
        }
        Location seat = block.getLocation().add(0.5, entry.furniture().seatY(), 0.5);
        seat.setYaw(plugin.blocks().yawAt(block));
        String tag = tagOf(block);
        ArmorStand stand = block.getWorld().spawn(seat, ArmorStand.class, armor -> {
            armor.setInvisible(true);
            armor.setMarker(true);
            armor.setGravity(false);
            armor.setInvulnerable(true);
            armor.setSilent(true);
            armor.setPersistent(false);
            armor.setCanTick(false);
            mark(armor, tag, entry.id());
        });
        if (!stand.addPassenger(player)) {
            stand.remove();
            return false;
        }
        return true;
    }

    /** Der Sitz wird nicht mehr gebraucht, sobald niemand mehr draufsitzt. */
    public void clearSeat(Entity seat) {
        if (seat instanceof ArmorStand stand && stand.getPersistentDataContainer()
                .has(posKey, PersistentDataType.STRING)) {
            stand.remove();
        }
    }

    // ------------------------------------------------------------------
    //  Zuordnung Entity <-> Block
    // ------------------------------------------------------------------

    /** Zu welchem Block gehört dieses Möbel-Entity? Null, wenn keins. */
    public Block blockOf(Entity entity) {
        String tag = entity.getPersistentDataContainer()
                .get(posKey, PersistentDataType.STRING);
        if (tag == null) {
            return null;
        }
        String[] parts = tag.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return entity.getWorld().getBlockAt(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String idOf(Entity entity) {
        return entity.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    private boolean belongsTo(Entity entity, Block block) {
        return tagOf(block).equals(entity.getPersistentDataContainer()
                .get(posKey, PersistentDataType.STRING));
    }

    private void mark(Entity entity, String tag, String id) {
        entity.getPersistentDataContainer().set(posKey, PersistentDataType.STRING, tag);
        entity.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
    }

    private static String tagOf(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }

    /** Alles im Umkreis eines Blocks - Möbel dürfen etwas über den Rand ragen. */
    private List<Entity> around(Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        return new ArrayList<>(block.getWorld().getNearbyEntities(center, 1.5, 1.5, 1.5));
    }

    /** Rundet die Blickrichtung auf die erlaubten Schritte. */
    static float snap(float yaw, int steps) {
        float normal = ((yaw % 360f) + 360f) % 360f;
        if (steps <= 0) {
            return 0f;
        }
        float size = 360f / steps;
        return Math.round(normal / size) * size % 360f;
    }
}
