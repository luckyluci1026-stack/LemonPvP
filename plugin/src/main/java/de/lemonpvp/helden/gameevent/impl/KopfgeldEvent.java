package de.lemonpvp.helden.gameevent.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.gameevent.GameEvent;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Text;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Kopfgeld: auf einen zufaelligen Spieler wird eine Belohnung ausgesetzt. */
public final class KopfgeldEvent extends GameEvent {

    private UUID target;
    private String targetName = "";

    public KopfgeldEvent(HeldenPlugin plugin) {
        super(plugin, "kopfgeld");
    }

    @Override
    public int durationSeconds() {
        return option("duration-seconds", 900);
    }

    public int reward() {
        return option("reward", 1000);
    }

    public UUID target() {
        return target;
    }

    public String targetName() {
        return targetName;
    }

    @Override
    public String description() {
        return Text.replace(plugin.messages().raw("events.kopfgeld-description"),
                "%target%", targetName,
                "%reward%", reward());
    }

    @Override
    public boolean onStart() {
        List<Player> candidates = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            HeldenProfile profile = plugin.profiles().get(player);
            if (profile != null && profile.fallen()) {
                continue;
            }
            candidates.add(player);
        }
        if (candidates.size() < 2) {
            plugin.messages().broadcastRaw("events.kopfgeld-no-target");
            return false;
        }

        Player chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        target = chosen.getUniqueId();
        targetName = chosen.getName();
        Compat.sound(chosen, "entity.wither.ambient", 0.8f, 1.2f);
        return true;
    }

    @Override
    public void onTick(int secondsElapsed) {
        if (target == null || secondsElapsed % 5 != 0) {
            return;
        }
        Player player = plugin.getServer().getPlayer(target);
        if (player == null) {
            return;
        }
        Compat.spawnParticle(player.getWorld(), Compat.particle("WAX_ON", "CRIT"),
                player.getLocation().add(0, 2.3, 0), 8, 0.3, 0.2, 0.3, 0.0);
    }

    @Override
    public void onStop() {
        target = null;
        targetName = "";
    }

    /**
     * Zahlt das Kopfgeld aus, wenn das Ziel besiegt wurde.
     *
     * @return {@code true}, wenn ausgezahlt wurde
     */
    public boolean claim(Player killer, Player victim) {
        if (target == null || killer == null || !victim.getUniqueId().equals(target)) {
            return false;
        }
        plugin.economy().give(killer, reward(), "economy.reason-bounty");
        plugin.messages().broadcastRaw("events.kopfgeld-claimed",
                "%killer%", killer.getName(),
                "%target%", victim.getName(),
                "%reward%", reward());
        target = null;
        targetName = "";
        return true;
    }
}
