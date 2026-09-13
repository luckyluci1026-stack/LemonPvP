/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
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

        String reason = context.getOrDefault("reason", "");
        if (reason == null || reason.isBlank()) reason = "Cheating";

        AcBanStore.ban(target.getUniqueId(), target.getName(), reason, sender.getName());
        AcBanEnforcer.kickBanned(target.getUniqueId(), reason);

        LogUtil.info("acban: " + target.getName() + " banned by " + sender.getName() + " (" + reason + ")");

        send(sender, "acban-success",
                "%prefix% <gradient:#6C5CE7:#00D4FF>%target%</gradient> <gray>has been banned.</gray> <dark_gray>(%reason%)</dark_gray>",
                target.getName(), reason);
    }

    static void send(Sender sender, String key, String fallback) {
        send(sender, key, fallback, null, null);
    }

    /** Renders a message key with BuckSMPAC's own %target% / %reason% placeholders. */
    static void send(Sender sender, String key, String fallback, String target, String reason) {
        String raw = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallback);
        if (target != null) raw = raw.replace("%target%", target);
        if (reason != null) raw = raw.replace("%reason%", MessageUtil.miniMessageSafe(reason));
        sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, raw)));
    }
}
