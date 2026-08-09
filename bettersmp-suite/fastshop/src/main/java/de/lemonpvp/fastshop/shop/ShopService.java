package de.lemonpvp.fastshop.shop;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.gui.ShopMenus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;

/**
 * Kauf-/Verkaufslogik über die Vault-/EssentialsX-Economy.
 */
public final class ShopService {

    private final FastShop plugin;

    public ShopService(FastShop plugin) {
        this.plugin = plugin;
    }

    private boolean noEconomy(Player player) {
        if (!plugin.economy().isEnabled()) {
            plugin.msgs().send(player, "no-economy");
            return true;
        }
        return false;
    }

    private String display(Material material) {
        return ShopMenus.prettyName(material);
    }

    // ---------------- Kaufen ----------------

    public void buy(Player player, ShopItem item, int amount) {
        if (noEconomy(player)) {
            return;
        }
        if (!item.buyable()) {
            plugin.msgs().send(player, "not-buyable");
            soundFail(player);
            return;
        }
        amount = Math.max(1, Math.min(64 * 9, amount));
        double cost = item.buy() * amount;
        if (!plugin.economy().has(player, cost)) {
            plugin.msgs().send(player, "not-enough-money",
                    "price", plugin.economy().format(cost),
                    "balance", plugin.economy().format(plugin.economy().balance(player)));
            soundFail(player);
            return;
        }
        if (!plugin.economy().withdraw(player, cost)) {
            plugin.msgs().send(player, "not-enough-money",
                    "price", plugin.economy().format(cost),
                    "balance", plugin.economy().format(plugin.economy().balance(player)));
            soundFail(player);
            return;
        }
        Map<Integer, ItemStack> leftover =
                player.getInventory().addItem(new ItemStack(item.material(), amount));
        int notAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (notAdded > 0) {
            // Nicht passende Menge zurückerstatten
            plugin.economy().deposit(player, item.buy() * notAdded);
            if (notAdded == amount) {
                plugin.msgs().send(player, "inventory-full");
            soundFail(player);
                return;
            }
        }
        int given = amount - notAdded;
        soundBuy(player);
        plugin.msgs().send(player, "bought",
                "amount", String.valueOf(given),
                "item", display(item.material()),
                "price", plugin.economy().format(item.buy() * given));
    }

    // ---------------- Verkaufen ----------------

    /** Verkauft den Stack in der Haupthand. */
    public void sellHand(Player player) {
        if (noEconomy(player)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            plugin.msgs().send(player, "nothing-in-hand");
            soundFail(player);
            return;
        }
        ShopItem item = plugin.shop().item(hand.getType());
        if (item == null || !item.sellable()) {
            plugin.msgs().send(player, "not-sellable");
            soundFail(player);
            return;
        }
        int amount = hand.getAmount();
        double price = item.sell() * plugin.shop().sellMultiplier() * amount;
        player.getInventory().setItemInMainHand(null);
        plugin.economy().deposit(player, price);
        soundSell(player);
        plugin.msgs().send(player, "sold",
                "amount", String.valueOf(amount), "item", display(item.material()),
                "price", plugin.economy().format(price));
    }

