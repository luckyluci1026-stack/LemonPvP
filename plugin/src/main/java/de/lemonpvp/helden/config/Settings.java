package de.lemonpvp.helden.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Getippte Sicht auf die config.yml. */
public final class Settings {

    public enum ZeroLivesAction {
        SPECTATOR,
        KICK,
        NOTHING
    }

    /** Wie eine Heldenfaehigkeit per Item ausgeloest wird. */
    public enum AbilityTrigger {
        RIGHT_CLICK,
        SNEAK_RIGHT_CLICK
    }

    private FileConfiguration config;

    private String prefix = "";
    private String projectName = "";
    private int autosaveSeconds = 300;

    private boolean detectFloodgate = true;
    private String bedrockUsernamePrefix = ".";
    private boolean stripHexColors = true;
    private boolean compactMenus = true;
    private boolean sendControlsHint = true;

    private AbilityTrigger abilityTrigger = AbilityTrigger.SNEAK_RIGHT_CLICK;
    private boolean selectHeroOnFirstJoin = true;
    private int heroChangeCooldownSeconds = 3600;
    private int passiveRefreshSeconds = 5;

    private boolean livesEnabled = true;
    private int livesStart = 3;
    private int livesMax = 6;
    private ZeroLivesAction zeroLivesAction = ZeroLivesAction.SPECTATOR;
    private boolean reviveEnabled = true;
    private int reviveCost = 250;
    private int reviveLives = 1;
    private int reviveProtectionSeconds = 15;

    private boolean teamFriendlyFire = false;
    private int combatTagSeconds = 15;
    private boolean killOnCombatLog = true;
    private int respawnProtectionSeconds = 10;
    private int announceKillstreakFrom = 3;

    private String currencyName = "Zitronen";
    private String currencySymbol = "";
    private int startBalance = 100;
    private int rewardPerKill = 50;
    private int killstreakBonus = 10;
    private int rewardPerAssist = 15;
    private int lossPerDeath = 0;

    private boolean hudEnabled = true;
    private String hudTitle = "";
    private int hudUpdateTicks = 20;
    private List<String> hudLines = new ArrayList<>();
    private boolean colorNametags = true;

    private boolean eventsEnabled = true;
    private int eventIntervalMinutes = 45;
    private int eventWarmupSeconds = 30;
    private List<String> eventRotation = new ArrayList<>();

    private String reviveItemId = "heldenherz";
    private String lifeCrystalItemId = "lebenskristall";
    private String currencyTokenItemId = "zitrone";
    private String returnStoneItemId = "rueckkehrstein";
    private int currencyTokenValue = 50;

    public void load(FileConfiguration config) {
        this.config = config;

        prefix = config.getString("general.prefix", "&8[&eHelden3&8] &r");
        projectName = config.getString("general.project-name", "&eHELDEN 3");
        autosaveSeconds = Math.max(30, config.getInt("general.autosave-seconds", 300));

        detectFloodgate = config.getBoolean("bedrock.detect-floodgate", true);
        bedrockUsernamePrefix = config.getString("bedrock.username-prefix", ".");
        stripHexColors = config.getBoolean("bedrock.strip-hex-colors", true);
        compactMenus = config.getBoolean("bedrock.compact-menus", true);
        sendControlsHint = config.getBoolean("bedrock.send-controls-hint", true);

        abilityTrigger = parseAbilityTrigger(config.getString("hero.ability-trigger", "SNEAK_RIGHT_CLICK"));
        selectHeroOnFirstJoin = config.getBoolean("hero.select-on-first-join", true);
        heroChangeCooldownSeconds = config.getInt("hero.change-cooldown-seconds", 3600);
        passiveRefreshSeconds = Math.max(1, config.getInt("hero.passive-refresh-seconds", 5));

        livesEnabled = config.getBoolean("lives.enabled", true);
        livesStart = Math.max(1, config.getInt("lives.start", 3));
        livesMax = Math.max(livesStart, config.getInt("lives.max", 6));
        zeroLivesAction = parseZeroLivesAction(config.getString("lives.on-zero", "SPECTATOR"));
        reviveEnabled = config.getBoolean("lives.revive-enabled", true);
        reviveCost = Math.max(0, config.getInt("lives.revive-cost", 250));
        reviveLives = Math.max(1, config.getInt("lives.revive-lives", 1));
        reviveProtectionSeconds = Math.max(0, config.getInt("lives.revive-protection-seconds", 15));

        teamFriendlyFire = config.getBoolean("combat.team-friendly-fire", false);
        combatTagSeconds = Math.max(0, config.getInt("combat.tag-seconds", 15));
        killOnCombatLog = config.getBoolean("combat.kill-on-combat-log", true);
        respawnProtectionSeconds = Math.max(0, config.getInt("combat.respawn-protection-seconds", 10));
        announceKillstreakFrom = Math.max(2, config.getInt("combat.announce-killstreak-from", 3));

        currencyName = config.getString("economy.name", "Zitronen");
        currencySymbol = config.getString("economy.symbol", "");
        startBalance = Math.max(0, config.getInt("economy.start-balance", 100));
        rewardPerKill = Math.max(0, config.getInt("economy.reward-per-kill", 50));
        killstreakBonus = Math.max(0, config.getInt("economy.killstreak-bonus", 10));
        rewardPerAssist = Math.max(0, config.getInt("economy.reward-per-assist", 15));
        lossPerDeath = Math.max(0, config.getInt("economy.loss-per-death", 0));

        hudEnabled = config.getBoolean("hud.enabled", true);
        hudTitle = config.getString("hud.title", "&eHELDEN 3");
        hudUpdateTicks = Math.max(5, config.getInt("hud.update-ticks", 20));
        hudLines = config.getStringList("hud.lines");
        colorNametags = config.getBoolean("hud.color-nametags", true);

        eventsEnabled = config.getBoolean("events.enabled", true);
        eventIntervalMinutes = Math.max(1, config.getInt("events.interval-minutes", 45));
        eventWarmupSeconds = Math.max(0, config.getInt("events.warmup-seconds", 30));
        eventRotation = config.getStringList("events.rotation");

        reviveItemId = config.getString("special-items.revive", "heldenherz");
        lifeCrystalItemId = config.getString("special-items.life-crystal", "lebenskristall");
        currencyTokenItemId = config.getString("special-items.currency-token", "zitrone");
        returnStoneItemId = config.getString("special-items.return-stone", "rueckkehrstein");
        currencyTokenValue = Math.max(0, config.getInt("special-items.currency-token-value", 50));
    }

