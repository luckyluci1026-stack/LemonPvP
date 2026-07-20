package net.milkbowl.vault.economy;

import org.bukkit.OfflinePlayer;

/**
 * Compile-only stub of the Vault Economy service interface.
 * At runtime the real interface from the installed Vault plugin is used;
 * this stub is never packaged into any plugin jar (scope: provided).
 * Only the members that the suite actually calls are declared.
 */
public interface Economy {

    boolean isEnabled();

    String getName();

    String format(double amount);

    String currencyNamePlural();

    String currencyNameSingular();

    boolean hasAccount(OfflinePlayer player);

    double getBalance(OfflinePlayer player);

    boolean has(OfflinePlayer player, double amount);

    EconomyResponse withdrawPlayer(OfflinePlayer player, double amount);

    EconomyResponse depositPlayer(OfflinePlayer player, double amount);
}