    /** Verkauft alle verkaufbaren, "normalen" Items im Inventar. */
    public void sellAll(Player player) {
        if (noEconomy(player)) {
            return;
        }
        double total = 0;
        int count = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!isPlain(stack)) {
                continue;
            }
            ShopItem item = plugin.shop().item(stack.getType());
            if (item == null || !item.sellable()) {
                continue;
            }
            total += item.sell() * plugin.shop().sellMultiplier() * stack.getAmount();
            count += stack.getAmount();
            contents[i] = null;
        }
        if (count == 0) {
            plugin.msgs().send(player, "nothing-sold");
            soundFail(player);
            return;
        }
        player.getInventory().setStorageContents(contents);
        plugin.economy().deposit(player, total);
        soundSell(player);
        plugin.msgs().send(player, "sold-multi",
                "count", String.valueOf(count), "price", plugin.economy().format(total));
    }

    /** Verkauft aus dem Inventar (Shop-GUI): gewünschte Menge, &lt;= 0 = alle. */
    public void sellFromInventory(Player player, Material material, int wanted) {
        if (noEconomy(player)) {
            return;
        }
        ShopItem item = plugin.shop().item(material);
        if (item == null || !item.sellable()) {
            plugin.msgs().send(player, "not-sellable");
            soundFail(player);
            return;
        }
        int available = countPlain(player, material);
        if (available <= 0) {
            plugin.msgs().send(player, "not-enough-items", "item", display(material));
            soundFail(player);
            return;
        }
        int amount = wanted <= 0 ? available : Math.min(wanted, available);
        removePlain(player, material, amount);
        double price = item.sell() * plugin.shop().sellMultiplier() * amount;
        plugin.economy().deposit(player, price);
        soundSell(player);
        plugin.msgs().send(player, "sold",
                "amount", String.valueOf(amount), "item", display(material),
                "price", plugin.economy().format(price));
    }

    /** Verkauft die im Sell-GUI abgelegten Items; nicht verkaufbare kommen zurück. */
    public void sellContents(Player player, org.bukkit.inventory.Inventory inv, int upTo) {
        double total = 0;
        int count = 0;
        ItemStack[] contents = inv.getContents();
        int limit = Math.min(upTo, contents.length);
        for (int i = 0; i < limit; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            ShopItem item = isPlain(stack) ? plugin.shop().item(stack.getType()) : null;
            if (item != null && item.sellable()) {
                total += item.sell() * plugin.shop().sellMultiplier() * stack.getAmount();
                count += stack.getAmount();
                contents[i] = null;
            }
        }
        inv.setContents(contents);
        // Rest (nicht verkauft) zurück ins Spielerinventar bzw. droppen
        returnContents(player, inv, limit);
        if (count > 0) {
            plugin.economy().deposit(player, total);
            soundSell(player);
        plugin.msgs().send(player, "sold-multi",
                    "count", String.valueOf(count), "price", plugin.economy().format(total));
        }
    }

    // ---------------- Helfer ----------------

    /** Wie viele verkaufbare (unveränderte) Exemplare hat der Spieler? */
    public int countSellable(Player player, Material material) {
        return countPlain(player, material);
    }

    /** Ist dieser Stapel unverändert (nicht benannt/verzaubert/beschädigt)? */
    public boolean isSellable(ItemStack stack) {
        return isPlain(stack);
    }

    /** Gibt alle Items aus einem Fenster zurück ins Spielerinventar. */
    public void returnContents(Player player, org.bukkit.inventory.Inventory inv, int upTo) {
        for (int i = 0; i < Math.min(upTo, inv.getSize()); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            inv.setItem(i, null);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(rest ->
                    player.getWorld().dropItemNaturally(player.getLocation(), rest));
        }
    }

    /** Kurzer Ton als Rückmeldung - lässt sich in der Config abschalten. */
    private void sound(Player player, String key, float pitch) {
        if (!plugin.getConfig().getBoolean("settings.sounds", true)) {
            return;
        }
        try {
            player.playSound(net.kyori.adventure.sound.Sound.sound(
                    net.kyori.adventure.key.Key.key(key),
                    net.kyori.adventure.sound.Sound.Source.MASTER, 0.7f, pitch));
        } catch (Exception ignored) {
            // ungültiger Sound-Key in der Config - dann eben lautlos
        }
    }

    private void soundBuy(Player player) {
        sound(player, "minecraft:entity.experience_orb.pickup", 1.2f);
    }

    private void soundSell(Player player) {
        sound(player, "minecraft:block.note_block.bell", 1.4f);
    }

    private void soundFail(Player player) {
        sound(player, "minecraft:block.note_block.bass", 0.8f);
    }

    private boolean isPlain(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        if (!stack.hasItemMeta()) {
            return true;
        }
        var meta = stack.getItemMeta();
        if (meta.hasDisplayName() || meta.hasEnchants() || meta.hasLore()
                || (meta instanceof Damageable dmg && dmg.hasDamage())) {
            return false;
        }
        return true;
    }

    private int countPlain(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material && isPlain(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removePlain(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material || !isPlain(stack)) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setStorageContents(contents);
    }
}
