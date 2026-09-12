package de.lemonpvp.smpcontent.ability;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Die Animationen für Spezialfähigkeiten.
 *
 * Jede Form läuft über mehrere Ticks, damit es sich bewegt und nicht nur
 * einmal aufblitzt. Alle Werte kommen aus der config.yml.
 */
public final class Animator {

    private final Plugin plugin;

    public Animator(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spielt eine selbst gebaute Animation aus der animationen.yml ab.
     * Die drei Formeln sagen für jedes Teilchen, wo es hinkommt.
     */
    public void playCustom(Player player, CustomAnimation animation, Particle particle,
                           Object data, double radius, double length, int ticks) {
        playCustom(player::getLocation, player.getEyeHeight(),
                animation, particle, data, radius, length, ticks);
    }

    /**
     * Dieselbe Animation, aber an einem beliebigen Ort statt an einem Spieler.
     *
     * Das braucht alles, was kein Spieler ist und trotzdem etwas zeigen soll -
     * ein Boss zum Beispiel. Der "ort" wird bei {@code follow: true} jeden
     * Tick neu gefragt, sonst nur einmal am Anfang; die Blickrichtung für
     * {@code relative-to: look} steckt im Yaw/Pitch dieser Position.
     */
    public void playCustom(java.util.function.Supplier<Location> ort, double augenhoehe,
                           CustomAnimation animation, Particle particle,
                           Object data, double radius, double length, int ticks) {
        int duration = Math.max(1, Math.min(200, ticks));
        Location start = ort.get().clone();
        Map<String, Double> vars = new HashMap<>();
        vars.put("ticks", (double) duration);
        vars.put("n", (double) animation.points());
        vars.put("radius", radius);
        vars.put("length", length);

        repeat(duration, tick -> {
            Location jetzt = animation.follow() ? ort.get().clone() : start.clone();
            Location origin = jetzt.clone();
            if (animation.frame() != CustomAnimation.Frame.PLAYER) {
                origin.add(0, augenhoehe, 0);
            }
            // Achsen für "relative-to: look": vorne, rechts, oben
            Vector forward = (animation.follow() ? jetzt : start).getDirection().normalize();
            Vector right = new Vector(-forward.getZ(), 0, forward.getX());
            if (right.lengthSquared() < 1.0E-6) {
                right = new Vector(1, 0, 0);
            }
            right.normalize();
            Vector up = right.clone().crossProduct(forward).normalize();

            vars.put("t", (double) tick);
            vars.put("p", tick / (double) duration);
            for (int i = 0; i < animation.points(); i++) {
                vars.put("i", (double) i);
                vars.put("a", 2 * Math.PI * i / animation.points());
                double x = animation.x().eval(vars);
                double y = animation.y().eval(vars);
                double z = animation.z().eval(vars);

                Location point = animation.frame() == CustomAnimation.Frame.LOOK
                        ? origin.clone()
                        .add(right.clone().multiply(x))
                        .add(up.clone().multiply(y))
                        .add(forward.clone().multiply(z))
                        : origin.clone().add(x, y, z);
                spawn(point, particle, data);
            }
        });
    }

    /** Startet eine Animation. Unbekannte Formen werden übersprungen. */
    public void play(Player player, String shape, Particle particle, Object data,
                     double radius, double length, int ticks, int density) {
        int duration = Math.max(1, Math.min(200, ticks));
        switch (shape.toLowerCase(Locale.ROOT)) {
            case "beam" -> beam(player, particle, data, length, duration, density);
            case "ring", "shockwave" -> ring(player, particle, data, radius, duration, density);
            case "spiral", "helix" -> spiral(player, particle, data, radius, duration, density);
            case "sphere", "burst" -> sphere(player, particle, data, radius, duration, density);
            case "slash" -> slash(player, particle, data, radius, duration, density);
            case "orbit" -> orbit(player, particle, data, radius, duration, density);
            case "trail" -> trail(player, particle, data, duration, density);
            default -> plugin.getLogger().warning("Unbekannte Animation: " + shape);
        }
    }

    // ------------------------------------------------------------------
    //  Formen
    // ------------------------------------------------------------------

    /** Strahl nach vorne, der über die Dauer immer weiter reicht. */
    private void beam(Player player, Particle particle, Object data,
                      double length, int ticks, int density) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double step = 1.0 / Math.max(1, density);

        repeat(ticks, tick -> {
            double reach = length * (tick + 1) / (double) ticks;
            for (double d = 0; d <= reach; d += step) {
                Location point = eye.clone().add(direction.clone().multiply(d));
                if (point.getBlock().getType().isOccluding()) {
                    break;
                }
                spawn(point, particle, data);
            }
        });
    }

