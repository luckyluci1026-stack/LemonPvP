package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Unlocks a kill effect when LemonCore awards one.
 *
 * <p>This lives apart from {@link PlayerListener} on purpose: its handler is typed on a
 * LemonCore class, and LemonCore is only a <em>softdepend</em>. Bukkit resolves every handler's
 * parameter type when a listener is registered, so a LemonCore-typed method inside
 * PlayerListener would make the whole registration fail when LemonCore is absent — silently
 * taking the join, quit and cosmetics-menu handlers down with it. Keeping it separate means the
 * rest of the cosmetics still work, and this listener is simply not registered.
 */
public class KillEffectRewardListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;

    public KillEffectRewardListener(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKillEffectReward(com.lemonpvp.lemoncore.events.KillEffectRewardEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;

        String effectId = event.getEffectId();
        if (KillEffectType.fromId(effectId).isEmpty()) {
            plugin.getLogger().warning("Received unknown kill effect id: " + effectId);
            return;
        }

        UUID effectUuid = event.getPlayerUuid();
        plugin.getCosmeticsManager().unlockKillEffect(effectUuid, effectId)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(effectUuid);
                    if (p == null) return;
                    p.sendMessage(MM.deserialize("<green>Unlocked kill effect: <yellow>"
                            + KillEffectType.fromId(effectId).map(KillEffectType::getDisplayName).orElse(effectId)
                            + "</yellow>!"));
                }));
    }
}
