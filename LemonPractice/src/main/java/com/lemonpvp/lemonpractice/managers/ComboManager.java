package com.lemonpvp.lemonpractice.managers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks hit combos during duels — the "Combo x5" action-bar staple of the big
 * PvP practice servers. Each landed hit on an opponent bumps the attacker's
 * combo and breaks the victim's own combo; a combo lapses after a few seconds of
 * not connecting (so it never carries between fights). Pure feedback — it never
 * touches damage.
 */
public class ComboManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final long TIMEOUT_MS = 4000L;

    private static final class Entry { int combo; long lastHit; }

    private final Map<UUID, Entry> combos = new ConcurrentHashMap<>();

    /** Records a hit from {@code attacker} on {@code victim}; shows the attacker their combo. */
    public void onHit(UUID attacker, UUID victim) {
        long now = System.currentTimeMillis();
        Entry a = combos.computeIfAbsent(attacker, k -> new Entry());
        if (now - a.lastHit > TIMEOUT_MS) a.combo = 0;
        a.combo++;
        a.lastHit = now;

        // Getting hit breaks the victim's own offensive combo.
        Entry v = combos.get(victim);
        if (v != null) v.combo = 0;

        if (a.combo >= 2) showCombo(attacker, a.combo);
    }

    public void clear(UUID uuid) {
        combos.remove(uuid);
    }

    private void showCombo(UUID attacker, int combo) {
        Player p = Bukkit.getPlayer(attacker);
        if (p == null || !p.isOnline()) return;
        // Colour ramps with the streak; pitch rises so a long combo feels earned.
        String color = combo >= 10 ? "<gradient:#ff5252:#ff9800>"
                     : combo >= 6  ? "<gradient:#ff9800:#fffb00>"
                     : combo >= 4  ? "<yellow>"
                     : "<gray>";
        p.sendActionBar(MM.deserialize("<!italic>" + color + "<bold>⚔ " + combo + " Combo"));
        float pitch = (float) Math.min(2.0, 1.0 + combo * 0.06);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
    }
}
