package de.lemonpvp.reportplus.gui;

import de.lemonpvp.reportplus.ReportPlus;
import de.lemonpvp.reportplus.store.BugEntry;
import de.lemonpvp.reportplus.store.ReportEntry;
import de.lemonpvp.reportplus.util.GuiItem;
import de.lemonpvp.reportplus.util.Kategorie;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Alle GUI-Fenster von ReportPlus an einem Ort - Holder + Aufbau, nach
 * dem Muster von FastShops ShopMenus: ein Fenster kennt selbst, was in
 * ihm steckt, statt eine Zuordnung nebenher zu pflegen.
 */
public final class Guis {

    private Guis() {
    }

    public static final int ITEMS_PER_PAGE = 45;
    public static final int NAV_PREV = 45;
    public static final int NAV_NEXT = 53;

    // ================= Spieler auswaehlen (/report ohne Ziel) =================

    public static final class PlayerPickerHolder implements InventoryHolder {
        private Inventory inventory;
        public final Map<Integer, UUID> ziele = new HashMap<>();

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void oeffnePlayerPicker(ReportPlus plugin, Player spieler, String titel) {
        List<Player> andere = new ArrayList<>(Bukkit.getOnlinePlayers());
        andere.remove(spieler);
        if (andere.isEmpty()) {
            plugin.msgs().send(spieler, "report.picker-empty");
            return;
        }

        PlayerPickerHolder holder = new PlayerPickerHolder();
        int groesse = Math.max(9, Math.min(54, ((andere.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(holder, groesse, GuiItem.mm(titel));
        holder.inventory = inv;

        int slot = 0;
        for (Player ziel : andere) {
            if (slot >= groesse) {
                break;
            }
            inv.setItem(slot, GuiItem.head(ziel, "<white>" + ziel.getName(),
                    List.of("<yellow>Klick zum Melden")));
            holder.ziele.put(slot, ziel.getUniqueId());
            slot++;
        }
        spieler.openInventory(inv);
    }

    // ================= Kategorie/Grund auswaehlen (Reports + Bugreports) =================

    public static final class KategoriePickerHolder implements InventoryHolder {
        private Inventory inventory;
        public final Map<Integer, String> optionen = new HashMap<>();
        public final Consumer<String> beiAuswahl;

        public KategoriePickerHolder(Consumer<String> beiAuswahl) {
            this.beiAuswahl = beiAuswahl;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void oeffneKategoriePicker(Player spieler, String titel, List<Kategorie> kategorien,
                                              Consumer<String> beiAuswahl) {
        KategoriePickerHolder holder = new KategoriePickerHolder(beiAuswahl);
        int groesse = Math.max(9, Math.min(54, ((kategorien.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(holder, groesse, GuiItem.mm(titel));
        holder.inventory = inv;

        int slot = 0;
        for (Kategorie kategorie : kategorien) {
            inv.setItem(slot, GuiItem.of(kategorie.icon(), kategorie.name(),
                    List.of("<gray>Klick zum Auswählen")));
            holder.optionen.put(slot, kategorie.id());
            slot++;
        }
        spieler.openInventory(inv);
    }

    // ================= Meldungs-Liste (Team) =================

    public static final class ReportListHolder implements InventoryHolder {
        private Inventory inventory;
        public final int page;
        public final Map<Integer, Integer> eintraege = new HashMap<>();

        public ReportListHolder(int page) {
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void oeffneReportListe(ReportPlus plugin, Player spieler, int page) {
        List<ReportEntry> offene = plugin.reports().offene();
        if (offene.isEmpty()) {
            spieler.closeInventory();
            plugin.msgs().send(spieler, "report.list-empty");
            return;
        }
        int totalPages = Math.max(1, (offene.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(totalPages - 1, page));

        ReportListHolder holder = new ReportListHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                GuiItem.mm(plugin.msgs().raw("report.list-title")
                        .replace("%anzahl%", String.valueOf(offene.size()))
                        + " <dark_gray>│ <gray>" + (page + 1) + "/" + totalPages));
        holder.inventory = inv;

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < offene.size(); i++) {
            ReportEntry eintrag = offene.get(start + i);
            var ziel = Bukkit.getOfflinePlayer(eintrag.ziel());
            inv.setItem(i, GuiItem.head(ziel, "<white>#" + eintrag.id() + " <dark_gray>» <white>" + eintrag.zielName(),
                    List.of(
                            "<gray>Von: <white>" + eintrag.reporterName(),
                            "<gray>Grund: <white>" + eintrag.kategorie(),
                            "<gray>Vor: <white>" + vorZeit(eintrag.zeit()),
                            "",
                            "<yellow>Klick für Details")));
            holder.eintraege.put(i, eintrag.id());
        }

        fussleiste(plugin, inv, page, totalPages);
        spieler.openInventory(inv);
    }

    public static final class ReportDetailHolder implements InventoryHolder {
        private Inventory inventory;
        public final int id;
        public final int zurueckZuSeite;

        public ReportDetailHolder(int id, int zurueckZuSeite) {
            this.id = id;
            this.zurueckZuSeite = zurueckZuSeite;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static final int ACT_TELEPORT = 11;
    public static final int ACT_RESOLVE = 13;
    public static final int ACT_DISMISS = 15;
    public static final int ACT_BACK = 22;

    public static void oeffneReportDetail(ReportPlus plugin, Player spieler, ReportEntry eintrag, int zurueckZuSeite) {
        ReportDetailHolder holder = new ReportDetailHolder(eintrag.id(), zurueckZuSeite);
        Inventory inv = Bukkit.createInventory(holder, 27,
                GuiItem.mm(plugin.msgs().raw("report.detail-title").replace("%id%", String.valueOf(eintrag.id()))));
        holder.inventory = inv;

        GuiItem.fuelle(inv, Material.GRAY_STAINED_GLASS_PANE);

        var ziel = Bukkit.getOfflinePlayer(eintrag.ziel());
        boolean online = ziel.isOnline();
        inv.setItem(4, GuiItem.head(ziel, "<white>" + eintrag.zielName(), List.of(
                "<gray>Gemeldet von: <white>" + eintrag.reporterName(),
                "<gray>Grund: <white>" + eintrag.kategorie(),
                "<gray>Zeitpunkt: <white>" + vorZeit(eintrag.zeit()),
                "<gray>Status: <white>" + (online ? "<green>online" : "<red>offline"))));

        inv.setItem(ACT_TELEPORT, GuiItem.of(online ? Material.ENDER_PEARL : Material.BARRIER,
                plugin.msgs().raw("gui.teleport"),
                List.of(online ? "<gray>Klick zum Teleportieren" : "<red>Gerade nicht online")));
        inv.setItem(ACT_RESOLVE, GuiItem.of(Material.LIME_DYE, plugin.msgs().raw("gui.resolve"), List.of()));
        inv.setItem(ACT_DISMISS, GuiItem.of(Material.RED_DYE, plugin.msgs().raw("gui.dismiss"), List.of()));
        inv.setItem(ACT_BACK, GuiItem.of(Material.ARROW, plugin.msgs().raw("gui.back"), List.of()));

        spieler.openInventory(inv);
    }

    // ================= Bugmeldungs-Liste (Team) =================

    public static final class BugListHolder implements InventoryHolder {
        private Inventory inventory;
        public final int page;
        public final Map<Integer, Integer> eintraege = new HashMap<>();

        public BugListHolder(int page) {
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void oeffneBugListe(ReportPlus plugin, Player spieler, int page) {
        List<BugEntry> offene = plugin.bugs().offene();
        if (offene.isEmpty()) {
            spieler.closeInventory();
            plugin.msgs().send(spieler, "bugreport.list-empty");
            return;
        }
        int totalPages = Math.max(1, (offene.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(totalPages - 1, page));

        BugListHolder holder = new BugListHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                GuiItem.mm(plugin.msgs().raw("bugreport.list-title")
                        .replace("%anzahl%", String.valueOf(offene.size()))
                        + " <dark_gray>│ <gray>" + (page + 1) + "/" + totalPages));
        holder.inventory = inv;

        List<Kategorie> kategorien = Kategorie.laden(plugin, "bugreport.categories");
        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < offene.size(); i++) {
            BugEntry eintrag = offene.get(start + i);
            Material icon = iconFuer(kategorien, eintrag.kategorie());
            inv.setItem(i, GuiItem.of(icon, "<white>#" + eintrag.id() + " <dark_gray>» <white>" + eintrag.kategorie(),
                    List.of(
                            "<gray>Von: <white>" + eintrag.reporterName(),
                            "<gray>Vor: <white>" + vorZeit(eintrag.zeit()),
                            "",
                            "<gray>" + kuerzen(eintrag.text()),
                            "",
                            "<yellow>Klick für Details")));
            holder.eintraege.put(i, eintrag.id());
        }

        fussleiste(plugin, inv, page, totalPages);
        spieler.openInventory(inv);
    }

    public static final class BugDetailHolder implements InventoryHolder {
        private Inventory inventory;
        public final int id;
        public final int zurueckZuSeite;

        public BugDetailHolder(int id, int zurueckZuSeite) {
            this.id = id;
            this.zurueckZuSeite = zurueckZuSeite;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void oeffneBugDetail(ReportPlus plugin, Player spieler, BugEntry eintrag, int zurueckZuSeite) {
        BugDetailHolder holder = new BugDetailHolder(eintrag.id(), zurueckZuSeite);
        Inventory inv = Bukkit.createInventory(holder, 27,
                GuiItem.mm(plugin.msgs().raw("bugreport.detail-title").replace("%id%", String.valueOf(eintrag.id()))));
        holder.inventory = inv;

        GuiItem.fuelle(inv, Material.GRAY_STAINED_GLASS_PANE);

        List<Kategorie> kategorien = Kategorie.laden(plugin, "bugreport.categories");
        Material icon = iconFuer(kategorien, eintrag.kategorie());
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Von: <white>" + eintrag.reporterName());
        lore.add("<gray>Zeitpunkt: <white>" + vorZeit(eintrag.zeit()));
        lore.add("");
        for (String zeile : umbrechen(eintrag.text(), 30)) {
            lore.add("<white>" + zeile);
        }
        inv.setItem(4, GuiItem.of(icon, "<white>#" + eintrag.id() + " <dark_gray>» " + eintrag.kategorie(), lore));

        inv.setItem(ACT_RESOLVE, GuiItem.of(Material.LIME_DYE, plugin.msgs().raw("gui.resolve"), List.of()));
        inv.setItem(ACT_DISMISS, GuiItem.of(Material.RED_DYE, plugin.msgs().raw("gui.dismiss"), List.of()));
        inv.setItem(ACT_BACK, GuiItem.of(Material.ARROW, plugin.msgs().raw("gui.back"), List.of()));

        spieler.openInventory(inv);
    }

    // ================= Kleinkram =================

    private static void fussleiste(ReportPlus plugin, Inventory inv, int page, int totalPages) {
        GuiItem.fuelleBereich(inv, 45, 54, Material.GRAY_STAINED_GLASS_PANE);
        if (page > 0) {
            inv.setItem(NAV_PREV, GuiItem.of(Material.ARROW, plugin.msgs().raw("gui.prev-page"), List.of()));
        }
        if (page < totalPages - 1) {
            inv.setItem(NAV_NEXT, GuiItem.of(Material.ARROW, plugin.msgs().raw("gui.next-page"), List.of()));
        }
    }

    private static Material iconFuer(List<Kategorie> kategorien, String id) {
        for (Kategorie kategorie : kategorien) {
            if (kategorie.id().equalsIgnoreCase(id)) {
                return kategorie.icon();
            }
        }
        return Material.PAPER;
    }

    private static String kuerzen(String text) {
        return text.length() <= 40 ? text : text.substring(0, 37) + "...";
    }

    private static List<String> umbrechen(String text, int breite) {
        List<String> zeilen = new ArrayList<>();
        StringBuilder aktuelle = new StringBuilder();
        for (String wort : text.split(" ")) {
            if (aktuelle.length() + wort.length() + 1 > breite && !aktuelle.isEmpty()) {
                zeilen.add(aktuelle.toString());
                aktuelle = new StringBuilder();
            }
            if (!aktuelle.isEmpty()) {
                aktuelle.append(' ');
            }
            aktuelle.append(wort);
        }
        if (!aktuelle.isEmpty()) {
            zeilen.add(aktuelle.toString());
        }
        return zeilen;
    }

    private static String vorZeit(long zeitpunkt) {
        long diffMs = System.currentTimeMillis() - zeitpunkt;
        Duration dauer = Duration.ofMillis(Math.max(0, diffMs));
        if (dauer.toDays() > 0) {
            return dauer.toDays() + "d";
        }
        if (dauer.toHours() > 0) {
            return dauer.toHours() + "h";
        }
        if (dauer.toMinutes() > 0) {
            return dauer.toMinutes() + "m";
        }
        return "gerade eben";
    }
}
