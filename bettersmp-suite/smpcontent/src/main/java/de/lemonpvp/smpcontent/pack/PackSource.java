package de.lemonpvp.smpcontent.pack;

import de.lemonpvp.smpcontent.SMPContent;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Findet das Java-Resourcepack, das dieser Server seinen Spielern gibt.
 *
 * Gesucht wird in dieser Reihenfolge:
 *
 *   1. was du dem Befehl mitgibst      /smpcontent bedrock &lt;URL|Datei&gt;
 *   2. resource-pack aus der server.properties (URL oder Datei auf der Platte)
 *   3. eine ZIP in plugins/SMPContent/pack/
 *   4. das selbst gebaute output/SMPPack.zip
 *
 * Eine URL wird heruntergeladen - das gehört darum auf einen Nebenthread,
 * niemals in den Servertakt.
 */
public final class PackSource {

    /** Gefundenes Pack: die Datei, woher sie kam, und ob sie danach weg darf. */
    public record Found(Path zip, String origin, boolean temporary) {
    }

    private PackSource() {
    }

    public static Found locate(SMPContent plugin, String given) throws IOException {
        List<String> tried = new ArrayList<>();

        if (given != null && !given.isBlank()) {
            Found found = fromValue(plugin, given.trim(), tried);
            if (found != null) {
                return found;
            }
            throw new IOException("Nichts gefunden unter: " + given.trim());
        }

        String configured = serverPack(plugin);
        if (configured != null && !configured.isBlank()) {
            Found found = fromValue(plugin, configured.trim(), tried);
            if (found != null) {
                return found;
            }
        }

        Path own = plugin.getDataFolder().toPath().resolve("pack");
        Path newest = newestZip(own);
        if (newest != null) {
            return new Found(newest, "plugins/SMPContent/pack/" + newest.getFileName(), false);
        }
        tried.add("plugins/SMPContent/pack/*.zip");

        Path built = plugin.getDataFolder().toPath().resolve("output/SMPPack.zip");
        if (Files.isRegularFile(built)) {
            return new Found(built, "output/SMPPack.zip", false);
        }
        tried.add("output/SMPPack.zip");

        throw new IOException("Kein Pack gefunden (" + String.join(", ", tried) + ")");
    }

    /** Ein Wert aus der server.properties oder vom Befehl: URL oder Pfad. */
    private static Found fromValue(SMPContent plugin, String value, List<String> tried)
            throws IOException {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return new Found(download(plugin, value), value, true);
        }

        String path = value;
        if (lower.startsWith("file:")) {
            try {
                path = Path.of(URI.create(value)).toString();
            } catch (RuntimeException ignored) {
                path = value.substring("file:".length());
            }
        }

        Path direct = Path.of(path);
        if (Files.isRegularFile(direct)) {
            return new Found(direct, path, false);
        }
        Path relative = serverRoot(plugin).resolve(path);
        if (Files.isRegularFile(relative)) {
            return new Found(relative, path, false);
        }
        tried.add(path);
        return null;
    }

    private static Path download(SMPContent plugin, String url) throws IOException {
        Path target = plugin.getDataFolder().toPath().resolve("build-download.zip");
        Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "SMPContent")
                .GET()
                .build();
        try {
            HttpResponse<Path> response = client.send(request,
                    HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new IOException("Server antwortete mit " + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Download abgebrochen");
        }
        if (!Files.isRegularFile(target) || Files.size(target) == 0) {
            throw new IOException("Leere Datei heruntergeladen");
        }
        return target;
    }

    /** resource-pack aus der server.properties - erst über die API, dann direkt. */
    private static String serverPack(SMPContent plugin) {
        try {
            String api = Bukkit.getServer().getResourcePack();
            if (api != null && !api.isBlank()) {
                return api;
            }
        } catch (Throwable ignored) {
            // Ältere oder abweichende Server: dann eben die Datei lesen
        }
        Path properties = serverRoot(plugin).resolve("server.properties");
        if (!Files.isRegularFile(properties)) {
            return null;
        }
        Properties props = new Properties();
        try (var in = Files.newBufferedReader(properties, StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (IOException ex) {
            plugin.getLogger().warning("server.properties nicht lesbar: " + ex.getMessage());
            return null;
        }
        return props.getProperty("resource-pack");
    }

    /** plugins/SMPContent -> plugins -> Serverordner. */
    static Path serverRoot(SMPContent plugin) {
        Path data = plugin.getDataFolder().toPath().toAbsolutePath();
        Path plugins = data.getParent();
        Path root = plugins == null ? null : plugins.getParent();
        return root != null ? root : Path.of(".").toAbsolutePath();
    }

    private static Path newestZip(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return null;
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith(".zip"))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException ex) {
                            return 0L;
                        }
                    }))
                    .orElse(null);
        }
    }
}
