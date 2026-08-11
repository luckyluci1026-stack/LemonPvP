package de.lemonpvp.helden.combat;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kampfstatus: Combat-Tag, Spawnschutz und wer zuletzt Schaden gemacht hat.
 *
 * <p>Die Schadensbeitraege werden pro Opfer mitgeschrieben, damit beim Tod auch
 * Assists vergeben werden koennen.</p>
 */
public final class CombatManager {

    /** So lange zaehlt ein Schadensbeitrag noch als Assist. */
    private static final long ASSIST_WINDOW_MILLIS = 20_000L;

    private final HeldenPlugin plugin;

    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> contributors = new HashMap<>();

    public CombatManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    /** Markiert Opfer und Angreifer als "im Kampf". */
    public void tag(Player victim, Player attacker) {
        int seconds = plugin.settings().combatTagSeconds();
        if (seconds <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + seconds * 1000L;

        boolean wasTagged = isTagged(victim);
        combatUntil.put(victim.getUniqueId(), until);
        if (!wasTagged) {
            plugin.messages().send(victim, "combat.tagged", "%seconds%", seconds);
        }

        if (attacker != null && !attacker.equals(victim)) {
            boolean attackerWasTagged = isTagged(attacker);
            combatUntil.put(attacker.getUniqueId(), until);
            if (!attackerWasTagged) {
                plugin.messages().send(attacker, "combat.tagged", "%seconds%", seconds);
            }
            contributors.computeIfAbsent(victim.getUniqueId(), ignored -> new LinkedHashMap<>())
                    .put(attacker.getUniqueId(), System.currentTimeMillis());
        }
    }

    public boolean isTagged(Player player) {
        return tagRemaining(player) > 0;
    }

    public long tagRemaining(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            combatUntil.remove(player.getUniqueId());
            return 0L;
        }
        return TimeUtil.toSecondsCeil(remaining);
    }

    public void clearTag(Player player) {
        combatUntil.remove(player.getUniqueId());
    }

    public void protect(Player player, int seconds) {
        if (seconds <= 0) {
            return;
        }
        protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public boolean isProtected(Player player) {
        return protectionRemaining(player) > 0;
    }

    public long protectionRemaining(Player player) {
        Long until = protectedUntil.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            protectedUntil.remove(player.getUniqueId());
            return 0L;
        }
        return TimeUtil.toSecondsCeil(remaining);
    }

    public void clearProtection(Player player) {
        protectedUntil.remove(player.getUniqueId());
    }

    /** Wer dem Opfer zuletzt Schaden gemacht hat - fuer Combat-Log-Kills. */
    public UUID lastAttacker(Player victim) {
        Map<UUID, Long> map = contributors.get(victim.getUniqueId());
        if (map == null || map.isEmpty()) {
            return null;
        }
        UUID latest = null;
        long latestTime = Long.MIN_VALUE;
        for (Map.Entry<UUID, Long> entry : map.entrySet()) {
            if (entry.getValue() > latestTime) {
                latestTime = entry.getValue();
                latest = entry.getKey();
            }
        }
        return System.currentTimeMillis() - latestTime <= ASSIST_WINDOW_MILLIS ? latest : null;
    }

    /** Alle Assistenten des Opfers ohne den Killer selbst. */
    public List<UUID> assistants(Player victim, UUID killer) {
        List<UUID> result = new ArrayList<>();
        Map<UUID, Long> map = contributors.get(victim.getUniqueId());
        if (map == null) {
            return result;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : map.entrySet()) {
            if (entry.getKey().equals(killer) || entry.getKey().equals(victim.getUniqueId())) {
                continue;
            }
            if (now - entry.getValue() <= ASSIST_WINDOW_MILLIS) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void clearContributors(Player victim) {
        contributors.remove(victim.getUniqueId());
    }

    /** Schreibt dem Killer Kill, Zitronen, Serie und ein evtl. Kopfgeld gut. */
    public void rewardKill(Player killer, Player victim) {
        HeldenProfile profile = plugin.profiles().getOrCreate(killer);
        profile.addKill();

        int reward = plugin.settings().rewardPerKill()
                + profile.killStreak() * plugin.settings().killstreakBonus();
        plugin.economy().give(killer, reward, "economy.reason-kill");

        if (profile.killStreak() >= plugin.settings().announceKillstreakFrom()) {
            plugin.messages().broadcastRaw("combat.killstreak",
                    "%player%", killer.getName(),
                    "%streak%", profile.killStreak());
        }
        plugin.events().claimBounty(killer, victim);
    }

    /** Belohnt alle, die dem Opfer kurz vorher noch Schaden gemacht haben. */
    public void rewardAssists(Player victim, UUID killerId) {
        for (UUID uuid : assistants(victim, killerId)) {
            Player assistant = Bukkit.getPlayer(uuid);
            if (assistant == null) {
                continue;
            }
            plugin.profiles().getOrCreate(assistant).addAssist();
            plugin.economy().give(assistant, plugin.settings().rewardPerAssist(), "economy.reason-assist");
        }
    }

    /** Alles zu einem Spieler vergessen (Quit, Reload). */
    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        combatUntil.remove(uuid);
        protectedUntil.remove(uuid);
        contributors.remove(uuid);
        for (Map<UUID, Long> map : contributors.values()) {
            map.remove(uuid);
        }
    }

    public void clearAll() {
        combatUntil.clear();
        protectedUntil.clear();
        contributors.clear();
    }
}
