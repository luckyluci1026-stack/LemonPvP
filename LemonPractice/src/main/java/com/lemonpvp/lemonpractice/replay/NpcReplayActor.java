package com.lemonpvp.lemonpractice.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.chat.RemoteChatSession;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A packet-based fake-player NPC actor shown only to a single viewer, used for
 * replay playback when PacketEvents is installed. All PacketEvents references are
 * confined to this class so the rest of the replay code still works (with the
 * armor-stand fallback) when the library is absent — it is only ever loaded
 * behind an isPluginEnabled("packetevents") + try/catch guard.
 */
public class NpcReplayActor implements ReplayActor {

    private final Plugin plugin;
    private final Player viewer;
    private final String name;
    private final UUID uuid;
    private final int entityId;
    private boolean sneaking = false;

    public NpcReplayActor(Plugin plugin, Player viewer, String name) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.name = trim16(name);
        this.uuid = UUID.randomUUID();
        this.entityId = SpigotReflectionUtil.generateEntityId();
    }

    public void spawn(org.bukkit.Location loc) {
        UserProfile profile = new UserProfile(uuid, name);
        copySkin(profile);

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile, true, 0, GameMode.SURVIVAL, (Component) null, (RemoteChatSession) null);
        send(new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                           WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED), info));

        Location pe = new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        send(new WrapperPlayServerSpawnEntity(entityId, uuid, EntityTypes.PLAYER, pe,
                loc.getYaw(), 0, null));

        // Show all skin layers (displayed-skin-parts metadata, index 17, value 0x7F).
        send(new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData<>(17, EntityDataTypes.BYTE, (byte) 0x7F))));

        // Hide the NPC's name from the real tab list shortly after; the entity stays visible.
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> send(new WrapperPlayServerPlayerInfoRemove(uuid)), 40L);
    }

    @Override
    public void teleport(org.bukkit.Location loc) {
        Location pe = new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        send(new WrapperPlayServerEntityTeleport(entityId, pe, true));
        send(new WrapperPlayServerEntityHeadLook(entityId, loc.getYaw()));
    }

    @Override
    public void setSneaking(boolean s) {
        if (s == sneaking) return;
        sneaking = s;
        // Base entity-flags metadata (index 0): bit 0x02 = crouching. Mirrors the
        // index-17 skin-layers metadata used in spawn().
        send(new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData<>(0, EntityDataTypes.BYTE, (byte) (s ? 0x02 : 0x00)))));
    }

    /** Plays the main-arm swing animation (bot attacks, replay hits). */
    public void swing() {
        send(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation(
                entityId,
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
                        .EntityAnimationType.SWING_MAIN_ARM));
    }

    /** Plays the red-flash hurt animation. */
    public void hurt() {
        send(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation(
                entityId,
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
                        .EntityAnimationType.HURT));
    }

    /** Shows held item + armor on the NPC (visual only — packets, no server entity). */
    public void equip(org.bukkit.inventory.ItemStack hand, org.bukkit.inventory.ItemStack helmet,
                      org.bukkit.inventory.ItemStack chest, org.bukkit.inventory.ItemStack legs,
                      org.bukkit.inventory.ItemStack boots) {
        try {
            List<com.github.retrooper.packetevents.protocol.player.Equipment> eq = new java.util.ArrayList<>();
            addEquip(eq, com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND, hand);
            addEquip(eq, com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET, helmet);
            addEquip(eq, com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE, chest);
            addEquip(eq, com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS, legs);
            addEquip(eq, com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS, boots);
            if (!eq.isEmpty()) {
                send(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment(
                        entityId, eq));
            }
        } catch (Throwable ignored) { }
    }

    private void addEquip(List<com.github.retrooper.packetevents.protocol.player.Equipment> list,
                          com.github.retrooper.packetevents.protocol.player.EquipmentSlot slot,
                          org.bukkit.inventory.ItemStack item) {
        if (item == null) return;
        list.add(new com.github.retrooper.packetevents.protocol.player.Equipment(slot,
                io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(item)));
    }

    @Override
    public void remove() {
        send(new WrapperPlayServerDestroyEntities(entityId));
        send(new WrapperPlayServerPlayerInfoRemove(uuid));
    }

    private void send(PacketWrapper<?> wrapper) {
        if (viewer == null || !viewer.isOnline()) return;
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, wrapper);
        } catch (Throwable ignored) { }
    }

    /** Copy the recorded player's skin if they happen to be online. */
    private void copySkin(UserProfile profile) {
        try {
            Player src = Bukkit.getPlayerExact(name);
            if (src == null) return;
            var user = PacketEvents.getAPI().getPlayerManager().getUser(src);
            if (user == null || user.getProfile() == null) return;
            List<TextureProperty> props = user.getProfile().getTextureProperties();
            if (props != null && !props.isEmpty()) profile.getTextureProperties().addAll(props);
        } catch (Throwable ignored) { }
    }

    private static String trim16(String s) {
        if (s == null || s.isEmpty()) return "NPC";
        return s.length() > 16 ? s.substring(0, 16) : s;
    }
}
