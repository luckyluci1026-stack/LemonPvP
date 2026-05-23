package com.lemonpvp.lemoncosmetics;

import com.lemonpvp.lemoncosmetics.commands.CosmeticsCommand;
import com.lemonpvp.lemoncosmetics.database.CosmeticsDatabase;
import com.lemonpvp.lemoncosmetics.listeners.ArrowTrailListener;
import com.lemonpvp.lemoncosmetics.listeners.KillListener;
import com.lemonpvp.lemoncosmetics.listeners.PlayerListener;
import com.lemonpvp.lemoncosmetics.managers.ArmorTrimManager;
import com.lemonpvp.lemoncosmetics.managers.ArrowTrailManager;
import com.lemonpvp.lemoncosmetics.managers.CosmeticsManager;
import com.lemonpvp.lemoncosmetics.managers.HatManager;
import com.lemonpvp.lemoncosmetics.managers.KillEffectManager;
import com.lemonpvp.lemoncosmetics.velocity.CosmeticsMessaging;
import org.bukkit.plugin.java.JavaPlugin;

public final class LemonCosmetics extends JavaPlugin {

    private CosmeticsDatabase database;
    private CosmeticsManager cosmeticsManager;
    private ArmorTrimManager armorTrimManager;
    private KillEffectManager killEffectManager;
    private HatManager hatManager;
    private ArrowTrailManager arrowTrailManager;
    private CosmeticsMessaging cosmeticsMessaging;

    @Override
    public void onEnable() {
        saveDefaultConfig();

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
        hatManager = new HatManager(this);
        arrowTrailManager = new ArrowTrailManager(this);

        cosmeticsMessaging = new CosmeticsMessaging(this);
        cosmeticsMessaging.register();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new ArrowTrailListener(this), this);

        var cosmeticsCmd = getCommand("cosmetics");
        if (cosmeticsCmd != null) {
            cosmeticsCmd.setExecutor(new CosmeticsCommand(this));
        }

        getLogger().info("LemonCosmetics enabled.");
    }

    @Override
    public void onDisable() {
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

    public HatManager getHatManager() {
        return hatManager;
    }

    public ArrowTrailManager getArrowTrailManager() {
        return arrowTrailManager;
    }

    public CosmeticsMessaging getCosmeticsMessaging() {
        return cosmeticsMessaging;
    }
}
