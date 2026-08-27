package com.lemonpvp.lemonpractice.replay;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Fallback replay actor: a named, player-headed armor stand (a real world entity
 * visible to everyone nearby). Used when PacketEvents is not installed.
 */
public class ArmorStandReplayActor implements ReplayActor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ArmorStand stand;

    public ArmorStandReplayActor(World world, Location loc, String name) {
        this.stand = world.spawn(loc, ArmorStand.class, a -> {
            a.setGravity(false);
            a.setInvulnerable(true);
            a.setBasePlate(false);
            a.setArms(true);
            a.setCustomNameVisible(true);
            a.customName(MM.deserialize("<yellow>" + name));
            a.setPersistent(false);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta sm) {
                try { sm.setOwningPlayer(Bukkit.getOfflinePlayer(name)); } catch (Exception ignored) {}
                head.setItemMeta(sm);
            }
            if (a.getEquipment() != null) a.getEquipment().setHelmet(head);
        });
    }

    @Override
    public void teleport(Location loc) {
        if (stand != null && stand.isValid()) stand.teleport(loc);
    }

    @Override
    public void remove() {
        if (stand != null && stand.isValid()) stand.remove();
    }
}
