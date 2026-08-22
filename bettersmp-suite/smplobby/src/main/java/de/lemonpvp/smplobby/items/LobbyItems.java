package de.lemonpvp.smplobby.items;

import de.lemonpvp.smplobby.SMPLobby;
import de.lemonpvp.smplobby.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Die Gegenstaende in der Schnellleiste.
 *
 * Woran ein Lobby-Item erkannt wird, ist der springende Punkt. Am Namen
 * geht es nicht - den kann man in einem Amboss aendern -, und am Material
 * auch nicht, sonst oeffnete jeder Kompass die Serverauswahl. Deshalb
 * bekommt jedes Item eine unsichtbare Notiz mit ins Gepaeck
 * (PersistentData). Die ueberlebt Umbenennen und laesst sich nicht
 * nachbauen.
 */
public final class LobbyItems {

    /** Was in der Notiz steht: der Name der Aktion. */
    public static final String AKTION_WAEHLER = "waehler";
    public static final String AKTION_VERSTECKEN = "verstecken";
    public static final String AKTION_REGELN = "regeln";

    private final SMPLobby plugin;
    private final NamespacedKey schluessel;

    public LobbyItems(SMPLobby plugin) {
        this.plugin = plugin;
        this.schluessel = new NamespacedKey(plugin, "aktion");
    }

    public boolean aktiv() {
        return plugin.getConfig().getBoolean("gegenstaende.aktiv", true);
    }

    /**
     * Die Schnellleiste neu bestuecken.
     *
     * Vorher wird geleert: Sonst sammelt sich bei jedem /smplobby items
     * eine weitere Reihe an, und irgendwann ist kein Platz mehr.
     */
    public void gib(Player spieler) {
        if (!aktiv()) {
            return;
        }
        spieler.getInventory().clear();
        for (Map<?, ?> roh : plugin.getConfig().getMapList("gegenstaende.liste")) {
            int slot = zahl(roh.get("slot"), -1);
            if (slot < 0 || slot > 8) {
                plugin.getLogger().warning("Gegenstand mit ungültigem Slot "
                        + roh.get("slot") + " übersprungen (erlaubt sind 0 bis 8).");
                continue;
            }
            Material material = material(String.valueOf(roh.get("material")));
            if (material == null) {
                plugin.getLogger().warning("Material \"" + roh.get("material")
                        + "\" gibt es nicht - Gegenstand übersprungen.");
                continue;
            }
            String name = roh.get("name") == null ? "" : String.valueOf(roh.get("name"));
            List<String> zeilen = new ArrayList<>();
            if (roh.get("beschreibung") instanceof List<?> liste) {
                for (Object zeile : liste) {
                    zeilen.add(String.valueOf(zeile));
                }
            }
            String aktion = roh.get("aktion") == null ? "keine" : String.valueOf(roh.get("aktion"));
            spieler.getInventory().setItem(slot, baue(material, name, zeilen, aktion));
        }
        spieler.getInventory().setHeldItemSlot(0);
    }

    public ItemStack baue(Material material, String name, List<String> beschreibung, String aktion) {
        ItemStack stapel = new ItemStack(material);
        ItemMeta meta = stapel.getItemMeta();
        if (meta != null) {
            meta.displayName(zeile(name));
            List<Component> lore = new ArrayList<>();
            for (String text : beschreibung) {
                lore.add(zeile(text));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(schluessel, PersistentDataType.STRING, aktion);
            stapel.setItemMeta(meta);
        }
        return stapel;
    }

    /** Welche Aktion an diesem Gegenstand haengt - oder null. */
    public String aktionVon(ItemStack stapel) {
        if (stapel == null || !stapel.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stapel.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(schluessel, PersistentDataType.STRING);
    }

    /** Ein Gegenstand mit derselben Aktion, aber anderem Aussehen. */
    public void tausche(Player spieler, String aktion, Material material, String name,
                       List<String> beschreibung) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack da = spieler.getInventory().getItem(slot);
            if (aktion.equals(aktionVon(da))) {
                spieler.getInventory().setItem(slot, baue(material, name, beschreibung, aktion));
                return;
            }
        }
    }

    // ------------------------------------------------------------ Kleinkram

    private Component zeile(String text) {
        // Ohne das steht alles kursiv - Minecraft macht das bei
        // umbenannten Gegenstaenden von sich aus.
        return Text.mm(text).decoration(TextDecoration.ITALIC, false);
    }

    private static Material material(String name) {
        if (name == null) {
            return null;
        }
        return Material.matchMaterial(name.toUpperCase(Locale.ROOT));
    }

    private static int zahl(Object wert, int ersatz) {
        if (wert instanceof Number nummer) {
            return nummer.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(wert));
        } catch (NumberFormatException fehler) {
            return ersatz;
        }
    }
}
