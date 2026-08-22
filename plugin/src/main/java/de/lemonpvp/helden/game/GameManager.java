package de.lemonpvp.helden.game;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.heart.HealthCompat;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Ausscheiden, Zurueckholen und die Siegbedingung. */
public final class GameManager {

    private final HeldenPlugin plugin;

    public GameManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEliminated(HeldenProfile profile) {
        return profile != null && profile.eliminated();
    }

    /** Alle Profile, die noch Herzen haben. */
    public List<HeldenProfile> alive() {
        List<HeldenProfile> result = new ArrayList<>();
        for (HeldenProfile profile : plugin.profiles().all()) {
            if (!profile.eliminated()) {
                result.add(profile);
            }
        }
        return result;
    }

    public int participants() {
        return plugin.profiles().size();
    }

    /**
     * Laesst einen Spieler ausscheiden und reisst ueber das Link-Herz
     * gegebenenfalls seinen Partner mit.
     */
    public void eliminate(HeldenProfile profile, String source, Set<UUID> visited) {
        if (profile == null || profile.eliminated()) {
            return;
        }

        profile.hearts(0);
        profile.eliminated(true);
        profile.eliminatedAt(System.currentTimeMillis());

        plugin.messages().broadcastRaw("game.eliminated-broadcast", "%player%", profile.name());

        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null) {
            plugin.messages().send(online, "game.eliminated-self");
            Compat.sound(online, "entity.wither.death", 1.0f, 0.8f);
            applyEliminationAction(online);
        }

        // Erst die Kette, dann der Siegcheck - sonst wird ein Sieger verkuendet,
        // bevor das Link-Herz den naechsten Spieler erwischt hat.
        plugin.links().applyChainLoss(profile, visited);
        plugin.links().reassignPartnersOf(profile);
        plugin.hud().updateAll();
        checkWinner();
    }

    private void applyEliminationAction(Player player) {
        Settings.EliminationAction action = plugin.settings().eliminationAction();
        switch (action) {
            case SPECTATOR -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    HealthCompat.reset(player);
                }
            });
            case KICK -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(plugin.messages().plain("game.eliminated-self"));
                }
            });
            case NOTHING -> {
                // Der Spieler bleibt normal im Spiel - nur ohne Herzen.
            }
        }
    }

    /** Holt einen ausgeschiedenen Spieler zurueck (Adminbefehl, Herz-Item). */
    public void restore(HeldenProfile profile) {
        if (profile == null || !profile.eliminated()) {
            return;
        }
        profile.eliminated(false);
        profile.eliminatedAt(0L);
        if (profile.hearts() <= 0) {
            profile.hearts(1);
        }

        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null) {
            if (online.getGameMode() == GameMode.SPECTATOR) {
                online.setGameMode(GameMode.SURVIVAL);
            }
            HealthCompat.applyHearts(online, profile.hearts());
        }
        plugin.messages().broadcastRaw("game.restored",
                "%player%", profile.name(),
                "%hearts%", profile.hearts());
        plugin.hud().updateAll();
    }

    /** Stellt beim Join den gespeicherten Zustand wieder her. */
    public void enforceState(Player player) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.eliminated()) {
            if (plugin.settings().enforceOnJoin()) {
                applyEliminationAction(player);
            }
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        plugin.hearts().apply(player);
    }

    /** Verkuendet den Sieger, sobald nur noch einer uebrig ist. */
    public void checkWinner() {
        if (!plugin.settings().announceWinner() || participants() < plugin.settings().minParticipants()) {
            return;
        }
        List<HeldenProfile> alive = alive();
        if (alive.size() != 1) {
            return;
        }

        HeldenProfile winner = alive.get(0);
        plugin.messages().broadcastRaw("game.winner",
                "%player%", winner.name(),
                "%hearts%", winner.hearts(),
                "%kills%", winner.kills());
        for (Player player : Bukkit.getOnlinePlayers()) {
            Compat.sound(player, "ui.toast.challenge_complete", 1.0f, 1.0f);
        }
    }

    /** Statuszeile fuer Scoreboard und Teilnehmerliste. */
    public String statusOf(HeldenProfile profile) {
        if (profile == null) {
            return Text.color(plugin.messages().raw("game.status-out"));
        }
        if (profile.eliminated()) {
            return Text.color(plugin.messages().raw("game.status-out"));
        }
        if (plugin.hearts().isOnLinkHeart(profile)) {
            return Text.color(plugin.messages().raw("game.status-link"));
        }
        return Text.color(plugin.messages().raw("game.status-alive"));
    }
}
