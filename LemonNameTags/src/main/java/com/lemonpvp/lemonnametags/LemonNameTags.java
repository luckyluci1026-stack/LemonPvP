package com.lemonpvp.lemonnametags;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * LemonNameTags — standalone over-head nametags via Text Display entities.
 *
 * <p>Shows each player's LuckPerms prefix (with its own gradient) followed by
 * their name in a rank-specific MiniMessage gradient — something vanilla
 * scoreboard-team nametags cannot do. Updates live on LuckPerms changes via
 * {@link UserDataRecalculateEvent}; cleans up reliably on quit/disable and
 * sweeps orphaned displays on enable.</p>
 */
public final class LemonNameTags extends JavaPlugin {

    private LuckPerms luckPerms;
    private RankGradients gradients;
    private NameTagManager manager;
    private EventSubscription<UserDataRecalculateEvent> lpSubscription;
    private BukkitTask validateTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<LuckPerms> rsp =
                getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (rsp == null) {
            getLogger().severe("LuckPerms not found — disabling LemonNameTags.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.luckPerms = rsp.getProvider();

        this.gradients = new RankGradients();
        this.gradients.load(getConfig(), getConfig().getBoolean("display.bold-ranked-names", true));

        this.manager = new NameTagManager(this, luckPerms, gradients);

        int swept = manager.cleanupOrphans();
        if (swept > 0) getLogger().info("Removed " + swept + " orphaned nametag display(s).");

        getServer().getPluginManager().registerEvents(new NameTagListener(this, manager), this);

        // Live updates on rank/permission changes (fired async by LuckPerms).
        this.lpSubscription = luckPerms.getEventBus().subscribe(
                this, UserDataRecalculateEvent.class, this::onUserDataRecalculate);

        // Periodic safety re-validation (re-mount after respawn/teleport/reload).
        int interval = Math.max(20, getConfig().getInt("validate-interval-ticks", 40));
        this.validateTask = getServer().getScheduler().runTaskTimer(
                this, manager::validateAll, interval, interval);

        // Reload case: build tags for everyone already online.
        for (Player p : getServer().getOnlinePlayers()) {
            manager.hideVanillaName(p);
            manager.create(p);
        }

        getLogger().info("LemonNameTags enabled (" + gradients.size() + " rank gradients).");
    }

    @Override
    public void onDisable() {
        if (lpSubscription != null) lpSubscription.close();
        if (validateTask != null) validateTask.cancel();
        if (manager != null) manager.removeAll();
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        UUID uuid = event.getUser().getUniqueId();
        // LuckPerms events fire off the main thread — hop back before touching entities.
        getServer().getScheduler().runTask(this, () -> {
            Player p = getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) manager.update(p);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonnametags.admin")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No permission."));
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                reloadConfig();
                gradients.load(getConfig(), getConfig().getBoolean("display.bold-ranked-names", true));
                for (Player p : getServer().getOnlinePlayers()) manager.create(p);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>LemonNameTags reloaded (" + gradients.size() + " gradients)."));
            }
            case "refresh" -> {
                for (Player p : getServer().getOnlinePlayers()) manager.update(p);
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Nametags refreshed."));
            }
            default -> sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>/lnt <white>reload <gray>| <white>refresh"));
        }
        return true;
    }
}
