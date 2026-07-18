package com.lemonpvp.lemoncosmetics.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The packet-only armor-stand "body double" a dance emote animates. Spawned per
 * viewer, dressed like the dancer (their own head + armor, or a fallback outfit),
 * and posed each frame via the armor-stand pose metadata. No server entity exists.
 *
 * <p>Armor-stand metadata layout (1.21): index 15 = stand flags
 * ({@code 0x04} show arms, {@code 0x08} hide baseplate), and the six pose
 * rotations sit at indices 16–21 (head, body, left arm, right arm, left leg,
 * right leg) as degree {@code Vector3f}s ({@code EntityDataTypes.ROTATION}).</p>
 */
public class DanceSession {

    private static final int IDX_STAND_FLAGS = 15;
    private static final int IDX_POSE_HEAD = 16;   // ..21 in the fixed order below
    private static final byte FLAG_SHOW_ARMS = 0x04;
    private static final byte FLAG_NO_BASEPLATE = 0x08;

    private final int entityId = SpigotReflectionUtil.generateEntityId();
    private final UUID entityUuid = UUID.randomUUID();
    private final Set<Player> viewers = ConcurrentHashMap.newKeySet();

    private final ItemStack helmet, chest, legs, boots;

    public DanceSession(ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots) {
        this.helmet = helmet;
        this.chest = chest;
        this.legs = legs;
        this.boots = boots;
    }

    /** Spawns and dresses the double for one viewer. */
    public void spawn(Player viewer, Location loc) {
        if (!viewers.add(viewer)) return;
        com.github.retrooper.packetevents.protocol.world.Location pe =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), 0f);
        send(viewer, new WrapperPlayServerSpawnEntity(entityId, entityUuid,
                EntityTypes.ARMOR_STAND, pe, loc.getYaw(), 0, null));
        send(viewer, new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData<>(IDX_STAND_FLAGS, EntityDataTypes.BYTE,
                        (byte) (FLAG_SHOW_ARMS | FLAG_NO_BASEPLATE)))));

        List<Equipment> eq = new ArrayList<>();
        addEquip(eq, EquipmentSlot.HELMET, helmet);
        addEquip(eq, EquipmentSlot.CHEST_PLATE, chest);
        addEquip(eq, EquipmentSlot.LEGGINGS, legs);
        addEquip(eq, EquipmentSlot.BOOTS, boots);
        if (!eq.isEmpty()) send(viewer, new WrapperPlayServerEntityEquipment(entityId, eq));
    }

    /** Applies one animation frame: pose metadata + a teleport carrying the spin yaw. */
    public void pose(DanceEmote.Pose p, Location base) {
        float yaw = base.getYaw() + p.yawOffset();
        com.github.retrooper.packetevents.protocol.world.Location pe =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        base.getX(), base.getY(), base.getZ(), yaw, 0f);
        List<EntityData<?>> pose = List.of(
                new EntityData<>(IDX_POSE_HEAD,     EntityDataTypes.ROTATION, p.head()),
                new EntityData<>(IDX_POSE_HEAD + 1, EntityDataTypes.ROTATION, p.body()),
                new EntityData<>(IDX_POSE_HEAD + 2, EntityDataTypes.ROTATION, p.leftArm()),
                new EntityData<>(IDX_POSE_HEAD + 3, EntityDataTypes.ROTATION, p.rightArm()),
                new EntityData<>(IDX_POSE_HEAD + 4, EntityDataTypes.ROTATION, p.leftLeg()),
                new EntityData<>(IDX_POSE_HEAD + 5, EntityDataTypes.ROTATION, p.rightLeg()));
        for (Player viewer : viewers) {
            if (!viewer.isOnline()) continue;
            send(viewer, new WrapperPlayServerEntityTeleport(entityId, pe, true));
            send(viewer, new WrapperPlayServerEntityMetadata(entityId, pose));
        }
    }

    /** Removes the double for everyone. */
    public void destroy() {
        for (Player viewer : viewers) {
            if (viewer.isOnline()) send(viewer, new WrapperPlayServerDestroyEntities(entityId));
        }
        viewers.clear();
    }

    private void addEquip(List<Equipment> list, EquipmentSlot slot, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        list.add(new Equipment(slot, SpigotConversionUtil.fromBukkitItemStack(item)));
    }

    private void send(Player viewer, PacketWrapper<?> wrapper) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, wrapper);
        } catch (Throwable ignored) { }
    }
}
