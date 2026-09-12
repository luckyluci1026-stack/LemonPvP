package de.lemonpvp.smpcontent.vehicle;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Der Fallschirm.
 *
 * Er hängt als Anzeige-Objekt über dem Spieler und bremst den Fall auf ein
 * gemütliches Tempo. Gelenkt wird mit den normalen Bewegungstasten - schräg
 * gegen den Wind kommt man erstaunlich weit.
 *
 * Aufgehen kann er auf zwei Wegen:
 *   - aus dem Schleudersitz heraus, wenn man zweimal schnell schleicht
 *   - von Hand, mit Rechtsklick auf das Fallschirm-Item im Fall
 *
 * Alle Werte (Sinkgeschwindigkeit, Lenkung, Öffnungsverzögerung, welches
 * Item als Schirm gezeigt wird) stehen in der fahrzeuge.yml.
 */
public final class ParachuteManager {

    private final SMPContent plugin;
    private final Map<UUID, Chute> open = new HashMap<>();

    public ParachuteManager(SMPContent plugin) {
        this.plugin = plugin;
    }

    private record Chute(ItemDisplay canopy, VehicleType.Eject settings) {
    }

    public boolean isOpen(Player player) {
        return open.containsKey(player.getUniqueId());
    }

    /**
     * Öffnet den Schirm - je nach Einstellung erst ein paar Ticks später,
     * damit man erst noch ein Stück fliegt.
     */
    public void deploy(Player player, VehicleType.Eject settings) {
        if (settings.parachute().isBlank() || isOpen(player)) {
            return;
        }
        if (settings.openAfter() <= 0) {
            spawn(player, settings);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> spawn(player, settings), settings.openAfter());
        }
    }

    private void spawn(Player player, VehicleType.Eject settings) {
        if (!player.isOnline() || player.isOnGround() || isOpen(player)) {
            return;
        }
        ItemStack model = model(settings.parachute());
        Location at = player.getLocation();
        ItemDisplay canopy = player.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(model);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTeleportDuration(2);
            // Groß und über dem Kopf - ein Schirm ist kein Hut
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 1.6f, 0f), t.getLeftRotation(),
                    new Vector3f(2.6f, 2.6f, 2.6f), t.getRightRotation()));
        });
        if (!player.addPassenger(canopy)) {
            canopy.remove();
            return;
        }
        open.put(player.getUniqueId(), new Chute(canopy, settings));
        player.getWorld().playSound(player.getLocation(), "item.elytra.flying", 0.7f, 1.4f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.4, 0.2, 0.4, 0.01);
    }

    /** Ein eigenes Item als Schirm, sonst wenigstens weiße Wolle. */
    private ItemStack model(String id) {
        CustomEntry entry = plugin.registry().get(id);
        if (entry != null) {
            return plugin.registry().create(entry, 1);
        }
        return new ItemStack(Material.WHITE_WOOL);
    }

    /** Jeden Tick: bremsen, lenken, und beim Aufsetzen wieder einpacken. */
    public void tick() {
        if (open.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Chute>> it = open.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Chute> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            Chute chute = entry.getValue();
            if (player == null || !player.isOnline() || !chute.canopy().isValid()) {
                chute.canopy().remove();
                it.remove();
                continue;
            }
            // Angekommen - oder eingestiegen: Schirm weg, und der Sturz zählt nicht
            if (player.isOnGround() || player.isInWater() || player.isGliding()
                    || player.getVehicle() != null) {
                player.setFallDistance(0);
                chute.canopy().remove();
                it.remove();
                continue;
            }

            VehicleType.Eject settings = chute.settings();
            Vector velocity = player.getVelocity();
            double x = velocity.getX() * 0.86;
            double z = velocity.getZ() * 0.86;
            Input input = player.getCurrentInput();
            if (settings.drift() > 0) {
                Vector look = player.getLocation().getDirection().setY(0).normalize();
                if (input.isForward()) {
                    x += look.getX() * settings.drift();
                    z += look.getZ() * settings.drift();
                }
                if (input.isBackward()) {
                    x -= look.getX() * settings.drift() * 0.6;
                    z -= look.getZ() * settings.drift() * 0.6;
                }
                Vector side = new Vector(-look.getZ(), 0, look.getX());
                if (input.isLeft()) {
                    x += side.getX() * settings.drift();
                    z += side.getZ() * settings.drift();
                }
                if (input.isRight()) {
                    x -= side.getX() * settings.drift();
                    z -= side.getZ() * settings.drift();
                }
            }
            player.setVelocity(new Vector(x, -settings.fallSpeed(), z));
            player.setFallDistance(0);
        }
    }

    /** Schirm von Hand einholen (oder beim Verlassen des Servers aufräumen). */
    public void close(Player player) {
        Chute chute = open.remove(player.getUniqueId());
        if (chute != null) {
            chute.canopy().remove();
            player.setFallDistance(0);
        }
    }

    /** Beim Herunterfahren: alle Schirme einholen. */
    public void clear() {
        for (Chute chute : open.values()) {
            // Erst absteigen lassen, dann entfernen - sonst bleibt beim
            // Spieler ein Reiter hängen, den niemand mehr wegnimmt.
            chute.canopy().leaveVehicle();
            chute.canopy().remove();
        }
        open.clear();
    }
}
