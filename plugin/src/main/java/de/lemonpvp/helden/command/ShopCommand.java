package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.ui.ShopMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /shop} - Zitronen ausgeben. */
public final class ShopCommand extends BaseCommand {

    public ShopCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.shop", true);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ShopMenu menu = new ShopMenu(plugin);
        if (!menu.isEnabled()) {
            plugin.messages().send(player, "shop.disabled");
            return;
        }
        plugin.messages().send(player, "economy.balance",
                "%amount%", plugin.profiles().getOrCreate(player).coins(),
                "%currency%", plugin.economy().currencyName());
        menu.open(player);
    }
}
