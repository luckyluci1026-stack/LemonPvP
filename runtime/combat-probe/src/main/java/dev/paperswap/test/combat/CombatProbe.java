package dev.paperswap.test.combat;

import com.destroystokyo.paper.event.player.PlayerAttackEntityCooldownResetEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Measures what a player attack does on the running server, with a server-side test player
 * (a {@code ServerPlayer} that is not logged in and not in the player list). No client is
 * involved; the attack methods the packet handlers call are called directly.
 *
 * <p>Scenarios (each with fresh target pigs that have 1000 health, no AI and no gravity):</p>
 * <ol>
 *     <li>spear jab through two targets ({@code PiercingWeapon.attack}): vanilla damages both
 *     with the same attack strength; Paper 1.21.11 resets the cooldown while damaging the
 *     first, so the second gets about 20 %;</li>
 *     <li>spear jab that hits nothing: vanilla resets the attack cooldown;</li>
 *     <li>sword attack ({@code Player.attack}): damage and cooldown reset (unchanged by the fix);</li>
 *     <li>attribute swap sword -&gt; spear in the same tick without an equipment update
 *     (vanilla, and Paper with {@code update-equipment-on-player-actions: false}): the jab
 *     uses the sword's attack damage; both targets must get it;</li>
 *     <li>the same with the equipment update Paper does by default on a slot change.</li>
 * </ol>
 *
 * <p>Uses reflection on the Mojang-mapped server classes of Minecraft 1.21.11. Tests only.</p>
 */
public final class CombatProbe extends JavaPlugin implements Listener {

    private static final UUID PROBE_ID = UUID.fromString("00000000-0000-4000-8000-00000000c0b3");
    private static final double HEALTH = 1000.0;

