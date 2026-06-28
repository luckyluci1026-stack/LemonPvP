package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.LemonLangManager;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Per-execution context for a LemonLang body. Carries the acting player, the
 * environment (variables/macros), the owning manager and a free-form data map
 * used to stash event-specific values ({attacker}, {victim}, {message}, arg1..).
 *
 * <p>{@link #resolveString(String)} interpolates every placeholder defined by
 * the language contract.
 */
public class ScriptContext {

    /** Matches {something} including nested-ish names like {arg1} or {my_var}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)\\}");

    private final Player player;
    private final String scriptName;
    private final Environment env;
    private final LemonLangManager manager;
    private final Map<String, String> data;

    public ScriptContext(Player player, String scriptName, Environment env, LemonLangManager manager) {
        this(player, scriptName, env, manager, new HashMap<>());
    }

    private ScriptContext(Player player, String scriptName, Environment env,
                          LemonLangManager manager, Map<String, String> data) {
        this.player = player;
        this.scriptName = scriptName;
        this.env = env;
        this.manager = manager;
        this.data = data;
    }

    public Player player() {
        return player;
    }

    public String scriptName() {
        return scriptName;
    }

    public Environment env() {
        return env;
    }

    public LemonLangManager manager() {
        return manager;
    }

    public Map<String, String> data() {
        return data;
    }

    /**
     * Returns a context that shares this context's player/script/manager/data
     * but uses a different (typically child) environment.
     */
    public ScriptContext fork(Environment childEnv) {
        return new ScriptContext(player, scriptName, childEnv, manager, data);
    }

    /**
     * Interpolates all {placeholders} in {@code raw}. Unknown placeholders are
     * left untouched. Null input yields an empty string.
     */
    public String resolveString(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.indexOf('{') < 0) {
            return raw;
        }
        Matcher m = PLACEHOLDER.matcher(raw);
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        while (m.find()) {
            String key = m.group(1);
            String value = resolvePlaceholder(key);
            // If we couldn't resolve, keep the original literal "{key}".
            String replacement = (value != null) ? value : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolves a single placeholder name. Returns null if there is no known
     * binding (so the caller can preserve the literal).
     */
    private String resolvePlaceholder(String key) {
        // 1) Event/runtime data map (arg1.., attacker, victim, message, loop, ...)
        if (data.containsKey(key)) {
            return data.get(key);
        }

        // 2) Built-in player / world placeholders.
        switch (key) {
            case "player":
            case "name":
                return player != null ? player.getName() : "Player";
            case "world":
                return (player != null && player.getWorld() != null)
                    ? player.getWorld().getName() : "world";
            case "x":
                return player != null ? String.valueOf(player.getLocation().getBlockX()) : "0";
            case "y":
                return player != null ? String.valueOf(player.getLocation().getBlockY()) : "0";
            case "z":
                return player != null ? String.valueOf(player.getLocation().getBlockZ()) : "0";
            case "health":
                return player != null ? String.valueOf((int) Math.round(player.getHealth())) : "0";
            case "food":
                return player != null ? String.valueOf(player.getFoodLevel()) : "0";
            case "gamemode":
                GameMode gm = player != null ? player.getGameMode() : null;
                return gm != null ? gm.name().toLowerCase() : "survival";
            case "online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "apples":
                return economy("apples");
            case "planks":
                return economy("planks");
            case "coins":
                return economy("coins");
            case "loop":
                // {loop} is normally supplied via the data map / child env; if not, default.
                String loopVar = env != null ? env.getVar("loop") : null;
                return loopVar != null ? loopVar : "0";
            default:
                break;
        }

        // 3) User-defined variables from the environment.
        if (env != null) {
            String v = env.getVar(key);
            if (v != null) {
                // Variables may themselves contain placeholders.
                return resolveString(v);
            }
        }

        // Unknown -> let caller keep the literal.
        return null;
    }

    /** Null-safe economy lookup via LemonCore's PlayerDataManager cache. */
    private String economy(String kind) {
        if (player == null) {
            return "0";
        }
        UUID uuid = player.getUniqueId();
        LemonCore core = manager != null && manager.getPlugin() instanceof LemonCore lc ? lc : null;
        if (core == null) {
            return "0";
        }
        PlayerData data = core.getPlayerDataManager().getCached(uuid);
        if (data == null) {
            return "0";
        }
        long amount;
        switch (kind) {
            case "apples":
                amount = data.getApples();
                break;
            case "planks":
                amount = data.getPlanks();
                break;
            case "coins":
                amount = data.getCoins();
                break;
            default:
                return "0";
        }
        return String.valueOf(amount);
    }

    /** Convenience: the acting player's location (may be null if no player). */
    public Location location() {
        return player != null ? player.getLocation() : null;
    }
}