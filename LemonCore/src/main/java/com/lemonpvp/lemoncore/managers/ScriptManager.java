package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import javax.tools.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ScriptManager {

    private final LemonCore plugin;
    private final File scriptsDir;
    private final File classesDir;

    private record LoadedScript(LemonScript instance, URLClassLoader loader) {}
    private final Map<String, LoadedScript> loaded = new LinkedHashMap<>();

    public ScriptManager(LemonCore plugin) {
        this.plugin = plugin;
        this.scriptsDir = new File(plugin.getDataFolder(), "scripts");
        this.classesDir = new File(plugin.getDataFolder(), "scripts_cache");
        scriptsDir.mkdirs();
        classesDir.mkdirs();
        writeExampleIfMissing();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public Set<String> getNames() { return Collections.unmodifiableSet(loaded.keySet()); }

    /** Loads all .java files in the scripts directory. Returns a map of name→error (empty = all ok). */
    public Map<String, String> loadAll() {
        Map<String, String> errors = new LinkedHashMap<>();
        File[] files = scriptsDir.listFiles(f -> f.getName().endsWith(".java"));
        if (files == null) return errors;
        for (File f : files) {
            String name = f.getName().replace(".java", "");
            String err = load(name);
            if (err != null) errors.put(name, err);
        }
        return errors;
    }

    /**
     * Compiles and loads a script by name (must match the filename without .java).
     * Returns null on success or an error string on failure.
     */
    public String load(String name) {
        File src = new File(scriptsDir, name + ".java");
        if (!src.exists()) return "File not found: scripts/" + name + ".java";
        if (loaded.containsKey(name)) return "Script already loaded. Use /coderl reload " + name;

        String compileErr = compile(src, name);
        if (compileErr != null) return compileErr;

        return instantiate(name);
    }

    /** Unloads a script. Returns false if not loaded. */
    public boolean unload(String name) {
        LoadedScript ls = loaded.remove(name);
        if (ls == null) return false;
        try {
            if (ls.instance() instanceof Listener l) HandlerList.unregisterAll(l);
            ls.instance().onUnload();
            ls.loader().close();
        } catch (Exception e) {
            plugin.getLogger().warning("[ScriptManager] Error unloading " + name + ": " + e.getMessage());
        }
        return true;
    }

    /** Reloads a script (unload + load). Returns null on success or error string. */
    public String reload(String name) {
        unload(name);
        return load(name);
    }

    /** Unloads all scripts (called on plugin disable). */
    public void unloadAll() {
        new ArrayList<>(loaded.keySet()).forEach(this::unload);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private String compile(File src, String name) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            return "No Java compiler available — start the server with a JDK (not just a JRE).";

        File outDir = new File(classesDir, name);
        outDir.mkdirs();

        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diag, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(src);
            List<String> opts = List.of("-classpath", buildClasspath(), "-d", outDir.getAbsolutePath(), "--release", "21");
            JavaCompiler.CompilationTask task = compiler.getTask(null, fm, diag, opts, null, units);
            boolean ok = task.call();
            if (!ok) {
                StringBuilder sb = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> d : diag.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR)
                        sb.append("Line ").append(d.getLineNumber()).append(": ").append(d.getMessage(null)).append("\n");
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "Compilation error: " + e.getMessage();
        }
        return null;
    }

    private String instantiate(String name) {
        File outDir = new File(classesDir, name);
        try {
            URL[] urls = { outDir.toURI().toURL() };
            URLClassLoader loader = new URLClassLoader(urls, plugin.getClass().getClassLoader());
            Class<?> cls = loader.loadClass(name);
            Object obj = cls.getDeclaredConstructor().newInstance();
            if (!(obj instanceof LemonScript script)) {
                loader.close();
                return "Class " + name + " does not implement LemonScript.";
            }
            if (script instanceof Listener l)
                plugin.getServer().getPluginManager().registerEvents(l, plugin);
            script.onLoad(plugin);
            loaded.put(name, new LoadedScript(script, loader));
            return null;
        } catch (Exception e) {
            return "Load error: " + e.getMessage();
        }
    }

    private String buildClasspath() {
        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path", "."));
        File pluginsDir = plugin.getDataFolder().getParentFile();
        File[] jars = pluginsDir.listFiles(f -> f.getName().endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars)
                cp.append(File.pathSeparatorChar).append(jar.getAbsolutePath());
        }
        return cp.toString();
    }

    private void writeExampleIfMissing() {
        File example = new File(scriptsDir, "ExampleScript.java");
        if (example.exists()) return;
        try (var out = new PrintWriter(example, StandardCharsets.UTF_8)) {
            out.println("import com.lemonpvp.lemoncore.LemonCore;");
            out.println("import com.lemonpvp.lemoncore.managers.LemonScript;");
            out.println("import org.bukkit.event.EventHandler;");
            out.println("import org.bukkit.event.Listener;");
            out.println("import org.bukkit.event.player.PlayerJoinEvent;");
            out.println();
            out.println("public class ExampleScript implements LemonScript, Listener {");
            out.println("    private LemonCore plugin;");
            out.println();
            out.println("    @Override");
            out.println("    public void onLoad(LemonCore plugin) {");
            out.println("        this.plugin = plugin;");
            out.println("        plugin.getLogger().info(\"ExampleScript geladen!\");");
            out.println("    }");
            out.println();
            out.println("    @Override");
            out.println("    public void onUnload() {");
            out.println("        plugin.getLogger().info(\"ExampleScript entladen.\");");
            out.println("    }");
            out.println();
            out.println("    @EventHandler");
            out.println("    public void onJoin(PlayerJoinEvent e) {");
            out.println("        // e.getPlayer().sendMessage(\"Hallo vom ExampleScript!\");");
            out.println("    }");
            out.println("}");
        } catch (Exception e) {
            plugin.getLogger().warning("[ScriptManager] Konnte ExampleScript.java nicht schreiben: " + e.getMessage());
        }
    }
}
