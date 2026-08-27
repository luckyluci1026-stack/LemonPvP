package com.lemonpvp.lemoncosmetics.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;

import java.util.UUID;

/**
 * One packet-only Display entity coupled to a wearer — a "hat" over the head or a
 * "cape"/"wings" on the back. It owns its fake entity id and knows how to place
 * itself relative to the wearer and render its appearance + transform. It holds
 * no viewer state; {@link DisplayCosmeticManager} decides who sees it.
 */
public class DisplayCosmetic {

    /** Where on the wearer the cosmetic sits (drives the position offset + orientation). */
    public enum Slot { HAT, CAPE, BACKPACK }

    private final int entityId;
    private final UUID entityUuid = UUID.randomUUID();
    private final EntityType type;
    private final Slot slot;

    // Appearance: exactly one of (item + displayType) or block is set.
    private final ItemStack item;
    private final byte itemDisplayType;
    private final BlockData block;

    // The resting transform (before any animation modifies the rotation).
    private final DisplayCosmeticPackets.Transform baseTransform;

    private DisplayCosmetic(EntityType type, Slot slot, ItemStack item, byte itemDisplayType,
                            BlockData block, DisplayCosmeticPackets.Transform baseTransform) {
        this.entityId = SpigotReflectionUtil.generateEntityId();
        this.type = type;
        this.slot = slot;
        this.item = item;
        this.itemDisplayType = itemDisplayType;
        this.block = block;
        this.baseTransform = baseTransform;
    }

    // ── Factories ───────────────────────────────────────────────────────────

    /**
     * A 3D hat: a player-head item display rendered as a head, floating just above
     * the wearer's head. Scale it up a touch so it reads as a proper hat.
     */
    public static DisplayCosmetic hat(ItemStack headItem) {
        DisplayCosmeticPackets.Transform t = DisplayCosmeticPackets.Transform.of(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1.15f, 1.15f, 1.15f),
                DisplayCosmeticPackets.noRotation());
        return new DisplayCosmetic(EntityTypes.ITEM_DISPLAY, Slot.HAT,
                headItem, DisplayCosmeticPackets.ITEM_DISPLAY_HEAD, null, t);
    }

    /**
     * A flat cape from a single vanilla block (e.g. red concrete). The block is a
     * 1×1×1 cube, so we scale it to a thin, tall, wide slab and tilt it slightly
     * back off the shoulders, then translate it up to back height and behind the
     * wearer. Those numbers are deliberately tweakable constants.
     */
    public static DisplayCosmetic cape(BlockData blockData) {
        // scale: X≈flat (thickness), Y=tall, Z=wide → a banner-like slab.
        Vector3f scale = new Vector3f(0.06f, 0.9f, 0.7f);
        // translate up to the upper back and slightly behind (local -X here is "back"
        // once the entity yaw matches the wearer; centre the slab on its own size).
        Vector3f translation = new Vector3f(-0.28f, 0.55f, -0.35f);
        // tilt ~12° back about the Z axis so it drapes rather than standing flat.
        var tilt = DisplayCosmeticPackets.axisAngle(0f, 0f, 1f, 12f);
        return new DisplayCosmetic(EntityTypes.BLOCK_DISPLAY, Slot.CAPE,
                null, (byte) 0, blockData, DisplayCosmeticPackets.Transform.of(translation, scale, tilt));
    }

    /**
     * Simple "wings": a wide, thin item-display slab behind the back. Pass any
     * flat-ish item (e.g. a dyed banner pattern item or a textured head); the wing
     * flap animation is driven by {@link DisplayCosmeticManager}.
     */
    public static DisplayCosmetic wings(ItemStack wingItem) {
        Vector3f scale = new Vector3f(1.6f, 1.0f, 0.05f);
        Vector3f translation = new Vector3f(0f, 1.2f, -0.3f);
        return new DisplayCosmetic(EntityTypes.ITEM_DISPLAY, Slot.BACKPACK,
                wingItem, DisplayCosmeticPackets.ITEM_DISPLAY_FIXED, null,
                DisplayCosmeticPackets.Transform.of(translation, scale, DisplayCosmeticPackets.noRotation()));
    }

    // ── Placement ───────────────────────────────────────────────────────────

    /**
     * The world location + orientation of the display for a given wearer position.
     * Hats sit above the head facing the wearer's look; capes/backpacks sit at the
     * feet with the body yaw so the transform's "behind" offset stays behind.
     */
    public Location locationFor(Location wearer) {
        Location loc = wearer.clone();
        if (slot == Slot.HAT) {
            loc.add(0, 2.15, 0); // just above a standing player's head
        }
        // Match the wearer's yaw so the local-space transform tracks their facing.
        loc.setPitch(0f);
        return loc;
    }

    // ── Packet emission (per viewer) ────────────────────────────────────────

    /** Spawns the display for one viewer: spawn → appearance → resting transform. */
    public void spawn(Player viewer, Location wearer) {
        Location loc = locationFor(wearer);
        send(viewer, DisplayCosmeticPackets.spawn(entityId, entityUuid, type, loc));
        if (block != null) {
            send(viewer, DisplayCosmeticPackets.blockAppearance(entityId, block));
        } else if (item != null) {
            send(viewer, DisplayCosmeticPackets.itemAppearance(entityId, item, itemDisplayType));
        }
        // 0-tick interpolation for the initial pose (snap into place).
        send(viewer, DisplayCosmeticPackets.transform(entityId, baseTransform, 0));
    }

    /** Snaps the display to the wearer's new position (task 4 — called per movement packet). */
    public void move(Player viewer, Location wearer) {
        send(viewer, DisplayCosmeticPackets.teleport(entityId, locationFor(wearer)));
    }

    /** Sends an animated transform (rotation/scale) that the client tweens over {@code interpTicks}. */
    public void animate(Player viewer, DisplayCosmeticPackets.Transform t, int interpTicks) {
        send(viewer, DisplayCosmeticPackets.transform(entityId, t, interpTicks));
    }

    public void remove(Player viewer) {
        send(viewer, DisplayCosmeticPackets.destroy(entityId));
    }

    public DisplayCosmeticPackets.Transform baseTransform() { return baseTransform; }
    public Slot slot() { return slot; }
    public int entityId() { return entityId; }

    private void send(Player viewer, PacketWrapper<?> wrapper) {
        if (viewer == null || !viewer.isOnline()) return;
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, wrapper);
        } catch (Throwable ignored) { }
    }
}
