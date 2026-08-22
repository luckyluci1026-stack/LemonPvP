package de.lemonpvp.helden.hud;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sidebar-Scoreboard.
 *
 * <p>Die Zeilen werden ueber Scoreboard-Teams gesetzt statt jedes Mal neu
 * registriert - sonst flackert die Anzeige, was besonders auf Bedrock
 * auffaellt.</p>
 */
public final class HudManager {

    private static final String OBJECTIVE_NAME = "helden3";
    private static final String LINE_TEAM_PREFIX = "h3line";
    private static final int MAX_PREFIX_LENGTH = 128;

    /**
     * Unsichtbare, garantiert eindeutige Eintraege - ein Scoreboard erlaubt
     * denselben Eintrag nur einmal, zwei Zeilen duerfen aber gleich aussehen.
     */
    private static final List<String> LINE_ENTRIES = buildLineEntries();

    private final HeldenPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;

    public HudManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    private static List<String> buildLineEntries() {
        List<String> entries = new ArrayList<>();
        for (ChatColor color : ChatColor.values()) {
            entries.add(color.toString() + ChatColor.RESET);
        }
        return List.copyOf(entries);
    }

    public void start() {
        stop();
        if (!plugin.settings().hudEnabled()) {
            return;
        }
        long period = plugin.settings().hudUpdateTicks();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAll, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void setup(Player player) {
        if (!plugin.settings().hudEnabled()) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                Text.color(plugin.settings().hudTitle()));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        update(player);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && player.isOnline()) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {
        if (!plugin.settings().hudEnabled()) {
            return;
        }
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) {
            setup(player);
            return;
        }
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            boards.remove(player.getUniqueId());
            setup(player);
            return;
        }
        writeLines(board, objective, render(player));
    }

    private List<String> render(Player player) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        HeldenProfile partner = plugin.links().partnerOf(profile);

        String link = partner == null
                ? Text.color(plugin.messages().raw("link.none"))
                : Text.color(Text.replace(plugin.messages().raw("link.partner"), "%partner%", partner.name()));

        List<String> lines = new ArrayList<>();
        for (String raw : plugin.settings().hudLines()) {
            lines.add(Text.color(Text.replace(raw,
                    "%player%", player.getName(),
                    "%hearts%", profile.hearts(),
                    "%link%", link,
                    "%kills%", profile.kills(),
                    "%deaths%", profile.pvpDeaths(),
                    "%natural%", profile.naturalDeaths(),
                    "%status%", plugin.game().statusOf(profile),
                    "%alive%", plugin.game().alive().size(),
                    "%participants%", plugin.game().participants(),
                    "%online%", Bukkit.getOnlinePlayers().size())));
        }
        return lines;
    }

    private void writeLines(Scoreboard board, Objective objective, List<String> lines) {
        int maxLines = Math.min(lines.size(), LINE_ENTRIES.size());
        for (int index = 0; index < maxLines; index++) {
            String entry = LINE_ENTRIES.get(index);
            String teamName = LINE_TEAM_PREFIX + index;

            Team lineTeam = board.getTeam(teamName);
            if (lineTeam == null) {
                lineTeam = board.registerNewTeam(teamName);
            }
            if (!lineTeam.hasEntry(entry)) {
                lineTeam.addEntry(entry);
            }
            lineTeam.setPrefix(Text.truncate(lines.get(index), MAX_PREFIX_LENGTH));
            objective.getScore(entry).setScore(maxLines - index);
        }

        // Zeilen aufraeumen, die es nach einem Reload nicht mehr gibt.
        for (int index = maxLines; index < LINE_ENTRIES.size(); index++) {
            String entry = LINE_ENTRIES.get(index);
            if (board.getEntries().contains(entry)) {
                board.resetScores(entry);
            }
        }
    }
}
