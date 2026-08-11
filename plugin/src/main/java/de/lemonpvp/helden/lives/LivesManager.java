package de.lemonpvp.helden.lives;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.item.ItemRegistry;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Das Lebenssystem des Projekts: jeder Tod kostet ein Leben, bei null ist man
 * "gefallen" und braucht ein Heldenherz.
 */
public final class LivesManager {

    private final HeldenPlugin plugin;

    public LivesManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.settings().livesEnabled();
    }

    public int lives(HeldenProfile profile) {
        return profile == null ? 0 : profile.lives();
    }

    public boolean isFallen(Player player) {
        HeldenProfile profile = plugin.profiles().get(player);
        return profile != null && profile.fallen();
    }

    /** Zieht nach einem Tod ein Leben ab und laesst den Spieler ggf. fallen. */
    public void handleDeath(Player player) {
        if (!enabled()) {
            return;
        }
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.fallen()) {
            return;
        }

        profile.lives(profile.lives() - 1);
        if (profile.lives() > 0) {
            plugin.messages().send(player, "lives.lost", "%lives%", profile.lives());
            return;
        }

        profile.fallen(true);
        plugin.messages().broadcastRaw("lives.fallen-broadcast", "%player%", player.getName());
        plugin.messages().send(player, "lives.fallen-self");
        applyFallenState(player);
    }

    /** Setzt den Zustand fuer 0 Leben durch (Zuschauer, Kick oder nichts). */
    public void applyFallenState(Player player) {
        Settings.ZeroLivesAction action = plugin.settings().zeroLivesAction();
        switch (action) {
            case SPECTATOR -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            });
            case KICK -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(plugin.messages().plain("lives.fallen-self"));
                }
            });
            case NOTHING -> {
                // Nichts zu tun - der Spieler bleibt normal im Spiel.
            }
        }
    }

    /** Stellt beim Join den richtigen Zustand her. */
    public void restoreOnJoin(Player player) {
        if (!enabled()) {
            return;
        }
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.fallen()) {
            applyFallenState(player);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    /** Gibt Leben, respektiert aber das konfigurierte Maximum. */
    public boolean addLives(Player player, int amount) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.lives() >= plugin.settings().livesMax()) {
            plugin.messages().send(player, "lives.max-reached", "%max%", plugin.settings().livesMax());
            return false;
        }
        profile.lives(Math.min(plugin.settings().livesMax(), profile.lives() + amount));
        plugin.messages().send(player, "lives.gained", "%lives%", profile.lives());
        return true;
    }

    /**
     * Belebt einen gefallenen Spieler wieder.
     *
     * @return {@code true}, wenn die Wiederbelebung geklappt hat
     */
    public boolean revive(Player healer, Player target) {
        if (!enabled()) {
            plugin.messages().send(healer, "lives.disabled");
            return false;
        }
        if (!plugin.settings().reviveEnabled()) {
            plugin.messages().send(healer, "lives.revive-disabled");
            return false;
        }
        if (healer.equals(target)) {
            plugin.messages().send(healer, "lives.revive-self");
            return false;
        }

        HeldenProfile targetProfile = plugin.profiles().getOrCreate(target);
        if (!targetProfile.fallen()) {
            plugin.messages().send(healer, "lives.revive-not-fallen", "%player%", target.getName());
            return false;
        }
        if (!consumeReviveItem(healer)) {
            plugin.messages().send(healer, "lives.revive-no-item");
            return false;
        }
        if (!plugin.economy().take(healer, plugin.settings().reviveCost(), "economy.reason-revive")) {
            // Das Herz wieder zurueckgeben, sonst ist es fuer nichts weg.
            ItemStack refund = plugin.items().stack(plugin.settings().reviveItemId(), 1);
            if (refund != null) {
                healer.getInventory().addItem(refund);
            }
            return false;
        }

        targetProfile.fallen(false);
        targetProfile.lives(plugin.settings().reviveLives());
        target.setGameMode(GameMode.SURVIVAL);

        Location spawn = plugin.spawnFor(target);
        if (spawn != null) {
            target.teleport(spawn);
        }
        plugin.combat().protect(target, plugin.settings().reviveProtectionSeconds());
        plugin.heroes().giveKit(target, plugin.heroes().of(target));
        plugin.hud().update(target);

        plugin.messages().broadcastRaw("lives.revived",
                "%player%", target.getName(),
                "%healer%", healer.getName());
        plugin.messages().send(target, "lives.revive-protection",
                "%seconds%", plugin.settings().reviveProtectionSeconds());
        Compat.sound(target, "ui.toast.challenge_complete", 1.0f, 1.0f);
        return true;
    }

    /** Nimmt dem Heiler genau ein Wiederbelebungs-Artefakt ab. */
    private boolean consumeReviveItem(Player healer) {
        String reviveItemId = plugin.settings().reviveItemId();
        ItemStack[] contents = healer.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!ItemRegistry.is(stack, reviveItemId)) {
                continue;
            }
            if (stack.getAmount() <= 1) {
                healer.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
            return true;
        }
        return false;
    }
}
