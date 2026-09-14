/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * The proxy half of BuckSMPAC's ban system.
 *
 * <p>The anticheat runs on the backends, so on its own it can only refuse a
 * player once they try to enter a server — they still reach the proxy and land
 * in limbo. This refuses them at the front door instead.</p>
 *
 * <p>It holds no anticheat logic and never reads the anticheat's own tables.
 * The backend pushes each ban over the {@code bucksmpac:bans} plugin channel
 * and this keeps its own copy — in a MySQL/MariaDB table it owns, or a text
 * file when no database is configured. Either way the list is in memory by the
 * time anybody connects, which is what makes an answer possible during
 * {@link PreLoginEvent}: at that point there is no backend to ask, and a
 * database round-trip would put its latency in front of every login.</p>
 */
@Plugin(
        id = "bucksmpac",
        name = "BuckSMPAC Proxy",
        version = "1.0.0",
        description = "Refuses BuckSMPAC-banned players at the proxy, before they reach any server",
        authors = {"BuckSMP"}
)
public class BuckSMPACProxy {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("bucksmpac", "bans");

    private static final String DEFAULT_SCREEN =
            "<gradient:#6C5CE7:#00D4FF><bold>▄▀▄▀▄  B U C K S M P  ▄▀▄▀▄</bold></gradient>"
                    + "<newline><newline><dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"
                    + "<newline><newline><white><bold>You are banned from this network.</bold></white>"
                    + "<newline><newline><gray>Reason</gray>  <dark_gray>»</dark_gray>  "
                    + "<gradient:#6C5CE7:#00D4FF>%reason%</gradient>"
                    + "<newline><gray>Date</gray>    <dark_gray>»</dark_gray>  <white>%date%</white>"
                    + "<newline><gray>Expires</gray> <dark_gray>»</dark_gray>  <gradient:#6C5CE7:#00D4FF>%remaining%</gradient>"
                    + "<newline><newline><dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"
                    + "<newline><newline><gray>Think this is a mistake?</gray>"
                    + "<newline><gradient:#6C5CE7:#00D4FF>discord.gg/bucksmp</gradient>";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ProxyBanList bans;
    private String screenTemplate = DEFAULT_SCREEN;

    @Inject
    public BuckSMPACProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        bans = new ProxyBanList(chooseStorage(), logger);
        bans.load();
        screenTemplate = loadScreen();

        server.getChannelRegistrar().register(CHANNEL);
        server.getCommandManager().register("acunban", new UnbanCommand());
        server.getCommandManager().register("acbans", new ListCommand());

