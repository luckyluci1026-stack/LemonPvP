package de.lemonpvp.lifesteal.hearts;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentrale Kill-/Tod-Logik: Herz-Transfer, Anti-Farm und Elimination.
 * Wird von PlayerDeathEvent UND vom BetterSMP-CombatLog genutzt.
 */
public final class KillHandler {

    private final LifestealPlus plugin;
    private final HeartsManager hearts;
    private final Eliminations eliminations;
    private final Map<String, Long> lastKill = new ConcurrentHashMap<>();

    public KillHandler(LifestealPlus plugin, HeartsManager hearts, Eliminations eliminations) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.eliminations = eliminations;
    }

    /**
     * @param victim     der gestorbene Spieler (online)
     * @param killer     UUID des Toeters (oder null bei Nicht-PvP-Tod)
     * @param killerName Anzeigename des Toeters
     * @param pvp        true, wenn es ein Spieler-Kill war
     */
    public void handle(Player victim, UUID killer, String killerName, boolean pvp) {
        int lose = plugin.getConfig().getInt("hearts.lose-per-death", 1);
        int gain = plugin.getConfig().getInt("hearts.gain-per-kill", 1);

        if (pvp && killer != null && !killer.equals(victim.getUniqueId())) {
            String block = antiFarm(killer, victim);
            if (block == null) {
                if (hearts.getHearts(killer) < hearts.maximum()) {
                    hearts.addHearts(killer, gain);
                    notify(killer, "kill-gained",
                            "amount", String.valueOf(gain),
                            "victim", victim.getName(),
                            "hearts", String.valueOf(hearts.getHearts(killer)));
                } else {
                    notify(killer, "max-reached");
                }
            } else {
                notify(killer, block.equals("ip") ? "anti-farm-ip" : "anti-farm",
                        "victim", victim.getName());
            }
        }

        boolean loseHearts = pvp
                || plugin.getConfig().getBoolean("hearts.lose-on-non-pvp-death", false);
        if (!loseHearts) {
            return;
        }
        hearts.addHearts(victim.getUniqueId(), -lose);
        int now = hearts.getHearts(victim.getUniqueId());
        if (pvp) {
            plugin.msgs().send(victim, "death-lost",
                    "amount", String.valueOf(lose), "killer", killerName,
                    "hearts", String.valueOf(now));
        } else {
            plugin.msgs().send(victim, "death-lost-generic",
                    "amount", String.valueOf(lose), "hearts", String.valueOf(now));
        }

        if (now <= hearts.eliminateAt()) {
            eliminations.eliminate(victim);
        }
    }

    /** @return null wenn Herzen erlaubt sind, sonst "ip" oder "farm". */
    private String antiFarm(UUID killer, Player victim) {
        if (!plugin.getConfig().getBoolean("anti-farm.enabled", true)) {
            return null;
        }
        Player killerPlayer = Bukkit.getPlayer(killer);
        if (plugin.getConfig().getBoolean("anti-farm.block-same-ip", true)
                && killerPlayer != null && sameIp(killerPlayer, victim)) {
            return "ip";
        }
        long cooldown = plugin.getConfig().getLong("anti-farm.same-victim-cooldown", 300) * 1000L;
        if (cooldown > 0) {
            String key = killer + ":" + victim.getUniqueId();
            long nowMs = System.currentTimeMillis();
            Long last = lastKill.get(key);
            if (last != null && nowMs - last < cooldown) {
                return "farm";
            }
            lastKill.put(key, nowMs);
        }
        return null;
    }

    private boolean sameIp(Player a, Player b) {
        if (a.getAddress() == null || b.getAddress() == null
                || a.getAddress().getAddress() == null || b.getAddress().getAddress() == null) {
            return false;
        }
        return a.getAddress().getAddress().getHostAddress()
                .equals(b.getAddress().getAddress().getHostAddress());
    }

    private void notify(UUID uuid, String path, String... rep) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.msgs().send(player, path, rep);
        }
    }
}
