package de.lemonpvp.helden.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Getippte Sicht auf die config.yml. */
public final class Settings {

    /** Was mit einem Spieler passiert, der alle Herzen verloren hat. */
    public enum EliminationAction {
        SPECTATOR,
        KICK,
        NOTHING
    }

    private FileConfiguration config;

    private String prefix = "";
    private String projectName = "";
    private int autosaveSeconds = 300;

    private int startHearts = 3;
    private boolean linkHeartEnabled = true;
    private int maxHearts = 6;
    private boolean pvpOnly = true;
    private int pvpCreditSeconds = 15;

    private boolean linkAnnounce = true;
    private boolean linkReassignOnPartnerOut = true;
    private boolean linkAllowMultiple = false;

    private boolean dummyEnabled = true;
    private int dummyLifetimeSeconds = 180;
    private boolean dummyPlayersOnly = true;
    private String dummyNameFormat = "&c%player% &7(Dummy)";

    private EliminationAction eliminationAction = EliminationAction.SPECTATOR;
    private int minParticipants = 2;
    private boolean announceWinner = true;
    private boolean enforceOnJoin = true;

    private int combatTagSeconds = 15;
    private int respawnProtectionSeconds = 5;

    private boolean hudEnabled = true;
    private String hudTitle = "";
    private int hudUpdateTicks = 20;
    private List<String> hudLines = new ArrayList<>();

    private String heartItemId = "herz";
    private int heartItemValue = 1;

    private boolean detectFloodgate = true;
    private String bedrockUsernamePrefix = ".";
    private boolean stripHexColors = true;
    private boolean sendControlsHint = true;

    public void load(FileConfiguration config) {
        this.config = config;

        prefix = config.getString("general.prefix", "&8[&c♥&8] &r");
        projectName = config.getString("general.project-name", "&cHELDEN 3");
        autosaveSeconds = Math.max(30, config.getInt("general.autosave-seconds", 300));

        startHearts = Math.max(1, config.getInt("hearts.start", 3));
        linkHeartEnabled = config.getBoolean("hearts.link-heart", true);
        maxHearts = Math.max(totalStartHearts(), config.getInt("hearts.max", 6));
        pvpOnly = config.getBoolean("hearts.pvp-only", true);
        pvpCreditSeconds = Math.max(0, config.getInt("hearts.pvp-credit-seconds", 15));

        linkAnnounce = config.getBoolean("link.announce", true);
        linkReassignOnPartnerOut = config.getBoolean("link.reassign-on-partner-out", true);
        linkAllowMultiple = config.getBoolean("link.allow-multiple", false);

        dummyEnabled = config.getBoolean("dummy.enabled", true);
        dummyLifetimeSeconds = Math.max(5, config.getInt("dummy.lifetime-seconds", 180));
        dummyPlayersOnly = config.getBoolean("dummy.players-only", true);
        dummyNameFormat = config.getString("dummy.name-format", "&c%player% &7(Dummy)");

        eliminationAction = parseElimination(config.getString("game.on-elimination", "SPECTATOR"));
        minParticipants = Math.max(2, config.getInt("game.min-participants", 2));
        announceWinner = config.getBoolean("game.announce-winner", true);
        enforceOnJoin = config.getBoolean("game.enforce-on-join", true);

        combatTagSeconds = Math.max(0, config.getInt("combat.tag-seconds", 15));
        respawnProtectionSeconds = Math.max(0, config.getInt("combat.respawn-protection-seconds", 5));

        hudEnabled = config.getBoolean("hud.enabled", true);
        hudTitle = config.getString("hud.title", "&cHELDEN 3");
        hudUpdateTicks = Math.max(5, config.getInt("hud.update-ticks", 20));
        hudLines = config.getStringList("hud.lines");

        heartItemId = config.getString("special-items.heart", "herz");
        heartItemValue = Math.max(1, config.getInt("special-items.heart-value", 1));

        detectFloodgate = config.getBoolean("bedrock.detect-floodgate", true);
        bedrockUsernamePrefix = config.getString("bedrock.username-prefix", ".");
        stripHexColors = config.getBoolean("bedrock.strip-hex-colors", true);
        sendControlsHint = config.getBoolean("bedrock.send-controls-hint", true);
    }

