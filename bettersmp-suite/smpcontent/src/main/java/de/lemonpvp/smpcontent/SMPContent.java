package de.lemonpvp.smpcontent;

import de.lemonpvp.smpcontent.command.ContentCommand;
import de.lemonpvp.smpcontent.content.ContentRegistry;
import de.lemonpvp.smpcontent.listener.BlockListener;
import de.lemonpvp.smpcontent.listener.GuiListener;
import de.lemonpvp.smpcontent.pack.PackGenerator;
import de.lemonpvp.smpcontent.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SMPContent - eigene Blöcke und Items passend zum SMP-Texturepack.
 *
 * Blöcke werden über feste Note-Block-Zustände umgesetzt; das Plugin hält
 * diese Zustände stabil und sorgt für die richtigen Drops. Items bekommen
 * ein item_model (Java) und CustomModelData (Geyser/Bedrock).
 */
public final class SMPContent extends JavaPlugin {

    private Msgs msgs;
    private ContentRegistry registry;
    private PackGenerator pack;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.registry = new ContentRegistry(this);
        this.pack = new PackGenerator(this);
        pack.ensureFolders();

        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        getCommand("smpcontent").setExecutor(new ContentCommand(this));

        getLogger().info("SMPContent aktiviert.");
    }

    public Msgs msgs() {
        return msgs;
    }

    public ContentRegistry registry() {
        return registry;
    }

    public PackGenerator pack() {
        return pack;
    }
}
