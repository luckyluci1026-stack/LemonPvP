package com.lemonpvp.lemonpractice.util;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Arena;
import com.lemonpvp.lemonpractice.model.Gamemode;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that this server is configured coherently, and says so out loud.
 *
 * <p>Most of what goes wrong on this network is not a crash — it is a setting that quietly makes
 * something impossible. A duels server left on {@code server-type: LOBBY} registers no duel
 * worlds, so nothing can ever match and the bot fallback can never fire; a gamemode with no arena
 * bound to it leaves players queueing forever. Both look identical from the inside: "it just
 * doesn't work".
 *
 * <p>These checks turn that class of problem into a line in the console on startup, and a full
 * report on {@code /lpractice doctor}.
 */
public final class SetupDoctor {

    public enum Level { OK, WARN, ERROR }

    /** One check result. {@code detail} should say what to actually do about it. */
    public record Finding(Level level, String title, String detail) {}

    private SetupDoctor() {}

    public static List<Finding> run(LemonPractice plugin) {
        List<Finding> out = new ArrayList<>();
        String type = plugin.getServerType();

        checkServerType(plugin, type, out);
        if ("DUELS".equals(type)) {
            checkDuelsSide(plugin, out);
        } else if ("LOBBY".equals(type)) {
            checkLobbySide(plugin, out);
        }
        checkQueueSettings(plugin, out);
        checkWorldEdit(plugin, out);
        return out;
    }

    // ── Checks ──────────────────────────────────────────────────────────────

    private static void checkServerType(LemonPractice plugin, String type, List<Finding> out) {
        if ("LOBBY".equals(type) || "DUELS".equals(type)) {
            out.add(new Finding(Level.OK, "Server type", type));
            return;
        }
        out.add(new Finding(Level.ERROR, "Server type is '" + type + "'",
                "Only LOBBY and DUELS are understood. Anything else is treated as 'not DUELS', so "
                + "no duel worlds are registered and no match or bot can ever start. Fix "
                + "server-type in LemonPractice/config.yml."));
    }

    private static void checkDuelsSide(LemonPractice plugin, List<Finding> out) {
        var arenas = plugin.getArenaManager().getAllArenas();
        long usable = arenas.stream().filter(Arena::isFullyConfigured).count();

        if (usable == 0) {
            out.add(new Finding(Level.ERROR, "No usable arenas on this duels server",
                    "Matchmaking and the 15s bot fallback both need a free arena, so every queued "
                    + "player will wait forever. Check duel-worlds.yml (its worlds must exist and "
                    + "load) or configure arenas with /aowarena."));
        } else {
            out.add(new Finding(Level.OK, "Arenas", usable + " usable of " + arenas.size() + " registered"));
        }

        // A gamemode nobody can play is the exact shape of the queue bug: it accepts you and
        // then can never serve you.
        List<String> unbound = new ArrayList<>();
        for (Gamemode gm : plugin.getGamemodeManager().getEnabledGamemodes()) {
            if (!plugin.getArenaManager().hasArenaForGamemode(gm.getId())) unbound.add(gm.getId());
        }
        if (!unbound.isEmpty()) {
            out.add(new Finding(Level.WARN, "Gamemodes with no arena: " + String.join(", ", unbound),
                    "These are offered in the queue menu but no arena is bound to them, so queueing "
                    + "one leaves the player waiting with no match and no bot. Either bind an arena "
                    + "or disable the gamemode in gamemodes.yml."));
        }

        // Duel worlds that are configured but did not load are a silent source of "no arenas".
        var dw = plugin.getDuelWorldsConfig();
        if (dw != null) {
            List<String> missing = new ArrayList<>();
            for (String name : dw.getStringList("worlds")) {
                if (Bukkit.getWorld(name) == null) missing.add(name);
            }
            if (!missing.isEmpty()) {
                out.add(new Finding(Level.WARN, "Duel worlds not loaded: " + String.join(", ", missing),
                        "They are listed in duel-worlds.yml but the server has no such world, so no "
                        + "arena was registered for them."));
            }
        }
    }

