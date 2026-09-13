/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.AcBanDuration;
import ac.grim.grimac.manager.AcBanEnforcer;
import ac.grim.grimac.manager.AcBanStore;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /acban <player> [reason]} — BuckSMPAC's own ban.
 *
 * <p>Takes an optional leading duration: {@code /acban Steve 7d flying}. A
 * first word that does not parse as one ({@code 7d}, {@code 12h},
 * {@code perm}) is treated as the start of the reason instead, so an admin
 * in a hurry still gets the configured default length.</p>
 *
 * <p>Mostly issued by the anticheat itself from punishments.yml and bans.yml,
 * but usable by hand. The ban lands in BuckSMPAC's own datastore rather than in
 * another plugin's, which is the whole point: it works without a ban plugin
 * being installed on the server the anticheat happens to run on.</p>
 */
public class AcBan implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("acban")
                        .permission("bucksmpac.acban")
                        .required("target", arguments.singlePlayerSelectorParser())
                        .optional("reason", StringParser.greedyStringParser())
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (!AcBanStore.isAvailable()) {
            send(sender, "acban-storage-unavailable",
                    "%prefix% <red>The ban list is unavailable — BuckSMPAC's database is off or failed to load. See the console.");
            return;
        }

        PlayerSelector selector = context.getOrDefault("target", null);
        if (selector == null) return;

        PlatformPlayer target = selector.getSinglePlayer().getPlatformPlayer();
        if (target == null) {
            send(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!");
            return;
        }

        String rest = context.getOrDefault("reason", "");
        if (rest == null) rest = "";

        // First word may be a duration. If it is not, it belongs to the reason.
        long durationMs = defaultDurationMs();
        String[] split = rest.strip().split("\\s+", 2);
        if (split.length > 0 && !split[0].isEmpty()) {
            Long parsed = AcBanDuration.parse(split[0]);
            if (parsed != null) {
                durationMs = parsed;
                rest = split.length > 1 ? split[1] : "";
            }
        }

        String reason = rest.isBlank() ? "Cheating" : rest.strip();
        long expires = durationMs == AcBanDuration.PERMANENT
                ? AcBanDuration.PERMANENT
                : System.currentTimeMillis() + durationMs;

        AcBanStore.ban(target.getUniqueId(), target.getName(), reason, sender.getName(), expires);
        AcBanEnforcer.kickBanned(target.getUniqueId(), reason, expires);

        LogUtil.info("acban: " + target.getName() + " banned by " + sender.getName()
                + " for " + AcBanDuration.remaining(expires) + " (" + reason + ")");

        send(sender, "acban-success",
                "%prefix% <gradient:#6C5CE7:#00D4FF>%target%</gradient> <gray>has been banned for</gray> "
                        + "<white>%duration%</white><gray>.</gray> <dark_gray>(%reason%)</dark_gray>",
                target.getName(), reason, AcBanDuration.remaining(expires));
    }

    /** How long a ban lasts when the command did not say. */
    private static long defaultDurationMs() {
        String raw = GrimAPI.INSTANCE.getConfigManager().getConfig()
                .getStringElse("acban-default-duration", "10d");
        Long parsed = AcBanDuration.parse(raw);
        return parsed == null ? java.util.concurrent.TimeUnit.DAYS.toMillis(10) : parsed;
    }

    static void send(Sender sender, String key, String fallback) {
        send(sender, key, fallback, null, null, null);
    }

    static void send(Sender sender, String key, String fallback, String target, String reason) {
        send(sender, key, fallback, target, reason, null);
    }

    /** Renders a message key with BuckSMPAC's %target% / %reason% / %duration% placeholders. */
    static void send(Sender sender, String key, String fallback, String target, String reason, String duration) {
        String raw = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallback);
        if (target != null) raw = raw.replace("%target%", target);
        if (reason != null) raw = raw.replace("%reason%", MessageUtil.miniMessageSafe(reason));
        if (duration != null) raw = raw.replace("%duration%", duration);
        sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, raw)));
    }
}
