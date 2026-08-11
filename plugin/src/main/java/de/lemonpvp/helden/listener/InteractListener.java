package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.item.ItemRegistry;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Rechtsklick-Logik: Heldenfaehigkeiten und Verbrauchsartefakte.
 *
 * <p>Auf Bedrock ist "langes Antippen" bzw. die Nutzen-Taste derselbe
 * Rechtsklick - dieselbe Steuerung funktioniert also auf beiden Editionen.</p>
 */
public final class InteractListener implements Listener {

    private final HeldenPlugin plugin;

    public InteractListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Nur einmal pro Rechtsklick reagieren - das Event feuert je Hand.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (handleSpecialItem(player, mainHand, event)) {
            return;
        }

        Hero hero = plugin.heroes().of(player);
        if (hero == null || hero.weaponId() == null || hero.weaponId().isEmpty()) {
            return;
        }
        if (plugin.settings().abilityTrigger() == Settings.AbilityTrigger.SNEAK_RIGHT_CLICK
                && !player.isSneaking()) {
            return;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!ItemRegistry.is(mainHand, hero.weaponId()) && !ItemRegistry.is(offHand, hero.weaponId())) {
            return;
        }
        if (plugin.abilities().trigger(player)) {
            // Verhindert, dass gleichzeitig der Bogen gespannt oder das Schild
            // gehoben wird - die Faehigkeit hat Vorrang.
            event.setCancelled(true);
        }
    }

    /** @return {@code true}, wenn ein Verbrauchsartefakt benutzt wurde */
    private boolean handleSpecialItem(Player player, ItemStack stack, PlayerInteractEvent event) {
        String itemId = ItemRegistry.idOf(stack);
        if (itemId == null) {
            return false;
        }

        if (itemId.equalsIgnoreCase(plugin.settings().lifeCrystalItemId())) {
            event.setCancelled(true);
            if (plugin.lives().addLives(player, 1)) {
                consumeOne(player, stack);
                plugin.messages().send(player, "item.lebenskristall-used");
                Compat.sound(player, "entity.player.levelup", 1.0f, 1.6f);
            }
            return true;
        }

        if (itemId.equalsIgnoreCase(plugin.settings().currencyTokenItemId())) {
            event.setCancelled(true);
            plugin.economy().give(player, plugin.settings().currencyTokenValue(), "economy.reason-shop");
            consumeOne(player, stack);
            plugin.messages().send(player, "item.zitrone-used");
            Compat.sound(player, "entity.experience_orb.pickup", 1.0f, 1.6f);
            return true;
        }

        if (itemId.equalsIgnoreCase(plugin.settings().returnStoneItemId())) {
            event.setCancelled(true);
            if (plugin.combat().isTagged(player)) {
                plugin.messages().send(player, "combat.blocked-in-combat");
                return true;
            }
            Location spawn = plugin.spawnFor(player);
            if (spawn == null) {
                return true;
            }
            player.teleport(spawn);
            consumeOne(player, stack);
            plugin.messages().send(player, "item.rueckkehr");
            Compat.sound(player, "entity.enderman.teleport", 1.0f, 1.0f);
            return true;
        }

        return false;
    }

    private void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
    }
}
