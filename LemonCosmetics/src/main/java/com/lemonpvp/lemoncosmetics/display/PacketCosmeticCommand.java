package com.lemonpvp.lemoncosmetics.display;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * {@code /pcosmetic <hat|head|cape|wings|off>} — try out the packet Display
 * cosmetics. Demonstrates all five spec tasks end to end: item/block displays
 * spawned by packet, scaled/rotated via the transform, a custom-textured player
 * head, live position tracking, and interpolated sway. Real cosmetics would be
 * driven from the cosmetics GUI, not this test command.
 */
public class PacketCosmeticCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // A sample skin texture (Base64 of the profile "textures" property). Swap for
    // any head texture value from a head database to show a custom 3D hat.
    private static final String SAMPLE_HEAD_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTA1YzY5Y2E1NzE2Y2VmMzM1ZTFhNGQ3M2E5ZTk0N2Q0M2M4YjM3ZjNjZDBkMWQwZWZmNzY3ZDcyMWNhZmYxNiJ9fX0=";

    private final LemonCosmetics plugin;

    public PacketCosmeticCommand(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        DisplayCosmeticManager manager = plugin.getDisplayCosmeticManager();
        if (manager == null) {
            msg(player, "<red>Packet cosmetics are unavailable (PacketEvents not installed).");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "hat" -> {
                manager.clear(player.getUniqueId());
                manager.equip(player, DisplayCosmetic.hat(new ItemStack(Material.DIAMOND_HELMET)));
                msg(player, "<green>Equipped a floating helmet hat.");
            }
            case "head" -> {
                manager.clear(player.getUniqueId());
                manager.equip(player, DisplayCosmetic.hat(SkinTextureUtil.head(SAMPLE_HEAD_TEXTURE)));
                msg(player, "<green>Equipped a custom-textured 3D head hat.");
            }
            case "cape" -> {
                manager.clear(player.getUniqueId());
                manager.equip(player, DisplayCosmetic.cape(Material.RED_CONCRETE.createBlockData()));
                msg(player, "<green>Equipped a red-concrete cape (swaying).");
            }
            case "wings" -> {
                manager.clear(player.getUniqueId());
                manager.equip(player, DisplayCosmetic.wings(new ItemStack(Material.ELYTRA)));
                msg(player, "<green>Equipped wings (flapping).");
            }
            case "off" -> {
                manager.clear(player.getUniqueId());
                msg(player, "<gray>Packet cosmetics removed.");
            }
            default -> msg(player, "<yellow>/pcosmetic <hat|head|cape|wings|off>");
        }
        return true;
    }

    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String p = args[0].toLowerCase(Locale.ROOT);
        return java.util.stream.Stream.of("hat", "head", "cape", "wings", "off")
                .filter(s -> s.startsWith(p)).toList();
    }
}
