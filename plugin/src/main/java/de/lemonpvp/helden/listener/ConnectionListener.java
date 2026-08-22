package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Join und Quit - inklusive Combat-Log-Puppe. */
public final class ConnectionListener implements Listener {

    private final HeldenPlugin plugin;

    public ConnectionListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HeldenProfile profile = plugin.profiles().getOrCreate(player);

        plugin.hud().setup(player);
        plugin.game().enforceState(player);

        // Rechtzeitig zurueck: die Puppe wird eingesammelt, alles bleibt ihm.
        plugin.dummies().rescue(player);

        if (!profile.eliminated()) {
            plugin.combat().protect(player, plugin.settings().respawnProtectionSeconds());
        }

        event.setJoinMessage(null);
        plugin.messages().broadcastRaw("join.join-broadcast",
                "%player%", player.getName(),
                "%hearts%", profile.hearts());
        plugin.messages().sendList(player, "join.welcome",
                "%project%", plugin.settings().projectName(),
                "%player%", player.getName(),
                "%hearts%", profile.hearts());

        if (plugin.settings().sendControlsHint() && plugin.bedrock().isBedrock(player)) {
            plugin.messages().send(player, "join.bedrock-hint");
        }

        // Falls jemand nach einem Neustart auf dem Link-Herz steht, aber keinen
        // Partner mehr hat.
        plugin.links().assignIfNeeded(profile);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        HeldenProfile profile = plugin.profiles().get(player);

        if (profile != null && shouldLeaveDummy(player, profile)) {
            plugin.dummies().spawnFor(player);
        }
        if (profile != null) {
            profile.lastSeen(System.currentTimeMillis());
        }

        event.setQuitMessage(null);
        plugin.messages().broadcastRaw("join.quit-broadcast", "%player%", player.getName());

        plugin.hud().remove(player);
        plugin.combat().clear(player);
    }

    private boolean shouldLeaveDummy(Player player, HeldenProfile profile) {
        return plugin.settings().dummyEnabled()
                && !profile.eliminated()
                && plugin.combat().isTagged(player);
    }
}
