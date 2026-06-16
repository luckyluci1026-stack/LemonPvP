package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.effects.FireSwarmEffect;
import com.lemonpvp.lemoncosmetics.effects.GoldenGapEffect;
import com.lemonpvp.lemoncosmetics.effects.SpookSwarmEffect;
import com.lemonpvp.lemoncosmetics.effects.TotemExplosionEffect;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class KillEffectManager {

    private final LemonCosmetics plugin;

    public KillEffectManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays the active kill effect for {@code killer} at {@code killLocation}.
     * The effect is dispatched asynchronously. If the killer has no active
     * effect this method is a no-op.
     *
     * @param killer       the player who scored the kill
     * @param killLocation the location where the kill occurred
     */
    public void playEffect(Player killer, Location killLocation) {
        KillEffectType effectType = plugin.getCosmeticsManager()
                .getActiveKillEffect(killer.getUniqueId());
        if (effectType == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (effectType) {
                case FIRE_SWARM      -> FireSwarmEffect.play(plugin, killLocation);
                case SPOOK_SWARM     -> SpookSwarmEffect.play(plugin, killLocation);
                case TOTEM_EXPLOSION -> TotemExplosionEffect.play(plugin, killLocation);
                case GOLDEN_GAP      -> GoldenGapEffect.play(plugin, killLocation);
            }
        });
    }
}
