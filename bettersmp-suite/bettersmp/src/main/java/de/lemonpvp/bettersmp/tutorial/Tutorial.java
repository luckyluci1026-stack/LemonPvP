package de.lemonpvp.bettersmp.tutorial;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.gui.GuiItems;
import de.lemonpvp.bettersmp.util.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Tutorial implements Listener, CommandExecutor {

    public record Thema(int platz, Material material, String befehl, String name, List<String> kurz, List<String> text) {
    }

    public static final class Menue implements InventoryHolder {
        private final Map<Integer, Thema> themen = new HashMap<>();
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final BetterSMP plugin;
    private YamlConfiguration daten = new YamlConfiguration();

    public Tutorial(BetterSMP plugin) {
        this.plugin = plugin;
        laden();
    }

    public void laden() {
        File datei = new File(plugin.getDataFolder(), "tutorial.yml");
        if (!datei.exists()) {
            plugin.saveResource("tutorial.yml", false);
        }
        daten = YamlConfiguration.loadConfiguration(datei);
    }

    public List<Thema> sichtbareThemen() {
        List<Thema> liste = new ArrayList<>();
        for (Map<?, ?> eintrag : daten.getMapList("themen")) {
            String befehl = text(eintrag.get("befehl"));
            if (!befehl.isEmpty() && Bukkit.getCommandMap().getCommand(befehl) == null) {
                continue;
            }
            Material material = Material.matchMaterial(text(eintrag.get("material")));
            liste.add(new Thema(zahl(eintrag.get("platz"), liste.size()),
                    material == null || !material.isItem() ? Material.PAPER : material,
                    befehl, text(eintrag.get("name")), zeilen(eintrag.get("kurz")), zeilen(eintrag.get("text"))));
        }
        return liste;
    }

    public void oeffnen(Player spieler) {
        int plaetze = Math.max(9, Math.min(54, (daten.getInt("plaetze", 27) / 9) * 9));
        Menue menue = new Menue();
        Inventory inventar = Bukkit.createInventory(menue, plaetze,
                Text.mm(daten.getString("titel", "<dark_gray>Tutorial")));
        menue.inventory = inventar;
        for (int i = 0; i < plaetze; i++) {
            inventar.setItem(i, GuiItems.filler(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (Thema thema : sichtbareThemen()) {
            if (thema.platz() < 0 || thema.platz() >= plaetze) {
                continue;
            }
            List<String> lore = new ArrayList<>(thema.kurz());
            lore.add("");
            lore.add("<yellow>Klick zum Lesen");
            inventar.setItem(thema.platz(), GuiItems.item(thema.material(), thema.name(), lore, false));
            menue.themen.put(thema.platz(), thema);
        }
        spieler.openInventory(inventar);
    }

    public void zeigen(Player spieler, Thema thema) {
        String kopf = daten.getString("kopfzeile", "");
        if (!kopf.isEmpty()) {
            spieler.sendMessage(Text.mm(kopf));
        }
        for (String zeile : thema.text()) {
            spieler.sendMessage(Text.mm(zeile));
        }
        String fuss = daten.getString("fusszeile", "");
        if (!fuss.isEmpty()) {
            spieler.sendMessage(Text.mm(fuss));
        }
        if (!kopf.isEmpty()) {
            spieler.sendMessage(Text.mm(kopf));
        }
        spieler.playSound(Sound.sound(Key.key("minecraft:item.book.page_turn"), Sound.Source.MASTER, 1f, 1f));
    }

    public void ersterJoin(Player spieler) {
        String hinweis = daten.getString("erster-join-hinweis", "");
        if (!hinweis.isEmpty()) {
            spieler.sendMessage(Text.mm(hinweis));
        }
        if (daten.getBoolean("beim-ersten-join-oeffnen", true)) {
            oeffnen(spieler);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (args.length == 0) {
            oeffnen(spieler);
            return true;
        }
        List<Thema> themen = sichtbareThemen();
        Thema gefunden = null;
        try {
            int nummer = Integer.parseInt(args[0]);
            if (nummer >= 1 && nummer <= themen.size()) {
                gefunden = themen.get(nummer - 1);
            }
        } catch (NumberFormatException keineZahl) {
            String gesucht = String.join(" ", args).toLowerCase(Locale.ROOT);
            for (Thema thema : themen) {
                if (Text.plain(Text.mm(thema.name())).toLowerCase(Locale.ROOT).contains(gesucht)) {
                    gefunden = thema;
                    break;
                }
            }
        }
        if (gefunden == null) {
            spieler.sendMessage(Text.mm(daten.getString("unbekannt", "<red>Das Thema gibt es nicht.")));
            return true;
        }
        zeigen(spieler, gefunden);
        return true;
    }

    @EventHandler
    public void beimKlick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menue menue)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player spieler)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        Thema thema = menue.themen.get(event.getSlot());
        if (thema == null) {
            return;
        }
        spieler.closeInventory();
        zeigen(spieler, thema);
    }

    @EventHandler
    public void beimZiehen(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menue) {
            event.setCancelled(true);
        }
    }

    private static String text(Object wert) {
        return wert == null ? "" : String.valueOf(wert).trim();
    }

    private static int zahl(Object wert, int standard) {
        if (wert instanceof Number nummer) {
            return nummer.intValue();
        }
        try {
            return Integer.parseInt(text(wert));
        } catch (NumberFormatException fehler) {
            return standard;
        }
    }

    private static List<String> zeilen(Object wert) {
        List<String> liste = new ArrayList<>();
        if (wert instanceof List<?> roh) {
            for (Object zeile : roh) {
                liste.add(String.valueOf(zeile));
            }
        } else if (wert != null) {
            liste.add(String.valueOf(wert));
        }
        return liste;
    }
}
