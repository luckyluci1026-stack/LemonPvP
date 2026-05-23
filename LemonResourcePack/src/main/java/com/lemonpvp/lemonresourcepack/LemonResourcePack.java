package com.lemonpvp.lemonresourcepack;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
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
    private byte[] packHash;   // SHA-1 as byte[], empty = no hash
    private boolean required;
    private String promptRaw;

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
        logger.info("LemonResourcePack enabled. Pack: {}", packUrl);
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
            packUrl  = getString(rp, "url", "");
            required = getBool(rp, "required", true);
            promptRaw = getString(rp, "prompt",
                    "<gradient:#fffb00:#00ff00>LemonPvP</gradient> <white>Please accept the Resource Pack.</white>");
            String sha1Hex = getString(rp, "sha1", "");
            packHash = parseHex(sha1Hex);
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

    public String getPackUrl()  { return packUrl; }
    public byte[] getPackHash() { return packHash; }
    public boolean isRequired() { return required; }
    public String getPromptRaw() { return promptRaw; }
}
