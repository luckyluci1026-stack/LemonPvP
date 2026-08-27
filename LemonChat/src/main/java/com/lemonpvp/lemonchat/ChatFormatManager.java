package com.lemonpvp.lemonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds chat lines from the configured MiniMessage formats. The format is
 * picked by the sender's LuckPerms primary group (falling back to the default
 * format), and {@code <prefix>}/{@code <suffix>} resolve from LuckPerms meta —
 * legacy {@code &}/{@code §} codes in meta are converted so both MiniMessage
 * and classic prefixes render.
 */
public class ChatFormatManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SEC = LegacyComponentSerializer.legacySection();

    private final LemonChat plugin;
    private String defaultFormat;
    private final Map<String, String> groupFormats = new HashMap<>();

    public ChatFormatManager(LemonChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        defaultFormat = plugin.getConfig().getString("format",
                "<prefix><name> <dark_gray>» <white><message><suffix>");
        groupFormats.clear();
        var section = plugin.getConfig().getConfigurationSection("group-formats");
        if (section != null) {
            for (String group : section.getKeys(false)) {
                String fmt = section.getString(group);
                if (fmt != null && !fmt.isBlank()) groupFormats.put(group.toLowerCase(Locale.ROOT), fmt);
            }
        }
    }

    public Component format(Player sender, Component displayName, Component message) {
        Component prefix = Component.empty();
        Component suffix = Component.empty();
        String format = defaultFormat;

        LuckPerms lp = luckPerms();
        if (lp != null) {
            try {
                CachedMetaData meta = lp.getPlayerAdapter(Player.class).getMetaData(sender);
                prefix = metaComponent(meta.getPrefix());
                suffix = metaComponent(meta.getSuffix());
                User user = lp.getPlayerAdapter(Player.class).getUser(sender);
                if (user != null) {
                    String fmt = groupFormats.get(user.getPrimaryGroup().toLowerCase(Locale.ROOT));
                    if (fmt != null) format = fmt;
                }
            } catch (Throwable ignored) {
                // LuckPerms hiccup — fall back to no prefix/suffix rather than eating chat.
            }
        }

        return MM.deserialize(format, TagResolver.resolver(
                Placeholder.component("prefix", prefix),
                Placeholder.component("suffix", suffix),
                Placeholder.component("name", displayName),
                Placeholder.component("message", message)));
    }

    /** LuckPerms meta string → Component; handles &-codes, §-codes and MiniMessage. */
    private Component metaComponent(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        if (raw.indexOf('§') >= 0) return LEGACY_SEC.deserialize(raw);
        if (raw.indexOf('&') >= 0 && !raw.contains("<")) return LEGACY_AMP.deserialize(raw);
        try {
            return MM.deserialize(raw);
        } catch (Exception e) {
            return Component.text(raw);
        }
    }

    private LuckPerms luckPerms() {
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            return null; // LuckPerms not installed
        }
    }
}