    private static void checkLobbySide(LemonPractice plugin, List<Finding> out) {
        // Queueing from the lobby is a handoff to the duels server; without a target it goes nowhere.
        String duels = plugin.getServersConfig() != null
                ? plugin.getServersConfig().getString("servers.duels.name", null) : null;
        if (duels == null || duels.isBlank()) {
            out.add(new Finding(Level.WARN, "servers.duels.name is not set",
                    "Queueing from the lobby transfers the player to the duels server and falls back "
                    + "to the name 'duels'. If the Velocity server is called something else, the "
                    + "transfer silently fails. Set it in servers.yml."));
        } else {
            out.add(new Finding(Level.OK, "Duels server target", duels));
        }

        // Arenas here usually mean this server was meant to be the duels server.
        long usable = plugin.getArenaManager().getAllArenas().stream()
                .filter(Arena::isFullyConfigured).count();
        if (usable > 0) {
            out.add(new Finding(Level.WARN, usable + " arena(s) configured on a LOBBY server",
                    "Arenas belong to the duels server. If this machine is meant to host duels, set "
                    + "server-type: DUELS — otherwise these arenas are never used."));
        }

        // A portal pointing at a gamemode that does not exist does nothing when walked into.
        List<String> bad = new ArrayList<>();
        var portals = plugin.getConfig().getConfigurationSection("lobby.portals");
        if (portals != null) {
            for (String key : portals.getKeys(false)) {
                if ("walk-in".equals(key)) continue;
                String gm = portals.getString(key, "");
                if (gm == null || gm.isBlank()) continue;
                if (!plugin.getGamemodeManager().exists(gm)) bad.add(key + " -> " + gm);
            }
        }
        if (!bad.isEmpty()) {
            out.add(new Finding(Level.WARN, "Portals pointing at unknown gamemodes: " + String.join(", ", bad),
                    "Walking into these arches does nothing. Use an id from gamemodes.yml."));
        }
    }

    private static void checkQueueSettings(LemonPractice plugin, List<Finding> out) {
        if (!plugin.getConfig().getBoolean("queue.bot.enabled", true)) {
            out.add(new Finding(Level.WARN, "Bot fallback is disabled",
                    "Players with no opponent will queue indefinitely. Set queue.bot.enabled: true "
                    + "to give them a practice bot instead."));
            return;
        }
        long after = plugin.getConfig().getLong("queue.bot.after-seconds", 15);
        if (after <= 0) {
            out.add(new Finding(Level.WARN, "queue.bot.after-seconds is " + after,
                    "A value of zero or less makes every queue start a bot immediately, so real "
                    + "opponents are never found."));
        } else {
            out.add(new Finding(Level.OK, "Bot fallback", "after " + after + "s"));
        }
    }

    private static void checkWorldEdit(LemonPractice plugin, List<Finding> out) {
        if (plugin.isWorldEditAvailable()) {
            out.add(new Finding(Level.OK, "WorldEdit/FAWE", "loaded — schematic arena tools available"));
        } else {
            out.add(new Finding(Level.OK, "WorldEdit/FAWE", "not loaded — duels and the lobby build "
                    + "work without it; only schematic arena tools are unavailable"));
        }
    }

    // ── Reporting ───────────────────────────────────────────────────────────

    /** Startup report: stays silent when everything is fine, so a warning still means something. */
    public static void logProblems(LemonPractice plugin) {
        List<Finding> findings = run(plugin);
        List<Finding> problems = findings.stream()
                .filter(f -> f.level() != Level.OK).toList();
        if (problems.isEmpty()) {
            plugin.getLogger().info("[Doctor] Setup looks healthy (" + plugin.getServerType()
                    + "). Run /lpractice doctor for the full report.");
            return;
        }
        plugin.getLogger().warning("[Doctor] " + problems.size()
                + " setup problem(s) found — run /lpractice doctor for details:");
        for (Finding f : problems) {
            String line = "[Doctor] " + f.level() + ": " + f.title() + " — " + f.detail();
            if (f.level() == Level.ERROR) plugin.getLogger().severe(line);
            else plugin.getLogger().warning(line);
        }
    }
}
