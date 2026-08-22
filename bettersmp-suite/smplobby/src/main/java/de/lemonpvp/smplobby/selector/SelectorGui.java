package de.lemonpvp.smplobby.selector;

import de.lemonpvp.smplobby.SMPLobby;
import de.lemonpvp.smplobby.util.Text;
import de.lemonpvp.smplobby.util.Werte;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Die Serverauswahl.
 *
 * Welcher Platz zu welchem Server gehoert, steht im Holder und nicht in
 * einer Tabelle nebenher: Ein Fenster weiss damit selbst, was es zeigt,
 * und wenn es zugeht, ist auch die Zuordnung weg. Eine Map neben dem
 * Fenster muesste man beim Schliessen aufraeumen - und genau das vergisst
 * man, bis der Server nach zwei Wochen langsam wird.
 */
public final class SelectorGui {

    /** Merkt sich, welcher Platz auf welchen Server zeigt. */
    public static final class Holder implements InventoryHolder {

        private Inventory inventar;
        /** Platz -> Servername. */
        public final Map<Integer, String> ziele = new HashMap<>();

        @Override
        public @NotNull Inventory getInventory() {
            return inventar;
        }
    }

    private final SMPLobby plugin;

    public SelectorGui(SMPLobby plugin) {
        this.plugin = plugin;
    }

    /** Alle Servernamen aus der Konfiguration - fuer die Spielerzahl-Abfrage. */
    public List<String> serverNamen() {
        List<String> raus = new ArrayList<>();
        for (Map<?, ?> roh : plugin.getConfig().getMapList("waehler.eintraege")) {
            String server = Werte.text(roh, "server", "");
            if (!server.isEmpty()) {
                raus.add(server);
            }
        }
        return raus;
    }

    public void oeffne(Player spieler) {
        Holder holder = new Holder();
        int plaetze = plaetze();
        holder.inventar = Bukkit.createInventory(holder, plaetze,
                Text.mm(plugin.getConfig().getString("waehler.titel", "Server wählen")));

        for (Map<?, ?> roh : plugin.getConfig().getMapList("waehler.eintraege")) {
            int platz = Werte.zahl(roh, "platz", -1);
            if (platz < 0 || platz >= plaetze) {
                plugin.getLogger().warning("Wähler-Eintrag auf Platz " + roh.get("platz")
                        + " passt nicht in " + plaetze + " Plätze - übersprungen.");
                continue;
            }
            Material material = Material.matchMaterial(
                    Werte.text(roh, "material", "").toUpperCase(Locale.ROOT));
            if (material == null) {
                plugin.getLogger().warning("Material \"" + roh.get("material")
                        + "\" gibt es nicht - Wähler-Eintrag übersprungen.");
                continue;
            }
            String server = Werte.text(roh, "server", "");
            if (server.isEmpty()) {
                plugin.getLogger().warning("Wähler-Eintrag ohne \"server\" übersprungen.");
                continue;
            }
            int online = plugin.proxy().zahl(server);

            ItemStack stapel = new ItemStack(material);
            ItemMeta meta = stapel.getItemMeta();
            if (meta != null) {
                meta.displayName(zeile(Werte.text(roh, "name", server)));
                List<Component> lore = new ArrayList<>();
                for (String text : Werte.zeilen(roh, "beschreibung")) {
                    // Solange der Proxy noch nichts gemeldet hat, steht dort
                    // ein Strich. Eine erfundene 0 waere schlimmer - dann
                    // glaubt man, der Server sei leer.
                    lore.add(zeile(text.replace("%spieler%",
                            online < 0 ? "–" : String.valueOf(online))));
                }
                meta.lore(lore);
                stapel.setItemMeta(meta);
            }
            holder.inventar.setItem(platz, stapel);
            holder.ziele.put(platz, server);
        }

        spieler.openInventory(holder.getInventory());
    }

    private int plaetze() {
        int wunsch = plugin.getConfig().getInt("waehler.plaetze", 27);
        // Ein Inventar hat immer ein Vielfaches von neun, hoechstens 54.
        // Ein krummer Wert wuerde beim Oeffnen eine Ausnahme werfen und
        // den Spieler mit einem leeren Bildschirm zuruecklassen.
        int reihen = Math.max(1, Math.min(6, Math.round(wunsch / 9f)));
        return reihen * 9;
    }

    private Component zeile(String text) {
        return Text.mm(text).decoration(TextDecoration.ITALIC, false);
    }
}
