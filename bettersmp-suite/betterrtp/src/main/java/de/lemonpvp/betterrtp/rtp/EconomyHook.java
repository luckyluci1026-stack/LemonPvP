package de.lemonpvp.betterrtp.rtp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optionaler Vault-Economy-Hook (nutzt z.B. EssentialsX-Economy).
 * Die Vault-Klassen werden nur über die innere Bridge angefasst, damit das
 * Plugin auch ganz ohne Vault lädt.
 */
public final class EconomyHook {

    private final Object economy; // Economy, aber als Object gehalten für sicheres Laden

    public EconomyHook() {
        Object found = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                found = Bridge.load();
            } catch (Throwable ignored) {
                // Vault/Economy nicht verfügbar
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

        static String format(Economy eco, double amount) {
            return eco.format(amount);
        }
    }
}
