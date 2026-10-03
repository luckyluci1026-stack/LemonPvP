package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.gui.GuiUtil;
import de.lemonpvp.fastshop.shop.ShopItem;
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
import java.util.Locale;
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
    public static final int VERKAUF_ITEM = 13;
    public static final int VERKAUF_PREIS = 22;
    public static final int VERKAUF_ZURUECK = 45;
    public static final int VERKAUF_EINTIPPEN = 48;
    public static final int VERKAUF_ANBIETEN = 50;
    public static final int VERKAUF_SCHLIESSEN = 52;

    private static final int[] PLUS_PLAETZE = {29, 30, 31, 32, 33};
    private static final int[] MINUS_PLAETZE = {38, 39, 40, 41, 42};
    private static final double[] SCHRITTE = {10, 100, 1_000, 10_000, 100_000};
    private static final double STANDARD_PREIS = 100;

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
        private final int seite;

        Eigene(Ansicht ansicht, int seite) {
            super(ansicht);
            this.seite = seite;
        }

        public int seite() {
            return seite;
        }
    }

    public static final class Verkaufen extends Menue {
        private int slot = -1;
        private ItemStack vorschau;
        private double preis;
        private boolean preisGesetzt;

        Verkaufen(Ansicht ansicht) {
            super(ansicht);
        }

        public int slot() {
            return slot;
        }

        public ItemStack vorschau() {
            return vorschau == null ? null : vorschau.clone();
        }

        public double preis() {
            return preis;
        }

        public boolean preisGesetzt() {
            return preisGesetzt;
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
                        List.of("<gray>Item oder Spielername,", "<gray>Deutsch oder Englisch, z.B. <white>Diamant", "",
                                "<yellow>Klick zum Suchen", "<dark_gray>Geht auch: /ah <Begriff>"))
                : GuiUtil.glowing(Material.OAK_SIGN, 1, "<white><bold>Suche: <yellow>" + sicher(aktuell.suche()),
                        List.of("<gray>" + liste.size() + " Treffer", "",
                                "<yellow>Klick für eine neue Suche", "<gray>Leer lassen zeigt wieder alles.")));
        inv.setItem(GUTHABEN, GuiUtil.head(spieler, "<green><bold>Dein Guthaben",
                List.of("<white>" + geld(plugin.economy().balance(spieler)), "",
                        "<gray>Angebote gesamt: <white>" + liste.size(), "", "<yellow>Klick zum Aktualisieren")));
        int eigeneAnzahl = haus().anzahlVon(spieler.getUniqueId());
        inv.setItem(EIGENE, GuiUtil.item(Material.ENDER_CHEST, 1, "<light_purple><bold>Deine Angebote",
                List.of("<gray>" + eigeneAnzahl + " von " + haus().maxAngebote() + " Plätzen belegt", "",
                        "<gray>Hier nimmst du Angebote zurück", "<gray>und holst abgelaufene Items ab.", "",
                        "<yellow>Klick zum Öffnen")));
        inv.setItem(VERKAUFEN, GuiUtil.item(Material.EMERALD, 1, "<green><bold>Item verkaufen",
                List.of("<gray>Item auswählen, Preis einstellen,", "<gray>fertig. Ein Angebot läuft <white>"
                                + haus().dauerStunden() + " Stunden<gray>.", "",
                        "<yellow>Klick zum Verkaufen", "<dark_gray>Geht auch: /ah sell <Preis>")));
        inv.setItem(SCHLIESSEN, GuiUtil.item(Material.BARRIER, 1, "<red><bold>Schließen", List.of()));
        spieler.openInventory(inv);
    }

    public void eigeneOeffnen(Player spieler, Ansicht zurueck) {
        eigeneOeffnen(spieler, zurueck, 0);
    }

    public void eigeneOeffnen(Player spieler, Ansicht zurueck, int seite) {
        List<Angebot> liste = haus().vonSpieler(spieler.getUniqueId());
        int seiten = Math.max(1, (liste.size() + PRO_SEITE - 1) / PRO_SEITE);
        int aktuell = Math.max(0, Math.min(seiten - 1, seite));
        Eigene holder = new Eigene(zurueck, aktuell);
        Inventory inv = Bukkit.createInventory(holder, 54, plugin.msgs().format("ah-title-own"));
        holder.inventory = inv;
        long jetzt = System.currentTimeMillis();
        int start = aktuell * PRO_SEITE;
        for (int i = 0; i < PRO_SEITE && start + i < liste.size(); i++) {
            Angebot angebot = liste.get(start + i);
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
        if (aktuell > 0) {
            inv.setItem(ZURUECK, GuiUtil.item(Material.ARROW, 1, "<yellow><bold>Vorherige Seite",
                    List.of("<gray>Seite " + aktuell + " von " + seiten)));
        }
        if (aktuell < seiten - 1) {
            inv.setItem(WEITER, GuiUtil.item(Material.ARROW, 1, "<yellow><bold>Nächste Seite",
                    List.of("<gray>Seite " + (aktuell + 2) + " von " + seiten)));
        }
        List<String> buch = new ArrayList<>(List.of("<gray>" + liste.size() + " von " + haus().maxAngebote() + " Plätzen belegt", "",
                "<gray>Abgelaufene Angebote zählen mit,", "<gray>bis du sie abholst."));
        if (seiten > 1) {
            buch.add("");
            buch.add("<gray>Seite <white>" + (aktuell + 1) + "</white> von <white>" + seiten);
        }
        inv.setItem(48, GuiUtil.item(Material.BOOK, 1, "<light_purple><bold>Deine Angebote", buch));
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

    public void verkaufenOeffnen(Player spieler, Ansicht zurueck, int slot, double preis, boolean preisGesetzt) {
        Verkaufen holder = new Verkaufen(zurueck);
        Inventory inv = Bukkit.createInventory(holder, 54, plugin.msgs().format("ah-title-sell"));
        holder.inventory = inv;
        waehlen(spieler, holder, slot);
        holder.preisGesetzt = preisGesetzt;
        holder.preis = preisGesetzt ? haus().preisBegrenzen(preis) : vorschlag(holder.vorschau);
        verkaufenZeichnen(spieler, holder);
        spieler.openInventory(inv);
    }

    public void verkaufenWaehlen(Player spieler, Verkaufen holder, int slot) {
        waehlen(spieler, holder, slot);
        if (!holder.preisGesetzt) {
            holder.preis = vorschlag(holder.vorschau);
        }
        verkaufenZeichnen(spieler, holder);
    }

    public void verkaufenPreisAendern(Player spieler, Verkaufen holder, double preis) {
        holder.preis = haus().preisBegrenzen(preis);
        holder.preisGesetzt = true;
        verkaufenZeichnen(spieler, holder);
    }

    public static double schrittAuf(int slot) {
        for (int i = 0; i < SCHRITTE.length; i++) {
            if (PLUS_PLAETZE[i] == slot) {
                return SCHRITTE[i];
            }
            if (MINUS_PLAETZE[i] == slot) {
                return -SCHRITTE[i];
            }
        }
        return 0;
    }

    public double vorschlag(ItemStack item) {
        double wert = shopWert(item);
        return haus().preisBegrenzen(wert > 0 ? wert : STANDARD_PREIS);
    }

    private double shopWert(ItemStack item) {
        if (item == null) {
            return 0;
        }
        ShopItem eintrag = plugin.shop().item(item.getType());
        if (eintrag == null || !eintrag.sellable()) {
            return 0;
        }
        return eintrag.sell() * plugin.shop().sellMultiplier() * item.getAmount();
    }

    private void waehlen(Player spieler, Verkaufen holder, int slot) {
        ItemStack item = slot < 0 || slot > AuktionsHaus.LETZTER_INVENTAR_PLATZ ? null : spieler.getInventory().getItem(slot);
        if (item == null || item.isEmpty()) {
            holder.slot = -1;
            holder.vorschau = null;
        } else {
            holder.slot = slot;
            holder.vorschau = item.clone();
        }
    }

    private void verkaufenZeichnen(Player spieler, Verkaufen holder) {
        Inventory inv = holder.inventory;
        ItemStack rahmen = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < 45; slot++) {
            inv.setItem(slot, rahmen);
        }
        ItemStack leiste = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, leiste);
        }
        if (holder.vorschau == null) {
            inv.setItem(VERKAUF_ITEM, GuiUtil.item(Material.HOPPER, 1, "<yellow><bold>Item auswählen",
                    List.of("<gray>Klick unten in deinem Inventar", "<gray>auf das Item, das du verkaufen willst.")));
        } else {
            inv.setItem(VERKAUF_ITEM, mitZeilen(holder.vorschau.clone(), List.of("<dark_gray>───────────────",
                    "<green>Dieses Item bietest du an", "<gray>Klick unten auf ein anderes Item,", "<gray>um es zu wechseln.")));
        }
        double steuer = haus().steuerAnteil();
        List<String> preisZeilen = new ArrayList<>();
        preisZeilen.add("<gray>Du bekommst: <white>" + geld(holder.preis * (1 - steuer))
                + (steuer > 0 ? " <dark_gray>(" + Math.round(steuer * 100) + "% Gebühr)" : ""));
        preisZeilen.add("<gray>Läuft: <white>" + haus().dauerStunden() + " Stunden");
        double shop = shopWert(holder.vorschau);
        if (shop > 0) {
            preisZeilen.add("<gray>Der Shop zahlt dafür: <white>" + geld(shop));
        }
        preisZeilen.add("");
        preisZeilen.add("<gray>Ändern mit den grünen und roten");
        preisZeilen.add("<gray>Feldern oder mit <white>Preis eintippen<gray>.");
        inv.setItem(VERKAUF_PREIS, GuiUtil.glowing(Material.GOLD_INGOT, 1, "<gold><bold>Preis: <green>" + geld(holder.preis),
                preisZeilen));
        for (int i = 0; i < SCHRITTE.length; i++) {
            inv.setItem(PLUS_PLAETZE[i], GuiUtil.item(Material.LIME_STAINED_GLASS_PANE, i + 1,
                    "<green><bold>+" + zahl(SCHRITTE[i]), List.of("<gray>Klick: Preis erhöhen")));
            inv.setItem(MINUS_PLAETZE[i], GuiUtil.item(Material.RED_STAINED_GLASS_PANE, i + 1,
                    "<red><bold>-" + zahl(SCHRITTE[i]), List.of("<gray>Klick: Preis senken")));
        }
        inv.setItem(VERKAUF_ZURUECK, GuiUtil.item(Material.ARROW, 1, "<yellow><bold>Zurück zum Auktionshaus", List.of()));
        inv.setItem(VERKAUF_EINTIPPEN, GuiUtil.item(Material.NAME_TAG, 1, "<white><bold>Preis eintippen",
                List.of("<gray>Genauen Preis eingeben,", "<gray>zum Beispiel <white>1500<gray>, <white>2.5k<gray>, <white>1m", "",
                        "<yellow>Klick zum Eintippen", "<dark_gray>Geht auch: /ah sell <Preis>")));
        inv.setItem(VERKAUF_ANBIETEN, anbietenKnopf(spieler, holder));
        inv.setItem(VERKAUF_SCHLIESSEN, GuiUtil.item(Material.BARRIER, 1, "<red><bold>Schließen", List.of()));
    }

    private ItemStack anbietenKnopf(Player spieler, Verkaufen holder) {
        if (holder.vorschau == null) {
            return GuiUtil.item(Material.GRAY_DYE, 1, "<gray><bold>Angebot erstellen",
                    List.of("<red>Wähle zuerst unten ein Item aus."));
        }
        if (haus().anzahlVon(spieler.getUniqueId()) >= haus().maxAngebote()) {
            return GuiUtil.item(Material.GRAY_DYE, 1, "<gray><bold>Angebot erstellen",
                    List.of("<red>Alle " + haus().maxAngebote() + " Plätze sind belegt.",
                            "<gray>Hol erst eins unter <white>Deine Angebote<gray> ab."));
        }
        return GuiUtil.glowing(Material.LIME_CONCRETE, 1, "<green><bold>Angebot erstellen",
                List.of("<white>" + holder.vorschau.getAmount() + "x " + AuktionsHaus.itemText(holder.vorschau),
                        "<gray>für <green>" + geld(holder.preis), "<gray>Läuft <white>" + haus().dauerStunden() + " Stunden", "",
                        "<yellow>Klick zum Anbieten"));
    }

    public static boolean istKaufen(int slot) {
        return slot == 10 || slot == 11 || slot == 12;
    }

    public static boolean istAbbrechen(int slot) {
        return slot == 14 || slot == 15 || slot == 16;
    }

    private ItemStack anzeige(Angebot angebot, long jetzt, List<String> hinweis) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add("<dark_gray>───────────────");
        zeilen.add("<gray>Preis: <green>" + geld(angebot.preis()));
        zeilen.add("<gray>Verkäufer: <white>" + angebot.verkaeuferName());
        zeilen.add("<gray>Endet in: <white>" + restzeit(angebot.endet() - jetzt));
        if (!hinweis.isEmpty()) {
            zeilen.add("");
            zeilen.addAll(hinweis);
        }
        return mitZeilen(angebot.item(), zeilen);
    }

    private static ItemStack mitZeilen(ItemStack item, List<String> zeilen) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        for (String text : zeilen) {
            lore.add(text.isEmpty() ? Component.empty() : zeile(text));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String zahl(double wert) {
        return String.format(Locale.GERMANY, "%,.0f", wert);
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