    private EliminationAction parseElimination(String raw) {
        if (raw == null) {
            return EliminationAction.SPECTATOR;
        }
        try {
            return EliminationAction.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return EliminationAction.SPECTATOR;
        }
    }

    /** Herzen beim Start inklusive Link-Herz - also 3 + 1 = 4. */
    public int totalStartHearts() {
        return startHearts + (linkHeartEnabled ? 1 : 0);
    }

    public Location spawn() {
        if (config == null) {
            return null;
        }
        String worldName = config.getString("world.spawn.world", "");
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world,
                config.getDouble("world.spawn.x"),
                config.getDouble("world.spawn.y"),
                config.getDouble("world.spawn.z"),
                (float) config.getDouble("world.spawn.yaw"),
                (float) config.getDouble("world.spawn.pitch"));
    }

    /** Projektspawn, sonst der Weltspawn der ersten geladenen Welt. */
    public Location spawnOrDefault() {
        Location spawn = spawn();
        if (spawn != null) {
            return spawn;
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0).getSpawnLocation();
    }

    public void writeSpawn(Location location) {
        if (config == null || location == null || location.getWorld() == null) {
            return;
        }
        config.set("world.spawn.world", location.getWorld().getName());
        config.set("world.spawn.x", location.getX());
        config.set("world.spawn.y", location.getY());
        config.set("world.spawn.z", location.getZ());
        config.set("world.spawn.yaw", (double) location.getYaw());
        config.set("world.spawn.pitch", (double) location.getPitch());
    }

    public String prefix() {
        return prefix;
    }

    public String projectName() {
        return projectName;
    }

    public int autosaveSeconds() {
        return autosaveSeconds;
    }

    public int startHearts() {
        return startHearts;
    }

    public boolean linkHeartEnabled() {
        return linkHeartEnabled;
    }

    public int maxHearts() {
        return maxHearts;
    }

    public boolean pvpOnly() {
        return pvpOnly;
    }

    public int pvpCreditSeconds() {
        return pvpCreditSeconds;
    }

    public boolean linkAnnounce() {
        return linkAnnounce;
    }

    public boolean linkReassignOnPartnerOut() {
        return linkReassignOnPartnerOut;
    }

    public boolean linkAllowMultiple() {
        return linkAllowMultiple;
    }

    public boolean dummyEnabled() {
        return dummyEnabled;
    }

    public int dummyLifetimeSeconds() {
        return dummyLifetimeSeconds;
    }

    public boolean dummyPlayersOnly() {
        return dummyPlayersOnly;
    }

    public String dummyNameFormat() {
        return dummyNameFormat;
    }

    public EliminationAction eliminationAction() {
        return eliminationAction;
    }

    public int minParticipants() {
        return minParticipants;
    }

    public boolean announceWinner() {
        return announceWinner;
    }

    public boolean enforceOnJoin() {
        return enforceOnJoin;
    }

    public int combatTagSeconds() {
        return combatTagSeconds;
    }

    public int respawnProtectionSeconds() {
        return respawnProtectionSeconds;
    }

    public boolean hudEnabled() {
        return hudEnabled;
    }

    public String hudTitle() {
        return hudTitle;
    }

    public int hudUpdateTicks() {
        return hudUpdateTicks;
    }

    public List<String> hudLines() {
        return hudLines;
    }

    public String heartItemId() {
        return heartItemId;
    }

    public boolean heartItemEnabled() {
        return heartItemId != null && !heartItemId.isEmpty();
    }

    public int heartItemValue() {
        return heartItemValue;
    }

    public boolean detectFloodgate() {
        return detectFloodgate;
    }

    public String bedrockUsernamePrefix() {
        return bedrockUsernamePrefix;
    }

    public boolean stripHexColors() {
        return stripHexColors;
    }

    public boolean sendControlsHint() {
        return sendControlsHint;
    }
}
