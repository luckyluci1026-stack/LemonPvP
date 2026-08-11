package de.lemonpvp.smpcontent.ability;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.util.Text;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Spezialfähigkeiten für eigene Items: Auslöser, Abklingzeit und die
 * Aktionen, die dann passieren - alles aus der Konfiguration.
 */
public final class Abilities {

    private final SMPContent plugin;
    private final Animator animator;

    /** Item-Id -> Fähigkeiten */
    private final Map<String, List<Ability>> byItem = new HashMap<>();
    /** Name -> selbst gebaute Animation aus der animationen.yml */
    private final Map<String, CustomAnimation> animations = new HashMap<>();
    /** Spieler -> Fähigkeit -> wann sie wieder darf */
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public Abilities(SMPContent plugin) {
        this.plugin = plugin;
        this.animator = new Animator(plugin);
    }

    // ------------------------------------------------------------------
    //  Laden
    // ------------------------------------------------------------------

    public void clear() {
        byItem.clear();
        animations.clear();
    }

    /** Liest den Abschnitt "abilities" eines Items. */
    public void register(String id, Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        List<Ability> parsed = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                try {
                    parsed.add(Ability.from(Ability.normalise(map)));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Fähigkeit bei " + id + " fehlerhaft: "
                            + ex.getMessage());
                }
            }
        }
        if (!parsed.isEmpty()) {
            byItem.put(id.toLowerCase(Locale.ROOT), parsed);
        }
    }

    /** Nimmt die eigenen Animationen aus der animationen.yml auf. */
    public void registerAnimation(String name, Map<String, Object> raw) {
        try {
            animations.put(name.toLowerCase(Locale.ROOT), CustomAnimation.from(raw));
        } catch (Exception ex) {
            plugin.getLogger().warning("Animation '" + name + "' fehlerhaft: " + ex.getMessage());
        }
    }

    public int count() {
        return byItem.values().stream().mapToInt(List::size).sum();
    }

    public int animationCount() {
        return animations.size();
    }

    /** Gibt es überhaupt eine Fähigkeit mit diesem Auslöser? */
    public boolean hasTrigger(String trigger) {
        return byItem.values().stream()
                .flatMap(List::stream)
                .anyMatch(ability -> ability.trigger().equals(trigger));
    }

    // ------------------------------------------------------------------
    //  Auslösen
    // ------------------------------------------------------------------

    /** Führt alle Fähigkeiten dieses Items für den Auslöser aus. */
    public boolean run(Player player, CustomEntry entry, String trigger, LivingEntity target) {
        List<Ability> list = byItem.get(entry.id().toLowerCase(Locale.ROOT));
        if (list == null) {
            return false;
        }
        boolean any = false;
        for (int index = 0; index < list.size(); index++) {
            Ability ability = list.get(index);
            if (!ability.trigger().equals(trigger)) {
                continue;
            }
            if (!ability.permission().isEmpty() && !player.hasPermission(ability.permission())) {
                continue;
            }
            String key = entry.id() + "#" + index;
            long remaining = remainingCooldown(player, key);
            if (remaining > 0) {
                if (!ability.name().isEmpty()) {
                    player.sendActionBar(Text.mm("<gray>" + ability.name()
                            + " ist bereit in <white>" + (remaining / 1000 + 1) + "s</white>"));
                }
                continue;
            }
            startCooldown(player, key, ability.cooldownSeconds());
            for (Map<String, Object> action : ability.actions()) {
                try {
                    perform(player, target, action);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Aktion '" + Ability.string(action, "type", "?")
                            + "' bei " + entry.id() + " fehlgeschlagen: " + ex);
                }
            }
            any = true;
        }
        return any;
    }

    // ------------------------------------------------------------------
    //  Die einzelnen Aktionen
    // ------------------------------------------------------------------

    private void perform(Player player, LivingEntity target, Map<String, Object> action) {
        String type = Ability.string(action, "type", "").toLowerCase(Locale.ROOT);
        switch (type) {
            case "animation" -> animation(player, action);
            case "sound" -> sound(player, action);
            case "potion" -> potion(player, target, action);
            case "damage" -> damage(player, target, action);
            case "damage-beam" -> damageBeam(player, action);
            case "heal" -> heal(player, action);
            case "launch" -> launch(player, action);
            case "push" -> shove(player, action, 1);
            case "pull" -> shove(player, action, -1);
            case "lightning" -> lightning(player, target, action);
            case "ignite" -> ignite(player, target, action);
            case "explosion" -> explosion(player, target, action);
            case "message" -> message(player, action);
            default -> plugin.getLogger().warning("Unbekannte Aktion: " + type);
        }
    }

    private void animation(Player player, Map<String, Object> action) {
        Particle particle = Animator.particleByName(Ability.string(action, "particle", "DUST"));
        if (particle == null) {
            plugin.getLogger().warning("Unbekanntes Teilchen: "
                    + Ability.string(action, "particle", ""));
            return;
        }
        double size = Ability.number(action, "size", 1.0);
        Object data = Animator.dataFor(particle, Ability.string(action, "color", "#FFFFFF"), size);
        double radius = Ability.number(action, "radius", 2.0);
        double length = Ability.number(action, "length", 10.0);
        int ticks = (int) Ability.number(action, "ticks", 10);
        int density = (int) Ability.number(action, "density", 2);

        String shape = Ability.string(action, "shape", "ring");
        CustomAnimation own = animations.get(shape.toLowerCase(Locale.ROOT));
        if (own != null) {
            animator.playCustom(player, own, particle, data, radius, length, ticks);
        } else {
            animator.play(player, shape, particle, data, radius, length, ticks, density);
        }
    }

    private void sound(Player player, Map<String, Object> action) {
        String name = Ability.string(action, "sound", "");
        if (name.isEmpty()) {
            return;
        }
        player.getWorld().playSound(player.getLocation(), name,
                (float) Ability.number(action, "volume", 1.0),
                (float) Ability.number(action, "pitch", 1.0));
    }

    private void potion(Player player, LivingEntity target, Map<String, Object> action) {
        PotionEffectType effect = Registry.EFFECT.get(
                NamespacedKey.minecraft(Ability.string(action, "effect", "speed")
                        .toLowerCase(Locale.ROOT)));
        if (effect == null) {
            plugin.getLogger().warning("Unbekannter Effekt: "
                    + Ability.string(action, "effect", ""));
            return;
        }
        LivingEntity who = resolve(player, target, action);
        if (who == null) {
            return;
        }
        who.addPotionEffect(new PotionEffect(effect,
                (int) (Ability.number(action, "duration", 5) * 20),
                Math.max(0, (int) Ability.number(action, "amplifier", 0) - 1)));
    }

    private void damage(Player player, LivingEntity target, Map<String, Object> action) {
        LivingEntity who = resolve(player, target, action);
        if (who != null && who != player) {
            who.damage(Ability.number(action, "amount", 2.0), player);
        }
    }

    /** Trifft alles, was auf der Blicklinie steht. */
    private void damageBeam(Player player, Map<String, Object> action) {
        double range = Ability.number(action, "range", 12);
        double amount = Ability.number(action, "amount", 4.0);
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        List<Entity> hit = new ArrayList<>();

        for (double d = 0; d <= range; d += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(d));
            if (point.getBlock().getType().isOccluding()) {
                break;
            }
            for (Entity entity : point.getWorld().getNearbyEntities(point, 0.8, 0.8, 0.8)) {
                if (entity != player && entity instanceof LivingEntity living
                        && !hit.contains(entity)) {
                    hit.add(entity);
                    living.damage(amount, player);
                }
            }
        }
    }

    private void heal(Player player, Map<String, Object> action) {
        double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? 20.0
                : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(max, player.getHealth() + Ability.number(action, "amount", 4.0)));
    }

    private void launch(Player player, Map<String, Object> action) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector push = direction.multiply(Ability.number(action, "forward", 1.5));
        push.setY(push.getY() + Ability.number(action, "up", 0.4));
        player.setVelocity(push);
    }

    /** Stößt alles in der Nähe weg (Richtung 1) oder zieht es heran (-1). */
    private void shove(Player player, Map<String, Object> action, int sign) {
        double radius = Ability.number(action, "radius", 4.0);
        double power = Ability.number(action, "power", 1.0);
        Location center = player.getLocation();
        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity == player || !(entity instanceof LivingEntity)) {
                continue;
            }
            Vector away = entity.getLocation().toVector().subtract(center.toVector());
            if (away.lengthSquared() < 0.01) {
                continue;
            }
            away.normalize().multiply(power * sign);
            away.setY(Math.max(0.25, away.getY()) * (sign > 0 ? 1 : 0.4));
            entity.setVelocity(away);
        }
    }

    private void lightning(Player player, LivingEntity target, Map<String, Object> action) {
        Location at = target != null
                ? target.getLocation()
                : lookTarget(player, Ability.number(action, "range", 20));
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        if (Ability.flag(action, "damage", true)) {
            world.strikeLightning(at);
        } else {
            world.strikeLightningEffect(at);
        }
    }

    private void ignite(Player player, LivingEntity target, Map<String, Object> action) {
        LivingEntity who = resolve(player, target, action);
        if (who != null && who != player) {
            who.setFireTicks((int) (Ability.number(action, "seconds", 3) * 20));
        }
    }

    private void explosion(Player player, LivingEntity target, Map<String, Object> action) {
        Location at = target != null
                ? target.getLocation()
                : lookTarget(player, Ability.number(action, "range", 12));
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        // Standardmäßig OHNE Blockschaden - sonst zerlegt es die Bauten
        world.createExplosion(at, (float) Ability.number(action, "power", 2.0),
                Ability.flag(action, "fire", false),
                Ability.flag(action, "break-blocks", false), player);
    }

    private void message(Player player, Map<String, Object> action) {
        Location at = player.getLocation();
        String text = Ability.string(action, "text", "")
                .replace("%x%", String.valueOf(at.getBlockX()))
                .replace("%y%", String.valueOf(at.getBlockY()))
                .replace("%z%", String.valueOf(at.getBlockZ()))
                .replace("%world%", at.getWorld() == null ? "?" : at.getWorld().getName())
                .replace("%player%", player.getName());
        if (text.isEmpty()) {
            return;
        }
        if (Ability.flag(action, "actionbar", false)) {
            player.sendActionBar(Text.mm(text));
        } else {
            player.sendMessage(Text.mm(text));
        }
    }

    // ------------------------------------------------------------------
    //  Helfer
    // ------------------------------------------------------------------

    /** "self" = der Spieler, "victim" = das getroffene Ziel. */
    private LivingEntity resolve(Player player, LivingEntity target, Map<String, Object> action) {
        String who = Ability.string(action, "target", "self").toLowerCase(Locale.ROOT);
        return switch (who) {
            case "victim", "ziel", "gegner" -> target;
            default -> player;
        };
    }

    /** Der Punkt, auf den der Spieler schaut (oder das Ende der Reichweite). */
    private Location lookTarget(Player player, double range) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        for (double d = 0; d <= range; d += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(d));
            if (point.getBlock().getType().isOccluding()) {
                return point;
            }
        }
        return eye.add(direction.multiply(range));
    }

    private long remainingCooldown(Player player, String key) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0;
        }
        Long until = map.get(key);
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    private void startCooldown(Player player, String key, int seconds) {
        if (seconds <= 0) {
            return;
        }
        cooldowns.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    public void forget(UUID player) {
        cooldowns.remove(player);
    }
}
