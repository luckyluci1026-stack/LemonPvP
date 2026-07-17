package com.lemonpvp.lemoncosmetics.display;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import java.util.List;
import java.util.UUID;

/**
 * The low-level PacketEvents builders for packet-only Display cosmetics — no
 * server-side entity is ever created, so this works on a vanilla client with no
 * mod and no resource pack. Every method here just <i>builds</i> a packet; the
 * {@link DisplayCosmetic}/{@link DisplayCosmeticManager} decide who to send it to.
 *
 * <h2>Display metadata indices</h2>
 * Item- and block-displays share a common {@code Display} base whose fields sit
 * at these fixed metadata indices (stable Minecraft 1.19.4 → 1.21). If a future
 * protocol inserts fields into the Entity/Display base, bump these constants.
 *
 * <h2>The transform (task 2)</h2>
 * Minecraft renders a display's model through an affine transform built from four
 * parts. Conceptually the client computes, for each model vertex {@code v}:
 * <pre>  v' = translation + leftRotation · ( scale ⊙ ( rightRotation · v ) )</pre>
 * — first rotate the raw model ({@code rightRotation}), then scale it
 * component-wise ({@code scale}, ⊙ = per-axis multiply), rotate again
 * ({@code leftRotation}), and finally move it ({@code translation}). This is the
 * singular-value-decomposition form of an arbitrary 3×3 matrix, which is why two
 * rotations bracket the scale. For our cosmetics one rotation is usually enough,
 * so we leave {@code rightRotation} as identity and put the tilt in
 * {@code leftRotation}.
 *
 * <p>A rotation of angle θ about a unit axis {@code (ax,ay,az)} is the quaternion
 * {@code (ax·sin(θ/2), ay·sin(θ/2), az·sin(θ/2), cos(θ/2))} — see {@link #axisAngle}.</p>
 */
public final class DisplayCosmeticPackets {

    public static final int IDX_INTERPOLATION_DELAY    = 8;   // ticks to wait before tweening
    public static final int IDX_INTERPOLATION_DURATION = 9;   // transformation tween length (ticks)
    public static final int IDX_TRANSLATION            = 11;  // Vector3f
    public static final int IDX_SCALE                  = 12;  // Vector3f
    public static final int IDX_LEFT_ROTATION          = 13;  // Quaternion
    public static final int IDX_RIGHT_ROTATION         = 14;  // Quaternion
    public static final int IDX_ITEMDISPLAY_ITEM         = 23; // ItemStack (ITEM_DISPLAY only)
    public static final int IDX_ITEMDISPLAY_DISPLAY_TYPE = 24; // Byte (ITEM_DISPLAY only)
    public static final int IDX_BLOCKDISPLAY_STATE       = 23; // block-state global id (BLOCK_DISPLAY only)

    /** Item-display "display" transform ids: 5 = HEAD (worn-head look), 8 = FIXED (flat, item-frame-like). */
    public static final byte ITEM_DISPLAY_HEAD = 5;
    public static final byte ITEM_DISPLAY_FIXED = 8;

    private DisplayCosmeticPackets() {}

    /** A display's render transform: right-rotate → scale → left-rotate → translate. */
    public record Transform(Vector3f translation, Vector3f scale,
                            Quaternion4f leftRotation, Quaternion4f rightRotation) {
        public static Transform of(Vector3f translation, Vector3f scale, Quaternion4f leftRotation) {
            return new Transform(translation, scale, leftRotation, noRotation());
        }
    }

    // ── Task 1: spawn ───────────────────────────────────────────────────────

    /** Builds the spawn packet for an item/block display at a world location. */
    public static WrapperPlayServerSpawnEntity spawn(int entityId, UUID uuid, EntityType type, org.bukkit.Location loc) {
        Location pe = new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        // objectData = 0 and no velocity: displays need neither.
        return new WrapperPlayServerSpawnEntity(entityId, uuid, type, pe, loc.getYaw(), 0, null);
    }

    // ── Task 2 & 5: transform + interpolation ───────────────────────────────

    /**
     * Builds the metadata packet that sets the render transform and the
     * interpolation duration. Because we set {@code interpolation_duration} the
     * client smoothly tweens from its current transform to this one over that many
     * ticks — so a packet sent only every few ticks still looks 60+ FPS smooth
     * (task 5). {@code interpolation_delay = 0} starts the tween on receipt.
     */
    public static WrapperPlayServerEntityMetadata transform(int entityId, Transform t, int interpolationTicks) {
        // Build via List.of(...) to mirror the proven metadata idiom used elsewhere.
        return new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData<>(IDX_INTERPOLATION_DELAY, EntityDataTypes.INT, 0),
                new EntityData<>(IDX_INTERPOLATION_DURATION, EntityDataTypes.INT, interpolationTicks),
                new EntityData<>(IDX_TRANSLATION, EntityDataTypes.VECTOR3F, t.translation()),
                new EntityData<>(IDX_SCALE, EntityDataTypes.VECTOR3F, t.scale()),
                new EntityData<>(IDX_LEFT_ROTATION, EntityDataTypes.QUATERNION, t.leftRotation()),
                new EntityData<>(IDX_RIGHT_ROTATION, EntityDataTypes.QUATERNION, t.rightRotation())));
    }

    // ── Task 3 appearance: what the display shows ───────────────────────────

    /** Item-display appearance: the item to render (e.g. a textured player head) + display context. */
    public static WrapperPlayServerEntityMetadata itemAppearance(int entityId,
                                                                 org.bukkit.inventory.ItemStack item,
                                                                 byte displayType) {
        return new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData<>(IDX_ITEMDISPLAY_ITEM, EntityDataTypes.ITEMSTACK,
                        SpigotConversionUtil.fromBukkitItemStack(item)),
                new EntityData<>(IDX_ITEMDISPLAY_DISPLAY_TYPE, EntityDataTypes.BYTE, displayType)));
    }

    /** Block-display appearance: a vanilla block state (e.g. red concrete) to render. */
    public static WrapperPlayServerEntityMetadata blockAppearance(int entityId, org.bukkit.block.data.BlockData block) {
        int globalId = SpigotConversionUtil.fromBukkitBlockData(block).getGlobalId();
        return new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData<>(IDX_BLOCKDISPLAY_STATE, EntityDataTypes.BLOCK_STATE, globalId)));
    }

    // ── Task 4 helper + teardown ────────────────────────────────────────────

    /** Snaps the display to a new world position (used by the no-lag movement tracker). */
    public static WrapperPlayServerEntityTeleport teleport(int entityId, org.bukkit.Location loc) {
        Location pe = new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        return new WrapperPlayServerEntityTeleport(entityId, pe, true);
    }

    public static WrapperPlayServerDestroyEntities destroy(int... entityIds) {
        return new WrapperPlayServerDestroyEntities(entityIds);
    }

    // ── Math helpers ────────────────────────────────────────────────────────

    /** Quaternion for a rotation of {@code degrees} about the unit axis {@code (ax,ay,az)}. */
    public static Quaternion4f axisAngle(float ax, float ay, float az, float degrees) {
        double half = Math.toRadians(degrees) / 2.0;
        float s = (float) Math.sin(half);
        return new Quaternion4f(ax * s, ay * s, az * s, (float) Math.cos(half));
    }

    /** The identity quaternion (no rotation). */
    public static Quaternion4f noRotation() {
        return new Quaternion4f(0f, 0f, 0f, 1f);
    }
}
