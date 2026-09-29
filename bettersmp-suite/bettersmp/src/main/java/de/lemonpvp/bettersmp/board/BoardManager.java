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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Nametags (Gradient-Prefix aus LuckPerms über dem Kopf + Tab,
 * Name weiß) und ein konfigurierbares Sidebar-Scoreboard - pro Spieler,
 * flackerfrei. Funktioniert automatisch für JEDE LuckPerms-Gruppe und ist
 * auch für Bedrock/Geyser-Spieler sauber sichtbar.
 */
public final class BoardManager {

    /** Sidebar-Zeilen nutzen unsichtbare, eindeutige Einträge (Farbcodes). */
    private static final char[] TOKENS = "0123456789abcdef".toCharArray();
    private static final List<String> STAT_PLATZHALTER =
            List.of("%kills%", "%deaths%", "%kd%", "%mobkills%", "%playtime%");
    private static final List<String> PLATZHALTER = List.of("%brand%", "%player%", "%rank%", "%world%",
            "%online%", "%max%", "%ping%", "%tps%", "%x%", "%y%", "%z%", "%money%", "%kills%", "%deaths%",
            "%kd%", "%mobkills%", "%playtime%", "%streak%");
    private static final int CACHE_GRENZE = 512;

    private record Nametag(String team, String prefix) {
    }

    private final BetterSMP plugin;

    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, String>> nametagTeams = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> nametagPrefixe = new HashMap<>();
    private final Map<UUID, Integer> sidebarLineCount = new HashMap<>();
    private final Map<UUID, String[]> sidebarTexte = new HashMap<>();
    private final Map<UUID, String> sidebarTitel = new HashMap<>();
    private final Map<UUID, StatSnapshot> statCache = new ConcurrentHashMap<>();
    private final Map<String, Component> prefixKomponenten = new HashMap<>();
    private final Map<String, Component> zeilenKomponenten = new HashMap<>();