    private int resetEvents;
    private Nms nms;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        var command = getCommand("combatprobe");
        if (command != null) {
            command.setExecutor((sender, cmd, label, args) -> {
                run(sender);
                return true;
            });
        }
    }

    /** @param event counted for the test player only */
    @EventHandler
    public void onCooldownReset(PlayerAttackEntityCooldownResetEvent event) {
        if (event.getPlayer().getUniqueId().equals(PROBE_ID)) {
            this.resetEvents++;
        }
    }

    private void say(CommandSender sender, String line) {
        sender.sendMessage("[CombatProbe] " + line);
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            getLogger().info(line);
        }
    }

    private void run(CommandSender sender) {
        try {
            if (this.nms == null) {
                this.nms = new Nms();
            }
            World world = getServer().getWorlds().get(0);
            Location spawn = world.getSpawnLocation();
            double y = world.getHighestBlockYAt(spawn) + 20.0;
            Location origin = new Location(world, spawn.getBlockX() + 0.5, y, spawn.getBlockZ() + 0.5);
            Object player = this.nms.testPlayer(world);

            Jab jab = jab(player, origin, Material.IRON_SPEAR, false, 2);
            say(sender, String.format(Locale.ROOT, "spear jab, two targets: first %.3f, second %.3f (ratio %.2f), cooldown ticker after %d,"
                    + " reset events %d", jab.damage().get(0), jab.damage().get(1), jab.ratio(), jab.tickerAfter(), jab.events()));

            Jab miss = jab(player, origin, Material.IRON_SPEAR, false, 0);
            say(sender, "spear jab, no target: cooldown ticker 100 -> " + miss.tickerAfter() + (miss.tickerAfter() == 0 ? " (reset)" : " (NOT reset)"));

            Jab sword = sword(player, origin);
            say(sender, String.format(Locale.ROOT, "sword attack: damage %.3f, cooldown ticker 100 -> %d, reset events %d",
                    sword.damage().get(0), sword.tickerAfter(), sword.events()));

            Jab swap = jab(player, origin, Material.IRON_SPEAR, true, 2);
            say(sender, String.format(Locale.ROOT, "attribute swap sword->spear, same tick, no equipment update: first %.3f, second %.3f"
                    + " (ratio %.2f)", swap.damage().get(0), swap.damage().get(1), swap.ratio()));

            Jab forced = swapWithEquipmentUpdate(player, origin);
            say(sender, String.format(Locale.ROOT, "attribute swap with Paper's equipment update on slot change: first %.3f, second %.3f",
                    forced.damage().get(0), forced.damage().get(1)));

            boolean vanilla = jab.ratio() > 0.95 && miss.tickerAfter() == 0 && swap.ratio() > 0.95;
            boolean paper12111 = jab.ratio() < 0.5 && miss.tickerAfter() != 0 && swap.ratio() < 0.5;
            boolean swordOk = sword.damage().get(0) > 0.0 && sword.tickerAfter() == 0 && sword.events() == 1;
            say(sender, "verdict: " + (vanilla ? "VANILLA COOLDOWN" : paper12111 ? "COOLDOWN RESET DURING DAMAGE" : "UNEXPECTED")
                    + ", sword " + (swordOk ? "OK" : "UNEXPECTED"));
        } catch (Throwable t) {
            Throwable cause = t instanceof InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : t;
            getLogger().log(Level.SEVERE, "probe failed", cause);
            say(sender, "probe failed: " + cause);
        }
    }

    /** Result of one scenario. */
    private record Jab(List<Double> damage, int tickerAfter, int events) {
        double ratio() {
            return this.damage.size() < 2 || this.damage.get(0) <= 0 ? 0 : this.damage.get(1) / this.damage.get(0);
        }
    }

    private List<Pig> targets(Location eye, double first, double second, int count) {
        List<Pig> pigs = new ArrayList<>();
        double[] distances = {first, second};
        for (int i = 0; i < count; i++) {
            // facing +X: pigs on the line of sight, centred at eye height (pig height 0.9)
            Location at = eye.clone().add(distances[i], -0.45, 0);
            pigs.add(eye.getWorld().spawn(at, Pig.class, pig -> {
                pig.setAI(false);
                pig.setGravity(false);
                pig.setSilent(true);
                pig.setPersistent(false);
                var max = pig.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) {
                    max.setBaseValue(HEALTH);
                }
                pig.setHealth(HEALTH);
            }));
        }
        return pigs;
    }

    private static List<Double> damage(List<Pig> pigs) {
        List<Double> out = new ArrayList<>();
        for (Pig pig : pigs) {
            out.add(HEALTH - pig.getHealth());
            pig.remove();
        }
        while (out.size() < 2) {
            out.add(0.0);
        }
        return out;
    }

    /**
     * @param swap   hold a sword (attributes applied), then select the spear without an equipment update
     * @param count  number of targets (0 = miss)
     */
    private Jab jab(Object player, Location origin, Material spear, boolean swap, int count) throws Exception {
        this.nms.place(player, origin);
        this.nms.hold(player, new ItemStack(Material.IRON_SWORD), new ItemStack(spear), swap ? 0 : 1);
        this.nms.equipmentUpdate(player);
        this.nms.ticker(player, 100);
        if (swap) {
            this.nms.select(player, 1); // the slot change of an attribute swap, before the tick that updates equipment
        }
        double[] range = this.nms.attackRange(this.nms.mainHand(player));
        double first = range[0] + 0.6;
        double second = Math.min(first + 1.0, range[1] - 0.2);
        List<Pig> pigs = targets(this.nms.eye(player), first, second, count);
        this.resetEvents = 0;
        this.nms.stab(player);
        return new Jab(damage(pigs), this.nms.ticker(player), this.resetEvents);
    }

    private Jab swapWithEquipmentUpdate(Object player, Location origin) throws Exception {
        this.nms.place(player, origin);
        this.nms.hold(player, new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SPEAR), 0);
        this.nms.equipmentUpdate(player);
        this.nms.ticker(player, 100);
        this.nms.select(player, 1);
        this.nms.equipmentUpdate(player); // what Paper does in handleSetCarriedItem by default
        double[] range = this.nms.attackRange(this.nms.mainHand(player));
        double first = range[0] + 0.6;
        List<Pig> pigs = targets(this.nms.eye(player), first, Math.min(first + 1.0, range[1] - 0.2), 2);
        this.resetEvents = 0;
        this.nms.stab(player);
        return new Jab(damage(pigs), this.nms.ticker(player), this.resetEvents);
    }

    private Jab sword(Object player, Location origin) throws Exception {
        this.nms.place(player, origin);
        this.nms.hold(player, new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SPEAR), 0);
        this.nms.equipmentUpdate(player);
        this.nms.ticker(player, 100);
        List<Pig> pigs = targets(this.nms.eye(player), 2.0, 0, 1);
        this.resetEvents = 0;
        this.nms.attack(player, pigs.get(0));
        return new Jab(damage(pigs), this.nms.ticker(player), this.resetEvents);
    }

    /** Reflection on the Mojang-mapped server classes (Minecraft 1.21.11). */
    private static final class Nms {
        private final ClassLoader loader = org.bukkit.Bukkit.getServer().getClass().getClassLoader();
        private Object player;

        Class<?> c(String name) throws ClassNotFoundException {
            return Class.forName(name, true, this.loader);
        }

        static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
            for (Method m : target.getClass().getMethods()) { // public, including interface default methods
                if (m.getName().equals(name) && fits(m.getParameterTypes(), args)) {
                    m.trySetAccessible();
                    return m.invoke(target, args);
                }
            }
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                for (Method m : k.getDeclaredMethods()) {
                    if (m.getName().equals(name) && fits(m.getParameterTypes(), args)) {
                        m.setAccessible(true);
                        return m.invoke(target, args);
                    }
                }
            }
            throw new NoSuchMethodException(target.getClass().getName() + "." + name + " with " + args.length + " argument(s)");
        }

        static Object create(Class<?> type, Object... args) throws ReflectiveOperationException {
            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                if (fits(ctor.getParameterTypes(), args)) {
                    ctor.setAccessible(true);
                    return ctor.newInstance(args);
                }
            }
            throw new NoSuchMethodException(type.getName() + " constructor with " + args.length + " argument(s)");
        }

        static Field field(Class<?> type, String name) throws NoSuchFieldException {
            for (Class<?> k = type; k != null; k = k.getSuperclass()) {
                try {
                    Field f = k.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // try the superclass
                }
            }
            throw new NoSuchFieldException(type.getName() + "." + name);
        }

        private static boolean fits(Class<?>[] params, Object[] args) {
            if (params.length != args.length) {
                return false;
            }
            for (int i = 0; i < params.length; i++) {
                Class<?> p = params[i].isPrimitive() ? box(params[i]) : params[i];
                if (args[i] != null && !p.isInstance(args[i])) {
                    return false;
                }
            }
            return true;
        }

        private static Class<?> box(Class<?> p) {
            return p == int.class ? Integer.class : p == boolean.class ? Boolean.class : p == float.class ? Float.class
                    : p == double.class ? Double.class : p == long.class ? Long.class : p;
        }

        /** A ServerPlayer with a packet listener whose connection drops every packet. */
        Object testPlayer(World world) throws Exception {
            if (this.player != null) {
                return this.player;
            }
            Object server = call(org.bukkit.Bukkit.getServer(), "getServer");
            Object level = call(world, "getHandle");
            Object profile;
            try {
                profile = create(c("com.mojang.authlib.GameProfile"), PROBE_ID, "PaperSwapProbe");
            } catch (NoSuchMethodException e) {
                // authlib with only the canonical constructor (id, name, properties)
                Class<?> properties = c("com.mojang.authlib.properties.PropertyMap");
                Object empty;
                try {
                    empty = properties.getField("EMPTY").get(null);
                } catch (NoSuchFieldException none) {
                    empty = create(properties);
                }
                profile = create(c("com.mojang.authlib.GameProfile"), PROBE_ID, "PaperSwapProbe", empty);
            }
            Object info = c("net.minecraft.server.level.ClientInformation").getMethod("createDefault").invoke(null);
            Object sp = create(c("net.minecraft.server.level.ServerPlayer"), server, level, profile, info);
            Class<?> flow = c("net.minecraft.network.protocol.PacketFlow");
            Object connection = create(c("net.minecraft.network.Connection"), enumConstant(flow, "SERVERBOUND"));
            Object channel = c("io.netty.channel.embedded.EmbeddedChannel").getConstructor().newInstance();
            call(channel, "close");
            field(connection.getClass(), "channel").set(connection, channel);
            field(connection.getClass(), "address").set(connection, new java.net.InetSocketAddress("127.0.0.1", 0));
            try {
                field(connection.getClass(), "preparing").set(connection, false); // closed and not preparing: send() drops packets
            } catch (NoSuchFieldException ignored) {
                // older layout: packets to a closed channel are queued, which is harmless for a few calls
            }
            Class<?> cookieType = c("net.minecraft.server.network.CommonListenerCookie");
            Object cookie = cookieType.getMethod("createInitial", c("com.mojang.authlib.GameProfile"), boolean.class).invoke(null, profile, false);
            create(c("net.minecraft.server.network.ServerGamePacketListenerImpl"), server, connection, sp, cookie);
            this.player = sp;
            return sp;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object enumConstant(Class<?> type, String name) {
            return Enum.valueOf((Class) type, name);
        }

        void place(Object sp, Location at) throws ReflectiveOperationException {
            call(sp, "setPos", at.getX(), at.getY(), at.getZ());
            call(sp, "setYRot", -90.0F); // facing +X
            call(sp, "setXRot", 0.0F);
            call(sp, "setYHeadRot", -90.0F);
            call(sp, "setDeltaMovement", create(c("net.minecraft.world.phys.Vec3"), 0.0, 0.0, 0.0));
        }

        Location eye(Object sp) throws ReflectiveOperationException {
            org.bukkit.entity.Player bukkit = (org.bukkit.entity.Player) call(sp, "getBukkitEntity");
            Location eye = bukkit.getEyeLocation();
            eye.setYaw(-90.0F);
            eye.setPitch(0.0F);
            return eye;
        }

        void hold(Object sp, ItemStack slot0, ItemStack slot1, int selected) throws ReflectiveOperationException {
            org.bukkit.entity.Player bukkit = (org.bukkit.entity.Player) call(sp, "getBukkitEntity");
            bukkit.getInventory().setItem(0, slot0);
            bukkit.getInventory().setItem(1, slot1);
            select(sp, selected);
        }

        void select(Object sp, int slot) throws ReflectiveOperationException {
            call(call(sp, "getInventory"), "setSelectedSlot", slot);
        }

        void equipmentUpdate(Object sp) throws ReflectiveOperationException {
            call(sp, "detectEquipmentUpdates");
        }

        void ticker(Object sp, int value) throws ReflectiveOperationException {
            field(c("net.minecraft.world.entity.LivingEntity"), "attackStrengthTicker").setInt(sp, value);
        }

        int ticker(Object sp) throws ReflectiveOperationException {
            return field(c("net.minecraft.world.entity.LivingEntity"), "attackStrengthTicker").getInt(sp);
        }

        Object mainHand(Object sp) throws ReflectiveOperationException {
            return call(sp, "getMainHandItem");
        }

        double[] attackRange(Object stack) throws ReflectiveOperationException {
            Object type = c("net.minecraft.core.component.DataComponents").getField("ATTACK_RANGE").get(null);
            Object range = call(stack, "get", type);
            if (range == null) {
                return new double[] {0.0, 3.0};
            }
            return new double[] {((Number) call(range, "minRange")).doubleValue(), ((Number) call(range, "maxRange")).doubleValue()};
        }

        /** What the STAB action does: PiercingWeapon.attack(player, MAINHAND). */
        void stab(Object sp) throws ReflectiveOperationException {
            Object type = c("net.minecraft.core.component.DataComponents").getField("PIERCING_WEAPON").get(null);
            Object weapon = call(mainHand(sp), "get", type);
            if (weapon == null) {
                throw new IllegalStateException("the main hand item has no piercing_weapon component");
            }
            Object slot = enumConstant(c("net.minecraft.world.entity.EquipmentSlot"), "MAINHAND");
            call(weapon, "attack", sp, slot);
        }

        /** What an attack packet does for a non-piercing weapon: Player.attack(target). */
        void attack(Object sp, Pig target) throws ReflectiveOperationException {
            call(sp, "attack", call(target, "getHandle"));
        }
    }
}
