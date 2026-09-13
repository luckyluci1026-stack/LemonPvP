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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

/**
 * The proxy half of BuckSMPAC's ban system.
 *
 * <p>The anticheat runs on the backends, so on its own it can only refuse a
 * player once they try to enter a server — they still reach the proxy and land
 * in limbo. This refuses them at the front door instead.</p>
 *
 * <p>It holds no anticheat logic and never talks to the anticheat's database.
 * The backend pushes each ban over the {@code bucksmpac:bans} plugin channel
 * and this keeps its own copy in {@code plugins/bucksmpac/bans.txt}, which is
 * what makes an answer possible during {@link PreLoginEvent} — at that point
 * there is no backend connection to ask.</p>
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
                    + "<newline><newline><white><bold>You are permanently banned.</bold></white>"
                    + "<newline><newline><gray>Reason</gray>  <dark_gray>»</dark_gray>  "
                    + "<gradient:#6C5CE7:#00D4FF>%reason%</gradient>"
                    + "<newline><gray>Date</gray>    <dark_gray>»</dark_gray>  <white>%date%</white>"
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
        bans = new ProxyBanList(dataDirectory.resolve("bans.txt"), logger);
        bans.load();
        screenTemplate = loadScreen();

        server.getChannelRegistrar().register(CHANNEL);
        server.getCommandManager().register("acunban", new UnbanCommand());
        server.getCommandManager().register("acbans", new ListCommand());

        logger.info("Ready — banned players are refused before they reach a server.");
    }

    // --- enforcement -------------------------------------------------------

    /**
     * Earliest possible refusal. Only the name is known here, which is enough:
     * the ban list is indexed by name as well as UUID.
     */
    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (bans == null) return;
        ProxyBanList.BanRecord record = bans.lookup(event.getUsername());
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
        ProxyBanList.BanRecord record = bans.lookup(event.getPlayer().getUniqueId());
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

        String payload = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|", 6);
        if (parts.length < 2) return;

        switch (parts[0]) {
            case "BAN" -> handleBan(parts);
            case "UNBAN" -> handleUnban(parts);
            default -> logger.warn("Unknown {} command: {}", CHANNEL.getId(), parts[0]);
        }
    }

    private void handleBan(String[] parts) {
        // BAN|uuid|name|epochMs|actor|reason
        if (parts.length < 6) return;
        UUID uuid = tryUuid(parts[1]);
        if (uuid == null) return;

        long when;
        try {
            when = Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            when = System.currentTimeMillis();
        }

        bans.add(new ProxyBanList.BanRecord(uuid, parts[2], when, parts[4], parts[5]));
        logger.info("Banned {} ({})", parts[2], parts[5]);

        // They are still connected at this moment - the backend disconnects
        // them too, but doing it here covers the case where it cannot.
        server.getPlayer(uuid).ifPresent(p ->
                p.disconnect(screen(new ProxyBanList.BanRecord(uuid, parts[2], when, parts[4], parts[5]))));
    }

    private void handleUnban(String[] parts) {
        // UNBAN|uuid|name
        UUID uuid = tryUuid(parts[1]);
        String name = parts.length > 2 ? parts[2] : null;
        if (bans.remove(uuid, name)) {
            logger.info("Unbanned {}", name != null && !name.isBlank() ? name : uuid);
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

    /** Reads ban-screen.txt, creating it from the default on first start. */
    private String loadScreen() {
        Path path = dataDirectory.resolve("ban-screen.txt");
        try {
            if (Files.exists(path)) {
                String text = Files.readString(path, StandardCharsets.UTF_8).strip();
                if (!text.isEmpty()) return text;
            } else {
                Files.createDirectories(dataDirectory);
                Files.writeString(path, DEFAULT_SCREEN, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.warn("Could not read or create {}; using the built-in screen.", path, e);
        }
        return DEFAULT_SCREEN;
    }

    private Component screen(ProxyBanList.BanRecord record) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(record.whenEpochMs()));
        return mini(screenTemplate
                .replace("%reason%", sanitise(record.reason()))
                .replace("%date%", date)
                .replace("%player%", sanitise(record.name())));
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

    @SuppressWarnings("unused")
    private Optional<Player> online(UUID uuid) {
        return server.getPlayer(uuid);
    }
}
