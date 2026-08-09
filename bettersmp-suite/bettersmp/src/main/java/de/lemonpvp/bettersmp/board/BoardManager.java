package de.lemonpvp.bettersmp.board;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.storage.StatSnapshot;
import de.lemonpvp.bettersmp.util.Durations;
import de.lemonpvp.bettersmp.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet Nametags (Gradient-Prefix aus LuckPerms über dem Kopf + Tab,
 * Name weiß) und ein konfigurierbares Sidebar-Scoreboard - pro Spieler,
 * flackerfrei. Funktioniert automatisch für JEDE LuckPerms-Gruppe und ist
 * auch für Bedrock/Geyser-Spieler sauber sichtbar.
 */
public final class BoardManager {

    /** Sidebar-Zeilen nutzen unsichtbare, eindeutige Einträge (Farbcodes). */
    private static final char[] TOKENS = "0123456789abcdef".toCharArray();

    private final BetterSMP plugin;

    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> nametagTeams = new HashMap<>();
    private final Map<UUID, Integer> sidebarLineCount = new HashMap<>();
    private final Map<UUID, StatSnapshot> statCache = new HashMap<>();

    private YamlConfiguration board;
    private BukkitTask nametagTask;
    private BukkitTask sidebarTask;
    private BukkitTask statTask;

    public BoardManager(BetterSMP plugin) {
        this.plugin = plugin;
        loadBoardConfig();
    }

