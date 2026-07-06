package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.lemonpvp.lemoncosmetics.model.WinEffectType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles unlocking, buying, equipping and rendering win effects — victory
 * celebrations played around the winner when a duel ends.
 */
public class WinEffectManager {

    private static final List<Color> FIREWORK_COLORS = List.of(
            Color.fromRGB(255, 251, 0), Color.fromRGB(0, 255, 68),
            Color.fromRGB(0, 176, 255), Color.fromRGB(255, 64, 129),
            Color.fromRGB(255, 145, 0), Color.fromRGB(213, 0, 249));

    private final LemonCosmetics plugin;
    private final NamespacedKey fireworkKey;

    public WinEffectManager(LemonCosmetics plugin) {
        this.plugin = plugin;
        this.fireworkKey = new NamespacedKey(plugin, "win_firework");
    }

    /** PDC key marking our celebration fireworks so their blast damage is cancelled. */
    public NamespacedKey getFireworkKey() {
        return fireworkKey;
    }

    // -----------------------------------------------------------------------
    // Access / unlock / buy
    // -----------------------------------------------------------------------

    /** Whether the player may equip this effect: owned or granted by permission. */
    public boolean canUse(Player player, WinEffectType effect) {
        if (player.hasPermission(effect.permission())) return true;
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        return cosmetics != null && cosmetics.ownsWinEffect(effect.id);
    }

    /** Adds the effect to the player's owned set (cache + DB) without equipping it. */
    public CompletableFuture<Void> unlock(UUID uuid, String effectId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedWinEffects().add(effectId);
        return plugin.getDatabase().saveWinEffect(uuid, effectId, false);
    }

    /** Buys a win effect with coins. Returns {@code false} if unaffordable. */
    public CompletableFuture<Boolean> buy(UUID uuid, String effectId) {
        WinEffectType effect = WinEffectType.fromId(effectId).orElse(null);
        if (effect == null) return CompletableFuture.completedFuture(false);

        return plugin.getCosmeticsManager().canAfford(uuid, effect.price).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            var lc = plugin.getCosmeticsManager().getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, effect.price, "cosmetics:wineffect:" + effectId, null)
                    .thenCompose(v -> unlock(uuid, effectId))
                    .thenApply(v -> true);
        });
    }

    /** Sets (or clears, when {@code effectId} is null) the active win effect. */
    public CompletableFuture<Void> setActive(UUID uuid, String effectId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setActiveWinEffectId(effectId);
        return effectId == null
                ? plugin.getDatabase().clearActiveWinEffect(uuid)
                : plugin.getDatabase().setActiveWinEffect(uuid, effectId);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Plays the winner's active win effect at their current location. Safe to
     * call from other plugins (LemonPractice calls this on duel end); no-op if
     * nothing is equipped or the player isn't loaded. Must run on the main
     * thread — hop if needed.
     */
    public void play(Player winner) {
        if (winner == null || !winner.isOnline()) return;
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(winner.getUniqueId());
        if (cosmetics == null) return;
        WinEffectType effect = WinEffectType.fromId(cosmetics.getActiveWinEffectId()).orElse(null);
        if (effect == null) return;

        Location loc = winner.getLocation().clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (effect == WinEffectType.FIREWORK_SHOW) {
                playFireworkShow(loc);
            } else {
                playSpiral(effect, loc);
            }
        });
    }

    /** 5 real fireworks launched around the win location over ~2 seconds. */
    private void playFireworkShow(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 5; i++) {
            long delay = i * 8L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location at = center.clone().add(rng.nextDouble(-2.5, 2.5), 0.5, rng.nextDouble(-2.5, 2.5));
                Firework fw = world.spawn(at, Firework.class, f -> {
                    FireworkMeta meta = f.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder()
                            .with(rng.nextBoolean() ? FireworkEffect.Type.BALL : FireworkEffect.Type.BURST)
                            .withColor(FIREWORK_COLORS.get(rng.nextInt(FIREWORK_COLORS.size())))
                            .withFade(FIREWORK_COLORS.get(rng.nextInt(FIREWORK_COLORS.size())))
                            .flicker(rng.nextBoolean())
                            .trail(true)
                            .build());
                    meta.setPower(rng.nextInt(1, 3));
                    f.setFireworkMeta(meta);
                    // Marked so KillListener cancels any blast damage from it.
                    f.getPersistentDataContainer().set(fireworkKey, PersistentDataType.BYTE, (byte) 1);
                });
            }, delay);
        }
    }

    /** Generic celebration: a rising 4-strand fountain around the winner (~34 ticks). */
    private int pc(int n) { return plugin.particleCount(n); }

    private void playSpiral(WinEffectType type, Location loc) {
        final World world = loc.getWorld();
        if (world == null || type.particle == null) return;
        if (pc(1) <= 0) return;
        if (type.sound != null) world.playSound(loc, type.sound, 1.0f, 1.1f);

        final Location base = loc.clone();
        // Opening double ground ring so the win lands with big impact.
        for (int i = 0; i < 60; i++) {
            double a = i * (Math.PI * 2 / 60);
            world.spawnParticle(type.particle, base.clone().add(Math.cos(a) * 2.6, 0.1, Math.sin(a) * 2.6),
                    pc(3), 0.06, 0.06, 0.06, 0.01);
            world.spawnParticle(type.particle, base.clone().add(Math.cos(a) * 1.4, 0.1, Math.sin(a) * 1.4),
                    pc(2), 0.05, 0.05, 0.05, 0.01);
        }

        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (base.getWorld() == null || t[0] >= 40) { task[0].cancel(); return; }
            int tick = t[0];
            double y = tick * 0.13;
            double angle = tick * 0.42;
            double r = 1.4;
            // Six helix strands + an expanding ground ring underneath.
            for (int strand = 0; strand < 6; strand++) {
                double a = angle + strand * (Math.PI / 3);
                world.spawnParticle(type.particle,
                        base.clone().add(Math.cos(a) * r, y, Math.sin(a) * r), pc(4), 0.07, 0.07, 0.07, 0.01);
            }
            double gr = 0.5 + tick * 0.2;
            for (int i = 0; i < 20; i++) {
                double a = i * (Math.PI * 2 / 20) + tick * 0.1;
                world.spawnParticle(type.particle, base.clone().add(Math.cos(a) * gr, 0.1, Math.sin(a) * gr),
                        pc(2), 0.04, 0.04, 0.04, 0);
            }
            // Crown burst at the top on the final ticks.
            if (tick >= 35) {
                world.spawnParticle(type.particle, base.clone().add(0, 4.6, 0), pc(90), 1.0, 0.5, 1.0, 0.1);
            }
            t[0]++;
        }, 0L, 1L);
    }
}
