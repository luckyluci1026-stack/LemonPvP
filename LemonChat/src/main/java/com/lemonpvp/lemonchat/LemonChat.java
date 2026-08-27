package com.lemonpvp.lemonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LemonChat — LuckPerms-aware MiniMessage chat formatting plus NoChatReports,
 * as one native plugin (replaces the third-party LPC + NoChatReports jars).
 *
 * <p>Two modes: alongside LemonCore, its moderation pipeline (cooldown, filters,
 * mutes, nicks) stays in charge and calls {@link #format} to render each line;
 * standalone, our own listener cancels the signed event and rebroadcasts the
 * formatted line unsigned — which is exactly what strips the report signatures.</p>
 */
public final class LemonChat extends JavaPlugin {

    private ChatFormatManager formatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        formatManager = new ChatFormatManager(this);

        boolean coreInstalled = getServer().getPluginManager().getPlugin("LemonCore") != null;
        if (!coreInstalled) {
            getServer().getPluginManager().registerEvents(new StandaloneChatListener(this), this);
            getLogger().info("LemonCore not found — running standalone chat pipeline.");
        } else {
            getLogger().info("LemonCore found — acting as its chat format engine.");
        }

        var cmd = getCommand("lemonchat");
        if (cmd != null) cmd.setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("lemonchat.admin")) return true;
            reloadConfig();
            formatManager.reload();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>LemonChat reloaded."));
            return true;
        });
        getLogger().info("LemonChat enabled.");
    }

    /**
     * Renders a chat line: group format + LuckPerms prefix/suffix around the
     * given display-name and message components. Called by LemonCore's chat
     * pipeline (or our standalone listener).
     */
    public Component format(Player sender, Component displayName, Component message) {
        return formatManager.format(sender, displayName, message);
    }

    public ChatFormatManager getFormatManager() { return formatManager; }
}