    public void loadBoardConfig() {
        File file = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!file.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        this.board = YamlConfiguration.loadConfiguration(file);
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setup(player);
        }
        if (nametagsEnabled()) {
            long ni = Math.max(20, plugin.getConfig().getLong("nametags.update-interval", 40));
            nametagTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateNametags, 20L, ni);
        }
        if (scoreboardEnabled()) {
            long si = Math.max(10, plugin.getConfig().getLong("scoreboard.update-interval", 20));
            sidebarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateSidebars, 20L, si);
        }
        statTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshStats, 40L, 100L);
    }

    public void stop() {
        if (nametagTask != null) nametagTask.cancel();
        if (sidebarTask != null) sidebarTask.cancel();
        if (statTask != null) statTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        boards.clear();
        nametagTeams.clear();
        sidebarLineCount.clear();
    }

    private boolean nametagsEnabled() {
        return plugin.getConfig().getBoolean("nametags.enabled", true);
    }

    private boolean scoreboardEnabled() {
        return plugin.getConfig().getBoolean("scoreboard.enabled", true);
    }

    public void setup(Player player) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        boards.put(player.getUniqueId(), sb);
        nametagTeams.put(player.getUniqueId(), new HashMap<>());
        player.setScoreboard(sb);
        if (scoreboardEnabled()) {
            updateSidebar(player);
        }
        if (nametagsEnabled()) {
            updateNametagsFor(player);
        }
        // Diesen neuen Spieler auch auf allen anderen Boards als Nametag zeigen
        if (nametagsEnabled()) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) {
                    applyNametag(viewer, player);
                }
            }
        }
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        nametagTeams.remove(player.getUniqueId());
        sidebarLineCount.remove(player.getUniqueId());
        statCache.remove(player.getUniqueId());
        // Team für diesen Spieler von allen anderen Boards entfernen
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Map<UUID, String> teams = nametagTeams.get(viewer.getUniqueId());
            Scoreboard sb = boards.get(viewer.getUniqueId());
            if (teams != null && sb != null) {
                String teamName = teams.remove(player.getUniqueId());
                if (teamName != null) {
                    Team team = sb.getTeam(teamName);
                    if (team != null) {
                        team.unregister();
                    }
                }
            }
        }
    }

    // ---------------- Nametags ----------------

    private void updateNametags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateNametagsFor(viewer);
        }
    }

    private void updateNametagsFor(Player viewer) {
        Scoreboard sb = boards.get(viewer.getUniqueId());
        if (sb == null) {
            return;
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            applyNametag(viewer, target);
        }
        // abgemeldete Ziele aufräumen
        Map<UUID, String> teams = nametagTeams.get(viewer.getUniqueId());
        if (teams != null) {
            teams.entrySet().removeIf(entry -> {
                if (Bukkit.getPlayer(entry.getKey()) == null) {
                    Team team = sb.getTeam(entry.getValue());
                    if (team != null) {
                        team.unregister();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void applyNametag(Player viewer, Player target) {
        Scoreboard sb = boards.get(viewer.getUniqueId());
        Map<UUID, String> teams = nametagTeams.get(viewer.getUniqueId());
        if (sb == null || teams == null) {
            return;
        }
        int weight = plugin.luckPerms().weight(target);
        String desired = teamName(weight, target.getUniqueId());
        String current = teams.get(target.getUniqueId());

        Team team;
        if (current == null) {
            team = registerTeam(sb, desired);
            teams.put(target.getUniqueId(), desired);
        } else if (!current.equals(desired)) {
            Team old = sb.getTeam(current);
            if (old != null) {
                old.unregister();
            }
            team = registerTeam(sb, desired);
            teams.put(target.getUniqueId(), desired);
        } else {
            team = sb.getTeam(current);
            if (team == null) {
                team = registerTeam(sb, desired);
            }
        }

        String prefix = plugin.luckPerms().prefix(target);
        team.prefix(prefix == null || prefix.isEmpty()
                ? Component.empty() : Text.mm(Text.legacyToMini(prefix)));
        team.color(NamedTextColor.WHITE);
        team.setOption(Team.Option.COLLISION_RULE,
                plugin.getConfig().getBoolean("nametags.collision", true)
                        ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    private Team registerTeam(Scoreboard sb, String name) {
        Team existing = sb.getTeam(name);
        return existing != null ? existing : sb.registerNewTeam(name);
    }

    /** Team-Name kodiert das Gewicht (für Tab-Sortierung) + kurze UUID (eindeutig). */
    private String teamName(int weight, UUID uuid) {
        int inverse = Math.max(0, Math.min(99999, 99999 - weight));
        String shortId = uuid.toString().replace("-", "").substring(0, 11);
        return String.format("%05d", inverse) + shortId;
    }

    // ---------------- Scoreboard ----------------

    private void updateSidebars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateSidebar(player);
        }
    }

    private void updateSidebar(Player viewer) {
        Scoreboard sb = boards.get(viewer.getUniqueId());
        if (sb == null) {
            return;
        }
        List<String> rawLines = board.getStringList("lines");
        if (rawLines.size() > 15) {
            rawLines = rawLines.subList(0, 15);
        }

        Objective obj = sb.getObjective("bsmp_side");
        int previous = sidebarLineCount.getOrDefault(viewer.getUniqueId(), -1);
        if (obj != null && previous != rawLines.size()) {
            obj.unregister();
            obj = null;
        }
        if (obj == null) {
            obj = sb.registerNewObjective("bsmp_side", Criteria.DUMMY,
                    Text.mm(resolve(viewer, board.getString("title", "%brand%"))));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            // Zeilen-Teams anlegen
            for (int i = 0; i < rawLines.size(); i++) {
                String entry = lineEntry(i);
                Team team = registerTeam(sb, "bl" + i);
                if (!team.hasEntry(entry)) {
                    team.addEntry(entry);
                }
                obj.getScore(entry).setScore(rawLines.size() - i);
            }
            sidebarLineCount.put(viewer.getUniqueId(), rawLines.size());
        }

        obj.displayName(Text.mm(resolve(viewer, board.getString("title", "%brand%"))));
        for (int i = 0; i < rawLines.size(); i++) {
            Team team = sb.getTeam("bl" + i);
            if (team != null) {
                team.prefix(Text.mm(resolve(viewer, rawLines.get(i))));
            }
        }
    }

    private String lineEntry(int index) {
        // eindeutiger, unsichtbarer Eintrag pro Zeile
        return "§" + TOKENS[index % TOKENS.length] + "§r";
    }

    // ---------------- Platzhalter ----------------

    private void refreshStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.database().getStats(player.getUniqueId())
                    .thenAccept(snapshot -> statCache.put(player.getUniqueId(), snapshot));
        }
    }

    private String resolve(Player viewer, String line) {
        if (line == null) {
            return "";
        }
        double tps = Math.min(20.0, Bukkit.getTPS()[0]);
        StatSnapshot s = statCache.getOrDefault(viewer.getUniqueId(), StatSnapshot.empty(viewer.getUniqueId()));
        String money = plugin.economy().isEnabled()
                ? plugin.economy().format(plugin.economy().balance((OfflinePlayer) viewer))
                : "-";
        String rank = rankString(viewer);
        var loc = viewer.getLocation();

        line = line
                .replace("%brand%", plugin.brand())
                .replace("%player%", viewer.getName())
                .replace("%rank%", rank)
                .replace("%world%", viewer.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%ping%", String.valueOf(viewer.getPing()))
                .replace("%tps%", String.format(java.util.Locale.US, "%.1f", tps))
                .replace("%x%", String.valueOf(loc.getBlockX()))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ()))
                .replace("%money%", money)
                .replace("%kills%", String.valueOf(s.kills()))
                .replace("%deaths%", String.valueOf(s.deaths()))
                .replace("%kd%", String.valueOf(s.kd()))
                .replace("%mobkills%", String.valueOf(s.mobKills()))
                .replace("%playtime%", s.playtime() <= 0 ? "0m" : Durations.humanize(s.playtime() * 1000L));

        return plugin.papi().apply(viewer, line);
    }

    private String rankString(Player viewer) {
        if (plugin.luckPerms().isAvailable()) {
            String prefix = plugin.luckPerms().prefix(viewer);
            if (prefix != null && !prefix.isEmpty()) {
                return Text.legacyToMini(prefix);
            }
            String group = plugin.luckPerms().primaryGroup(viewer);
            return group.isEmpty() ? "" : Character.toUpperCase(group.charAt(0)) + group.substring(1);
        }
        return "";
    }

    // ---------------- Zugriff für Listener ----------------

    public List<Player> tracked() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : boards.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }
}
