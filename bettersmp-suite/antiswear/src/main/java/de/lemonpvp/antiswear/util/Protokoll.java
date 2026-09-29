package de.lemonpvp.antiswear.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Protokoll {

    private static final DateTimeFormatter ZEIT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final File datei;
    private final ExecutorService schreiber = Executors.newSingleThreadExecutor(aufgabe -> {
        Thread thread = new Thread(aufgabe, "AntiSwear-Protokoll");
        thread.setDaemon(true);
        return thread;
    });

    public Protokoll(JavaPlugin plugin, String dateiname) {
        this.plugin = plugin;
        this.datei = new File(plugin.getDataFolder(), dateiname);
    }

    public void schreiben(String... spalten) {
        StringBuilder zeile = new StringBuilder(LocalDateTime.now().format(ZEIT));
        for (String spalte : spalten) {
            if (spalte != null && !spalte.isEmpty()) {
                zeile.append(" | ").append(spalte.replace('\n', ' ').replace('\r', ' '));
            }
        }
        zeile.append(System.lineSeparator());
        String text = zeile.toString();
        schreiber.execute(() -> {
            try {
                Files.createDirectories(datei.getParentFile().toPath());
                Files.writeString(datei.toPath(), text, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException fehler) {
                plugin.getLogger().warning(datei.getName() + " liess sich nicht schreiben: " + fehler.getMessage());
            }
        });
    }

    public void schliessen() {
        schreiber.shutdown();
        try {
            schreiber.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException fehler) {
            Thread.currentThread().interrupt();
        }
    }
}
