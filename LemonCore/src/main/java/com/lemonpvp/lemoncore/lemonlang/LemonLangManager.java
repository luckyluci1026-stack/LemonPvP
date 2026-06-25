package com.lemonpvp.lemoncore.lemonlang;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.ast.*;
import com.lemonpvp.lemoncore.lemonlang.parser.Parser;
import com.lemonpvp.lemoncore.lemonlang.runtime.*;
import com.lemonpvp.lemoncore.lemonlang.token.LineReader;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Central manager for LemonLang v3. Loads all {@code *.lemon} scripts from
 * {@code plugins/LemonCore/lemonlang/}, parses them, and registers all
 * triggers, items, commands, GUIs, and schedules.
 *
 * <p>Entry point from {@link LemonCore}: {@link #loadAll()} / {@link #reload()} /
 * {@link #shutdown()}.
 */
public final class LemonLangManager {

    private static final Logger LOG = Logger.getLogger("LemonLang");

    private final LemonCore plugin;
    private final Environment globalEnv = new Environment();
    private final CooldownRegistry cooldownRegistry = new CooldownRegistry();
    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final GuiRegistry guiRegistry = new GuiRegistry();
    private final SchedulerManager schedulerManager = new SchedulerManager();

    /** script name → parsed program */
    private final Map<String, Program> programs = new ConcurrentHashMap<>();
    /** Registered dynamic commands, kept for unregistration */
    private final List<DynamicCommand> dynamicCommands = new ArrayList<>();
    /** script name → last LemonLangError (parse or runtime) */
    private final Map<String, LemonLangError> lastErrors = new ConcurrentHashMap<>();
    /** script name → LemonLangScript metadata */
    private final Map<String, LemonLangScript> scripts = new ConcurrentHashMap<>();

    public LemonLangManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    // ---- Public API -----------------------------------------------------------

    /** Load (or reload) all .lemon scripts from the lemonlang/ directory. */
    public void loadAll() {
        // Ensure lemonlang directory exists
        File lemonDir = new File(plugin.getDataFolder(), "lemonlang");
        lemonDir.mkdirs();

        // Copy embedded example/doc resources from jar (only if not already present)
        extractResourceDir("lemonlang/docs", lemonDir);
        extractResourceDir("lemonlang/examples", lemonDir);

        // Find all .lemon files
        File[] lemonFiles = lemonDir.listFiles((dir, name) -> name.endsWith(".lemon"));
        if (lemonFiles == null || lemonFiles.length == 0) {
            LOG.info("[LemonLang] Keine .lemon Dateien in " + lemonDir.getPath() + " gefunden.");
            return;
        }

        for (File file : lemonFiles) {
            loadScript(file);
        }

        LOG.info("[LemonLang] " + programs.size() + " Skript(e) geladen, " +
                itemRegistry.size() + " Item(s), " +
                guiRegistry.size() + " GUI(s), " +
                schedulerManager.size() + " Timer.");
    }

    /** Reload all scripts: cancel tasks, unregister commands, clear registries, reload. */
    public void reload() {
        shutdown();
        programs.clear();
        scripts.clear();
        lastErrors.clear();
        loadAll();
    }

    /** Reload a specific script by name (without extension). */
    public void reloadScript(String name) {
        // Find the file
        File lemonDir = new File(plugin.getDataFolder(), "lemonlang");
        File file = new File(lemonDir, name + ".lemon");
        if (!file.exists()) {
            LOG.warning("[LemonLang] Skript '" + name + "' nicht gefunden.");
            return;
        }
        // Remove old script
        programs.remove(name);
        scripts.remove(name);
        lastErrors.remove(name);
        loadScript(file);
    }

    /** Stop all tasks and unregister all dynamic commands. */
    public void shutdown() {
        schedulerManager.cancelAll();
        for (DynamicCommand cmd : dynamicCommands) {
            cmd.unregister(plugin);
        }
        dynamicCommands.clear();
        itemRegistry.clear();
        guiRegistry.clear();
    }

    // ---- Getters ---------------------------------------------------------------

    public Plugin getPlugin() { return plugin; }
    public Environment getEnvironment() { return globalEnv; }
    public CooldownRegistry getCooldownRegistry() { return cooldownRegistry; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public GuiRegistry getGuiRegistry() { return guiRegistry; }
    public Map<String, Program> getPrograms() { return Collections.unmodifiableMap(programs); }
    public Map<String, LemonLangScript> getScripts() { return Collections.unmodifiableMap(scripts); }
    public Optional<LemonLangError> getLastError(String name) { return Optional.ofNullable(lastErrors.get(name)); }

    /**
     * Returns all top-level TriggerDef nodes across all loaded programs matching the given event name.
     * Item-specific triggers (rightclick, leftclick on items) are NOT included here —
     * they are handled separately by the event listener.
     */
    public List<TriggerDef> getTriggers(String event) {
        List<TriggerDef> result = new ArrayList<>();
        for (Program program : programs.values()) {
            for (Node node : program.nodes()) {
                if (node instanceof TriggerDef t && event.equalsIgnoreCase(t.event())) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    // ---- Internal loading ------------------------------------------------------

    private void loadScript(File file) {
        String name = file.getName().replace(".lemon", "");
        LemonLangScript script = new LemonLangScript(name, file);
        scripts.put(name, script);

        String source;
        try {
            source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LemonLangError err = new LemonLangError(name + ".lemon", 0,
                    "Datei konnte nicht gelesen werden: " + e.getMessage(), null);
            script.addError(err);
            lastErrors.put(name, err);
            LOG.warning(err.formatForConsole());
            return;
        }

        // Parse
        Program program;
        try {
            var lines = new LineReader(source, name + ".lemon").read();
            program = Parser.parse(lines, name + ".lemon");
        } catch (LemonLangError e) {
            script.addError(e);
            lastErrors.put(name, e);
            LOG.warning(e.formatForConsole());
            return;
        } catch (Exception e) {
            LemonLangError err = new LemonLangError(name + ".lemon", 0,
                    "Unerwarteter Parse-Fehler: " + e.getMessage(), null);
            script.addError(err);
            lastErrors.put(name, err);
            LOG.warning(err.formatForConsole());
            return;
        }

        programs.put(name, program);
        script.setProgram(program);

        // Register everything from the program
        int itemCount = 0, cmdCount = 0, guiCount = 0, triggerCount = 0;
        Interpreter interp = new Interpreter(name + ".lemon");

        for (Node node : program.nodes()) {
            switch (node) {
                case SetStmt s -> globalEnv.setVar(s.name(), s.expression());
                case DefineStmt d -> globalEnv.defineMacro(d.name(), d.body());
                case TriggerDef t -> triggerCount++;
                case EveryDef e -> {
                    schedulerManager.schedule(e, plugin, interp, globalEnv);
                }
                case ItemDef item -> {
                    itemRegistry.register(item, plugin);
                    itemCount++;
                }
                case CommandDef cmd -> {
                    DynamicCommand dynCmd = new DynamicCommand(cmd, plugin, interp, globalEnv);
                    if (dynCmd.register(plugin)) {
                        dynamicCommands.add(dynCmd);
                        cmdCount++;
                    }
                }
                case GuiDef gui -> {
                    guiRegistry.register(gui);
                    guiCount++;
                }
                case Program p -> {} // nested programs shouldn't occur
                default -> {}
            }
        }

        script.setCounts(itemCount, cmdCount, guiCount, triggerCount);
        script.markLoaded();
        LOG.info("[LemonLang] '" + name + ".lemon' geladen: " + triggerCount + " Trigger, " +
                itemCount + " Items, " + cmdCount + " Commands, " + guiCount + " GUIs.");
    }

    // ---- Resource extraction ---------------------------------------------------

    /**
     * Copies embedded resources from the given jar resource directory to the
     * given filesystem target directory. Skips files that already exist.
     */
    private void extractResourceDir(String resourceDir, File targetBase) {
        // We attempt to copy known sub-paths; since we can't enumerate jar directories
        // easily, we rely on the plugin's saveResource() for known paths.
        // Resources are listed explicitly here to avoid classpath scanning.
        String[] docFiles = {
            "docs/00_LIESMICH.txt", "docs/01_grundlagen.txt",
            "docs/02_variablen_macros.txt", "docs/03_nachrichten.txt",
            "docs/04_wirtschaft.txt", "docs/05_items.txt",
            "docs/06_commands.txt", "docs/07_guis.txt",
            "docs/08_trigger.txt", "docs/09_bedingungen.txt",
            "docs/10_effekte.txt", "docs/11_referenz.txt"
        };
        String[] exampleFiles = {
            "examples/daily_belohnung.lemon", "examples/kirsche.lemon",
            "examples/koordinaten.lemon", "examples/pvp_arena.lemon"
        };

        String[] files;
        if (resourceDir.equals("lemonlang/docs")) files = docFiles;
        else if (resourceDir.equals("lemonlang/examples")) files = exampleFiles;
        else return;

        for (String relativePath : files) {
            String jarPath = resourceDir + "/" + relativePath.substring(relativePath.indexOf('/') + 1);
            File target = new File(targetBase, relativePath);
            if (target.exists()) continue;
            target.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(jarPath)) {
                if (in != null) {
                    Files.copy(in, target.toPath());
                }
            } catch (IOException e) {
                // Not critical — skip
            }
        }
    }
}
