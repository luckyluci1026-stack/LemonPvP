package com.lemonpvp.lemoncosmetics;

import com.lemonpvp.lemoncosmetics.commands.CosmeticsCommand;
import com.lemonpvp.lemoncosmetics.database.CosmeticsDatabase;
import com.lemonpvp.lemoncosmetics.listeners.ArrowTrailListener;
import com.lemonpvp.lemoncosmetics.listeners.KillListener;
import com.lemonpvp.lemoncosmetics.listeners.PlayerListener;
import com.lemonpvp.lemoncosmetics.managers.ArmorTrimManager;
import com.lemonpvp.lemoncosmetics.managers.ArrowTrailManager;
import com.lemonpvp.lemoncosmetics.managers.CosmeticsManager;
import com.lemonpvp.lemoncosmetics.managers.DeathEffectManager;
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
    private ArrowTrailManager arrowTrailManager;
    private TagManager tagManager;
    private CosmeticsMessaging cosmeticsMessaging;
    private org.bukkit.configuration.file.FileConfiguration serversConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServersConfig();

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
        arrowTrailManager = new ArrowTrailManager(this);
        tagManager = new TagManager(this);

        cosmeticsMessaging = new CosmeticsMessaging(this);
        cosmeticsMessaging.register();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new ArrowTrailListener(this), this);

        var cosmeticsCmd = getCommand("cosmetics");
        if (cosmeticsCmd != null) {
            cosmeticsCmd.setExecutor(new CosmeticsCommand(this));
        }
        var tagsCmd = getCommand("tags");
        if (tagsCmd != null) {
            tagsCmd.setExecutor(new TagsCommand(this));
        }

        getLogger().info("LemonCosmetics enabled.");
    }

    @Override
    public void onDisable() {
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

    public KillEffectManager getKillEffectManager() {
        return killEffectManager;
    }

    public DeathEffectManager getDeathEffectManager() {
        return deathEffectManager;
    }

    public WinEffectManager getWinEffectManager() {
        return winEffectManager;
    }

    public ArrowTrailManager getArrowTrailManager() {
        return arrowTrailManager;
    }

    public TagManager getTagManager() {
        return tagManager;
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
