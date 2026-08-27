package com.lemonpvp.lemoncore.lemonlang;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Watches the {@code lemonlang/} directory for file-system changes and
 * auto-reloads modified or newly created {@code .lemon} scripts. Deleted
 * scripts are only logged — they remain registered until a full
 * {@code /lemonlang reload} is issued.
 *
 * <p>Runs in a daemon thread so it does not block server shutdown.
 */
public final class ScriptWatcher implements Runnable {

    private static final Logger LOG = Logger.getLogger("LemonLang");

    private final Path watchDir;
    private final LemonLangManager manager;
    private final Plugin plugin;
    private volatile boolean running = true;
    private Thread thread;

    public ScriptWatcher(Path watchDir, LemonLangManager manager, Plugin plugin) {
        this.watchDir = watchDir;
        this.manager = manager;
        this.plugin = plugin;
    }

    /** Start the watcher thread. Safe to call more than once only after {@link #stop()}. */
    public void start() {
        thread = new Thread(this, "LemonLang-ScriptWatcher");
        thread.setDaemon(true);
        thread.start();
        LOG.info("[LemonLang] ScriptWatcher gestartet — überwacht: " + watchDir);
    }

    /** Signal the watcher to stop and interrupt the blocking {@code take()} call. */
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            watchDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (running && !Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take(); // blocks until an event arrives
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    String fileName = pathEvent.context().toString();

                    if (!fileName.endsWith(".lemon")) continue;

                    String scriptName = fileName.substring(0, fileName.length() - 6);
                    WatchEvent.Kind<?> kind = event.kind();

                    // Schedule the actual work on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            LOG.info("[LemonLang] Skript gelöscht: " + scriptName
                                    + " — verwende /lemonlang reload zum vollständigen Entladen.");
                        } else {
                            // CREATE or MODIFY — reload the individual script
                            LOG.info("[LemonLang] Skript geändert: " + scriptName + " — lade neu...");
                            manager.reloadScript(scriptName);
                        }
                    });
                }

                if (!key.reset()) break;
            }
        } catch (IOException e) {
            if (running) {
                LOG.warning("[LemonLang] ScriptWatcher Fehler: " + e.getMessage());
            }
        }
    }
}
