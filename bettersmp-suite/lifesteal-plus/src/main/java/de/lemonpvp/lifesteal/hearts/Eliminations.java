package de.lemonpvp.lifesteal.hearts;

import de.lemonpvp.lifesteal.LifestealPlus;
import de.lemonpvp.lifesteal.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Führt Elimination (Spectator/Bann) und Wiederbelebung aus.
 */
public final class Eliminations {

    private final LifestealPlus plugin;
    private final HeartsManager hearts;

    public Eliminations(LifestealPlus plugin, HeartsManager hearts) {
        this.plugin = plugin;
        this.hearts = hearts;
    }

    private String mode() {
        return plugin.getConfig().getString("elimination.mode", "SPECTATOR")
                .toUpperCase(Locale.ROOT);
    }

    /** Eliminiert einen Spieler (nach Herzverlust bis eliminate-at). */
    public void eliminate(Player player) {
        UUID uuid = player.getUniqueId();
        hearts.setEliminated(uuid, true);

        String broadcast = plugin.getConfig().getString("elimination.broadcast", "");
        if (broadcast != null && !broadcast.isBlank()) {
            Bukkit.getServer().sendMessage(Text.mm(broadcast.replace("%player%", player.getName())));
        } else {
            plugin.msgs().broadcast("eliminated-broadcast", "player", player.getName());
        }
        plugin.msgs().send(player, "eliminated-self");

        if (mode().equals("BAN")) {
            applyBan(player);
        } else {
            // Nach dem Tod ggf. respawnen, dann in den Zuschauermodus setzen
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (player.isDead()) {
                    player.spigot().respawn();
                }
                player.setGameMode(GameMode.SPECTATOR);
            });
        }
    }

    private void applyBan(Player player) {
        String reason = plugin.getConfig().getString("elimination.ban-message", "Eliminated");
        int minutes = plugin.getConfig().getInt("elimination.ban-minutes", 0);
        Date expiry = minutes > 0 ? Date.from(Instant.now().plus(Duration.ofMinutes(minutes))) : null;
        // Paper-Komfortmethode: Profil bannen UND Spieler kicken
        Bukkit.getScheduler().runTask(plugin,
                () -> player.ban(reason, expiry, "LifestealPlus", true));
    }

    /** Sorgt beim Join dafür, dass eliminierte Spieler im Spectator bleiben. */
    public void enforceOnJoin(Player player) {
        if (hearts.isEliminated(player.getUniqueId()) && mode().equals("SPECTATOR")) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /** Belebt einen Spieler wieder (per Command oder Revive-Item). */
    public boolean revive(UUID target, CommandSender by) {
        if (!hearts.isEliminated(target)) {
            plugin.msgs().send(by, "revive-not-eliminated");
            return false;
        }
        hearts.setEliminated(target, false);
        int reviveHearts = plugin.getConfig().getInt("revive.hearts-on-revive", 3);
        hearts.setHearts(target, reviveHearts);

        Player online = Bukkit.getPlayer(target);
        String name = online != null ? online.getName()
                : String.valueOf(Bukkit.getOfflinePlayer(target).getName());

        if (mode().equals("BAN") && name != null && !name.equals("null")) {
            // pardon(String) ist unabhängig vom BanList-Typparameter -> compilierbar
            Bukkit.getBanList(BanList.Type.PROFILE).pardon(name);
        }
        if (online != null) {
            online.setGameMode(GameMode.SURVIVAL);
            hearts.applyToPlayer(target, reviveHearts);
            online.setHealth(Math.min(online.getHealth(), reviveHearts * 2.0));
        }

        String byName = by instanceof Player p ? p.getName() : "Konsole";
        plugin.msgs().broadcast("revive-broadcast", "player", name, "by", byName);
        Component ok = plugin.msgs().format("revive-success", "player", name);
        by.sendMessage(ok);
        return true;
    }
}
