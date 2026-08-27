package com.lemonpvp.lemontraining;

import com.lemonpvp.lemontraining.commands.LeaveCommand;
import com.lemonpvp.lemontraining.database.TrainingDatabase;
import com.lemonpvp.lemontraining.listeners.TrainingListener;
import com.lemonpvp.lemontraining.managers.ArenaManager;
import com.lemonpvp.lemontraining.managers.PracticeManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LemonTraining extends JavaPlugin {

    private TrainingDatabase database;
    private ArenaManager arenaManager;
    private PracticeManager practiceManager;
    private FileConfiguration messages;
    private FileConfiguration serversConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServersConfig();
        saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        database = new TrainingDatabase(this);
        try {
            database.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        arenaManager = new ArenaManager(this);
        practiceManager = new PracticeManager(this);

        // BungeeCord outgoing for sendToLobby in AbstractPractice
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Commands and listeners
        getCommand("leave").setExecutor(new LeaveCommand(this));
        getServer().getPluginManager().registerEvents(new TrainingListener(this), this);

        // Remove any training zombies left over from a previous crash
        cleanupOrphanZombies();

        // Build arenas async (non-blocking startup)
        getServer().getScheduler().runTask(this, arenaManager::buildArenas);

        getLogger().info("LemonTraining enabled.");
    }

    @Override
    public void onDisable() {
        if (practiceManager != null) practiceManager.endAllSessions();
        if (database != null) database.close();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("LemonTraining disabled.");
    }

    public TrainingDatabase getDatabase()       { return database; }
    public ArenaManager getArenaManager()       { return arenaManager; }
    public PracticeManager getPracticeManager() { return practiceManager; }
    public FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = YamlConfiguration.loadConfiguration(f);
    }

    public FileConfiguration getMessages()      { return messages; }

    private void cleanupOrphanZombies() {
        NamespacedKey key = new NamespacedKey(this, "training_zombie");
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Zombie z : world.getEntitiesByClass(Zombie.class)) {
                if (z.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                    z.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) getLogger().info("Removed " + removed + " orphaned training zombie(s) from previous session.");
    }
}
