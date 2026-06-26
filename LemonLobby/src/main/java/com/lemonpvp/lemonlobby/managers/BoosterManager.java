package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.model.BoosterTier;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BoosterManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonLobby plugin;
    private final Map<UUID, Database.BoosterEntry> cache    = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar>               bossBars = new ConcurrentHashMap<>();

    public BoosterManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    // ── Load / Unload ─────────────────────────────────────────────────────────

    public CompletableFuture<Void> load(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Database.BoosterEntry entry = plugin.getDatabase().loadBoosterEntry(uuid);
            if (entry != null && isActive(entry)) cache.put(uuid, entry);
        });
    }

    /** Called from main thread after load() completes to show the bossbar. */
    public void showBoosterBar(Player player) {
        Database.BoosterEntry entry = cache.get(player.getUniqueId());
        if (entry == null || !isActive(entry)) return;
        BossBar bar = buildBar(entry);
        bossBars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.hideBossBar(bar);
        }
    }

    // ── Tick (called every second from LemonLobby task) ───────────────────────

    public void tickBossBars() {
        for (var it = bossBars.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            UUID   uuid = entry.getKey();
            BossBar bar  = entry.getValue();

            Player p = Bukkit.getPlayer(uuid);
            if (p == null) { it.remove(); continue; }

            Database.BoosterEntry cached = cache.get(uuid);
            if (cached == null || !isActive(cached)) {
                cache.remove(uuid);
                p.hideBossBar(bar);
                it.remove();
                p.sendMessage(MM.deserialize(
                        "<!italic><gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient>"
                        + " <dark_gray>»</dark_gray> <red>Dein Verstärker ist abgelaufen!"));
                continue;
            }

            long remaining = cached.expiresAt() - System.currentTimeMillis();
            long total     = (long) cached.tier().durationSeconds * 1_000;
            float progress = Math.max(0f, Math.min(1f, (float) remaining / total));
            bar.name(buildBarTitle(cached.tier(), remaining));
            bar.progress(progress);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public int getBonus(UUID uuid) {
        Database.BoosterEntry entry = cache.get(uuid);
        if (entry == null || !isActive(entry)) { cache.remove(uuid); return 0; }
        return entry.tier().bonusApples;
    }

    public Database.BoosterEntry getActive(UUID uuid) {
        Database.BoosterEntry entry = cache.get(uuid);
        if (entry == null || !isActive(entry)) { cache.remove(uuid); return null; }
        return entry;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public void activate(UUID uuid, BoosterTier tier) {
        long expiresAt = System.currentTimeMillis() + (long) tier.durationSeconds * 1_000;
        Database.BoosterEntry entry = new Database.BoosterEntry(tier, expiresAt);
        cache.put(uuid, entry);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().saveBoosterEntry(uuid, tier, expiresAt));

        // Bossbar (activate is always called from main thread)
        BossBar bar = buildBar(entry);
        BossBar old = bossBars.put(uuid, bar);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            if (old != null) p.hideBossBar(old);
            p.showBossBar(bar);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isActive(Database.BoosterEntry entry) {
        return System.currentTimeMillis() < entry.expiresAt();
    }

    private BossBar buildBar(Database.BoosterEntry entry) {
        long remaining = entry.expiresAt() - System.currentTimeMillis();
        long total     = (long) entry.tier().durationSeconds * 1_000;
        float progress = Math.max(0f, Math.min(1f, (float) remaining / total));
        return BossBar.bossBar(
                buildBarTitle(entry.tier(), remaining),
                progress,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS);
    }

    private Component buildBarTitle(BoosterTier tier, long remainingMs) {
        return MM.deserialize("<!italic><gold>⚡ Verstärker " + tier.displayName
                + " <yellow>▸ <white>" + formatDuration(remainingMs) + " verbleibend");
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0s";
        long mins = millis / 60_000;
        long secs = (millis / 1_000) % 60;
        return mins > 0 ? mins + "m " + secs + "s" : secs + "s";
    }
}
