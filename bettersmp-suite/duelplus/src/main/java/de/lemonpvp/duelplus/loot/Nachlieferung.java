package de.lemonpvp.duelplus.loot;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Nachlieferung {

    private Nachlieferung() {
    }

    public static void zustellen(DuelPlus plugin, Player spieler, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        List<ItemStack> kopien = new ArrayList<>();
        for (ItemStack item : items) {
            kopien.add(item.clone());
        }
        Map<Integer, ItemStack> rest = spieler.getInventory().addItem(kopien.toArray(new ItemStack[0]));
        if (!rest.isEmpty()) {
            rest = spieler.getEnderChest().addItem(rest.values().toArray(new ItemStack[0]));
        }
        if (rest.isEmpty()) {
            plugin.msgs().send(spieler, "loot-delivered", "anzahl", String.valueOf(items.size()));
            return;
        }
        for (ItemStack uebrig : rest.values()) {
            Item entity = spieler.getWorld().dropItem(spieler.getLocation(), uebrig);
            entity.setOwner(spieler.getUniqueId());
            entity.setUnlimitedLifetime(true);
            entity.setInvulnerable(true);
            entity.setPickupDelay(0);
        }
        plugin.msgs().send(spieler, "loot-delivered-dropped", "anzahl", String.valueOf(rest.size()));
    }
}
