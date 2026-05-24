package com.lemonpvp.lemontraining.practice;

import com.google.common.io.ByteStreams;
import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeSession;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPractice {

    protected final LemonTraining plugin;
    protected final Player player;
    protected final PracticeSession session;
    protected final List<BukkitTask> tasks = new ArrayList<>();

    protected static final MiniMessage MM = MiniMessage.miniMessage();

    public AbstractPractice(LemonTraining plugin, Player player, PracticeSession session) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
    }

    public abstract void start();

    public abstract void end();

    public PracticeSession getSession() {
        return session;
    }

    protected void giveLeaveItem() {
        ItemStack leave = new ItemStack(Material.RED_DYE);
        ItemMeta meta = leave.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<red>Leave</red>"));
            NamespacedKey key = new NamespacedKey(plugin, "leave");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            leave.setItemMeta(meta);
        }
        player.getInventory().setItem(8, leave);
    }

    protected void sendToLobby() {
        com.google.common.io.ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(plugin.getConfig().getString("lobby-server", "lobby"));
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void cancelTasks() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    protected void applyPracticeEffects() {
        // Keep food level at 20 every 30 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                player.setFoodLevel(20);
                player.setSaturation(20f);
            }
        }, 0L, 30L));
    }
}
