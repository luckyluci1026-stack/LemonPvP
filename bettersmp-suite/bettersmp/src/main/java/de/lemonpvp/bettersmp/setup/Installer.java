package de.lemonpvp.bettersmp.setup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Laedt fehlende Begleit-Plugins beim Serverstart von den OFFIZIELLEN Quellen
 * (GitHub-Releases, luckperms.net) herunter und legt sie im plugins/-Ordner ab.
 *
 * Laeuft vollstaendig asynchron - der Main-Thread wird nie durch Netzwerk-IO
 * blockiert. Nach der Installation ist ein einmaliger Neustart noetig.
 */
public final class Installer {

    private final BetterSMP plugin;
    private final HttpClient http;

    public Installer(BetterSMP plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(
                        plugin.getConfig().getInt("installer.timeout-seconds", 30)))
                .build();
    }

    /** Startet die Installation asynchron; Rueckmeldungen gehen an {@code feedback}. */
    public void installAsync(CommandSender feedback) {
        // Bereits vorhandene Plugins auf dem Main-Thread erfassen (Thread-Sicherheit)
        Set<String> present = scanPresent();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(feedback, present));
    }

    private Set<String> scanPresent() {
        Set<String> names = new java.util.HashSet<>();
        for (var loaded : Bukkit.getPluginManager().getPlugins()) {
            names.add(loaded.getName().toLowerCase(Locale.ROOT));
        }
        File[] jars = pluginsDir().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                names.add(jar.getName().toLowerCase(Locale.ROOT).replace(".jar", ""));
            }
        }
        return names;
    }

    private File pluginsDir() {
        return plugin.getDataFolder().getParentFile();
    }

    private boolean isPresent(String key, Set<String> present) {
        String lower = key.toLowerCase(Locale.ROOT);
        return present.stream().anyMatch(n -> n.equals(lower) || n.startsWith(lower));
    }

    private void run(CommandSender feedback, Set<String> present) {
        msg(feedback, "installer.checking");
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("installer.plugins");
        if (root == null) {
            msg(feedback, "installer.nothing-to-do");
            return;
        }
        int installed = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null || !sec.getBoolean("enabled", true)) {
                continue;
            }
            if (isPresent(key, present)) {
                msg(feedback, "installer.already-installed", "plugin", key);
                continue;
            }
            try {
                msg(feedback, "installer.downloading", "plugin", key);
                String source = sec.getString("source", "github").toLowerCase(Locale.ROOT);
                List<String> files = switch (source) {
                    case "luckperms" -> installLuckPerms();
                    case "github" -> installGithub(sec);
                    default -> throw new IllegalStateException("Unbekannte Quelle: " + source);
                };
                for (String file : files) {
                    msg(feedback, "installer.downloaded", "plugin", key, "file", file);
                }
                installed++;
            } catch (Exception e) {
                msg(feedback, "installer.failed", "plugin", key,
                        "error", String.valueOf(e.getMessage()));
                plugin.getLogger().warning("Installation von " + key + " fehlgeschlagen: " + e);
            }
        }
        if (installed > 0) {
            msg(feedback, "installer.restart-needed");
        } else {
            msg(feedback, "installer.nothing-to-do");
        }
    }

    // ---------------- GitHub ----------------

    private List<String> installGithub(ConfigurationSection sec) throws Exception {
        String repo = sec.getString("repo");
        if (repo == null) {
            throw new IllegalStateException("Kein 'repo' angegeben");
        }
        JsonObject release = getJson("https://api.github.com/repos/" + repo + "/releases/latest")
                .getAsJsonObject();
        String tag = release.get("tag_name").getAsString();
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        JsonArray assets = release.getAsJsonArray("assets");

        List<String> wanted = sec.getStringList("assets");
        if (wanted.isEmpty()) {
            wanted = List.of("{name}-{version}.jar");
        }

        List<String> downloaded = new ArrayList<>();
        for (String pattern : wanted) {
            String assetName = pattern.replace("{version}", version);
            String url = findAsset(assets, assetName);
            if (url == null) {
                // Nur Pflicht-Asset (erstes) fuehrt zum Fehler; Zusatz-Module sind optional
                if (downloaded.isEmpty()) {
                    throw new IllegalStateException("Asset '" + assetName + "' nicht gefunden. Verfuegbar: "
                            + availableAssetNames(assets));
                }
                continue;
            }
            downloaded.add(download(url, assetName));
        }
        return downloaded;
    }

    private String findAsset(JsonArray assets, String exactName) {
        for (var element : assets) {
            JsonObject asset = element.getAsJsonObject();
            if (asset.get("name").getAsString().equals(exactName)) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    private String availableAssetNames(JsonArray assets) {
        List<String> names = new ArrayList<>();
        for (var element : assets) {
            names.add(element.getAsJsonObject().get("name").getAsString());
        }
        return names.stream().filter(n -> n.endsWith(".jar")).collect(Collectors.joining(", "));
    }

    // ---------------- LuckPerms ----------------

    private List<String> installLuckPerms() throws Exception {
        JsonObject data = getJson("https://metadata.luckperms.net/data/all").getAsJsonObject();
        JsonObject downloads = data.getAsJsonObject("downloads");
        String url = downloads.get("bukkit").getAsString();
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        return List.of(download(url, fileName));
    }

    // ---------------- HTTP ----------------

    private com.google.gson.JsonElement getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "BetterSMP-Installer")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(plugin.getConfig().getInt("installer.timeout-seconds", 30)))
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " von " + url);
        }
        return JsonParser.parseString(response.body());
    }

    private String download(String url, String fileName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "BetterSMP-Installer")
                .timeout(Duration.ofSeconds(
                        plugin.getConfig().getInt("installer.timeout-seconds", 30) * 4L))
                .GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " beim Download");
        }
        Path target = pluginsDir().toPath().resolve(fileName);
        Path tmp = pluginsDir().toPath().resolve(fileName + ".part");
        try (InputStream in = response.body()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void msg(CommandSender to, String path, String... rep) {
        // Nachrichten immer auf dem Main-Thread senden
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (to != null) {
                plugin.msgs().send(to, path, rep);
            }
        });
    }
}
