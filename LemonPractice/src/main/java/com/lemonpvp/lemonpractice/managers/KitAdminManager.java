package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Free-form admin editor for the per-gamemode PRESET kits. Unlike the sort-only
 * player editor, the admin arranges the kit in their own (creative) inventory —
 * add, remove, enchant anything — and saves it. The result is stored as the
 * gamemode's admin preset ({@link KitManager#saveAdminKit}), overriding kits.yml
 * on every duel server.
 *
 * <p>The admin's real inventory + gamemode are snapshotted on entry and restored
 * on save/cancel, so editing never eats their items.</p>
 */
public class KitAdminManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();

    private record Session(String gamemode, ItemStack[] prevInv, ItemStack[] prevArmor, GameMode prevMode) {}

    public KitAdminManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    public boolean isEditing(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public String editingGamemode(UUID uuid) {
        Session s = sessions.get(uuid);
        return s != null ? s.gamemode() : null;
    }

    /** Enters edit mode: snapshot, switch to creative, load the current preset into the inventory. */
    public void enter(Player player, String gamemode) {
        UUID uuid = player.getUniqueId();
        if (sessions.containsKey(uuid)) cancel(player); // never stack sessions

        sessions.put(uuid, new Session(gamemode.toLowerCase(),
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone(),
                player.getGameMode()));

        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();

        // Fill with the current preset (admin edit if present, else kits.yml).
        PlayerKit preset = plugin.getKitManager().getPresetKit(gamemode);
        if (preset != null) {
            preset.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item.clone()));
        }

        player.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Kit Admin</bold></gradient> "
                + "<gray>Editing the <yellow>" + gamemode + " <gray>preset. Arrange items + armour freely, then "
                + "<white>/kitadmin save<gray>. <white>/kitadmin cancel<gray> to discard."));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.6f, 1.1f);
    }

    /** Reads the inventory into the preset, saves it, and restores the admin. */
    public boolean save(Player player) {
        UUID uuid = player.getUniqueId();
        Session s = sessions.get(uuid);
        if (s == null) return false;

        PlayerKit kit = new PlayerKit(KitManager.ADMIN_KIT_UUID, s.gamemode());
        for (int i = 0; i < 36; i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it != null && it.getType() != Material.AIR) kit.setSlot(i, it.clone());
        }
        ItemStack[] armor = player.getInventory().getArmorContents(); // [feet, legs, chest, head]
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                kit.setSlot(KitManager.ARMOR_FEET + i, armor[i].clone());
            }
        }

        plugin.getKitManager().saveAdminKit(s.gamemode(), kit);
        restore(player, s);
        sessions.remove(uuid);

        player.sendMessage(MM.deserialize("<!italic><green>Saved the <yellow>" + s.gamemode()
                + " <green>preset. <gray>It now applies to every duel on all servers."));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        return true;
    }

    /** Discards the session and restores the admin's inventory + gamemode. */
    public boolean cancel(Player player) {
        Session s = sessions.remove(player.getUniqueId());
        if (s == null) return false;
        restore(player, s);
        player.sendMessage(MM.deserialize("<!italic><gray>Kit edit cancelled — nothing saved."));
        return true;
    }

    private void restore(Player player, Session s) {
        player.getInventory().clear();
        player.getInventory().setContents(s.prevInv());
        player.getInventory().setArmorContents(s.prevArmor());
        player.setGameMode(s.prevMode());
    }

    /** Drops a session without touching the (offline) player — quit cleanup. */
    public void dropSession(UUID uuid) {
        sessions.remove(uuid);
    }
}
