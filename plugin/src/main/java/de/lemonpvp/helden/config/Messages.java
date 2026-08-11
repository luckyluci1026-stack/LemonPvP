package de.lemonpvp.helden.config;

import de.lemonpvp.helden.util.BedrockSupport;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Zugriff auf messages.yml.
 *
 * <p>Jede Nachricht wird pro Empfaenger eingefaerbt, damit Bedrock-Spieler
 * statt kaputter Hex-Codes die passende Legacy-Farbe sehen.</p>
 */
public final class Messages {

    private final ConfigFile file;
    private final Settings settings;
    private final BedrockSupport bedrock;

    public Messages(Plugin plugin, Settings settings, BedrockSupport bedrock) {
        this.file = new ConfigFile(plugin, "messages.yml");
        this.settings = settings;
        this.bedrock = bedrock;
    }

    public void reload() {
        file.reload();
    }

    /** Unformatierter Text aus der YAML, inklusive {@code &}-Codes. */
    public String raw(String path) {
        String value = file.get().getString(path);
        return value == null ? "&cFehlender Text: &f" + path : value;
    }

    public List<String> rawList(String path) {
        List<String> list = file.get().getStringList(path);
        if (list.isEmpty()) {
            String single = file.get().getString(path);
            if (single != null) {
                list = new ArrayList<>(List.of(single));
            }
        }
        return list;
    }

    /** Fertig eingefaerbte Nachricht ohne Prefix. */
    public String format(CommandSender receiver, String path, Object... placeholders) {
        return bedrock.format(receiver, Text.replace(raw(path), placeholders));
    }

    /** Fertig eingefaerbte Nachricht fuer Logs und Vergleiche (immer Java-Farben). */
    public String plain(String path, Object... placeholders) {
        return Text.color(Text.replace(raw(path), placeholders));
    }

    public void send(CommandSender receiver, String path, Object... placeholders) {
        if (receiver == null) {
            return;
        }
        receiver.sendMessage(bedrock.format(receiver, settings.prefix() + Text.replace(raw(path), placeholders)));
    }

    /** Wie {@link #send}, aber ohne Plugin-Prefix (fuer Listen und Header). */
    public void sendRaw(CommandSender receiver, String path, Object... placeholders) {
        if (receiver == null) {
            return;
        }
        receiver.sendMessage(format(receiver, path, placeholders));
    }

    public void sendList(CommandSender receiver, String path, Object... placeholders) {
        if (receiver == null) {
            return;
        }
        for (String line : rawList(path)) {
            receiver.sendMessage(bedrock.format(receiver, Text.replace(line, placeholders)));
        }
    }

    /** Sendet eine bereits gebaute Zeile (kein YAML-Pfad) mit Prefix. */
    public void sendText(CommandSender receiver, String message) {
        if (receiver == null) {
            return;
        }
        receiver.sendMessage(bedrock.format(receiver, settings.prefix() + message));
    }

    public void broadcast(String path, Object... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, path, placeholders);
        }
        Bukkit.getConsoleSender().sendMessage(plain(path, placeholders));
    }

    public void broadcastRaw(String path, Object... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendRaw(player, path, placeholders);
        }
        Bukkit.getConsoleSender().sendMessage(plain(path, placeholders));
    }

    public void broadcastList(String path, Object... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendList(player, path, placeholders);
        }
    }
}
