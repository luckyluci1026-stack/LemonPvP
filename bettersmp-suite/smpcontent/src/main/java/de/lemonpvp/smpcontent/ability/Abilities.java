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
            case "projectile", "shoot", "schuss" -> projectile(player, action);
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

    /**
     * Schießt etwas ab - damit werden aus eigenen Items Pistolen, Gewehre,
     * Schrotflinten oder Raketenwerfer.
     *
     * <pre>
     * - type: projectile
     *   entity: ARROW      ARROW, SNOWBALL, FIREBALL, WIND_CHARGE, EGG, TRIDENT ...
     *   speed: 3.0         wie schnell
     *   damage: 7.0        Schaden (nur bei Pfeilen)
     *   spread: 1.5        Streuung in Grad
     *   amount: 8          mehrere auf einmal - eine Schrotflinte
     *   gravity: true      false = fliegt geradeaus
     *   fire: false        brennende Pfeile
     *   pierce: 0          wie viele Gegner ein Pfeil durchschlägt
     *   ammo: "smp:kugel"  Munition, die dabei verbraucht wird
     * </pre>
     */
    private void projectile(Player player, Map<String, Object> action) {
        String name = Ability.string(action, "entity", "ARROW")
                .toUpperCase(Locale.ROOT).trim();
        Class<? extends org.bukkit.entity.Entity> kind = switch (name) {
            case "SNOWBALL", "SCHNEEBALL" -> org.bukkit.entity.Snowball.class;
            case "EGG", "EI" -> org.bukkit.entity.Egg.class;
            case "FIREBALL", "FEUERBALL" -> org.bukkit.entity.Fireball.class;
            case "SMALL_FIREBALL" -> org.bukkit.entity.SmallFireball.class;
            case "WIND_CHARGE", "WINDSTOSS" -> org.bukkit.entity.WindCharge.class;
            case "TRIDENT", "DREIZACK" -> org.bukkit.entity.Trident.class;
            case "ENDER_PEARL", "ENDERPERLE" -> org.bukkit.entity.EnderPearl.class;
            case "SHULKER_BULLET" -> org.bukkit.entity.ShulkerBullet.class;
            case "LLAMA_SPIT" -> org.bukkit.entity.LlamaSpit.class;
            default -> org.bukkit.entity.Arrow.class;
        };

        // Munition: ist keine da, klickt es nur
        String ammo = Ability.string(action, "ammo", "");
        if (!ammo.isBlank() && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                && !takeAmmo(player, ammo)) {
            return;
        }

        int amount = Math.max(1, (int) Ability.number(action, "amount", 1));
        double speed = Ability.number(action, "speed", 2.5);
        double spread = Ability.number(action, "spread", 0.0);
        double damage = Ability.number(action, "damage", 0.0);
        boolean gravity = Ability.flag(action, "gravity", true);
        boolean fire = Ability.flag(action, "fire", false);
        int pierce = (int) Ability.number(action, "pierce", 0);
        // Eine Kugel bleibt nicht im Boden stecken. Pfeile tun das von Haus
        // aus, und dann steht vor einem eine Wand aus Pfeilen - genau das
        // sieht man beim Schiessen auf den Boden. "bullet: true" raeumt das
        // Geschoss beim Aufschlag weg und macht es unterwegs unsichtbar.
        boolean bullet = Ability.flag(action, "bullet", false);

        for (int i = 0; i < amount; i++) {
            Vector direction = player.getEyeLocation().getDirection();
            if (spread > 0) {
                double radians = Math.toRadians(spread);
                direction.add(new Vector(
                        (Math.random() - 0.5) * radians,
                        (Math.random() - 0.5) * radians,
                        (Math.random() - 0.5) * radians));
            }
            // Aus einem Fahrzeug heraus weiter vorn ansetzen: sonst steckt
            // das Geschoss sofort in der eigenen Karosserie fest, und der
            // Schuss geht scheinbar ins Leere.
            double abstand = player.getVehicle() != null ? 2.2 : 0.6;
            org.bukkit.entity.Entity shot = player.getWorld().spawn(
                    player.getEyeLocation().add(direction.clone().multiply(abstand)), kind);
            shot.setVelocity(direction.normalize().multiply(speed));
            if (shot instanceof org.bukkit.entity.Projectile projectile) {
                projectile.setShooter(player);
            }
            if (!gravity) {
                shot.setGravity(false);
            }
            if (fire) {
                shot.setFireTicks(200);
            }
            if (shot instanceof org.bukkit.entity.AbstractArrow arrow) {
                if (damage > 0) {
                    arrow.setDamage(damage);
                }
                arrow.setPierceLevel(Math.max(0, Math.min(127, pierce)));
                arrow.setPickupStatus(
                        org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
            }
            if (bullet) {
                shot.getPersistentDataContainer().set(KUGEL,
                        org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                shot.setInvisible(true);
                spurLegen(shot);
            }
        }
    }

    /** Merkzettel: das ist eine Kugel von uns und keine gewöhnliche Munition. */
    public static final org.bukkit.NamespacedKey KUGEL =
            new org.bukkit.NamespacedKey("smpcontent", "kugel");

    /**
     * Die Leuchtspur.
     *
     * Die Kugel selbst ist unsichtbar - ohne Spur sähe man gar nichts und
     * wüsste nie, wohin man geschossen hat. Zwei kleine Teilchen pro Tick
     * reichen für einen sauberen Strich und kosten nichts.
     */
    private void spurLegen(org.bukkit.entity.Entity shot) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!shot.isValid() || ++ticks > 100) {
                    cancel();
                    return;
                }
                shot.getWorld().spawnParticle(org.bukkit.Particle.CRIT,
                        shot.getLocation(), 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Nimmt ein Stück Munition aus dem Inventar. */
    private boolean takeAmmo(Player player, String ammo) {
        var entry = plugin.registry().get(ammo.startsWith("smp:") ? ammo.substring(4) : ammo);
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            org.bukkit.inventory.ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            boolean match = entry != null
                    ? entry.id().equals(plugin.registry().idOf(stack))
                    : stack.getType().name().equalsIgnoreCase(ammo);
            if (match) {
                stack.setAmount(stack.getAmount() - 1);
                return true;
            }
        }
        return false;
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
                if (istZiel(entity, player) && !hit.contains(entity)) {
                    hit.add(entity);
                    ((LivingEntity) entity).damage(amount, player);
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
        bewegen(player).setVelocity(push);
    }

    /**
     * Ist das überhaupt ein Ziel?
     *
     * Unsichtbare Rüstungsständer sind keine Gegner, sondern Technik: der
     * Sitz eines Stuhls, das Gerüst eines Fahrzeugs. Ein Druckstoß würde
     * sonst sämtliche Möbel und Autos der Umgebung wegschleudern - mitsamt
     * den Leuten, die darauf sitzen.
     */
    private static boolean istZiel(Entity entity, Player player) {
        if (entity == player || !(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof org.bukkit.entity.ArmorStand stand && !stand.isVisible()) {
            return false;
        }
        // Das eigene Fahrzeug und die Mitfahrer bleiben auch verschont
        return entity != player.getVehicle();
    }

    /**
     * Wer geschoben werden soll.
     *
     * Sitzt der Spieler in einem Fahrzeug, gehört der Schwung dorthin - ein
     * Mitfahrer kann sich selbst nicht bewegen, der Server setzt ihn einfach
     * wieder auf seinen Platz. Sprungstiefel im Auto täten sonst schlicht
     * nichts, ohne dass man je erführe warum.
     */
    private static Entity bewegen(Player player) {
        Entity fahrzeug = player.getVehicle();
        return fahrzeug != null ? fahrzeug : player;
    }

    /** Stößt alles in der Nähe weg (Richtung 1) oder zieht es heran (-1). */
    private void shove(Player player, Map<String, Object> action, int sign) {
        double radius = Ability.number(action, "radius", 4.0);
        double power = Ability.number(action, "power", 1.0);
        Location center = player.getLocation();
        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!istZiel(entity, player)) {
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
