package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.gui.GuiUtil;
import de.lemonpvp.fastshop.util.Text;
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
import java.util.Map;
import java.util.UUID;

public final class AuktionsMenus {

    public static final int PRO_SEITE = 45;
    public static final int ZURUECK = 45;
    public static final int SORTIERUNG = 46;
    public static final int FILTER = 47;
    public static final int SUCHE = 48;
    public static final int GUTHABEN = 49;
    public static final int EIGENE = 50;
    public static final int VERKAUFEN = 51;
    public static final int SCHLIESSEN = 52;
    public static final int WEITER = 53;
    public static final int KAUF_ITEM = 13;
    public static final int EIGENE_ZURUECK = 49;

    public record Ansicht(int seite, Sortierung sortierung, Filter filter, String suche) {

        public static Ansicht start() {
            return new Ansicht(0, Sortierung.NEUESTE, Filter.ALLE, "");
        }

        public Ansicht mitSeite(int neueSeite) {
            return new Ansicht(neueSeite, sortierung, filter, suche);
        }
    }

    public abstract static class Menue implements InventoryHolder {
        Inventory inventory;
        private final Ansicht ansicht;
        final Map<Integer, UUID> angebote = new HashMap<>();

        Menue(Ansicht ansicht) {
            this.ansicht = ansicht;
        }

        public Ansicht ansicht() {
            return ansicht;
        }

        public UUID angebotAuf(int slot) {
            return angebote.get(slot);
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static final class Uebersicht extends Menue {
        Uebersicht(Ansicht ansicht) {
            super(ansicht);
        }
    }

    public static final class Eigene extends Menue {
        Eigene(Ansicht ansicht) {
            super(ansicht);
        }
    }

    public static final class Bestaetigung extends Menue {
        private final UUID angebot;

        Bestaetigung(Ansicht ansicht, UUID angebot) {
            super(ansicht);
            this.angebot = angebot;
        }

        public UUID angebot() {
            return angebot;
        }
    }

    private final FastShop plugin;

    public AuktionsMenus(FastShop plugin) {
        this.plugin = plugin;
    }

    private AuktionsHaus haus() {
        return plugin.auktionen();
    }

    private String geld(double betrag) {
        return plugin.economy().format(betrag);
    }

    public void uebersichtOeffnen(Player spieler, Ansicht ansicht) {
        List<Angebot> liste = haus().kaufbare(ansicht.sortierung(), ansicht.filter(), ansicht.suche());
        int seiten = Math.max(1, (liste.size() + PRO_SEITE - 1) / PRO_SEITE);
        Ansicht aktuell = ansicht.mitSeite(Math.max(0, Math.min(seiten - 1, ansicht.seite())));
        Uebersicht holder = new Uebersicht(aktuell);
        Inventory inv = Bukkit.createInventory(holder, 54, plugin.msgs().format("ah-title",
                "page", String.valueOf(aktuell.seite() + 1), "pages", String.valueOf(seiten)));
        holder.inventory = inv;

        long jetzt = System.currentTimeMillis();
        int start = aktuell.seite() * PRO_SEITE;
        for (int i = 0; i < PRO_SEITE && start + i < liste.size(); i++) {
            Angebot angebot = liste.get(start + i);
            boolean eigenes = angebot.verkaeufer().equals(spieler.getUniqueId());
            inv.setItem(i, anzeige(angebot, jetzt, List.of(eigenes ? "<aqua>Das ist dein Angebot" : "<yellow>Klick zum Kaufen")));
            holder.angebote.put(i, angebot.id());
        }

        ItemStack rahmen = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, rahmen);
        }
        if (aktuell.seite() > 0) {
            inv.setItem(ZURUECK, GuiUtil.item(Material.ARROW, 1, "<yellow><bold>Vorherige Seite",
                    List.of("<gray>Seite " + aktuell.seite() + " von " + seiten)));
        }
        if (aktuell.seite() < seiten - 1) {
            inv.setItem(WEITER, GuiUtil.item(Material.ARROW, 1, "<yellow><bold>Nächste Seite",
                    List.of("<gray>Seite " + (aktuell.seite() + 2) + " von " + seiten)));
        }
        inv.setItem(SORTIERUNG, GuiUtil.item(Material.HOPPER, 1, "<gold><bold>Sortierung",
                auswahl(Sortierung.values(), aktuell.sortierung())));
        inv.setItem(FILTER, GuiUtil.item(aktuell.filter().symbol(), 1, "<aqua><bold>Kategorie",
                auswahl(Filter.values(), aktuell.filter())));
        inv.setItem(SUCHE, aktuell.suche().isBlank()
                ? GuiUtil.item(Material.OAK_SIGN, 1, "<white><bold>Suchen",
                        List.of("<gray>Tippe <white>/ah search <Begriff>", "<gray>zum Beispiel <white>/ah search diamond"))
                : GuiUtil.glowing(Material.OAK_SIGN, 1, "<white><bold>Suche: <yellow>" + sicher(aktuell.suche()),
                        List.of("<gray>" + liste.size() + " Treffer", "", "<yellow>Klick zum Zurücksetzen")));
        inv.setItem(GUTHABEN, GuiUtil.head(spieler, "<green><bold>Dein Guthaben",
                List.of("<white>" + geld(plugin.economy().balance(spieler)), "",
                        "<gray>Angebote gesamt: <white>" + liste.size(), "", "<yellow>Klick zum Aktualisieren")));
        int eigeneAnzahl = haus().anzahlVon(spieler.getUniqueId());
        inv.setItem(EIGENE, GuiUtil.item(Material.ENDER_CHEST, 1, "<light_purple><bold>Deine Angebote",
                List.of("<gray>" + eigeneAnzahl + " von " + haus().maxAngebote() + " Plätzen belegt", "",
                        "<gray>Hier nimmst du Angebote zurück", "<gray>und holst abgelaufene Items ab.", "",
                        "<yellow>Klick zum Öffnen")));
        inv.setItem(VERKAUFEN, GuiUtil.item(Material.EMERALD, 1, "<green><bold>Item verkaufen",
                List.of("<gray>Nimm das Item in die Hand und tippe", "<white>/ah sell <Preis>", "",
                        "<gray>Beispiele: <white>1500<gray>, <white>2.5k<gray>, <white>1m",
                        "<gray>Ein Angebot läuft <white>" + haus().dauerStunden() + " Stunden<gray>.")));
        inv.setItem(SCHLIESSEN, GuiUtil.item(Material.BARRIER, 1, "<red><bold>Schließen", List.of()));
        spieler.openInventory(inv);
    }

