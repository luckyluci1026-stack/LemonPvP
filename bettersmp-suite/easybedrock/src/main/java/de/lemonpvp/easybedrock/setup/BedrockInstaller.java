package de.lemonpvp.easybedrock.setup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lemonpvp.easybedrock.EasyBedrock;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

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
import java.util.Locale;

/**
 * Lädt Geyser + Floodgate von den OFFIZIELLEN GeyserMC-Downloads und optionale
 * Erweiterungen (GeyserConnect, Thunder) von GitHub. Vollständig asynchron.
 */
public final class BedrockInstaller {

    private static final String GEYSER_URL =
            "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot";
    private static final String FLOODGATE_URL =
            "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot";

    private final EasyBedrock plugin;
    private final HttpClient http;

    public BedrockInstaller(EasyBedrock plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(timeout()))
                .build();
    }

    private int timeout() {
        return plugin.getConfig().getInt("installer.timeout-seconds", 30);
    }

    private File pluginsDir() {
        return plugin.getDataFolder().getParentFile();
    }

    public void installAsync(CommandSender feedback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(feedback));
    }

    private void run(CommandSender feedback) {
        msg(feedback, "checking");
        int installed = 0;

        if (plugin.getConfig().getBoolean("installer.geyser", true)) {
            installed += core(feedback, "GeyserMC", GEYSER_URL, "Geyser-Spigot.jar", "geyser");
        }
        if (plugin.getConfig().getBoolean("installer.floodgate", true)) {
            installed += core(feedback, "Floodgate", FLOODGATE_URL, "floodgate-spigot.jar", "floodgate");
        }
        installed += extension(feedback, "GeyserConnect", "installer.geyserconnect");
        installed += extension(feedback, "Thunder", "installer.thunder");

        if (installed > 0) {
            msg(feedback, "restart-needed");
        } else {
            msg(feedback, "nothing-to-do");
        }
    }

    private int core(CommandSender feedback, String name, String url, String fileName, String marker) {
        if (jarPresent(pluginsDir(), marker)) {
            msg(feedback, "already-installed", "comp", name);
            return 0;
        }
        try {
            msg(feedback, "downloading", "comp", name);
            String file = download(url, new File(pluginsDir(), fileName));
            msg(feedback, "downloaded", "comp", name, "file", file);
            return 1;
        } catch (Exception e) {
            msg(feedback, "failed", "comp", name, "error", String.valueOf(e.getMessage()));
            plugin.getLogger().warning(name + " Download fehlgeschlagen: " + e);
            return 0;
        }
    }

    private int extension(CommandSender feedback, String name, String path) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) {
            return 0;
        }
        String repo = plugin.getConfig().getString(path + ".repo", "");
        String asset = plugin.getConfig().getString(path + ".asset", name + ".jar");
        if (repo == null || repo.isBlank()) {
            msg(feedback, "failed", "comp", name, "error", "keine Quelle (repo) gesetzt");
            return 0;
        }
        File extDir = new File(pluginsDir(), "Geyser-Spigot/extensions");
        if (jarPresent(extDir, name.toLowerCase(Locale.ROOT))) {
            msg(feedback, "already-installed", "comp", name);
            return 0;
        }
        try {
            msg(feedback, "downloading", "comp", name);
            String url = latestGithubAsset(repo, asset);
            extDir.mkdirs();
            String file = download(url, new File(extDir, asset));
            msg(feedback, "downloaded", "comp", name, "file", file);
            return 1;
        } catch (Exception e) {
            msg(feedback, "failed", "comp", name, "error", String.valueOf(e.getMessage()));
            return 0;
        }
    }

    private boolean jarPresent(File dir, String marker) {
        File[] jars = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jar")
                && n.toLowerCase(Locale.ROOT).contains(marker));
        return jars != null && jars.length > 0;
    }

    private String latestGithubAsset(String repo, String assetName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                .header("User-Agent", "EasyBedrock")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(timeout())).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonArray assets = JsonParser.parseString(response.body())
                .getAsJsonObject().getAsJsonArray("assets");
        String firstJar = null;
        for (var element : assets) {
            JsonObject asset = element.getAsJsonObject();
            String n = asset.get("name").getAsString();
            if (n.equals(assetName)) {
                return asset.get("browser_download_url").getAsString();
            }
            if (firstJar == null && n.endsWith(".jar")) {
                firstJar = asset.get("browser_download_url").getAsString();
            }
        }
        if (firstJar != null) {
            return firstJar;
        }
        throw new IllegalStateException("Kein passendes Asset gefunden");
    }

    private String download(String url, File target) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "EasyBedrock")
                .timeout(Duration.ofSeconds(timeout() * 4L)).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        Path tmp = target.toPath().resolveSibling(target.getName() + ".part");
        try (InputStream in = response.body()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target.getName();
    }

    private void msg(CommandSender to, String path, String... rep) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (to != null) {
                plugin.msgs().send(to, path, rep);
            }
        });
    }
}
