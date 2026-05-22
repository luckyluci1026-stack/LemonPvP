package com.lemonpvp.lemonquests.listeners;

import com.lemonpvp.lemonquests.LemonQuests;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class QuestProgressListener implements Listener {

    private final LemonQuests plugin;

    public QuestProgressListener(LemonQuests plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards a KILLS point to the killer when an entity is killed by a player.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killer = event.getEntity().getKiller();
        if (!(killer instanceof Player playerKiller)) return;

        // Do not count self-kills
        if (event.getEntity().equals(playerKiller)) return;

        plugin.getQuestManager().onStatUpdate(playerKiller.getUniqueId(), "KILLS", 1);
    }

    /**
     * Awards a DEATHS point to the player who died.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        plugin.getQuestManager().onStatUpdate(victim.getUniqueId(), "DEATHS", 1);
    }
}