    /** Ring am Boden, der nach außen wächst. */
    private void ring(Player player, Particle particle, Object data,
                      double radius, int ticks, int density) {
        Location center = player.getLocation().clone().add(0, 0.2, 0);
        int points = Math.max(8, density * 12);

        repeat(ticks, tick -> {
            double current = radius * (tick + 1) / (double) ticks;
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                spawn(center.clone().add(Math.cos(angle) * current, 0,
                        Math.sin(angle) * current), particle, data);
            }
        });
    }

    /** Doppelte Helix, die am Spieler hochläuft. */
    private void spiral(Player player, Particle particle, Object data,
                        double radius, int ticks, int density) {
        Location base = player.getLocation().clone();
        double rise = 2.2 / ticks;

        repeat(ticks, tick -> {
            for (int i = 0; i < Math.max(1, density); i++) {
                double progress = tick + i / (double) Math.max(1, density);
                double angle = progress * 0.7;
                double height = progress * rise;
                spawn(base.clone().add(Math.cos(angle) * radius, height,
                        Math.sin(angle) * radius), particle, data);
                spawn(base.clone().add(Math.cos(angle + Math.PI) * radius, height,
                        Math.sin(angle + Math.PI) * radius), particle, data);
            }
        });
    }

    /** Kugel, die sich vom Spieler weg aufbläht. */
    private void sphere(Player player, Particle particle, Object data,
                        double radius, int ticks, int density) {
        Location center = player.getLocation().clone().add(0, 1, 0);
        int points = Math.max(20, density * 30);

        repeat(ticks, tick -> {
            double current = radius * (tick + 1) / (double) ticks;
            for (int i = 0; i < points; i++) {
                // Gleichmäßig verteilte Punkte auf der Kugel (Goldener Winkel)
                double y = 1 - 2.0 * i / (points - 1.0);
                double ring = Math.sqrt(Math.max(0, 1 - y * y));
                double angle = Math.PI * (3 - Math.sqrt(5)) * i;
                spawn(center.clone().add(Math.cos(angle) * ring * current, y * current,
                        Math.sin(angle) * ring * current), particle, data);
            }
        });
    }

    /** Schwungbogen vor dem Spieler - passt zu Schwertern. */
    private void slash(Player player, Particle particle, Object data,
                       double radius, int ticks, int density) {
        Location eye = player.getEyeLocation();
        double yaw = Math.toRadians(eye.getYaw());
        int points = Math.max(10, density * 10);

        repeat(ticks, tick -> {
            double sweep = Math.PI * 1.1;
            double start = -sweep / 2 + sweep * tick / (double) ticks;
            for (int i = 0; i < points; i++) {
                double angle = start + sweep * i / points / (double) ticks;
                double x = -Math.sin(yaw + angle) * radius;
                double z = Math.cos(yaw + angle) * radius;
                double y = Math.cos(angle) * 0.6 - 0.2;
                spawn(eye.clone().add(x, y, z), particle, data);
            }
        });
    }

    /** Teilchen kreisen um den Spieler und folgen ihm. */
    private void orbit(Player player, Particle particle, Object data,
                       double radius, int ticks, int density) {
        int points = Math.max(2, density);

        repeat(ticks, tick -> {
            Location center = player.getLocation().clone().add(0, 1, 0);
            for (int i = 0; i < points; i++) {
                double angle = tick * 0.35 + 2 * Math.PI * i / points;
                spawn(center.clone().add(Math.cos(angle) * radius,
                        Math.sin(tick * 0.2) * 0.3, Math.sin(angle) * radius), particle, data);
            }
        });
    }

    /** Spur an den Füßen, solange die Animation läuft. */
    private void trail(Player player, Particle particle, Object data, int ticks, int density) {
        repeat(ticks, tick -> {
            Location at = player.getLocation().clone().add(0, 0.1, 0);
            for (int i = 0; i < Math.max(1, density); i++) {
                spawn(at.clone().add((Math.random() - 0.5) * 0.6, 0,
                        (Math.random() - 0.5) * 0.6), particle, data);
            }
        });
    }

    // ------------------------------------------------------------------

    private void repeat(int ticks, java.util.function.IntConsumer frame) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= ticks) {
                    cancel();
                    return;
                }
                frame.accept(tick);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawn(Location at, Particle particle, Object data) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        if (data == null) {
            world.spawnParticle(particle, at, 1, 0, 0, 0, 0);
        } else {
            world.spawnParticle(particle, at, 1, 0, 0, 0, 0, data);
        }
    }

    // ------------------------------------------------------------------
    //  Teilchen aus der Konfiguration
    // ------------------------------------------------------------------

    /** "DUST", "END_ROD", "FLAME" ... - unbekannte Namen ergeben null. */
    public static Particle particleByName(String name) {
        if (name == null || name.isEmpty()) {
            return Particle.DUST;
        }
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Manche Teilchen brauchen Zusatzdaten - DUST zum Beispiel die Farbe.
     * Ohne passende Daten würde spawnParticle sonst eine Exception werfen.
     */
    public static Object dataFor(Particle particle, String color, double size) {
        Class<?> type = particle.getDataType();
        if (type == Void.class) {
            return null;
        }
        if (Particle.DustOptions.class.isAssignableFrom(type)) {
            return new Particle.DustOptions(parseColor(color), (float) Math.max(0.1, size));
        }
        if (Particle.DustTransition.class.isAssignableFrom(type)) {
            Color from = parseColor(color);
            return new Particle.DustTransition(from, Color.WHITE, (float) Math.max(0.1, size));
        }
        // Alles andere (Block-/Item-Daten) wird nicht unterstützt
        return null;
    }

    public static Color parseColor(String value) {
        if (value == null || value.isEmpty()) {
            return Color.WHITE;
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        try {
            return Color.fromRGB(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException ex) {
            return Color.WHITE;
        }
    }
}
