package de.lemonpvp.bettersmp.home;

import de.lemonpvp.bettersmp.BetterSMP;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HomeTeleport implements Listener {

    private static final Set<Material> GEFAEHRLICH = Set.of(Material.LAVA, Material.FIRE, Material.SOUL_FIRE,
            Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW, Material.WITHER_ROSE, Material.POINTED_DRIPSTONE);

    private record Warten(Home home, Location start, BukkitTask aufgabe) {
    }

    private final BetterSMP plugin;
    private final Map<UUID, Warten> wartend = new HashMap<>();

    public HomeTeleport(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public boolean wartet(UUID spieler) {
        return wartend.containsKey(spieler);
    }

    public void starten(Player spieler, Home home) {
        abbrechen(spieler.getUniqueId());
        if (gesperrt(spieler)) {
            return;
        }
        if (home.ort() == null) {
            plugin.msgs().send(spieler, "homes.welt-fehlt", "name", home.name());
            return;
        }
        int sekunden = spieler.hasPermission("bettersmp.homes.sofort") ? 0
                : Math.max(0, Math.min(30, plugin.getConfig().getInt("homes.aufwaermen-sekunden", 3)));
        if (sekunden == 0) {
            teleportieren(spieler, home);
            return;
        }
        plugin.msgs().send(spieler, "homes.warten", "name", home.name(), "sekunden", String.valueOf(sekunden));
        UUID id = spieler.getUniqueId();
        BukkitTask aufgabe = new BukkitRunnable() {
            private int rest = sekunden;

            @Override
            public void run() {
                if (!spieler.isOnline()) {
                    abbrechen(id);
                    return;
                }
                if (rest <= 0) {
                    wartend.remove(id);
                    cancel();
                    teleportieren(spieler, home);
                    return;
                }
                spieler.sendActionBar(plugin.msgs().format("homes.countdown", "sekunden", String.valueOf(rest)));
                rest--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        wartend.put(id, new Warten(home, spieler.getLocation(), aufgabe));
    }

    public void abbrechen(UUID spieler) {
        Warten warten = wartend.remove(spieler);
        if (warten != null) {
            warten.aufgabe().cancel();
        }
    }

    private boolean gesperrt(Player spieler) {
        UUID id = spieler.getUniqueId();
        if (plugin.combat() != null && plugin.combat().isTagged(id)) {
            long sekunden = Math.max(1, (plugin.combat().remainingMillis(id) + 999) / 1000);
            plugin.msgs().send(spieler, "homes.im-kampf", "sekunden", String.valueOf(sekunden));
            return true;
        }
        if (plugin.freeze() != null && plugin.freeze().istEingefroren(id)) {
            plugin.msgs().send(spieler, "freeze.command-blocked");
            return true;
        }
        return false;
    }

    private void teleportieren(Player spieler, Home home) {
        if (!spieler.isOnline() || gesperrt(spieler)) {
            return;
        }
        Location ziel = home.ort();
        if (ziel == null) {
            plugin.msgs().send(spieler, "homes.welt-fehlt", "name", home.name());
            return;
        }
        ziel.getWorld().getChunkAtAsync(ziel).thenAccept(chunk -> {
            if (!spieler.isOnline()) {
                return;
            }
            Location sicher = sichererOrt(ziel);
            if (sicher == null) {
                plugin.msgs().send(spieler, "homes.unsicher", "name", home.name());
                return;
            }
            spieler.teleportAsync(sicher, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept(geklappt -> {
                if (geklappt && spieler.isOnline()) {
                    plugin.msgs().send(spieler, "homes.angekommen", "name", home.name());
                    spieler.playSound(Sound.sound(Key.key("minecraft", "entity.enderman.teleport"), Sound.Source.PLAYER, 0.8f, 1.2f),
                            Sound.Emitter.self());
                }
            });
        });
    }

    static Location sichererOrt(Location ziel) {
        World welt = ziel.getWorld();
        int x = ziel.getBlockX();
        int z = ziel.getBlockZ();
        int y = ziel.getBlockY();
        if (ortOk(welt, x, y, z, ziel.getY() - y > 0.01)) {
            return ziel.clone();
        }
        for (int abstand = 1; abstand <= 10; abstand++) {
            for (int richtung : new int[]{1, -1}) {
                int neuY = y + abstand * richtung;
                if (ortOk(welt, x, neuY, z, false)) {
                    Location ort = ziel.clone();
                    ort.setX(x + 0.5);
                    ort.setY(neuY);
                    ort.setZ(z + 0.5);
                    return ort;
                }
            }
        }
        return null;
    }

    private static boolean ortOk(World welt, int x, int y, int z, boolean aufTeilblock) {
        if (y <= welt.getMinHeight() || y + 1 >= welt.getMaxHeight()) {
            return false;
        }
        Block fuesse = welt.getBlockAt(x, y, z);
        Block kopf = welt.getBlockAt(x, y + 1, z);
        Block boden = welt.getBlockAt(x, y - 1, z);
        if (!frei(kopf) || GEFAEHRLICH.contains(fuesse.getType()) || GEFAEHRLICH.contains(boden.getType())) {
            return false;
        }
        if (aufTeilblock) {
            return !fuesse.getType().isOccluding();
        }
        return frei(fuesse) && (boden.getType().isSolid() || boden.getType() == Material.WATER);
    }

    private static boolean frei(Block block) {
        return block.isPassable() && !GEFAEHRLICH.contains(block.getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimBewegen(PlayerMoveEvent event) {
        Warten warten = wartend.get(event.getPlayer().getUniqueId());
        if (warten == null) {
            return;
        }
        Location von = warten.start();
        Location nach = event.getTo();
        if (nach.getWorld() != von.getWorld() || nach.distanceSquared(von) > 0.25) {
            abbrechen(event.getPlayer().getUniqueId());
            plugin.msgs().send(event.getPlayer(), "homes.abgebrochen");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimSchaden(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player spieler && wartend.containsKey(spieler.getUniqueId())) {
            abbrechen(spieler.getUniqueId());
            plugin.msgs().send(spieler, "homes.abgebrochen-schaden");
        }
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        abbrechen(event.getPlayer().getUniqueId());
    }
}