    private AbilityTrigger parseAbilityTrigger(String raw) {
        if (raw == null) {
            return AbilityTrigger.SNEAK_RIGHT_CLICK;
        }
        try {
            return AbilityTrigger.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return AbilityTrigger.SNEAK_RIGHT_CLICK;
        }
    }

    private ZeroLivesAction parseZeroLivesAction(String raw) {
        if (raw == null) {
            return ZeroLivesAction.SPECTATOR;
        }
        try {
            return ZeroLivesAction.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ZeroLivesAction.SPECTATOR;
        }
    }

    /** Rohzugriff fuer Event-Einstellungen, die jedes Event selbst liest. */
    public ConfigurationSection eventSection(String eventId) {
        return config == null ? null : config.getConfigurationSection("events." + eventId);
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

    public boolean detectFloodgate() {
        return detectFloodgate;
    }

    public String bedrockUsernamePrefix() {
        return bedrockUsernamePrefix;
    }

    public boolean stripHexColors() {
        return stripHexColors;
    }

    public boolean compactMenus() {
        return compactMenus;
    }

    public boolean sendControlsHint() {
        return sendControlsHint;
    }

    public AbilityTrigger abilityTrigger() {
        return abilityTrigger;
    }

    public boolean selectHeroOnFirstJoin() {
        return selectHeroOnFirstJoin;
    }

    public int heroChangeCooldownSeconds() {
        return heroChangeCooldownSeconds;
    }

    public int passiveRefreshSeconds() {
        return passiveRefreshSeconds;
    }

    public boolean livesEnabled() {
        return livesEnabled;
    }

    public int livesStart() {
        return livesStart;
    }

    public int livesMax() {
        return livesMax;
    }

    public ZeroLivesAction zeroLivesAction() {
        return zeroLivesAction;
    }

    public boolean reviveEnabled() {
        return reviveEnabled;
    }

    public int reviveCost() {
        return reviveCost;
    }

    public int reviveLives() {
        return reviveLives;
    }

    public int reviveProtectionSeconds() {
        return reviveProtectionSeconds;
    }

    public boolean teamFriendlyFire() {
        return teamFriendlyFire;
    }

    public int combatTagSeconds() {
        return combatTagSeconds;
    }

    public boolean killOnCombatLog() {
        return killOnCombatLog;
    }

    public int respawnProtectionSeconds() {
        return respawnProtectionSeconds;
    }

    public int announceKillstreakFrom() {
        return announceKillstreakFrom;
    }

    public String currencyName() {
        return currencyName;
    }

    public String currencySymbol() {
        return currencySymbol;
    }

    public int startBalance() {
        return startBalance;
    }

    public int rewardPerKill() {
        return rewardPerKill;
    }

    public int killstreakBonus() {
        return killstreakBonus;
    }

    public int rewardPerAssist() {
        return rewardPerAssist;
    }

    public int lossPerDeath() {
        return lossPerDeath;
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

    public boolean colorNametags() {
        return colorNametags;
    }

    public boolean eventsEnabled() {
        return eventsEnabled;
    }

    public int eventIntervalMinutes() {
        return eventIntervalMinutes;
    }

    public int eventWarmupSeconds() {
        return eventWarmupSeconds;
    }

    public List<String> eventRotation() {
        return eventRotation.isEmpty() ? new ArrayList<>(Arrays.asList("blutmond", "zitronenregen", "kopfgeld")) : eventRotation;
    }

    public String reviveItemId() {
        return reviveItemId;
    }

    public String lifeCrystalItemId() {
        return lifeCrystalItemId;
    }

    public String currencyTokenItemId() {
        return currencyTokenItemId;
    }

    public String returnStoneItemId() {
        return returnStoneItemId;
    }

    public int currencyTokenValue() {
        return currencyTokenValue;
    }
}
