package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class AuktionsHaus {

    public enum AnbietenErgebnis {
        ANGEBOTEN,
        KEINE_WIRTSCHAFT,
        NICHTS_IN_DER_HAND,
        VERAENDERT,
        PREIS_AUSSERHALB,
        ZU_VIELE
    }

    public static final int LETZTER_INVENTAR_PLATZ = 35;

    public enum KaufErgebnis {
        GEKAUFT,
        KEINE_WIRTSCHAFT,
        NICHT_MEHR_DA,
        EIGENES,
        KEIN_PLATZ,
        ZU_WENIG_GELD
    }

    public enum RuecknahmeErgebnis {
        ZURUECK,
        NICHT_MEHR_DA,
        KEIN_PLATZ
    }

    public record Verkauf(String itemText, int menge, double preis, String kaeufer) {
    }

    private record Zeile(UUID id, UUID verkaeufer, String name, double preis, long erstellt, long endet,
                         String status, String item) {
    }

    private record Stand(List<Zeile> angebote, Map<UUID, List<Verkauf>> meldungen) {
    }

    private final FastShop plugin;
    private final File datei;
    private final Map<UUID, Angebot> angebote = new LinkedHashMap<>();
    private final Map<UUID, List<Verkauf>> offeneMeldungen = new HashMap<>();
    private final ExecutorService speicherer = Executors.newSingleThreadExecutor(aufgabe -> {
        Thread thread = new Thread(aufgabe, "FastShop-Auktionshaus");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<Stand> ausstehend = new AtomicReference<>();

    public AuktionsHaus(FastShop plugin) {
        this.plugin = plugin;
        this.datei = new File(plugin.getDataFolder(), "auktionen.yml");
        laden();
        Bukkit.getScheduler().runTaskTimer(plugin, this::ablaufPruefen, 20L * 30, 20L * 30);
    }

    public int dauerStunden() {
        return Math.max(1, plugin.getConfig().getInt("auktionshaus.dauer-stunden", 48));
    }

    public int maxAngebote() {
        return Math.max(1, plugin.getConfig().getInt("auktionshaus.max-angebote", 50));
    }

    public double minPreis() {
        return Math.max(0.01, plugin.getConfig().getDouble("auktionshaus.min-preis", 1));
    }

    public double maxPreis() {
        return Math.max(minPreis(), plugin.getConfig().getDouble("auktionshaus.max-preis", 1_000_000_000));
    }

    public double steuerAnteil() {
        double prozent = plugin.getConfig().getDouble("auktionshaus.steuer-prozent", 0);
        return Math.max(0, Math.min(100, prozent)) / 100.0;
    }

    public double preisBegrenzen(double preis) {
        if (!Double.isFinite(preis)) {
            return minPreis();
        }
        return runden(Math.max(minPreis(), Math.min(maxPreis(), preis)));
    }

    public Angebot angebot(UUID id) {
        return angebote.get(id);
    }

    public List<Angebot> kaufbare(Sortierung sortierung, Filter filter, String suche) {
        long jetzt = System.currentTimeMillis();
        String begriff = suche == null ? "" : suche.toLowerCase(Locale.ROOT).trim();
        List<Angebot> liste = new ArrayList<>();
        for (Angebot angebot : angebote.values()) {
            if (angebot.kaufbar(jetzt) && filter.passt(angebot.typ()) && (begriff.isEmpty() || angebot.passtZu(begriff))) {
                liste.add(angebot);
            }
        }
        liste.sort(sortierung.reihenfolge());
        return liste;
    }

    public List<Angebot> vonSpieler(UUID spieler) {
        long jetzt = System.currentTimeMillis();
        List<Angebot> liste = new ArrayList<>();
        for (Angebot angebot : angebote.values()) {
            if (angebot.verkaeufer().equals(spieler)) {
                liste.add(angebot);
            }
        }
        liste.sort(Comparator.comparing((Angebot angebot) -> angebot.kaufbar(jetzt)).thenComparingLong(Angebot::endet));
        return liste;
    }

    public int anzahlVon(UUID spieler) {
        int anzahl = 0;
        for (Angebot angebot : angebote.values()) {
            if (angebot.verkaeufer().equals(spieler)) {
                anzahl++;
            }
        }
        return anzahl;
    }

    public AnbietenErgebnis anbieten(Player spieler, int slot, ItemStack erwartet, double preis) {
        if (!plugin.economy().isEnabled()) {
            return AnbietenErgebnis.KEINE_WIRTSCHAFT;
        }
        if (slot < 0 || slot > LETZTER_INVENTAR_PLATZ) {
            return AnbietenErgebnis.NICHTS_IN_DER_HAND;
        }
        PlayerInventory inventar = spieler.getInventory();
        ItemStack item = inventar.getItem(slot);
        if (item == null || item.isEmpty()) {
            return AnbietenErgebnis.NICHTS_IN_DER_HAND;
        }
        if (erwartet != null && (!item.isSimilar(erwartet) || item.getAmount() != erwartet.getAmount())) {
            return AnbietenErgebnis.VERAENDERT;
        }
        if (!Double.isFinite(preis) || preis < minPreis() || preis > maxPreis()) {
            return AnbietenErgebnis.PREIS_AUSSERHALB;
        }
        if (anzahlVon(spieler.getUniqueId()) >= maxAngebote()) {
            return AnbietenErgebnis.ZU_VIELE;
        }
        long jetzt = System.currentTimeMillis();
        Angebot angebot = new Angebot(UUID.randomUUID(), spieler.getUniqueId(), spieler.getName(), item,
                runden(preis), jetzt, jetzt + dauerStunden() * 3_600_000L, Angebot.Status.AKTIV);
        inventar.setItem(slot, null);
        angebote.put(angebot.id(), angebot);
        speichern();
        return AnbietenErgebnis.ANGEBOTEN;
    }

    public KaufErgebnis kaufen(Player kaeufer, UUID id) {
        Angebot angebot = angebote.get(id);
        if (angebot == null || !angebot.kaufbar(System.currentTimeMillis())) {
            return KaufErgebnis.NICHT_MEHR_DA;
        }
        if (angebot.verkaeufer().equals(kaeufer.getUniqueId())) {
            return KaufErgebnis.EIGENES;
        }
        if (!plugin.economy().isEnabled()) {
            return KaufErgebnis.KEINE_WIRTSCHAFT;
        }
        if (kaeufer.getInventory().firstEmpty() == -1) {
            return KaufErgebnis.KEIN_PLATZ;
        }
        if (!plugin.economy().has(kaeufer, angebot.preis()) || !plugin.economy().withdraw(kaeufer, angebot.preis())) {
            return KaufErgebnis.ZU_WENIG_GELD;
        }
        angebote.remove(id);
        geben(kaeufer, angebot.item());
        double auszahlung = runden(angebot.preis() * (1 - steuerAnteil()));
        OfflinePlayer verkaeufer = Bukkit.getOfflinePlayer(angebot.verkaeufer());
        if (!plugin.economy().depositOffline(verkaeufer, auszahlung)) {
            plugin.getLogger().severe("Auktionshaus: " + angebot.verkaeuferName() + " (" + angebot.verkaeufer()
                    + ") konnte die Zahlung von " + auszahlung + " nicht gutgeschrieben werden - bitte von Hand nachbuchen.");
        }
        Verkauf verkauf = new Verkauf(itemText(angebot.item()), angebot.menge(), auszahlung, kaeufer.getName());
        Player online = verkaeufer.getPlayer();
        if (online != null && online.isOnline()) {
            melden(online, "ah-sold", verkauf);
        } else {
            offeneMeldungen.computeIfAbsent(angebot.verkaeufer(), schluessel -> new ArrayList<>()).add(verkauf);
        }
        speichern();
        return KaufErgebnis.GEKAUFT;
    }

    public RuecknahmeErgebnis zuruecknehmen(Player spieler, UUID id) {
        Angebot angebot = angebote.get(id);
        if (angebot == null || !angebot.verkaeufer().equals(spieler.getUniqueId())) {
            return RuecknahmeErgebnis.NICHT_MEHR_DA;
        }
        if (spieler.getInventory().firstEmpty() == -1) {
            return RuecknahmeErgebnis.KEIN_PLATZ;
        }
        angebote.remove(id);
        geben(spieler, angebot.item());
        speichern();
        return RuecknahmeErgebnis.ZURUECK;
    }

    public void beimJoin(Player spieler) {
        ablaufPruefen();
        List<Verkauf> verkaeufe = offeneMeldungen.remove(spieler.getUniqueId());
        if (verkaeufe != null) {
            for (Verkauf verkauf : verkaeufe) {
                melden(spieler, "ah-sold-offline", verkauf);
            }
            speichern();
        }
        long jetzt = System.currentTimeMillis();
        int abgelaufen = 0;
        for (Angebot angebot : angebote.values()) {
            if (angebot.verkaeufer().equals(spieler.getUniqueId()) && !angebot.kaufbar(jetzt)) {
                abgelaufen++;
            }
        }
        if (abgelaufen > 0) {
            senden(spieler, "ah-expired-join", "count", String.valueOf(abgelaufen));
        }
    }

    private void ablaufPruefen() {
        long jetzt = System.currentTimeMillis();
        boolean geaendert = false;
        for (Angebot angebot : angebote.values()) {
            if (angebot.status() == Angebot.Status.AKTIV && jetzt >= angebot.endet()) {
                angebot.ablaufen();
                geaendert = true;
                Player verkaeufer = Bukkit.getPlayer(angebot.verkaeufer());
                if (verkaeufer != null) {
                    senden(verkaeufer, "ah-expired", "amount", String.valueOf(angebot.menge()),
                            "item", itemText(angebot.item()));
                }
            }
        }
        if (geaendert) {
            speichern();
        }
    }

    private void geben(Player spieler, ItemStack item) {
        for (ItemStack rest : spieler.getInventory().addItem(item).values()) {
            spieler.getWorld().dropItem(spieler.getLocation(), rest).setOwner(spieler.getUniqueId());
        }
    }

    private void melden(Player spieler, String schluessel, Verkauf verkauf) {
        senden(spieler, schluessel, "buyer", verkauf.kaeufer(), "amount", String.valueOf(verkauf.menge()),
                "item", verkauf.itemText(), "price", plugin.economy().format(verkauf.preis()));
    }

    public void senden(CommandSender empfaenger, String schluessel, String... ersetzungen) {
        String[] alle = new String[ersetzungen.length + 2];
        alle[0] = "ah";
        alle[1] = plugin.msgs().raw("ah-prefix");
        System.arraycopy(ersetzungen, 0, alle, 2, ersetzungen.length);
        plugin.msgs().send(empfaenger, schluessel, alle);
    }

    public static String itemText(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) {
                return MiniMessage.miniMessage().escapeTags(PlainTextComponentSerializer.plainText().serialize(name));
            }
        }
        return "<lang:" + item.translationKey() + ">";
    }

    private static double runden(double betrag) {
        return Math.round(betrag * 100.0) / 100.0;
    }

    private void laden() {
        if (!datei.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(datei);
        ConfigurationSection bereich = yml.getConfigurationSection("angebote");
        if (bereich != null) {
            for (String schluessel : bereich.getKeys(false)) {
                ConfigurationSection eintrag = bereich.getConfigurationSection(schluessel);
                if (eintrag == null) {
                    continue;
                }
                try {
                    ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(eintrag.getString("item", "")));
                    Angebot angebot = new Angebot(UUID.fromString(schluessel), UUID.fromString(eintrag.getString("verkaeufer", "")),
                            eintrag.getString("name", "?"), item, eintrag.getDouble("preis"), eintrag.getLong("erstellt"),
                            eintrag.getLong("endet"), Angebot.Status.valueOf(eintrag.getString("status", "AKTIV")));
                    angebote.put(angebot.id(), angebot);
                } catch (Exception fehler) {
                    plugin.getLogger().warning("Auktionshaus: Angebot " + schluessel + " konnte nicht geladen werden: "
                            + fehler.getMessage());
                }
            }
        }
        ConfigurationSection meldungen = yml.getConfigurationSection("meldungen");
        if (meldungen != null) {
            for (String schluessel : meldungen.getKeys(false)) {
                List<Verkauf> liste = new ArrayList<>();
                for (Map<?, ?> eintrag : meldungen.getMapList(schluessel)) {
                    liste.add(new Verkauf(String.valueOf(eintrag.get("item")), zahl(eintrag.get("menge")).intValue(),
                            zahl(eintrag.get("preis")).doubleValue(), String.valueOf(eintrag.get("kaeufer"))));
                }
                try {
                    offeneMeldungen.put(UUID.fromString(schluessel), liste);
                } catch (IllegalArgumentException fehler) {
                    plugin.getLogger().warning("Auktionshaus: ungueltige Spieler-ID in auktionen.yml: " + schluessel);
                }
            }
        }
    }

    private static Number zahl(Object wert) {
        return wert instanceof Number nummer ? nummer : 0;
    }

    private Stand schnappschuss() {
        List<Zeile> zeilen = new ArrayList<>(angebote.size());
        for (Angebot angebot : angebote.values()) {
            zeilen.add(new Zeile(angebot.id(), angebot.verkaeufer(), angebot.verkaeuferName(), angebot.preis(),
                    angebot.erstellt(), angebot.endet(), angebot.status().name(), angebot.itemDaten()));
        }
        Map<UUID, List<Verkauf>> meldungen = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<Verkauf>> eintrag : offeneMeldungen.entrySet()) {
            meldungen.put(eintrag.getKey(), List.copyOf(eintrag.getValue()));
        }
        return new Stand(zeilen, meldungen);
    }

    private static String alsText(Stand stand) {
        YamlConfiguration yml = new YamlConfiguration();
        for (Zeile angebot : stand.angebote()) {
            String pfad = "angebote." + angebot.id();
            yml.set(pfad + ".verkaeufer", angebot.verkaeufer().toString());
            yml.set(pfad + ".name", angebot.name());
            yml.set(pfad + ".preis", angebot.preis());
            yml.set(pfad + ".erstellt", angebot.erstellt());
            yml.set(pfad + ".endet", angebot.endet());
            yml.set(pfad + ".status", angebot.status());
            yml.set(pfad + ".item", angebot.item());
        }
        for (Map.Entry<UUID, List<Verkauf>> eintrag : stand.meldungen().entrySet()) {
            List<Map<String, Object>> liste = new ArrayList<>();
            for (Verkauf verkauf : eintrag.getValue()) {
                Map<String, Object> werte = new LinkedHashMap<>();
                werte.put("item", verkauf.itemText());
                werte.put("menge", verkauf.menge());
                werte.put("preis", verkauf.preis());
                werte.put("kaeufer", verkauf.kaeufer());
                liste.add(werte);
            }
            yml.set("meldungen." + eintrag.getKey(), liste);
        }
        return yml.saveToString();
    }

    private void speichern() {
        if (ausstehend.getAndSet(schnappschuss()) == null) {
            speicherer.execute(() -> {
                Stand stand = ausstehend.getAndSet(null);
                if (stand != null) {
                    schreiben(alsText(stand));
                }
            });
        }
    }

    public void speichernSofort() {
        String inhalt = alsText(schnappschuss());
        ausstehend.set(null);
        speicherer.shutdown();
        try {
            speicherer.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException fehler) {
            Thread.currentThread().interrupt();
        }
        schreiben(inhalt);
    }

    private void schreiben(String inhalt) {
        try {
            Files.createDirectories(datei.getParentFile().toPath());
            File zwischen = new File(datei.getParentFile(), "auktionen.yml.tmp");
            Files.writeString(zwischen.toPath(), inhalt, StandardCharsets.UTF_8);
            try {
                Files.move(zwischen.toPath(), datei.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fehler) {
                Files.move(zwischen.toPath(), datei.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException fehler) {
            plugin.getLogger().severe("Auktionshaus: auktionen.yml konnte nicht gespeichert werden: " + fehler.getMessage());
        }
    }
}