        logger.info("Ready — banned players are refused before they reach a server.");
    }

    /**
     * Database when config.properties says so, the text file otherwise.
     *
     * <p>A database that cannot be reached falls back to the file rather than
     * leaving the proxy with no ban list at all — losing durability is bad,
     * letting every banned player back in is worse. The reason is logged.</p>
     */
    private BanStorage chooseStorage() {
        Path configFile = dataDirectory.resolve("config.properties");
        ProxyBanConfig config = ProxyBanConfig.loadOrCreate(configFile, logger);

        if (!config.databaseEnabled()) {
            return new FileBanStorage(dataDirectory.resolve("bans.txt"), logger);
        }

        SqlBanStorage sql = new SqlBanStorage(config.host(), config.port(), config.database(),
                config.user(), config.password(), logger);
        try {
            sql.connectAndPrepare();
            return sql;
        } catch (SQLException e) {
            logger.error("Could not reach the database named in {} — falling back to bans.txt. "
                    + "Bans still work, they just are not shared with anything else.", configFile, e);
            return new FileBanStorage(dataDirectory.resolve("bans.txt"), logger);
        }
    }

    // --- enforcement -------------------------------------------------------

    /**
     * Earliest possible refusal. Only the name is known here, which is enough:
     * the ban list is indexed by name as well as UUID.
     */
    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (bans == null) return;
        BanRecord record = bans.lookup(event.getUsername());
        if (record == null) return;

        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(screen(record)));
        logger.info("Refused {} at pre-login — banned ({})", event.getUsername(), record.reason());
    }

    /**
     * Backstop for a player whose name changed since the ban, or whose ban was
     * recorded without a name. By here the UUID is authoritative.
     */
    @Subscribe
    public void onLogin(LoginEvent event) {
        if (bans == null) return;
        BanRecord record = bans.lookup(event.getPlayer().getUniqueId());
        if (record == null) return;

        event.setResult(com.velocitypowered.api.event.ResultedEvent.ComponentResult.denied(screen(record)));
        logger.info("Refused {} at login — banned ({})", event.getPlayer().getUsername(), record.reason());
    }

    // --- backend push ------------------------------------------------------

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;

        // Never let this reach a player: it is server-to-proxy control traffic.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        // Only a backend server may drive the ban list. A client that guesses
        // the channel name must not be able to ban people.
        if (!(event.getSource() instanceof ServerConnection)) {
            logger.warn("Ignoring a {} message that did not come from a backend server.", CHANNEL.getId());
            return;
        }
        if (bans == null) return;

        BanMessage message = BanMessage.parse(new String(event.getData(), StandardCharsets.UTF_8));
        if (message == null) {
            logger.warn("Ignoring an unreadable {} message.", CHANNEL.getId());
            return;
        }

        switch (message.kind()) {
            case BAN -> handleBan(message);
            case UNBAN -> handleUnban(message);
        }
    }

    private void handleBan(BanMessage message) {
        BanRecord record = message.toRecord();
        bans.add(record);
        logger.info("Banned {} ({})", record.name(), record.reason());

        // They are still connected at this moment - the backend disconnects
        // them too, but doing it here covers the case where it cannot.
        server.getPlayer(record.uuid()).ifPresent(p -> p.disconnect(screen(record)));
    }

    private void handleUnban(BanMessage message) {
        String name = message.name().isBlank() ? null : message.name();
        if (bans.remove(message.uuid(), name)) {
            logger.info("Unbanned {}", name != null ? name : message.uuid());
        }
    }

    // --- commands ----------------------------------------------------------

    private final class UnbanCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            if (args.length != 1) {
                source.sendMessage(mini("<gray>Usage: <white>/acunban <player></white></gray>"));
                return;
            }

            UUID uuid = tryUuid(args[0]);
            boolean removed = bans.remove(uuid, uuid == null ? args[0] : null);
            source.sendMessage(mini(removed
                    ? "<gradient:#6C5CE7:#00D4FF>" + args[0] + "</gradient> <gray>is no longer banned.</gray>"
                    : "<gray>No proxy ban found for </gray><white>" + args[0] + "</white><gray>.</gray>"));
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("bucksmpac.acunban");
        }
    }

    private final class ListCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            invocation.source().sendMessage(mini(
                    "<gradient:#6C5CE7:#00D4FF>BuckSMPAC</gradient> <gray>holds </gray><white>"
                            + bans.size() + "</white><gray> ban(s) on this proxy.</gray>"));
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("bucksmpac.acunban");
        }
    }

    // --- helpers -----------------------------------------------------------

    /**
     * Reads ban-screen.txt, creating it from the default on first start.
     *
     * <p>The result is checked here rather than where it is used. An unclosed
     * tag makes MiniMessage throw, and the place that renders this is inside
     * the login path — a throw there costs every banned player their refusal,
     * or worse, lets them through. Better to find out at startup, with a line
     * in the log saying which file to fix.</p>
     */
    private String loadScreen() {
        Path path = dataDirectory.resolve("ban-screen.txt");
        try {
            if (Files.exists(path)) {
                String text = Files.readString(path, StandardCharsets.UTF_8).strip();
                if (!text.isEmpty()) return validated(text, path);
            } else {
                Files.createDirectories(dataDirectory);
                Files.writeString(path, DEFAULT_SCREEN, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.warn("Could not read or create {}; using the built-in screen.", path, e);
        }
        return DEFAULT_SCREEN;
    }

    private String validated(String template, Path path) {
        try {
            // The placeholders are substituted before rendering, so fill them
            // with something harmless to exercise the whole template.
            MiniMessage.miniMessage().deserialize(template
                    .replace("%reason%", "reason")
                    .replace("%date%", "01.01.2026 00:00")
                    .replace("%remaining%", "7 days 0 hours")
                    .replace("%player%", "Player"));
            return template;
        } catch (RuntimeException e) {
            logger.error("{} is not valid MiniMessage, so the built-in ban screen is being used instead. "
                    + "Fix the tags and restart the proxy.", path, e);
            return DEFAULT_SCREEN;
        }
    }

    private Component screen(BanRecord record) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(record.whenEpochMs()));
        return mini(screenTemplate
                .replace("%reason%", sanitise(record.reason()))
                .replace("%date%", date)
                .replace("%remaining%", remaining(record.expiresEpochMs()))
                .replace("%player%", sanitise(record.name())));
    }

    /** Same wording the backend uses, kept here so the proxy needs no shared code. */
    private static String remaining(long expiresEpochMs) {
        if (expiresEpochMs == 0L) return "never";
        long left = expiresEpochMs - System.currentTimeMillis();
        if (left <= 0) return "expired";

        long days = TimeUnit.MILLISECONDS.toDays(left);
        long hours = TimeUnit.MILLISECONDS.toHours(left) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(left) % 60;

        if (days > 0) return plural(days, "day") + " " + plural(hours, "hour");
        if (hours > 0) return plural(hours, "hour") + " " + plural(minutes, "minute");
        return plural(Math.max(1, minutes), "minute");
    }

    private static String plural(long value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private static Component mini(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    /** Stops a reason from smuggling MiniMessage tags into the screen. */
    private static String sanitise(String input) {
        return input == null ? "" : input.replace("<", "\\<");
    }

    private static UUID tryUuid(String input) {
        try {
            return UUID.fromString(input.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
