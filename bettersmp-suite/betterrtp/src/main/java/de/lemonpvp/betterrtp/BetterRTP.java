package de.lemonpvp.betterrtp;

import de.lemonpvp.betterrtp.command.AdminCommand;
import de.lemonpvp.betterrtp.command.RTPCommand;
import de.lemonpvp.betterrtp.rtp.RTPManager;
import de.lemonpvp.betterrtp.rtp.WarmupListener;
import de.lemonpvp.betterrtp.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BetterRTP - vollständig asynchrones, sicheres Random-Teleport-Plugin.
 */
public final class BetterRTP extends JavaPlugin {

    private Msgs msgs;
    private RTPManager rtp;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.rtp = new RTPManager(this);

        Bukkit.getPluginManager().registerEvents(new WarmupListener(this), this);
        getCommand("rtp").setExecutor(new RTPCommand(this));
        getCommand("betterrtp").setExecutor(new AdminCommand(this));

        getLogger().info("BetterRTP aktiviert.");
    }

    @Override
    public void onDisable() {
        if (rtp != null) {
            rtp.shutdown();
        }
    }

    public Msgs msgs() {
        return msgs;
    }

    public RTPManager rtp() {
        return rtp;
    }
}
