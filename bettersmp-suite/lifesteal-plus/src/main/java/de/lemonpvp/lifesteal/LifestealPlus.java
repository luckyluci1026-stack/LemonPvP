package de.lemonpvp.lifesteal;

import de.lemonpvp.lifesteal.command.HeartsCommand;
import de.lemonpvp.lifesteal.command.LifestealAdminCommand;
import de.lemonpvp.lifesteal.command.ReviveCommand;
import de.lemonpvp.lifesteal.command.WithdrawCommand;
import de.lemonpvp.lifesteal.hearts.Eliminations;
import de.lemonpvp.lifesteal.hearts.HeartItems;
import de.lemonpvp.lifesteal.hearts.HeartsManager;
import de.lemonpvp.lifesteal.hearts.KillHandler;
import de.lemonpvp.lifesteal.hook.CombatLogBridge;
import de.lemonpvp.lifesteal.hook.LifestealPapiExpansion;
import de.lemonpvp.lifesteal.listener.ItemListener;
import de.lemonpvp.lifesteal.listener.LifestealListener;
import de.lemonpvp.lifesteal.revive.ReviveManager;
import de.lemonpvp.lifesteal.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lifesteal+ - Herzverlust, Herz-Items, Elimination/Revive und
 * BetterSMP-CombatLog-Integration.
 */
public final class LifestealPlus extends JavaPlugin {

    private Msgs msgs;
    private HeartsManager hearts;
    private Eliminations eliminations;
    private HeartItems items;
    private KillHandler killHandler;
    private ReviveManager revive;

    private final Set<UUID> combatLogGuard = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.hearts = new HeartsManager(this);
        this.eliminations = new Eliminations(this, hearts);
        this.items = new HeartItems(this);
        this.killHandler = new KillHandler(this, hearts, eliminations);
        this.revive = new ReviveManager(this);

        items.registerRecipes();

        Bukkit.getPluginManager().registerEvents(new LifestealListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(revive, this);

        getCommand("hearts").setExecutor(new HeartsCommand(this));
        getCommand("withdraw").setExecutor(new WithdrawCommand(this));
        getCommand("revive").setExecutor(new ReviveCommand(this));
        getCommand("lifesteal").setExecutor(new LifestealAdminCommand(this));

        // Herzen fuer bereits online befindliche Spieler synchronisieren (Reload)
        Bukkit.getOnlinePlayers().forEach(player -> {
            hearts.syncOnJoin(player);
            eliminations.enforceOnJoin(player);
        });

        hookBetterSMP();
        hookPlaceholderApi();

        getLogger().info("Lifesteal+ aktiviert.");
    }

    @Override
    public void onDisable() {
        if (hearts != null) {
            hearts.save();
        }
    }

    private void hookBetterSMP() {
        if (Bukkit.getPluginManager().getPlugin("BetterSMP") != null
                && getConfig().getBoolean("combatlog.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new CombatLogBridge(this), this);
            getLogger().info("BetterSMP-CombatLog-Integration aktiv.");
        } else {
            getLogger().info("BetterSMP nicht gefunden - CombatLog-Integration inaktiv.");
        }
    }

    private void hookPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new LifestealPapiExpansion(this).register();
                getLogger().info("PlaceholderAPI-Expansion registriert.");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI-Expansion fehlgeschlagen: " + t.getMessage());
            }
        }
    }

    /** Markiert einen kommenden Tod als bereits vom CombatLog behandelt. */
    public void addCombatLogGuard(UUID uuid) {
        combatLogGuard.add(uuid);
        Bukkit.getScheduler().runTaskLater(this, () -> combatLogGuard.remove(uuid), 40L);
    }

    /** Gibt true zurueck (und entfernt), wenn dieser Tod schon behandelt wurde. */
    public boolean consumeCombatLogGuard(UUID uuid) {
        return combatLogGuard.remove(uuid);
    }

    public Msgs msgs() {
        return msgs;
    }

    public HeartsManager hearts() {
        return hearts;
    }

    public Eliminations eliminations() {
        return eliminations;
    }

    public HeartItems items() {
        return items;
    }

    public KillHandler killHandler() {
        return killHandler;
    }

    public ReviveManager revive() {
        return revive;
    }
}
