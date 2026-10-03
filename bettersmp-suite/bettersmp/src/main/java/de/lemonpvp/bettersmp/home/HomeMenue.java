package de.lemonpvp.bettersmp.home;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.gui.GuiItems;
import de.lemonpvp.bettersmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HomeMenue implements Listener {

    private static final Material[] BETTEN = {Material.RED_BED, Material.LIME_BED, Material.LIGHT_BLUE_BED,
            Material.YELLOW_BED, Material.PURPLE_BED, Material.ORANGE_BED, Material.PINK_BED};

    enum Art {
        TELEPORT, SETZEN, LOESCHEN_FRAGEN, VERSCHIEBEN_FRAGEN, LOESCHEN, VERSCHIEBEN, ZURUECK
    }

    record Aktion(Art art, int platz) {
    }

    public static final class Fenster implements InventoryHolder {
        private Inventory inventory;
        final Map<Integer, Aktion> aktionen = new HashMap<>();
        final Map<Integer, Aktion> rechtsklick = new HashMap<>();

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final BetterSMP plugin;
    private final HomeSpeicher speicher;
    private final HomeTeleport teleport;

    public HomeMenue(BetterSMP plugin, HomeSpeicher speicher, HomeTeleport teleport) {
        this.plugin = plugin;
        this.speicher = speicher;
        this.teleport = teleport;
    }

    static int[] positionen(int anzahl) {
        int schritt = anzahl <= 4 ? 2 : 1;
        int start = 13 - (anzahl - 1) * schritt / 2;
        int[] felder = new int[anzahl];
        for (int i = 0; i < anzahl; i++) {
            felder[i] = start + i * schritt;
        }
        return felder;
    }

    public void oeffnen(Player spieler) {
        int anzahl = speicher.plaetze();
        int belegt = speicher.belegt(spieler.getUniqueId());
        Fenster fenster = new Fenster();
        fenster.inventory = Bukkit.createInventory(fenster, 27,
                Text.mm("<gradient:#6C5CE7:#00D4FF><bold>Homes</bold></gradient> <dark_gray>» <gray>" + belegt + "/" + anzahl + " belegt"));
        ItemStack rand = GuiItems.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            fenster.inventory.setItem(i, rand);
        }
        fenster.inventory.setItem(4, GuiItems.item(Material.COMPASS, "<gradient:#6C5CE7:#00D4FF><bold>Deine Homes</bold></gradient>", List.of(
                "<gray>Belegt: <white>" + belegt + "</white> von <white>" + anzahl + "</white>",
                "",
                "<gray>Bett anklicken: <white>hinteleportieren</white>",
                "<gray>Rechtsklick aufs Bett: <white>hierher verschieben</white>",
                "",
                "<dark_gray>/sethome [Name] · /home <Name> · /delhome <Name>"), false));
        int[] felder = positionen(anzahl);
        for (int i = 0; i < anzahl; i++) {
            int platz = i + 1;
            Home home = speicher.home(spieler.getUniqueId(), platz);
            int feld = felder[i];
            if (home == null) {
                fenster.inventory.setItem(feld, GuiItems.item(Material.GRAY_DYE,
                        "<gray>" + HomeSpeicher.standardName(platz) + " <dark_gray>· frei", List.of(
                                "<gray>Noch nicht gesetzt.",
                                "",
                                "<yellow>▶ Klick: Home hier setzen"), false));
                fenster.inventory.setItem(feld + 9, GuiItems.item(Material.LIME_DYE, "<green>Hier setzen", List.of(
                        "<gray>Setzt <white>" + HomeSpeicher.standardName(platz) + "</white> auf deine Position."), false));
                fenster.aktionen.put(feld, new Aktion(Art.SETZEN, platz));
                fenster.aktionen.put(feld + 9, new Aktion(Art.SETZEN, platz));
            } else {
                fenster.inventory.setItem(feld, GuiItems.item(BETTEN[(platz - 1) % BETTEN.length],
                        "<white><bold>" + Text.sicher(home.name()) + "</bold>", List.of(
                                "<gray>Welt: <white>" + weltName(home),
                                "<gray>Position: <white>" + Math.round(Math.floor(home.x())) + " · " + Math.round(Math.floor(home.y()))
                                        + " · " + Math.round(Math.floor(home.z())),
                                "",
                                "<green>▶ Klick: hinteleportieren",
                                "<yellow>▶ Rechtsklick: hierher verschieben"), false));
                fenster.inventory.setItem(feld + 9, GuiItems.item(Material.RED_DYE, "<red>Löschen", List.of(
                        "<gray>Entfernt <white>" + Text.sicher(home.name()) + "</white>.",
                        "<dark_gray>Du wirst vorher noch gefragt."), false));
                fenster.aktionen.put(feld, new Aktion(Art.TELEPORT, platz));
                fenster.rechtsklick.put(feld, new Aktion(Art.VERSCHIEBEN_FRAGEN, platz));
                fenster.aktionen.put(feld + 9, new Aktion(Art.LOESCHEN_FRAGEN, platz));
            }
        }
        spieler.openInventory(fenster.inventory);
    }

    void fragen(Player spieler, int platz, boolean loeschen) {
        Home home = speicher.home(spieler.getUniqueId(), platz);
        if (home == null) {
            oeffnen(spieler);
            return;
        }
        Fenster fenster = new Fenster();
        String frage = loeschen ? "löschen?" : "hierher verschieben?";
        fenster.inventory = Bukkit.createInventory(fenster, 27,
                Text.mm("<dark_gray>" + Text.sicher(home.name()) + " " + frage));
        ItemStack rand = GuiItems.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            fenster.inventory.setItem(i, rand);
        }
        fenster.inventory.setItem(13, GuiItems.item(BETTEN[(platz - 1) % BETTEN.length], "<white><bold>" + Text.sicher(home.name()) + "</bold>",
                List.of("<gray>Welt: <white>" + weltName(home)), false));
        fenster.inventory.setItem(11, GuiItems.item(Material.LIME_CONCRETE,
                loeschen ? "<green><bold>Ja, löschen</bold>" : "<green><bold>Ja, hierher verschieben</bold>",
                List.of(loeschen ? "<gray>Das Home ist danach weg." : "<gray>Das Home zeigt danach auf deine Position."), false));
        fenster.inventory.setItem(15, GuiItems.item(Material.RED_CONCRETE, "<red><bold>Nein, zurück</bold>", List.of(), false));
        fenster.aktionen.put(11, new Aktion(loeschen ? Art.LOESCHEN : Art.VERSCHIEBEN, platz));
        fenster.aktionen.put(15, new Aktion(Art.ZURUECK, platz));
        spieler.openInventory(fenster.inventory);
    }

    static String weltName(Home home) {
        World welt = home.weltFinden();
        if (welt == null) {
            return home.welt() == null ? "fehlt" : home.welt() + " (fehlt)";
        }
        return switch (welt.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            case NORMAL -> welt.getName().toLowerCase(Locale.ROOT).startsWith("world") ? "Oberwelt" : welt.getName();
            default -> welt.getName();
        };
    }

    @EventHandler
    public void beimKlick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Fenster fenster)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player spieler) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        boolean rechts = event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT;
        Aktion aktion = rechts && fenster.rechtsklick.containsKey(event.getSlot())
                ? fenster.rechtsklick.get(event.getSlot()) : fenster.aktionen.get(event.getSlot());
        if (aktion == null) {
            return;
        }
        ausfuehren(spieler, aktion);
    }

    void ausfuehren(Player spieler, Aktion aktion) {
        switch (aktion.art()) {
            case TELEPORT -> {
                Home home = speicher.home(spieler.getUniqueId(), aktion.platz());
                spieler.closeInventory();
                if (home != null) {
                    teleport.starten(spieler, home);
                }
            }
            case SETZEN -> {
                plugin.homeBefehle().setzen(spieler, aktion.platz(), null);
                oeffnen(spieler);
            }
            case LOESCHEN_FRAGEN -> fragen(spieler, aktion.platz(), true);
            case VERSCHIEBEN_FRAGEN -> fragen(spieler, aktion.platz(), false);
            case LOESCHEN -> {
                plugin.homeBefehle().loeschen(spieler, aktion.platz());
                oeffnen(spieler);
            }
            case VERSCHIEBEN -> {
                Home home = speicher.home(spieler.getUniqueId(), aktion.platz());
                plugin.homeBefehle().setzen(spieler, aktion.platz(), home == null ? null : home.name());
                oeffnen(spieler);
            }
            case ZURUECK -> oeffnen(spieler);
        }
    }

    @EventHandler
    public void beimZiehen(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Fenster) {
            event.setCancelled(true);
        }
    }
}
