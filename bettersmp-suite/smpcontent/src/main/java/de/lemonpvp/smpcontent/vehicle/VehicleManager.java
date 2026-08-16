package de.lemonpvp.smpcontent.vehicle;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Fahrbare Fahrzeuge: Autos, Jets und Züge.
 *
 * Ein Fahrzeug besteht aus einem unsichtbaren Rüstungsständer (der fährt und
 * den man reitet) und einem ItemDisplay als Karosserie. Gelenkt wird mit den
 * ganz normalen Bewegungstasten - Paper verrät sie über
 * {@link Player#getCurrentInput()}, deshalb ist keine Extra-Mod nötig.
 *
 *   Auto   W/S Gas und Bremse, A/D lenken, fällt und fährt eine Stufe hoch
 *   Jet    W schiebt in Blickrichtung, Leertaste hoch, Schleichen runter
 *   Zug    fährt nur auf Schienen und folgt ihnen, auch um Kurven
 *
 * Alles läuft in einem einzigen Takt über alle Fahrzeuge - das ist billiger
 * als eine Aufgabe pro Fahrzeug.
 */
public final class VehicleManager {

    private final SMPContent plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey partKey;
    /** Zu welchem Fahrzeug ein Teil gehört - sonst greift man beim Nachbarn zu. */
    private final NamespacedKey ownerKey;
    private final Map<String, VehicleType> types = new LinkedHashMap<>();
    private final Map<UUID, Ride> active = new LinkedHashMap<>();
    private ConfigProblem.Report problem;

    public VehicleManager(SMPContent plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "vehicle");
        this.partKey = new NamespacedKey(plugin, "vehicle_part");
        this.ownerKey = new NamespacedKey(plugin, "vehicle_owner");
    }

    /** Ein Fahrzeug, das gerade in der Welt steht. */
    private static final class Ride {
        final VehicleType type;
        final ArmorStand base;
        final ItemDisplay body;
        float yaw;
        double speed;
        int fuelLeft;
        int soundTick;

        Ride(VehicleType type, ArmorStand base, ItemDisplay body, float yaw, int fuelLeft) {
            this.type = type;
            this.base = base;
            this.body = body;
            this.yaw = yaw;
            this.fuelLeft = fuelLeft;
        }
    }

    // ------------------------------------------------------------------
    //  Typen laden
    // ------------------------------------------------------------------

    public void load() {
        types.clear();
        problem = null;
        File file = new File(plugin.getDataFolder(), "fahrzeuge.yml");
        if (!file.exists()) {
            plugin.saveResource("fahrzeuge.yml", false);
        }
        ConfigProblem.Result result = ConfigProblem.load(file);
        if (!result.ok()) {
            problem = result.problem();
            ConfigProblem.log(plugin.getLogger(), problem);
            return;
        }
        YamlConfiguration config = result.config();
        ConfigurationSection section = config.getConfigurationSection("vehicles");
        if (section == null) {
            section = config.getConfigurationSection("fahrzeuge");
        }
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection one = section.getConfigurationSection(id);
            if (one != null) {
                types.put(id.toLowerCase(Locale.ROOT),
                        VehicleType.read(id.toLowerCase(Locale.ROOT), one));
            }
        }
    }

    public Map<String, VehicleType> types() {
        return types;
    }

    public ConfigProblem.Report problem() {
        return problem;
    }

    /** Welcher Fahrzeugtyp wird von diesem eigenen Item gesetzt? */
    public VehicleType byItem(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (VehicleType type : types.values()) {
            if (type.item().equalsIgnoreCase(itemId)) {
                return type;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Hinstellen und Einsteigen
    // ------------------------------------------------------------------

    public void spawn(VehicleType type, Location where, float yaw) {
        World world = where.getWorld();
        Location at = where.clone();
        at.setYaw(yaw);
        at.setPitch(0);

        ArmorStand base = world.spawn(at, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setGravity(type.kind() != VehicleType.Kind.JET);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(true);
            stand.setCustomNameVisible(false);
            stand.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.id());
            stand.getPersistentDataContainer().set(partKey, PersistentDataType.STRING, "base");
        });

        String owner = base.getUniqueId().toString();
        ItemDisplay body = world.spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(modelItem(type));
            display.setRotation(yaw, 0f);
            display.setPersistent(true);
            display.setInvulnerable(true);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(2);
            if (type.scale() != 1.0) {
                float scale = (float) type.scale();
                Transformation t = display.getTransformation();
                display.setTransformation(new Transformation(t.getTranslation(),
                        t.getLeftRotation(), new Vector3f(scale, scale, scale),
                        t.getRightRotation()));
            }
            display.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.id());
            display.getPersistentDataContainer().set(partKey, PersistentDataType.STRING, "body");
            display.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner);
        });

        world.spawn(at, Interaction.class, hitbox -> {
            hitbox.setInteractionWidth((float) type.width());
            hitbox.setInteractionHeight((float) type.height());
            hitbox.setResponsive(true);
            hitbox.setPersistent(true);
            hitbox.getPersistentDataContainer()
                    .set(typeKey, PersistentDataType.STRING, type.id());
            hitbox.getPersistentDataContainer()
                    .set(partKey, PersistentDataType.STRING, "seat");
            hitbox.getPersistentDataContainer()
                    .set(ownerKey, PersistentDataType.STRING, owner);
        });

        active.put(base.getUniqueId(), new Ride(type, base, body, yaw, type.range()));
    }

    private ItemStack modelItem(VehicleType type) {
        CustomEntry entry = plugin.registry().get(type.model());
        if (entry != null) {
            return plugin.registry().create(entry, 1);
        }
        return new ItemStack(Material.MINECART);
    }

    /** Einsteigen: der Spieler reitet den Rüstungsständer. */
    public boolean enter(Player player, Entity clicked) {
        String id = clicked.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (id == null) {
            return false;
        }
        ArmorStand base = baseNear(clicked);
        if (base == null) {
            return false;
        }
        Ride ride = active.get(base.getUniqueId());
        if (ride == null) {
            ride = adopt(base);
        }
        if (ride == null || base.getPassengers().size() >= ride.type.seats()) {
            return false;
        }
        return base.addPassenger(player);
    }

    /**
     * Der Rüstungsständer, der zu einem angeklickten Teil gehört.
     *
     * Über die gespeicherte Kennung, nicht über die Nähe: sonst steigt man
     * bei zwei dicht nebeneinander geparkten Autos ins falsche ein.
     */
    private ArmorStand baseNear(Entity part) {
        if (part instanceof ArmorStand stand) {
            return stand;
        }
        String owner = part.getPersistentDataContainer()
                .get(ownerKey, PersistentDataType.STRING);
        if (owner == null) {
            return null;
        }
        Entity found = Bukkit.getEntity(UUID.fromString(owner));
        return found instanceof ArmorStand stand ? stand : null;
    }

    /**
     * Ein Fahrzeug, das nach einem Neustart noch in der Welt steht, wieder in
     * den Takt aufnehmen.
     */
    private Ride adopt(ArmorStand base) {
        String id = base.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        VehicleType type = id == null ? null : types.get(id.toLowerCase(Locale.ROOT));
        if (type == null) {
            return null;
        }
        ItemDisplay body = null;
        String owner = base.getUniqueId().toString();
        for (Entity nearby : base.getWorld().getNearbyEntities(base.getLocation(), 3, 3, 3)) {
            if (nearby instanceof ItemDisplay display && owner.equals(display
                    .getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING))) {
                body = display;
                break;
            }
        }
        Ride ride = new Ride(type, base, body, base.getLocation().getYaw(), type.range());
        active.put(base.getUniqueId(), ride);
        return ride;
    }

    /** Beim Laden eines Chunks alle Fahrzeuge darin wieder aufnehmen. */
    public void adoptChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ArmorStand stand && "base".equals(stand
                    .getPersistentDataContainer().get(partKey, PersistentDataType.STRING))
                    && !active.containsKey(stand.getUniqueId())) {
                adopt(stand);
            }
        }
    }

    /** Fahrzeug einsammeln: Klick mit leerer Hand beim Schleichen. */
    public boolean pickUp(Player player, Entity clicked) {
        ArmorStand base = baseNear(clicked);
        if (base == null || !base.getPassengers().isEmpty()) {
            return false;
        }
        Ride ride = active.get(base.getUniqueId());
        if (ride == null) {
            ride = adopt(base);
        }
        if (ride == null) {
            return false;
        }
        CustomEntry item = plugin.registry().get(ride.type.item());
        if (item != null) {
            plugin.registry().give(player, item, 1);
        }
        remove(base);
        return true;
    }

    private void remove(ArmorStand base) {
        Ride ride = active.remove(base.getUniqueId());
        if (ride != null && ride.body != null) {
            ride.body.remove();
        }
        String owner = base.getUniqueId().toString();
        for (Entity nearby : base.getWorld().getNearbyEntities(base.getLocation(), 3, 3, 3)) {
            if (owner.equals(nearby.getPersistentDataContainer()
                    .get(ownerKey, PersistentDataType.STRING))) {
                nearby.remove();
            }
        }
        base.remove();
    }

    // ------------------------------------------------------------------
    //  Der Takt
    // ------------------------------------------------------------------

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        if (active.isEmpty()) {
            return;
        }
        List<UUID> gone = new ArrayList<>();
        for (Map.Entry<UUID, Ride> entry : active.entrySet()) {
            Ride ride = entry.getValue();
            if (!ride.base.isValid()) {
                gone.add(entry.getKey());
                continue;
            }
            drive(ride);
        }
        gone.forEach(active::remove);
    }

    private void drive(Ride ride) {
        Player driver = null;
        for (Entity passenger : ride.base.getPassengers()) {
            if (passenger instanceof Player player) {
                driver = player;
                break;
            }
        }

        if (driver == null) {
            // Niemand am Steuer: ausrollen
            ride.speed *= 0.8;
            if (Math.abs(ride.speed) < 0.01) {
                ride.speed = 0;
            }
        } else {
            steer(ride, driver);
        }

        Vector velocity = switch (ride.type.kind()) {
            case JET -> fly(ride, driver);
            case TRAIN -> rails(ride);
            case CAR -> roll(ride);
        };
        ride.base.setVelocity(velocity);
        ride.base.setRotation(ride.yaw, 0f);

        if (ride.body != null && ride.body.isValid()) {
            Location at = ride.base.getLocation();
            at.setYaw(ride.yaw);
            // Fliegendes nickt mit, Fahrendes bleibt waagerecht
            at.setPitch(ride.type.kind() == VehicleType.Kind.JET && driver != null
                    ? Math.max(-70, Math.min(70, driver.getLocation().getPitch())) : 0);
            ride.body.teleport(at);
        }
        // Alle halbe Sekunde reicht - zwanzigmal wäre Lärm und Last
        if (driver != null && ride.speed != 0 && !ride.type.sound().isBlank()
                && ++ride.soundTick % 10 == 0) {
            ride.base.getWorld().playSound(ride.base.getLocation(),
                    ride.type.sound(), 0.4f, 1.0f);
        }
    }

    /** Gas, Bremse und Lenkung aus den Bewegungstasten. */
    private void steer(Ride ride, Player driver) {
        Input input = driver.getCurrentInput();
        double max = ride.type.speed();
        if (input.isForward()) {
            ride.speed = Math.min(max, ride.speed + ride.type.power());
        } else if (input.isBackward()) {
            ride.speed = Math.max(-max * 0.4, ride.speed - ride.type.power());
        } else {
            ride.speed *= 0.94;
            if (Math.abs(ride.speed) < 0.01) {
                ride.speed = 0;
            }
        }
        // Lenken geht nur, solange man rollt - wie im echten Leben
        if (ride.speed != 0 && ride.type.kind() != VehicleType.Kind.TRAIN) {
            double factor = ride.speed < 0 ? -1 : 1;
            if (input.isLeft()) {
                ride.yaw -= ride.type.turn() * factor;
            }
            if (input.isRight()) {
                ride.yaw += ride.type.turn() * factor;
            }
        }
        if (ride.speed != 0) {
            burn(ride, driver);
        }
    }

    /** Sprit verbrauchen, wenn einer eingestellt ist. */
    private void burn(Ride ride, Player driver) {
        if (ride.type.fuel().isBlank()) {
            return;
        }
        if (ride.fuelLeft-- > 0) {
            return;
        }
        CustomEntry fuel = plugin.registry().get(ride.type.fuel());
        ItemStack need = fuel != null ? plugin.registry().create(fuel, 1)
                : new ItemStack(materialOr(ride.type.fuel(), Material.COAL), 1);
        if (driver.getInventory().containsAtLeast(need, 1)) {
            driver.getInventory().removeItem(need);
            ride.fuelLeft = ride.type.range();
        } else {
            ride.speed = 0;
            plugin.msgs().send(driver, "vehicle-empty");
        }
    }

    private static Material materialOr(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    /** Auto: Vortrieb waagerecht, Schwerkraft senkrecht, eine Stufe schafft es. */
    private Vector roll(Ride ride) {
        Vector forward = direction(ride.yaw).multiply(ride.speed);
        double y = ride.base.getVelocity().getY();
        if (ride.base.isOnGround()) {
            y = 0;
            if (ride.speed != 0 && blocked(ride) && free(ride)) {
                y = 0.42;   // eine Stufe hoch
            }
        }
        return new Vector(forward.getX(), y, forward.getZ());
    }

    /**
     * Fliegen.
     *
     * Gesteuert wird mit dem Kopf: Wohin du schaust, da geht die Nase hin -
     * nach oben ziehen steigt, nach unten drücken geht in den Sturzflug. Wie
     * stark, hängt vom Schub ab; im Stand nickt gar nichts. Leertaste und
     * Schleichen heben und senken zusätzlich gerade, das braucht man zum
     * Landen und beim Schweben.
     *
     * Ohne Schub sackt ein Flugzeug langsam durch. Ein Hubschrauber
     * ({@code hover: true}) bleibt stattdessen in der Luft stehen - das ist
     * der ganze Unterschied zwischen den beiden.
     */
    private Vector fly(Ride ride, Player driver) {
        Vector forward = direction(ride.yaw).multiply(ride.speed);
        double y = 0;
        if (driver != null) {
            Input input = driver.getCurrentInput();

            // Nase folgt dem Blick. In Minecraft ist oben ein negativer
            // Nickwinkel, darum das Minus.
            double pitch = Math.max(-70, Math.min(70, driver.getLocation().getPitch()));
            y = -Math.sin(Math.toRadians(pitch)) * ride.speed;
            // Was nach oben geht, fehlt vorne - sonst klettert man mit
            // voller Reisegeschwindigkeit senkrecht
            double rest = Math.max(0.2, Math.cos(Math.toRadians(pitch)));
            forward.multiply(rest);

            if (input.isJump()) {
                y += ride.type.speed() * 0.5;
            } else if (input.isSneak()) {
                y -= ride.type.speed() * 0.5;
            }
            if (ride.speed == 0 && !input.isJump() && !input.isSneak()) {
                y = ride.type.hover() ? 0 : -0.08;
            }
        } else {
            y = ride.type.hover() ? 0 : -0.15;
        }
        return new Vector(forward.getX(), y, forward.getZ());
    }

    /**
     * Zug: fährt nur, wenn unter ihm eine Schiene liegt, und folgt deren
     * Verlauf. Bei Kurven wird die Richtung genommen, die am ehesten der
     * bisherigen Fahrtrichtung entspricht - sonst würde der Zug umkehren.
     */
    private Vector rails(Ride ride) {
        Block block = ride.base.getLocation().getBlock();
        Rail rail = railAt(block);
        double lift = 0;
        if (rail == null) {
            rail = railAt(block.getRelative(0, -1, 0));
        }
        if (rail == null) {
            rail = railAt(block.getRelative(0, 1, 0));
            if (rail != null) {
                lift = 0.5;
            }
        }
        if (rail == null) {
            ride.speed *= 0.6;
            return new Vector(0, ride.base.getVelocity().getY(), 0);
        }

        Vector[] options = shapeDirections(rail);
        Vector heading = direction(ride.yaw);
        Vector best = options[0];
        double bestDot = -2;
        for (Vector option : options) {
            double dot = option.getX() * heading.getX() + option.getZ() * heading.getZ();
            if (dot > bestDot) {
                bestDot = dot;
                best = option;
            }
        }
        ride.yaw = (float) Math.toDegrees(Math.atan2(-best.getX(), best.getZ()));
        Vector move = best.clone().multiply(Math.abs(ride.speed));
        return new Vector(move.getX(), lift + move.getY(), move.getZ());
    }

    private static Rail railAt(Block block) {
        return block.getBlockData() instanceof Rail rail ? rail : null;
    }

    /** Beide Richtungen, in die eine Schienenform zeigt. */
    private static Vector[] shapeDirections(Rail rail) {
        return switch (rail.getShape()) {
            case NORTH_SOUTH -> new Vector[]{new Vector(0, 0, -1), new Vector(0, 0, 1)};
            case EAST_WEST -> new Vector[]{new Vector(1, 0, 0), new Vector(-1, 0, 0)};
            case ASCENDING_NORTH -> new Vector[]{new Vector(0, 0.5, -1), new Vector(0, -0.5, 1)};
            case ASCENDING_SOUTH -> new Vector[]{new Vector(0, 0.5, 1), new Vector(0, -0.5, -1)};
            case ASCENDING_EAST -> new Vector[]{new Vector(1, 0.5, 0), new Vector(-1, -0.5, 0)};
            case ASCENDING_WEST -> new Vector[]{new Vector(-1, 0.5, 0), new Vector(1, -0.5, 0)};
            case SOUTH_EAST -> new Vector[]{new Vector(0, 0, 1), new Vector(1, 0, 0)};
            case SOUTH_WEST -> new Vector[]{new Vector(0, 0, 1), new Vector(-1, 0, 0)};
            case NORTH_WEST -> new Vector[]{new Vector(0, 0, -1), new Vector(-1, 0, 0)};
            case NORTH_EAST -> new Vector[]{new Vector(0, 0, -1), new Vector(1, 0, 0)};
        };
    }

    /** Steht direkt vor dem Fahrzeug etwas Festes? */
    private boolean blocked(Ride ride) {
        Location front = ride.base.getLocation()
                .add(direction(ride.yaw).multiply(ride.speed < 0 ? -0.8 : 0.8));
        return front.getBlock().getType().isSolid();
    }

    /** Und ist darüber Platz, um hochzufahren? */
    private boolean free(Ride ride) {
        Location front = ride.base.getLocation()
                .add(direction(ride.yaw).multiply(ride.speed < 0 ? -0.8 : 0.8));
        return !front.getBlock().getRelative(0, 1, 0).getType().isSolid();
    }

    private static Vector direction(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vector(-Math.sin(radians), 0, Math.cos(radians));
    }
}
