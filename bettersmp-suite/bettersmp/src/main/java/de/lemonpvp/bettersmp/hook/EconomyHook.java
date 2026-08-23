package de.lemonpvp.bettersmp.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optionaler Vault-Economy-Hook (für /stats und das Scoreboard).
 * Die Vault-Klassen werden nur über die innere Bridge angefasst.
 */
public final class EconomyHook {

    private final Object economy;

    public EconomyHook() {
        Object found = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                found = Bridge.load();
            } catch (Throwable ignored) {
                // kein Vault/Economy
            }
        }
        this.economy = found;
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        return isEnabled() ? Bridge.balance((Economy) economy, player) : 0.0;
    }

    public String format(double amount) {
        return isEnabled() ? Bridge.format((Economy) economy, amount) : String.valueOf(amount);
    }

    /** Zahlt gut, falls Vault/eine Economy da ist. Sonst passiert nichts. */
    public boolean deposit(OfflinePlayer player, double amount) {
        return isEnabled() && Bridge.deposit((Economy) economy, player, amount);
    }

    private static final class Bridge {
        static Economy load() {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp == null ? null : rsp.getProvider();
        }

        static double balance(Economy eco, OfflinePlayer player) {
            return eco.getBalance(player);
        }

        static String format(Economy eco, double amount) {
            return eco.format(amount);
        }

        static boolean deposit(Economy eco, OfflinePlayer player, double amount) {
            return eco.depositPlayer(player, amount).transactionSuccess();
        }
    }
}
