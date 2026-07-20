package de.lemonpvp.lifesteal.revive;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet die Revive-Item-Eingabe: Nach Rechtsklick tippt der Spieler den
 * Zielnamen in den Chat. Verbraucht bei Erfolg genau ein Revive-Item.
 */
public final class ReviveManager implements Listener {

    private final LifestealPlus plugin;
    private final Map<UUID, Long> pending = new ConcurrentHashMap<>();
    private static final long PROMPT_TIMEOUT_MS = 30_000;

    public ReviveManager(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    public void startPrompt(Player player) {
        pending.put(player.getUniqueId(), System.currentTimeMillis());
        plugin.msgs().send(player, "revive-prompt");
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Long started = pending.get(player.getUniqueId());
        if (started == null) {
            return;
        }
        event.setCancelled(true);
        pending.remove(player.getUniqueId());

        if (System.currentTimeMillis() - started > PROMPT_TIMEOUT_MS) {
            plugin.msgs().send(player, "revive-cancelled");
            return;
        }
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (input.equalsIgnoreCase("abbrechen") || input.equalsIgnoreCase("cancel")) {
            plugin.msgs().send(player, "revive-cancelled");
            return;
        }

        // Zurueck auf den Main-Thread fuer Inventar-/Welt-Zugriffe
        Bukkit.getScheduler().runTask(plugin, () -> resolve(player, input));
    }

    private void resolve(Player reviver, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(name);
        }
        UUID targetId = target.getUniqueId();
        if (!plugin.hearts().isEliminated(targetId)) {
            plugin.msgs().send(reviver, "revive-not-eliminated");
            return;
        }
        if (plugin.eliminations().revive(targetId, reviver)) {
            consumeOneRevive(reviver);
        }
    }

    private void consumeOneRevive(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.items().isRevive(item)) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }
}
