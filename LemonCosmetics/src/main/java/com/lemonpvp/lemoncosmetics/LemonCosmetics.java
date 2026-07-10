package com.lemonpvp.lemoncosmetics;

import com.lemonpvp.lemoncosmetics.cape.MapCosmeticSlot;
import com.lemonpvp.lemoncosmetics.commands.MapCosmeticCommand;
import com.lemonpvp.lemoncosmetics.commands.CosmeticsCommand;
import com.lemonpvp.lemoncosmetics.database.CosmeticsDatabase;
import com.lemonpvp.lemoncosmetics.managers.CapeManager;
import com.lemonpvp.lemoncosmetics.listeners.ArrowTrailListener;
import com.lemonpvp.lemoncosmetics.listeners.KillListener;
import com.lemonpvp.lemoncosmetics.listeners.PlayerListener;
import com.lemonpvp.lemoncosmetics.managers.ArmorTrimManager;
import com.lemonpvp.lemoncosmetics.managers.ArrowTrailManager;
import com.lemonpvp.lemoncosmetics.managers.CosmeticsManager;
import com.lemonpvp.lemoncosmetics.managers.DeathEffectManager;
import com.lemonpvp.lemoncosmetics.managers.ExplosionParticleManager;
import com.lemonpvp.lemoncosmetics.managers.KillEffectManager;
import com.lemonpvp.lemoncosmetics.managers.TagManager;
import com.lemonpvp.lemoncosmetics.managers.WinEffectManager;
import com.lemonpvp.lemoncosmetics.commands.TagsCommand;
import com.lemonpvp.lemoncosmetics.velocity.CosmeticsMessaging;
import org.bukkit.plugin.java.JavaPlugin;

public final class LemonCosmetics extends JavaPlugin {

    private CosmeticsDatabase database;
    private CosmeticsManager cosmeticsManager;
    private ArmorTrimManager armorTrimManager;
    private KillEffectManager killEffectManager;
    private DeathEffectManager deathEffectManager;
    private WinEffectManager winEffectManager;
    private ExplosionParticleManager explosionParticleManager;
    private ArrowTrailManager arrowTrailManager;
    private TagManager tagManager;
    private CapeManager capeManager;
    private com.lemonpvp.lemoncosmetics.managers.EmoteManager emoteManager;
    private CosmeticsMessaging cosmeticsMessaging;
    private org.bukkit.configuration.file.FileConfiguration serversConfig;

    /** Global particle-count multiplier (config {@code cosmetics.particle-density}). */
    private volatile double particleDensity = 1.0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServersConfig();
        reloadParticleDensity();

        database = new CosmeticsDatabase(this);
        try {
            database.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        cosmeticsManager = new CosmeticsManager(this);
        armorTrimManager = new ArmorTrimManager(this);
        killEffectManager = new KillEffectManager(this);
        deathEffectManager = new DeathEffectManager(this);
        winEffectManager = new WinEffectManager(this);
        explosionParticleManager = new ExplosionParticleManager(this);
        arrowTrailManager = new ArrowTrailManager(this);
        tagManager = new TagManager(this);
        capeManager = new CapeManager(this);
        emoteManager = new com.lemonpvp.lemoncosmetics.managers.EmoteManager(this);

        cosmeticsMessaging = new CosmeticsMessaging(this);
        cosmeticsMessaging.register();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new ArrowTrailListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemoncosmetics.listeners.ExplosionListener(this), this);

        var cosmeticsCmd = getCommand("cosmetics");
        if (cosmeticsCmd != null) {
            cosmeticsCmd.setExecutor(new CosmeticsCommand(this));
        }
        var tagsCmd = getCommand("tags");
        if (tagsCmd != null) {
            tagsCmd.setExecutor(new TagsCommand(this));
        }
        var capeCmd = getCommand("cape");
        if (capeCmd != null) {
            capeCmd.setExecutor(new MapCosmeticCommand(this, MapCosmeticSlot.CAPE, "cape", "lemoncosmetics.cape"));
        }
        var bandanaCmd = getCommand("bandana");
        if (bandanaCmd != null) {
            bandanaCmd.setExecutor(new MapCosmeticCommand(this, MapCosmeticSlot.BANDANA, "bandana", "lemoncosmetics.bandana"));
        }
        var emoteCmd = getCommand("emote");
        if (emoteCmd != null) {
            emoteCmd.setExecutor(new com.lemonpvp.lemoncosmetics.commands.EmoteCommand(this));
        }

        getLogger().info("LemonCosmetics enabled.");
    }

    @Override
    public void onDisable() {
        if (emoteManager != null) emoteManager.shutdown();
        if (capeManager != null) capeManager.shutdown();
        if (arrowTrailManager != null) arrowTrailManager.cancelAll();
        if (cosmeticsMessaging != null) cosmeticsMessaging.unregister();
        if (database != null) database.disconnect();
        getLogger().info("LemonCosmetics disabled.");
    }

    public CosmeticsDatabase getDatabase() {
        return database;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public ArmorTrimManager getArmorTrimManager() {
        return armorTrimManager;
    }

    /** Reloads the particle-density multiplier from config (clamped 0.0–4.0). */
    public void reloadParticleDensity() {
        particleDensity = Math.max(0.0, Math.min(4.0,
                getConfig().getDouble("cosmetics.particle-density", 1.0)));
    }

    /**
     * Scales a base particle count by the configured density. Returns 0 when
     * density is 0 (effects off), otherwise at least 1 for any positive base so
     * a low multiplier never fully hides a single accent particle.
     */
    public int particleCount(int base) {
        if (base <= 0) return 0;
        if (particleDensity <= 0.0) return 0;
        return Math.max(1, (int) Math.round(base * particleDensity));
    }

    /** The effective (clamped) particle-density multiplier currently in effect. */
    public double getParticleDensity() {
        return particleDensity;
    }

    public KillEffectManager getKillEffectManager() {
        return killEffectManager;
    }

    public DeathEffectManager getDeathEffectManager() {
        return deathEffectManager;
    }

    public WinEffectManager getWinEffectManager() {
        return winEffectManager;
    }

    public ExplosionParticleManager getExplosionParticleManager() {
        return explosionParticleManager;
    }

    public ArrowTrailManager getArrowTrailManager() {
        return arrowTrailManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public CapeManager getCapeManager() {
        return capeManager;
    }

    public com.lemonpvp.lemoncosmetics.managers.EmoteManager getEmoteManager() {
        return emoteManager;
    }

    public org.bukkit.configuration.file.FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }

    public CosmeticsMessaging getCosmeticsMessaging() {
        return cosmeticsMessaging;
    }
}
