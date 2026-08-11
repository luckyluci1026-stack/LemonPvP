package de.lemonpvp.helden.economy;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Text;
import org.bukkit.entity.Player;

/** Die Projektwaehrung ("Zitronen"). */
public final class EconomyManager {

    private final HeldenPlugin plugin;

    public EconomyManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public String currencyName() {
        return plugin.settings().currencyName();
    }

    public String currencyDisplay() {
        String symbol = plugin.settings().currencySymbol();
        return symbol == null || symbol.isEmpty()
                ? currencyName()
                : Text.color(symbol) + " " + currencyName();
    }

    public int balance(HeldenProfile profile) {
        return profile == null ? 0 : profile.coins();
    }

    public boolean has(HeldenProfile profile, int amount) {
        return profile != null && profile.coins() >= amount;
    }

    public void set(HeldenProfile profile, int amount) {
        if (profile != null) {
            profile.coins(amount);
        }
    }

    /** Gutschrift mit Chatmeldung. {@code reasonPath} zeigt auf messages.yml. */
    public void give(Player player, int amount, String reasonPath) {
        if (amount <= 0) {
            return;
        }
        HeldenProfile profile = plugin.profiles().get(player);
        if (profile == null) {
            return;
        }
        profile.coins(profile.coins() + amount);
        plugin.messages().send(player, "economy.received",
                "%amount%", amount,
                "%currency%", currencyName(),
                "%reason%", plugin.messages().format(player, reasonPath));
    }

    /** Abbuchung. {@code false}, wenn das Guthaben nicht reicht. */
    public boolean take(Player player, int amount, String reasonPath) {
        if (amount <= 0) {
            return true;
        }
        HeldenProfile profile = plugin.profiles().get(player);
        if (profile == null || profile.coins() < amount) {
            plugin.messages().send(player, "economy.not-enough",
                    "%amount%", amount,
                    "%currency%", currencyName());
            return false;
        }
        profile.coins(profile.coins() - amount);
        plugin.messages().send(player, "economy.lost",
                "%amount%", amount,
                "%currency%", currencyName(),
                "%reason%", plugin.messages().format(player, reasonPath));
        return true;
    }

    /** Stiller Abzug ohne Meldung (z. B. Tod). */
    public void withdrawSilently(HeldenProfile profile, int amount) {
        if (profile != null && amount > 0) {
            profile.coins(Math.max(0, profile.coins() - amount));
        }
    }
}
