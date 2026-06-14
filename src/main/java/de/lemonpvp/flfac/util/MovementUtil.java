package de.lemonpvp.flfac.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/** Shared helpers for the movement checks (exemptions, surroundings, ...). */
public final class MovementUtil {

    private MovementUtil() {
    }

    /**
     * True if a player should be exempt from movement checks for reasons that
     * legitimately break normal physics (flight, vehicles, liquids, ...).
     */
    public static boolean isExempt(Player player) {
        if (player.isFlying() || player.getAllowFlight() || player.isGliding()) {
            return true;
        }
        if (player.isInsideVehicle() || player.isDead()) {
            return true;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        if (player.isInWater() || isInLava(player) || player.isRiptiding()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return true;
        }
        return isOnClimbable(player) || isInCobweb(player);
    }

    public static boolean isInLava(Player player) {
        Material type = player.getLocation().getBlock().getType();
        return type == Material.LAVA;
    }

    public static boolean isOnClimbable(Player player) {
        Material type = player.getLocation().getBlock().getType();
        return type == Material.LADDER || type == Material.VINE
                || type == Material.SCAFFOLDING || type == Material.TWISTING_VINES
                || type == Material.WEEPING_VINES || type == Material.CAVE_VINES;
    }

    public static boolean isInCobweb(Player player) {
        return player.getLocation().getBlock().getType() == Material.COBWEB
                || player.getEyeLocation().getBlock().getType() == Material.COBWEB;
    }

    /** Block directly beneath the player's feet. */
    public static Block blockBelow(Location location) {
        return location.clone().subtract(0.0D, 0.1D, 0.0D).getBlock();
    }

    public static boolean isOnIce(Location location) {
        Material type = blockBelow(location).getType();
        return type == Material.ICE || type == Material.PACKED_ICE
                || type == Material.BLUE_ICE || type == Material.FROSTED_ICE;
    }

    public static boolean isOnSlime(Location location) {
        return blockBelow(location).getType() == Material.SLIME_BLOCK;
    }

    public static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }
}
