package de.lemonpvp.bettersmp.chat;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Chat im LPC-MiniMessage-Stil: LuckPerms-Prefix/Suffix, PlaceholderAPI,
 * klickbare Links, @Erwähnungen - und auf Wunsch als System-Nachricht
 * gesendet, damit Chat-Reports (Spieler melden) ins Leere laufen.
 */
public final class ChatModule implements Listener {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private final BetterSMP plugin;

    public ChatModule(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());

        List<Player> mentioned = new ArrayList<>();
        Component message = buildMessage(player, raw, mentioned);

        String format = formatFor(player);
        format = format.replace("%player%", player.getName())
                .replace("%displayname%", Text.plain(player.displayName()))
                .replace("%prefix%", Text.legacyToMini(plugin.luckPerms().prefix(player)))
                .replace("%suffix%", Text.legacyToMini(plugin.luckPerms().suffix(player)));
        format = plugin.papi().apply(player, format);

        Component out = Text.mm(format, Placeholder.component("message", message));

        if (plugin.getConfig().getBoolean("chat.no-chat-reports", true)) {
            // Als System-Nachricht senden -> nicht signiert, nicht meldbar
            event.setCancelled(true);
            Bukkit.getServer().sendMessage(out);
        } else {
            event.renderer((source, displayName, msg, viewer) -> out);
        }

        notifyMentions(player, mentioned);
    }

    private String formatFor(Player player) {
        ConfigurationSection formats = plugin.getConfig().getConfigurationSection("chat.formats");
        String fallback = "%prefix%<white>%player%</white> <dark_gray>»</dark_gray> <message>";
        if (formats == null) {
            return fallback;
        }
        String group = plugin.luckPerms().primaryGroup(player);
        String format = formats.getString(group);
        if (format == null) {
            format = formats.getString("default", fallback);
        }
        return format;
    }

    private Component buildMessage(Player player, String raw, List<Player> mentioned) {
        Component message = player.hasPermission("bettersmp.chat.format")
                ? Text.mm(raw)
                : Component.text(raw);

        if (plugin.getConfig().getBoolean("chat.clickable-links", true)) {
            message = message.replaceText(TextReplacementConfig.builder()
                    .match(URL)
                    .replacement((result, builder) -> Component.text(result.group())
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(result.group())))
                    .build());
        }

        if (plugin.getConfig().getBoolean("chat.mentions.enabled", true)) {
            String color = plugin.getConfig().getString("chat.mentions.color", "<#00D4FF>");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                Pattern mention = Pattern.compile("(?i)@" + Pattern.quote(online.getName()));
                if (!mention.matcher(raw).find()) {
                    continue;
                }
                mentioned.add(online);
                // Standalone-Komponente: Farbe ohne Schließtag genügt und ist robust
                message = message.replaceText(TextReplacementConfig.builder()
                        .match(mention)
                        .replacement((result, builder) -> Text.mm(color + result.group()))
                        .build());
            }
        }
        return message;
    }

    private void notifyMentions(Player sender, List<Player> mentioned) {
        if (mentioned.isEmpty()) {
            return;
        }
        String soundKey = plugin.getConfig()
                .getString("chat.mentions.sound", "minecraft:block.note_block.pling");
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player target : mentioned) {
                target.sendActionBar(plugin.msgs()
                        .format("mentions.notify-actionbar", "player", sender.getName()));
                try {
                    target.playSound(Sound.sound(Key.key(soundKey), Sound.Source.PLAYER, 0.8f, 1.4f));
                } catch (Exception ignored) {
                    // Ungültiger Sound-Key in der Config - Erwähnung bleibt stumm
                }
            }
        });
    }
}