    private YamlConfiguration board;
    private List<String> zeilen = List.of();
    private String titel = "%brand%";
    private List<String> benutzt = List.of();
    private boolean statsBenutzt;
    private Boolean kollision;
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
        List<String> geladen = board.getStringList("lines");
        this.zeilen = List.copyOf(geladen.size() > 15 ? geladen.subList(0, 15) : geladen);
        this.titel = board.getString("title", "%brand%");
        List<String> texte = new ArrayList<>(zeilen);
        texte.add(titel);
        List<String> gefunden = new ArrayList<>();
        for (String platzhalter : PLATZHALTER) {
            for (String text : texte) {
                if (text != null && text.contains(platzhalter)) {
                    gefunden.add(platzhalter);
                    break;
                }
            }
        }
        this.benutzt = List.copyOf(gefunden);
        this.statsBenutzt = gefunden.stream().anyMatch(STAT_PLATZHALTER::contains);
        sidebarTexte.clear();
        sidebarTitel.clear();
        zeilenKomponenten.clear();
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
        nametagPrefixe.clear();
        sidebarLineCount.clear();
        sidebarTexte.clear();
        sidebarTitel.clear();
        prefixKomponenten.clear();
        zeilenKomponenten.clear();
        kollision = null;
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
        nametagPrefixe.put(player.getUniqueId(), new HashMap<>());
        sidebarLineCount.remove(player.getUniqueId());
        sidebarTexte.remove(player.getUniqueId());
        sidebarTitel.remove(player.getUniqueId());
        player.setScoreboard(sb);
        if (scoreboardEnabled()) {
            updateSidebar(player);
        }
        if (nametagsEnabled()) {
            boolean kollidieren = kollisionAktuell();
            Nametag eigenes = nametag(player);
            for (Player target : Bukkit.getOnlinePlayers()) {
                applyNametag(player, target, target.equals(player) ? eigenes : nametag(target), kollidieren);
            }
            // Diesen neuen Spieler auch auf allen anderen Boards als Nametag zeigen
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) {
                    applyNametag(viewer, player, eigenes, kollidieren);
                }
            }
        }
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        nametagTeams.remove(player.getUniqueId());
        nametagPrefixe.remove(player.getUniqueId());
        sidebarLineCount.remove(player.getUniqueId());
        sidebarTexte.remove(player.getUniqueId());
        sidebarTitel.remove(player.getUniqueId());
        statCache.remove(player.getUniqueId());
        // Team für diesen Spieler von allen anderen Boards entfernen
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Map<UUID, String> teams = nametagTeams.get(viewer.getUniqueId());
            Scoreboard sb = boards.get(viewer.getUniqueId());
            Map<UUID, String> prefixe = nametagPrefixe.get(viewer.getUniqueId());
            if (prefixe != null) {
                prefixe.remove(player.getUniqueId());
            }
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

    private boolean kollisionAktuell() {
        boolean wert = plugin.getConfig().getBoolean("nametags.collision", true);
        if (kollision != null && kollision != wert) {
            for (Map<UUID, String> prefixe : nametagPrefixe.values()) {
                prefixe.clear();
            }
        }
        kollision = wert;
        return wert;
    }

    private Nametag nametag(Player target) {
        String prefix = plugin.luckPerms().prefix(target);
        return new Nametag(teamName(plugin.luckPerms().weight(target), target.getUniqueId()),
                prefix == null ? "" : prefix);
    }

    private void updateNametags() {
        boolean kollidieren = kollisionAktuell();
        Map<UUID, Nametag> daten = new HashMap<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            daten.put(target.getUniqueId(), nametag(target));
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateNametagsFor(viewer, daten, kollidieren);
        }
    }

    private void updateNametagsFor(Player viewer, Map<UUID, Nametag> daten, boolean kollidieren) {
        Scoreboard sb = boards.get(viewer.getUniqueId());
        if (sb == null) {
            return;
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            Nametag nametag = daten.get(target.getUniqueId());
            if (nametag != null) {
                applyNametag(viewer, target, nametag, kollidieren);
            }
        }
        // abgemeldete Ziele aufräumen
        Map<UUID, String> teams = nametagTeams.get(viewer.getUniqueId());
        Map<UUID, String> prefixe = nametagPrefixe.get(viewer.getUniqueId());
        if (teams != null && teams.size() > daten.size()) {
            teams.entrySet().removeIf(entry -> {
                if (Bukkit.getPlayer(entry.getKey()) == null) {
                    Team team = sb.getTeam(entry.getValue());
                    if (team != null) {
                        team.unregister();
                    }
                    if (prefixe != null) {
                        prefixe.remove(entry.getKey());
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void applyNametag(Player viewer, Player target, Nametag nametag, boolean kollidieren) {
        Scoreboard sb = boards.get(viewer.getUniqueId());
        Map<UUID, String> teams = nametagTeams.get(viewer.getUniqueId());
        Map<UUID, String> prefixe = nametagPrefixe.get(viewer.getUniqueId());
        if (sb == null || teams == null || prefixe == null) {
            return;
        }
        String desired = nametag.team();
        String current = teams.get(target.getUniqueId());
        Team team = current == null ? null : sb.getTeam(current);

        if (team != null && current.equals(desired) && nametag.prefix().equals(prefixe.get(target.getUniqueId()))
                && team.hasEntry(target.getName())) {
            return;
        }
        if (current != null && !current.equals(desired)) {
            if (team != null) {
                team.unregister();
            }
            team = null;
        }
        boolean neu = team == null;
        if (neu) {
            team = registerTeam(sb, desired);
            teams.put(target.getUniqueId(), desired);
        }
        if (neu || !nametag.prefix().equals(prefixe.get(target.getUniqueId()))) {
            team.prefix(prefixKomponente(nametag.prefix()));
            prefixe.put(target.getUniqueId(), nametag.prefix());
            if (neu) {
                team.color(NamedTextColor.WHITE);
            }
            Team.OptionStatus status = kollidieren ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER;
            if (team.getOption(Team.Option.COLLISION_RULE) != status) {
                team.setOption(Team.Option.COLLISION_RULE, status);
            }
        }
        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    private Component prefixKomponente(String prefix) {
        if (prefix.isEmpty()) {
            return Component.empty();
        }
        Component fertig = prefixKomponenten.get(prefix);
        if (fertig == null) {
            if (prefixKomponenten.size() >= CACHE_GRENZE) {
                prefixKomponenten.clear();
            }
            fertig = Text.mm(prefix);
            prefixKomponenten.put(prefix, fertig);
        }
        return fertig;
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
        zeilenKomponenten.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateSidebar(player);
        }
    }

    private void updateSidebar(Player viewer) {
        Scoreboard sb = boards.get(viewer.getUniqueId());
        if (sb == null) {
            return;
        }
        List<String> rawLines = zeilen;

        Objective obj = sb.getObjective("bsmp_side");
        int previous = sidebarLineCount.getOrDefault(viewer.getUniqueId(), -1);
        if (obj != null && previous != rawLines.size()) {
            obj.unregister();
            obj = null;
        }
        String[][] ersetzungen = ersetzungen(viewer);
        String titelText = resolve(viewer, titel, ersetzungen);
        if (obj == null) {
            obj = sb.registerNewObjective("bsmp_side", Criteria.DUMMY, zeile(titelText));
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
            sidebarTexte.remove(viewer.getUniqueId());
            sidebarTitel.put(viewer.getUniqueId(), titelText);
        }

        if (!titelText.equals(sidebarTitel.get(viewer.getUniqueId()))) {
            obj.displayName(zeile(titelText));
            sidebarTitel.put(viewer.getUniqueId(), titelText);
        }
        String[] vorher = sidebarTexte.get(viewer.getUniqueId());
        if (vorher == null || vorher.length != rawLines.size()) {
            vorher = new String[rawLines.size()];
            sidebarTexte.put(viewer.getUniqueId(), vorher);
        }
        for (int i = 0; i < rawLines.size(); i++) {
            String text = resolve(viewer, rawLines.get(i), ersetzungen);
            if (text.equals(vorher[i])) {
                continue;
            }
            Team team = sb.getTeam("bl" + i);
            if (team != null) {
                team.prefix(zeile(text));
                vorher[i] = text;
            }
        }
    }

    private Component zeile(String text) {
        Component fertig = zeilenKomponenten.get(text);
        if (fertig == null) {
            if (zeilenKomponenten.size() >= CACHE_GRENZE) {
                zeilenKomponenten.clear();
            }
            fertig = Text.mm(text);
            zeilenKomponenten.put(text, fertig);
        }
        return fertig;
    }

    private String lineEntry(int index) {
        // eindeutiger, unsichtbarer Eintrag pro Zeile
        return "§" + TOKENS[index % TOKENS.length] + "§r";
    }

    // ---------------- Platzhalter ----------------

    private void refreshStats() {
        if (!statsBenutzt) {
            statCache.clear();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            plugin.database().getStats(id).thenAccept(snapshot -> {
                if (snapshot != null && boards.containsKey(id)) {
                    statCache.put(id, snapshot);
                }
            });
        }
    }

    private String[][] ersetzungen(Player viewer) {
        String[][] liste = new String[benutzt.size()][];
        StatSnapshot s = statsBenutzt
                ? statCache.getOrDefault(viewer.getUniqueId(), StatSnapshot.empty(viewer.getUniqueId()))
                : null;
        for (int i = 0; i < liste.length; i++) {
            String platzhalter = benutzt.get(i);
            liste[i] = new String[]{platzhalter, wert(viewer, platzhalter, s)};
        }
        return liste;
    }

    private String wert(Player viewer, String platzhalter, StatSnapshot s) {
        return switch (platzhalter) {
            case "%brand%" -> plugin.brand();
            case "%player%" -> viewer.getName();
            case "%rank%" -> rankString(viewer);
            case "%world%" -> viewer.getWorld().getName();
            case "%online%" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "%max%" -> String.valueOf(Bukkit.getMaxPlayers());
            case "%ping%" -> String.valueOf(viewer.getPing());
            case "%tps%" -> String.format(java.util.Locale.US, "%.1f", Math.min(20.0, Bukkit.getTPS()[0]));
            case "%x%" -> String.valueOf(viewer.getLocation().getBlockX());
            case "%y%" -> String.valueOf(viewer.getLocation().getBlockY());
            case "%z%" -> String.valueOf(viewer.getLocation().getBlockZ());
            case "%money%" -> plugin.economy().isEnabled()
                    ? plugin.economy().format(plugin.economy().balance((OfflinePlayer) viewer))
                    : "-";
            case "%kills%" -> String.valueOf(s.kills());
            case "%deaths%" -> String.valueOf(s.deaths());
            case "%kd%" -> String.valueOf(s.kd());
            case "%mobkills%" -> String.valueOf(s.mobKills());
            case "%playtime%" -> s.playtime() <= 0 ? "0m" : Durations.humanize(s.playtime() * 1000L);
            case "%streak%" -> String.valueOf(plugin.killstreaks().serie(viewer.getUniqueId()));
            default -> platzhalter;
        };
    }

    private String resolve(Player viewer, String line, String[][] ersetzungen) {
        if (line == null) {
            return "";
        }
        for (String[] ersetzung : ersetzungen) {
            line = line.replace(ersetzung[0], ersetzung[1]);
        }
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
