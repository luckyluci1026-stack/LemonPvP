package de.lemonpvp.helden.heart;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Das Herzsystem des Projekts.
 *
 * <p>Herzen sind gleichzeitig Leben und Maximalgesundheit: wer drei Herzen hat,
 * laeuft mit sechs Lebenspunkten herum. Verloren gehen sie ausschliesslich an
 * andere Spieler - Sturz, Lava, Mobs und Hunger kosten nichts.</p>
 */
public final class HeartManager {

    private final HeldenPlugin plugin;

    public HeartManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public int hearts(HeldenProfile profile) {
        return profile == null ? 0 : profile.hearts();
    }

    /** {@code true}, wenn nur noch das geteilte Link-Herz uebrig ist. */
    public boolean isOnLinkHeart(HeldenProfile profile) {
        return plugin.settings().linkHeartEnabled()
                && profile != null
                && !profile.eliminated()
                && profile.hearts() == 1;
    }

    /** Schreibt die Herzzahl des Profils auf die Maximalgesundheit. */
    public void apply(Player player) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.eliminated()) {
            HealthCompat.reset(player);
            return;
        }
        HealthCompat.applyHearts(player, profile.hearts());
    }

    /**
     * Zieht ein Herz ab - der einzige Weg, auf dem im Projekt Herzen verloren
     * gehen. Bei null Herzen scheidet der Spieler aus, was ueber das Link-Herz
     * weitere Spieler mitreissen kann.
     */
    public void loseHeart(Player victim, Player killer) {
        loseHeart(plugin.profiles().getOrCreate(victim), killer == null ? null : killer.getName(), new HashSet<>());
    }

    /**
     * @param visited schuetzt vor Endlosschleifen, wenn zwei Link-Herzen
     *                gegenseitig aufeinander zeigen
     */
    public void loseHeart(HeldenProfile profile, String source, Set<UUID> visited) {
        if (profile == null || profile.eliminated() || !visited.add(profile.uuid())) {
            return;
        }

        profile.hearts(profile.hearts() - 1);
        Player online = Bukkit.getPlayer(profile.uuid());

        if (profile.hearts() <= 0) {
            plugin.game().eliminate(profile, source, visited);
            return;
        }

        if (online != null) {
            HealthCompat.applyHearts(online, profile.hearts());
            plugin.messages().send(online, "hearts.lost", "%hearts%", profile.hearts());
            Compat.sound(online, "entity.wither.hurt", 0.7f, 0.6f);
        }
        if (source != null) {
            plugin.messages().broadcastRaw("hearts.lost-broadcast",
                    "%victim%", profile.name(),
                    "%killer%", source,
                    "%hearts%", profile.hearts());
        }

        plugin.links().assignIfNeeded(profile);
        plugin.hud().updateAll();
    }

    /** Gibt Herzen bis zur konfigurierten Obergrenze. */
    public boolean addHearts(Player player, int amount) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.hearts() >= plugin.settings().maxHearts()) {
            plugin.messages().send(player, "hearts.max-reached", "%max%", plugin.settings().maxHearts());
            return false;
        }

        profile.hearts(Math.min(plugin.settings().maxHearts(), profile.hearts() + amount));
        if (profile.eliminated() && profile.hearts() > 0) {
            plugin.game().restore(profile);
        }
        HealthCompat.applyHearts(player, profile.hearts());

        // Wer wieder ueber dem letzten Herz steht, braucht kein Link-Herz mehr.
        if (!isOnLinkHeart(profile)) {
            plugin.links().clear(profile);
        }
        plugin.messages().send(player, "hearts.gained", "%hearts%", profile.hearts());
        Compat.sound(player, "entity.player.levelup", 1.0f, 1.4f);
        plugin.hud().updateAll();
        return true;
    }

    /** Setzt die Herzen hart auf einen Wert (Adminbefehl). */
    public void setHearts(HeldenProfile profile, int hearts) {
        if (profile == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(plugin.settings().maxHearts(), hearts));
        profile.hearts(clamped);

        if (clamped <= 0) {
            plugin.game().eliminate(profile, null, new HashSet<>());
            return;
        }
        if (profile.eliminated()) {
            plugin.game().restore(profile);
        }

        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null) {
            HealthCompat.applyHearts(online, clamped);
        }
        if (!isOnLinkHeart(profile)) {
            plugin.links().clear(profile);
        } else {
            plugin.links().assignIfNeeded(profile);
        }
        plugin.hud().updateAll();
    }

    /** Setzt alle Profile auf die Startherzen zurueck (neue Season). */
    public void resetAll() {
        int start = plugin.settings().totalStartHearts();
        for (HeldenProfile profile : plugin.profiles().all()) {
            profile.hearts(start);
            profile.eliminated(false);
            profile.eliminatedAt(0L);
            profile.linkPartner(null);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.game().enforceState(player);
            apply(player);
        }
        plugin.hud().updateAll();
    }
}