    public void eigeneOeffnen(Player spieler, Ansicht zurueck) {
        Eigene holder = new Eigene(zurueck);
        Inventory inv = Bukkit.createInventory(holder, 54, plugin.msgs().format("ah-title-own"));
        holder.inventory = inv;
        long jetzt = System.currentTimeMillis();
        List<Angebot> liste = haus().vonSpieler(spieler.getUniqueId());
        for (int i = 0; i < PRO_SEITE && i < liste.size(); i++) {
            Angebot angebot = liste.get(i);
            List<String> hinweis = angebot.kaufbar(jetzt)
                    ? List.of("<green>Aktiv", "<yellow>Klick zum Zurücknehmen")
                    : List.of("<red>Abgelaufen", "<yellow>Klick zum Abholen");
            inv.setItem(i, anzeige(angebot, jetzt, hinweis));
            holder.angebote.put(i, angebot.id());
        }
        ItemStack rahmen = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, rahmen);
        }
        inv.setItem(48, GuiUtil.item(Material.BOOK, 1, "<light_purple><bold>Deine Angebote",
                List.of("<gray>" + liste.size() + " von " + haus().maxAngebote() + " Plätzen belegt", "",
                        "<gray>Abgelaufene Angebote zählen mit,", "<gray>bis du sie abholst.")));
        inv.setItem(EIGENE_ZURUECK, GuiUtil.item(Material.ARROW, 1, "<yellow><bold>Zurück zum Auktionshaus", List.of()));
        spieler.openInventory(inv);
    }

    public void bestaetigungOeffnen(Player spieler, Angebot angebot, Ansicht zurueck) {
        Bestaetigung holder = new Bestaetigung(zurueck, angebot.id());
        Inventory inv = Bukkit.createInventory(holder, 27, plugin.msgs().format("ah-title-confirm"));
        holder.inventory = inv;
        ItemStack rahmen = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < 27; slot++) {
            inv.setItem(slot, rahmen);
        }
        ItemStack ja = GuiUtil.item(Material.LIME_STAINED_GLASS_PANE, 1, "<green><bold>Kaufen für " + geld(angebot.preis()),
                List.of("<gray>Dein Guthaben: <white>" + geld(plugin.economy().balance(spieler))));
        ItemStack nein = GuiUtil.item(Material.RED_STAINED_GLASS_PANE, 1, "<red><bold>Abbrechen", List.of());
        for (int slot : new int[]{10, 11, 12}) {
            inv.setItem(slot, ja);
        }
        for (int slot : new int[]{14, 15, 16}) {
            inv.setItem(slot, nein);
        }
        inv.setItem(KAUF_ITEM, anzeige(angebot, System.currentTimeMillis(), List.of()));
        spieler.openInventory(inv);
    }

    public static boolean istKaufen(int slot) {
        return slot == 10 || slot == 11 || slot == 12;
    }

    public static boolean istAbbrechen(int slot) {
        return slot == 14 || slot == 15 || slot == 16;
    }

    private ItemStack anzeige(Angebot angebot, long jetzt, List<String> hinweis) {
        ItemStack item = angebot.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(zeile("<dark_gray>───────────────"));
        lore.add(zeile("<gray>Preis: <green>" + geld(angebot.preis())));
        lore.add(zeile("<gray>Verkäufer: <white>" + angebot.verkaeuferName()));
        lore.add(zeile("<gray>Endet in: <white>" + restzeit(angebot.endet() - jetzt)));
        if (!hinweis.isEmpty()) {
            lore.add(Component.empty());
            for (String text : hinweis) {
                lore.add(zeile(text));
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static <T extends Enum<T>> List<String> auswahl(T[] werte, T aktuell) {
        List<String> zeilen = new ArrayList<>();
        for (T wert : werte) {
            String name = wert instanceof Sortierung sortierung ? sortierung.anzeige() : ((Filter) wert).anzeige();
            zeilen.add(wert == aktuell ? "<green>» " + name : "<gray>  " + name);
        }
        zeilen.add("");
        zeilen.add("<yellow>Klick zum Wechseln");
        return zeilen;
    }

    private static Component zeile(String text) {
        return Text.mm(text).decoration(TextDecoration.ITALIC, false);
    }

    private static String sicher(String text) {
        return text.replace("<", "").replace(">", "").replace("&", "");
    }

    public static String restzeit(long millis) {
        if (millis <= 0) {
            return "abgelaufen";
        }
        long minuten = Math.max(1, millis / 60_000L);
        long tage = minuten / (60 * 24);
        long stunden = (minuten / 60) % 24;
        long rest = minuten % 60;
        if (tage > 0) {
            return tage + "T " + stunden + "Std";
        }
        if (stunden > 0) {
            return stunden + "Std " + rest + "Min";
        }
        return rest + "Min";
    }
}
