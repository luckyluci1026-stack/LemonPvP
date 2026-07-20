package de.lemonpvp.bettersmp.combat;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Kampf-Tags: Wer kaempft mit wem und wie lange noch.
 * Ein Tick-Task (1x pro Sekunde) aktualisiert Actionbar und laesst Tags auslaufen.
 */
public final class CombatManager {

    private record Tag(long until, UUID opponent) {
    }

    private final BetterSMP plugin;
    private final Map<UUID, Tag> tags = new ConcurrentHashMap<>();
    private BukkitTask task;

    public CombatManager(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        tags.clear();
    }

    private long tagMillis() {
        return plugin.getConfig().getLong("combat.tag-seconds", 15) * 1000L;
    }

    public void tag(Player a, Player b) {
        long until = System.currentTimeMillis() + tagMillis();
        boolean aNew = !isTagged(a.getUniqueId());
        boolean bNew = !isTagged(b.getUniqueId());
        tags.put(a.getUniqueId(), new Tag(until, b.getUniqueId()));
        tags.put(b.getUniqueId(), new Tag(until, a.getUniqueId()));
        String seconds = String.valueOf(tagMillis() / 1000);
        if (aNew) {
            plugin.msgs().send(a, "combat.tagged", "opponent", b.getName(), "seconds", seconds);
        }
        if (bNew) {
            plugin.msgs().send(b, "combat.tagged", "opponent", a.getName(), "seconds", seconds);
        }
    }

    public boolean isTagged(UUID uuid) {
        Tag tag = tags.get(uuid);
        return tag != null && tag.until() > System.currentTimeMillis();
    }

    public UUID opponent(UUID uuid) {
        Tag tag = tags.get(uuid);
        return tag == null ? null : tag.opponent();
    }

    public long remainingMillis(UUID uuid) {
        Tag tag = tags.get(uuid);
        if (tag == null) {
            return 0L;
        }
        return Math.max(0L, tag.until() - System.currentTimeMillis());
    }

    /** Tag entfernen, ohne "frei"-Nachricht. */
    public void untag(UUID uuid) {
        tags.remove(uuid);
    }

    /** Tag entfernen und dem Spieler (falls online) Bescheid geben. */
    public void release(UUID uuid) {
        if (tags.remove(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.msgs().send(player, "combat.expired");
            }
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean actionbar = plugin.getConfig().getBoolean("combat.actionbar", true);
        for (Map.Entry<UUID, Tag> entry : tags.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (entry.getValue().until() <= now) {
                tags.remove(entry.getKey());
                if (player != null) {
                    plugin.msgs().send(player, "combat.expired");
                }
                continue;
            }
            if (actionbar && player != null) {
                long seconds = Math.max(1, (entry.getValue().until() - now + 999) / 1000);
                player.sendActionBar(plugin.msgs().format("combat.actionbar",
                        "seconds", String.valueOf(seconds)));
            }
        }
    }
}
