package de.lemonpvp.bettersmp.reward;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /daily - eine Kleinigkeit fuers Wiederkommen, mit Bonus fuer die Serie.
 *
 * Der Tag wird als Kalendertag gerechnet (LocalDate, nicht "24 Stunden
 * seit dem letzten Mal"): Sonst koennte sich jemand durch fruehes
 * Abholen eine zweite Belohnung am selben Tag erschleichen, indem er
 * die Uhrzeit ein paar Minuten vorzieht. Ein Kalendertag ist eindeutig.
 *
 * Gespeichert wird in einer eigenen daily.yml statt in BetterSMPs
 * Datenbank: Zwei Zahlen pro Spieler brauchen kein Datenbankschema, das
 * bei jedem Update mitwandern muesste.
 */
public final class DailyRewardManager {

    private final BetterSMP plugin;
    private YamlConfiguration daten;
    private File datei;

    public DailyRewardManager(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public void load() {
        datei = new File(plugin.getDataFolder(), "daily.yml");
        daten = YamlConfiguration.loadConfiguration(datei);
    }

    private long heute() {
        return LocalDate.now(ZoneId.systemDefault()).toEpochDay();
    }

    /** @return true, wenn heute schon abgeholt wurde */
    public boolean bereitsAbgeholt(UUID spieler) {
        return daten.getLong(spieler + ".tag", Long.MIN_VALUE) == heute();
    }

    /** Wie viele Kalendertage bis zum naechsten Abholen - fuer die Anzeige. */
    public long stundenBisMitternacht() {
        java.time.LocalDateTime jetzt = java.time.LocalDateTime.now();
        java.time.LocalDateTime mitternacht = jetzt.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(jetzt, mitternacht).toMinutes() / 60 + 1;
    }

    /**
     * Holt ab und meldet die neue Serienlaenge zurueck.
     *
     * Die Serie waechst nur, wenn der letzte Abholtag genau gestern war -
     * ein ausgelassener Tag reisst sie ab. Wer zum allerersten Mal
     * abholt, startet bei 1.
     */
    public int abholen(Player spieler) {
        String pfad = spieler.getUniqueId().toString();
        long letzterTag = daten.getLong(pfad + ".tag", Long.MIN_VALUE);
        int bisherigeSerie = daten.getInt(pfad + ".serie", 0);
        long heute = heute();

        int neueSerie = (letzterTag == heute - 1) ? bisherigeSerie + 1 : 1;
        daten.set(pfad + ".tag", heute);
        daten.set(pfad + ".serie", neueSerie);
        speichern();

        gibBelohnung(spieler, neueSerie);
        return neueSerie;
    }

    private void gibBelohnung(Player spieler, int serie) {
        for (Map<?, ?> eintrag : plugin.getConfig().getMapList("daily-reward.items")) {
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(
                    String.valueOf(eintrag.get("material")).toUpperCase(Locale.ROOT));
            if (material == null) {
                continue;
            }
            int menge = zahl(eintrag.get("amount"), 1);
            gibOderWirfVorDieFuesse(spieler, new ItemStack(material, menge));
        }

        double basis = plugin.getConfig().getDouble("daily-reward.money-base", 0);
        double proSerie = plugin.getConfig().getDouble("daily-reward.money-per-streak-day", 0);
        double gesamt = basis + proSerie * Math.min(serie, plugin.getConfig()
                .getInt("daily-reward.money-streak-cap", 30));
        if (gesamt > 0 && plugin.economy().isEnabled()) {
            plugin.economy().deposit(spieler, gesamt);
        }
    }

    /** Ins Inventar, oder vor die Fuesse, wenn es voll ist - nie einfach verschwinden lassen. */
    private void gibOderWirfVorDieFuesse(Player spieler, ItemStack stapel) {
        List<ItemStack> nichtPassend = new ArrayList<>(spieler.getInventory().addItem(stapel).values());
        for (ItemStack rest : nichtPassend) {
            spieler.getWorld().dropItem(spieler.getLocation(), rest);
        }
    }

    private void speichern() {
        try {
            daten.save(datei);
        } catch (IOException fehler) {
            plugin.getLogger().warning("daily.yml liess sich nicht speichern: " + fehler.getMessage());
        }
    }

    private static int zahl(Object wert, int ersatz) {
        if (wert instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(wert));
        } catch (NumberFormatException | NullPointerException ex) {
            return ersatz;
        }
    }
}
