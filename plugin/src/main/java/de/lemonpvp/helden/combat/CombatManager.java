package de.lemonpvp.helden.combat;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kampfstatus: Combat-Tag, Spawnschutz und die Frage, wer einen Tod zu
 * verantworten hat.
 *
 * <p>Der letzte Angreifer wird mitgeschrieben, weil im Projekt nur Tode durch
 * andere Spieler ein Herz kosten - und weil ein Opfer nach einem Treffer noch
 * in die Lava fallen kann.</p>
 */
public final class CombatManager {

    private record LastHit(UUID attacker, long at) {
    }

    private final HeldenPlugin plugin;

    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, LastHit> lastHits = new HashMap<>();

    public CombatManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    /** Markiert Opfer und Angreifer als "im Kampf" und merkt sich den Treffer. */
    public void tag(Player victim, Player attacker) {
        if (attacker != null && !attacker.equals(victim)) {
            lastHits.put(victim.getUniqueId(), new LastHit(attacker.getUniqueId(), System.currentTimeMillis()));
        }

        int seconds = plugin.settings().combatTagSeconds();
        if (seconds <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + seconds * 1000L;

        applyTag(victim, until, seconds);
        if (attacker != null && !attacker.equals(victim)) {
            applyTag(attacker, until, seconds);
        }
    }

    private void applyTag(Player player, long until, int seconds) {
        boolean wasTagged = isTagged(player);
        combatUntil.put(player.getUniqueId(), until);
        if (!wasTagged) {
            plugin.messages().send(player, "combat.tagged", "%seconds%", seconds);
        }
    }

    public boolean isTagged(Player player) {
        return tagRemaining(player) > 0;
    }

    public long tagRemaining(Player player) {
        return remaining(combatUntil, player.getUniqueId());
    }

    public void clearTag(Player player) {
        combatUntil.remove(player.getUniqueId());
    }

    public void protect(Player player, int seconds) {
        if (seconds > 0) {
            protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        }
    }

    public boolean isProtected(Player player) {
        return protectionRemaining(player) > 0;
    }

    public long protectionRemaining(Player player) {
        return remaining(protectedUntil, player.getUniqueId());
    }

    public void clearProtection(Player player) {
        protectedUntil.remove(player.getUniqueId());
    }

    private long remaining(Map<UUID, Long> map, UUID uuid) {
        Long until = map.get(uuid);
        if (until == null) {
            return 0L;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            map.remove(uuid);
            return 0L;
        }
        return TimeUtil.toSecondsCeil(left);
    }

    /**
     * Der Spieler, dem dieser Tod angerechnet wird - oder {@code null}, wenn
     * zu lange kein Spieler mehr getroffen hat.
     */
    public UUID lastAttacker(Player victim) {
        LastHit hit = lastHits.get(victim.getUniqueId());
        if (hit == null) {
            return null;
        }
        long window = plugin.settings().pvpCreditSeconds() * 1000L;
        return System.currentTimeMillis() - hit.at() <= window ? hit.attacker() : null;
    }

    public void clearLastAttacker(Player victim) {
        lastHits.remove(victim.getUniqueId());
    }

    /** Alles zu einem Spieler vergessen. */
    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        combatUntil.remove(uuid);
        protectedUntil.remove(uuid);
        lastHits.remove(uuid);
    }

    public void clearAll() {
        combatUntil.clear();
        protectedUntil.clear();
        lastHits.clear();
    }
}
