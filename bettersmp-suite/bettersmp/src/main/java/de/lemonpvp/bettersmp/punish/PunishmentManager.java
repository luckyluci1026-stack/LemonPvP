package de.lemonpvp.bettersmp.punish;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.storage.Punishment;
import de.lemonpvp.bettersmp.util.Durations;
import de.lemonpvp.bettersmp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fuehrt Bans und Mutes aus, rendert den Ban-Screen und haelt aktive Mutes
 * fuer schnelle Chat-Pruefungen im Cache.
 */
public final class PunishmentManager {

    private final BetterSMP plugin;
    private final PunishmentConfig config;
    private final Map<UUID, Punishment> muteCache = new ConcurrentHashMap<>();

    public PunishmentManager(BetterSMP plugin, PunishmentConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public PunishmentConfig config() {
        return config;
    }

    private String actorName(CommandSender actor) {
        return actor instanceof Player p ? p.getName() : "Konsole";
    }

    // ================= BAN =================

    public void ban(CommandSender actor, OfflinePlayer target, String reasonKey) {
        PunishmentConfig.Reason reason = config.banReason(reasonKey);
        if (reason == null) {
            plugin.msgs().send(actor, "ban.unknown-reason",
                    "reason", reasonKey, "list", String.join(", ", config.banReasonKeys()));
            return;
        }
        Player online = target.getPlayer();
        if (online != null && online.hasPermission("bettersmp.ban.exempt")) {
            plugin.msgs().send(actor, "ban.exempt", "player", online.getName());
            return;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : uuid.toString();

        plugin.database().getBan(uuid).thenAccept(existing -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (existing != null) {
                plugin.msgs().send(actor, "ban.already-banned", "player", name);
                return;
            }
            long now = System.currentTimeMillis();
            long expires = reason.durationMillis() == 0 ? 0 : now + reason.durationMillis();
            String screen = renderScreen(reason, name, expires, actorName(actor));
            Punishment ban = new Punishment(uuid, name, reason.key(), reason.display(),
                    screen, expires, actorName(actor), now);

            plugin.database().setBan(ban).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (online != null) {
                    online.kick(Text.mm(screen));
                }
                plugin.msgs().send(actor, "ban.success", "player", name,
                        "reason", reason.display(),
                        "duration", durationWord(reason.durationMillis(), config.banPermanentWord()));
                if (plugin.getConfig().getBoolean("punishments.broadcast", true)) {
                    plugin.msgs().broadcast("ban.broadcast", "player", name,
                            "actor", actorName(actor), "reason", reason.display());
                }
            }));
        }));
    }

    public void unban(CommandSender actor, OfflinePlayer target) {
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        plugin.database().removeBan(target.getUniqueId())
                .thenAccept(removed -> Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.msgs().send(actor, removed ? "ban.unban-success" : "ban.not-banned",
                                "player", name)));
    }

    private String renderScreen(PunishmentConfig.Reason reason, String player, long expires, String actor) {
        List<String> lines = reason.screen() != null ? reason.screen() : config.banDefaultScreen();
        String joined = String.join("\n", lines);
        return joined
                .replace("%player%", player)
                .replace("%reason%", reason.display())
                .replace("%duration%", durationWord(reason.durationMillis(), config.banPermanentWord()))
                .replace("%expires%", Durations.expiry(expires, config.banPermanentWord()))
                .replace("%actor%", actor)
                .replace("%brand%", plugin.brand());
    }

    private String durationWord(long durationMillis, String permanentWord) {
        return durationMillis == 0 ? permanentWord : Durations.humanize(durationMillis);
    }

    // ================= MUTE =================

    public void mute(CommandSender actor, OfflinePlayer target, String reasonKey) {
        PunishmentConfig.Reason reason = config.muteReason(reasonKey);
        if (reason == null) {
            plugin.msgs().send(actor, "mute.unknown-reason",
                    "reason", reasonKey, "list", String.join(", ", config.muteReasonKeys()));
            return;
        }
        Player online = target.getPlayer();
        if (online != null && online.hasPermission("bettersmp.mute.exempt")) {
            plugin.msgs().send(actor, "mute.exempt", "player", online.getName());
            return;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : uuid.toString();

        plugin.database().getMute(uuid).thenAccept(existing -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (existing != null) {
                plugin.msgs().send(actor, "mute.already-muted", "player", name);
                return;
            }
            long now = System.currentTimeMillis();
            long expires = reason.durationMillis() == 0 ? 0 : now + reason.durationMillis();
            Punishment mute = new Punishment(uuid, name, reason.key(), reason.display(),
                    null, expires, actorName(actor), now);
            plugin.database().setMute(mute).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                muteCache.put(uuid, mute);
                plugin.msgs().send(actor, "mute.success", "player", name,
                        "reason", reason.display(),
                        "duration", durationWord(reason.durationMillis(), config.mutePermanentWord()));
                if (plugin.getConfig().getBoolean("punishments.broadcast", true)) {
                    plugin.msgs().broadcast("mute.broadcast", "player", name,
                            "actor", actorName(actor), "reason", reason.display());
                }
            }));
        }));
    }

    public void unmute(CommandSender actor, OfflinePlayer target) {
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        muteCache.remove(target.getUniqueId());
        plugin.database().removeMute(target.getUniqueId())
                .thenAccept(removed -> Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.msgs().send(actor, removed ? "mute.unmute-success" : "mute.not-muted",
                                "player", name)));
    }

    /** Laedt einen Mute beim Join in den Cache. */
    public void loadMute(UUID uuid) {
        plugin.database().getMute(uuid).thenAccept(mute -> {
            if (mute != null) {
                muteCache.put(uuid, mute);
            }
        });
    }

    public void unloadMute(UUID uuid) {
        muteCache.remove(uuid);
    }

    /** Aktiver Mute oder null (räumt abgelaufene selbst auf). */
    public Punishment activeMute(UUID uuid) {
        Punishment mute = muteCache.get(uuid);
        if (mute == null) {
            return null;
        }
        if (mute.isExpired()) {
            muteCache.remove(uuid);
            plugin.database().removeMute(uuid);
            return null;
        }
        return mute;
    }

    /** Sendet die Mute-Nachricht an einen Spieler, der zu schreiben versucht. */
    public void sendMuteNotice(Player player, Punishment mute) {
        String remaining = mute.permanent()
                ? config.mutePermanentWord()
                : Durations.humanize(mute.remainingMillis());
        for (String line : config.muteMessage()) {
            String text = line
                    .replace("%player%", player.getName())
                    .replace("%reason%", mute.display() == null ? mute.reason() : mute.display())
                    .replace("%remaining%", remaining)
                    .replace("%actor%", mute.actor() == null ? "-" : mute.actor())
                    .replace("%brand%", plugin.brand())
                    .replace("%prefix%", plugin.msgs().raw("prefix").replace("%brand%", plugin.brand()));
            player.sendMessage(Text.mm(text));
        }
    }

    /** Ban-Screen als Komponente fuer das Login-Event. */
    public Component banScreen(Punishment ban) {
        return Text.mm(ban.screen() == null ? "<red>Du bist gebannt.</red>" : ban.screen());
    }
}
