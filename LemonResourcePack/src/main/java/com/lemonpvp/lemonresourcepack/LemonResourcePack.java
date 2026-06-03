package com.lemonpvp.lemonresourcepack;

import com.google.inject.Inject;
import com.lemonpvp.lemonresourcepack.maintenance.MaintenanceDatabase;
import com.lemonpvp.lemonresourcepack.maintenance.MaintenanceListener;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Plugin(
        id = "lemonresourcepack",
        name = "LemonResourcePack",
        version = "1.0.0",
        description = "Forces resource pack acceptance on join",
        authors = {"LemonPvP"}
)
public class LemonResourcePack {

    static final MiniMessage MM = MiniMessage.miniMessage();

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private String packUrl;
    private byte[] packHash;
    private boolean required;
    private String promptRaw;

    // Maintenance state
    private MaintenanceDatabase maintenanceDb;
    private final AtomicBoolean maintenanceEnabled = new AtomicBoolean(false);
    private final Set<UUID> maintenanceWhitelist = new CopyOnWriteArraySet<>();
    private String maintenanceDiscordLink = "https://discord.gg/eZWP9EGW7";

    @Inject
    public LemonResourcePack(ProxyServer server, Logger logger,
                              @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        loadConfig();
        server.getEventManager().register(this, new ResourcePackListener(this, server, logger));
        server.getEventManager().register(this,
                new MaintenanceListener(maintenanceEnabled, maintenanceWhitelist, maintenanceDiscordLink));

        // Refresh maintenance state from DB every 30 seconds
        server.getScheduler()
                .buildTask(this, this::refreshMaintenance)
                .repeat(30, TimeUnit.SECONDS)
                .schedule();

        logger.info("LemonResourcePack enabled. Pack: {}", packUrl);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (maintenanceDb != null) maintenanceDb.close();
    }

    private void refreshMaintenance() {
        if (maintenanceDb == null) return;
        boolean state = maintenanceDb.loadMaintenanceState();
        maintenanceEnabled.set(state);
        Set<UUID> fresh = maintenanceDb.loadWhitelist();
        maintenanceWhitelist.clear();
        maintenanceWhitelist.addAll(fresh);
    }

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        saveDefaultConfig();
        try (InputStream in = Files.newInputStream(dataDirectory.resolve("config.yml"))) {
            Map<String, Object> root = new Yaml().load(in);

            Map<String, Object> rp = (Map<String, Object>) root.get("resourcepack");
            packUrl   = getString(rp, "url", "");
            required  = getBool(rp, "required", true);
            promptRaw = getString(rp, "prompt",
                    "<gradient:#fffb00:#00ff00>LemonPvP</gradient> <white>ᴘʟᴇᴀꜱᴇ ᴀᴄᴄᴇᴘᴛ ᴛᴏ ᴘʟᴀʏ.</white>");
            String sha1Hex = getString(rp, "sha1", "");
            packHash = parseHex(sha1Hex);

            // Database section
            Map<String, Object> dbSection = (Map<String, Object>) root.get("database");
            if (dbSection != null) {
                try {
                    maintenanceDb = new MaintenanceDatabase(dbSection, logger);
                    maintenanceEnabled.set(maintenanceDb.loadMaintenanceState());
                    maintenanceWhitelist.addAll(maintenanceDb.loadWhitelist());
                    logger.info("Maintenance DB connected (maintenance={})", maintenanceEnabled.get());
                } catch (Exception e) {
                    logger.error("Failed to connect maintenance database: {}", e.getMessage());
                }
            }

            // Maintenance section
            Map<String, Object> maint = (Map<String, Object>) root.get("maintenance");
            if (maint != null) {
                maintenanceDiscordLink = getString(maint, "discord-link", maintenanceDiscordLink);
            }

        } catch (IOException e) {
            logger.error("Failed to load config: {}", e.getMessage());
        }
    }

    private void saveDefaultConfig() {
        Path configFile = dataDirectory.resolve("config.yml");
        if (Files.exists(configFile)) return;
        try {
            Files.createDirectories(dataDirectory);
            try (InputStream in = getClass().getResourceAsStream("/config.yml");
                 OutputStream out = Files.newOutputStream(configFile)) {
                if (in != null) in.transferTo(out);
            }
        } catch (IOException e) {
            logger.error("Could not save default config: {}", e.getMessage());
        }
    }

    private static String getString(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String s ? s : def;
    }

    private static boolean getBool(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        return v instanceof Boolean b ? b : def;
    }

    private static byte[] parseHex(String hex) {
        if (hex == null || hex.isBlank()) return new byte[0];
        hex = hex.strip();
        int len = hex.length();
        if (len % 2 != 0) return new byte[0];
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return new byte[0];
            data[i / 2] = (byte) ((hi << 4) | lo);
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getPackUrl()   { return packUrl; }
    public byte[] getPackHash()  { return packHash; }
    public boolean isRequired()  { return required; }
    public String getPromptRaw() { return promptRaw; }
}
