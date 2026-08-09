package de.lemonpvp.betterrtp.rtp;

import de.lemonpvp.betterrtp.BetterRTP;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Steuert Ablauf, Cooldowns, Warmup und Economy für /rtp.
 */
public final class RTPManager {

    private final BetterRTP plugin;
    private final SafeLocationFinder finder;
    private final EconomyHook economy;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> warmups = new ConcurrentHashMap<>();
    private final Map<UUID, Location> warmupOrigin = new ConcurrentHashMap<>();

    public RTPManager(BetterRTP plugin) {
        this.plugin = plugin;
        this.finder = new SafeLocationFinder(plugin);
        this.economy = new EconomyHook();
    }

    private int cfg(String path, int def) {
        return plugin.getConfig().getInt("settings." + path, def);
    }

    public boolean isTeleporting(UUID uuid) {
        return warmups.containsKey(uuid);
    }

    public Profile profileFor(World world) {
        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
        if (worlds != null) {
            ConfigurationSection own = worlds.getConfigurationSection(world.getName());
            if (own != null) {
                return Profile.from(own);
            }
            ConfigurationSection def = worlds.getConfigurationSection("default");
            if (def != null) {
                return Profile.from(def);
            }
        }
        return new Profile(true, 0, 0, 500, 10000, true, java.util.Set.of());
    }

    /** Startet eine RTP-Anfrage mit allen Prüfungen und dem Warmup. */
    public void request(Player player, World world) {
        UUID uuid = player.getUniqueId();
        if (isTeleporting(uuid)) {
            plugin.msgs().send(player, "already-teleporting");
            return;
        }
        Profile profile = profileFor(world);
        if (!profile.enabled() && !player.hasPermission("betterrtp.admin")) {
            plugin.msgs().send(player, "world-disabled");
            return;
        }
        long remaining = cooldownRemaining(uuid);
        if (remaining > 0 && !player.hasPermission("betterrtp.bypass.cooldown")) {
            plugin.msgs().send(player, "cooldown", "seconds", String.valueOf(remaining));
            return;
        }

        double cost = plugin.getConfig().getDouble("economy.cost", 0);
        boolean chargeEnabled = plugin.getConfig().getBoolean("economy.enabled", false)
                && economy.isEnabled() && cost > 0
                && !player.hasPermission("betterrtp.bypass.cost");
        if (chargeEnabled && !economy.has(player, cost)) {
            plugin.msgs().send(player, "not-enough-money", "cost", economy.format(cost));
            return;
        }

        int warmupSeconds = cfg("warmup-seconds", 3);
        if (warmupSeconds <= 0 || player.hasPermission("betterrtp.bypass.warmup")) {
            finishAndTeleport(player, world, profile, chargeEnabled, cost);
            return;
        }
        startWarmup(player, world, profile, warmupSeconds, chargeEnabled, cost);
    }

    private void startWarmup(Player player, World world, Profile profile,
                             int seconds, boolean charge, double cost) {
        UUID uuid = player.getUniqueId();
        warmupOrigin.put(uuid, player.getLocation());
        plugin.msgs().send(player, "warmup", "seconds", String.valueOf(seconds));

        BukkitTask task = new BukkitRunnable() {
            int left = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelWarmup(uuid);
                    return;
                }
                if (left <= 0) {
                    warmups.remove(uuid);
                    warmupOrigin.remove(uuid);
                    cancel();
                    finishAndTeleport(player, world, profile, charge, cost);
                    return;
                }
                player.sendActionBar(plugin.msgs()
                        .format("warmup-actionbar", "seconds", String.valueOf(left)));
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        warmups.put(uuid, task);
    }

    /** Von WarmupListener bei Bewegung/Schaden aufgerufen. */
    public void cancelWarmup(UUID uuid) {
        BukkitTask task = warmups.remove(uuid);
        warmupOrigin.remove(uuid);
        if (task != null) {
            task.cancel();
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                plugin.msgs().send(player, "warmup-cancelled");
            }
        }
    }

    public Location warmupOrigin(UUID uuid) {
        return warmupOrigin.get(uuid);
    }

    private void finishAndTeleport(Player player, World world, Profile profile,
                                   boolean charge, double cost) {
        UUID uuid = player.getUniqueId();
        // Als "in Arbeit" markieren, damit keine zweite Anfrage startet
        warmups.put(uuid, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
        }, 1L));
        plugin.msgs().send(player, "searching");

        finder.find(world, profile, cfg("max-attempts", 60)).thenAccept(location -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                warmups.remove(uuid);
                if (!player.isOnline()) {
                    return;
                }
                if (location == null) {
                    plugin.msgs().send(player, "failed");
                    return;
                }
                if (charge && !economy.withdraw(player, cost)) {
                    plugin.msgs().send(player, "not-enough-money", "cost", economy.format(cost));
                    return;
                }
                setCooldown(uuid);
                player.teleportAsync(location).thenAccept(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        onArrive(player, location, charge, cost);
                    }
                });
            });
        });
    }

    private void onArrive(Player player, Location location, boolean charged, double cost) {
        player.setFallDistance(0);
        int invuln = cfg("invulnerable-after-seconds", 3);
        if (invuln > 0) {
            player.setInvulnerable(true);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setInvulnerable(false);
                }
            }, invuln * 20L);
        }
        plugin.msgs().send(player, "success",
                "x", String.valueOf(location.getBlockX()),
                "y", String.valueOf(location.getBlockY()),
                "z", String.valueOf(location.getBlockZ()),
                "world", location.getWorld().getName());
        if (charged) {
            plugin.msgs().send(player, "paid", "cost", economy.format(cost));
        }
    }

    private long cooldownRemaining(UUID uuid) {
        Long until = cooldowns.get(uuid);
        if (until == null) {
            return 0;
        }
        long diff = until - System.currentTimeMillis();
        return diff <= 0 ? 0 : (diff + 999) / 1000;
    }

    private void setCooldown(UUID uuid) {
        int seconds = cfg("cooldown-seconds", 30);
        if (seconds > 0) {
            cooldowns.put(uuid, System.currentTimeMillis() + seconds * 1000L);
        }
    }

    public void shutdown() {
        for (BukkitTask task : warmups.values()) {
            task.cancel();
        }
        warmups.clear();
        warmupOrigin.clear();
    }
}
