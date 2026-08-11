package de.lemonpvp.helden.team;

import de.lemonpvp.helden.config.ConfigFile;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.player.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Verwaltet die Fraktionen und deren Zuordnung. */
public final class TeamManager {

    private final Plugin plugin;
    private final ProfileManager profiles;
    private final ConfigFile file;
    private final Map<String, HeldenTeam> teams = new LinkedHashMap<>();

    private boolean autoAssign = true;
    private int maxSize = -1;
    private boolean playerSwitch = false;

    public TeamManager(Plugin plugin, ProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.file = new ConfigFile(plugin, "teams.yml");
    }

    public void reload() {
        teams.clear();
        file.reload();

        autoAssign = file.get().getBoolean("settings.auto-assign", true);
        maxSize = file.get().getInt("settings.max-size", -1);
        playerSwitch = file.get().getBoolean("settings.player-switch", false);

        ConfigurationSection root = file.get().getConfigurationSection("teams");
        if (root == null) {
            plugin.getLogger().warning("teams.yml enthaelt keinen 'teams'-Abschnitt.");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            ChatColor color = parseColor(section.getString("color", "WHITE"), id);
            teams.put(id.toLowerCase(Locale.ROOT), new HeldenTeam(
                    id.toLowerCase(Locale.ROOT),
                    section.getString("display", id),
                    section.getString("tag", ""),
                    color,
                    readSpawn(section.getConfigurationSection("spawn"))));
        }

        plugin.getLogger().info("Teams geladen: " + teams.size());
    }

    private ChatColor parseColor(String raw, String teamId) {
        try {
            return ChatColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Team '" + teamId + "' hat eine unbekannte Farbe: " + raw);
            return ChatColor.WHITE;
        }
    }

    private Location readSpawn(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world", "");
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    public HeldenTeam get(String id) {
        return id == null ? null : teams.get(id.toLowerCase(Locale.ROOT));
    }

    public HeldenTeam of(HeldenProfile profile) {
        return profile == null ? null : get(profile.teamId());
    }

    public HeldenTeam of(Player player) {
        return of(profiles.get(player));
    }

    public Collection<HeldenTeam> all() {
        return teams.values();
    }

    public Set<String> ids() {
        return teams.keySet();
    }

    public int size() {
        return teams.size();
    }

    public boolean playerSwitchAllowed() {
        return playerSwitch;
    }

    public boolean autoAssignEnabled() {
        return autoAssign;
    }

    public boolean isFull(HeldenTeam team) {
        return maxSize > 0 && members(team.id()).size() >= maxSize;
    }

    /** Alle Profile eines Teams, auch die offline gespeicherten. */
    public List<HeldenProfile> members(String teamId) {
        List<HeldenProfile> result = new ArrayList<>();
        if (teamId == null) {
            return result;
        }
        for (HeldenProfile profile : profiles.all()) {
            if (teamId.equalsIgnoreCase(profile.teamId())) {
                result.add(profile);
            }
        }
        return result;
    }

    public List<Player> onlineMembers(String teamId) {
        List<Player> result = new ArrayList<>();
        if (teamId == null) {
            return result;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            HeldenProfile profile = profiles.get(player);
            if (profile != null && teamId.equalsIgnoreCase(profile.teamId())) {
                result.add(player);
            }
        }
        return result;
    }

    public void assign(HeldenProfile profile, HeldenTeam team) {
        if (profile == null) {
            return;
        }
        profile.teamId(team == null ? null : team.id());
    }

    /** Steckt das Profil in das aktuell kleinste Team. */
    public HeldenTeam assignSmallest(HeldenProfile profile) {
        HeldenTeam smallest = null;
        int smallestSize = Integer.MAX_VALUE;
        for (HeldenTeam team : teams.values()) {
            int size = members(team.id()).size();
            if (size < smallestSize) {
                smallestSize = size;
                smallest = team;
            }
        }
        if (smallest != null) {
            assign(profile, smallest);
        }
        return smallest;
    }

    /** {@code true}, wenn beide Spieler im selben (nicht leeren) Team sind. */
    public boolean sameTeam(Player first, Player second) {
        if (first == null || second == null || first.equals(second)) {
            return false;
        }
        HeldenProfile firstProfile = profiles.get(first);
        HeldenProfile secondProfile = profiles.get(second);
        if (firstProfile == null || secondProfile == null) {
            return false;
        }
        String firstTeam = firstProfile.teamId();
        return firstTeam != null && !firstTeam.isEmpty() && firstTeam.equalsIgnoreCase(secondProfile.teamId());
    }
}
