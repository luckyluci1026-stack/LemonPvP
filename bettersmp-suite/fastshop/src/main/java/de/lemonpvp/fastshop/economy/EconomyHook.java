package de.lemonpvp.fastshop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optionaler Vault-Economy-Hook (nutzt z.B. EssentialsX-Economy).
 * Die Vault-Klassen werden nur ueber die innere Bridge angefasst, damit das
 * Plugin auch ganz ohne Vault laedt.
 */
public final class EconomyHook {

    private final Object economy; // Economy, aber als Object gehalten fuer sicheres Laden

    public EconomyHook() {
        Object found = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                found = Bridge.load();
            } catch (Throwable ignored) {
                // Vault/Economy nicht verfuegbar
            }
        }
        this.economy = found;
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return isEnabled() && Bridge.has((Economy) economy, player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return isEnabled() && Bridge.withdraw((Economy) economy, player, amount);
    }

    public boolean deposit(Player player, double amount) {
        return isEnabled() && Bridge.deposit((Economy) economy, player, amount);
    }

    public double balance(Player player) {
        return isEnabled() ? Bridge.balance((Economy) economy, player) : 0.0;
    }

    public String format(double amount) {
        return isEnabled() ? Bridge.format((Economy) economy, amount) : String.valueOf(amount);
    }

    private static final class Bridge {
        static Economy load() {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp == null ? null : rsp.getProvider();
        }

        static boolean has(Economy eco, Player player, double amount) {
            return eco.has(player, amount);
        }

        static boolean withdraw(Economy eco, Player player, double amount) {
            return eco.withdrawPlayer(player, amount).transactionSuccess();
        }

        static boolean deposit(Economy eco, Player player, double amount) {
            return eco.depositPlayer(player, amount).transactionSuccess();
        }

        static double balance(Economy eco, Player player) {
            return eco.getBalance(player);
        }

        static String format(Economy eco, double amount) {
            return eco.format(amount);
        }
    }
}
