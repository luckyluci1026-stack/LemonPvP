package com.lemonpvp.lemonresourcepack;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class ResourcePackListener {

    private final LemonResourcePack plugin;
    private final ProxyServer server;
    private final Logger logger;

    public ResourcePackListener(LemonResourcePack plugin, ProxyServer server, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Send pack when player finishes connecting to any backend server
    // -------------------------------------------------------------------------

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnect(ServerPostConnectEvent event) {
        // Only send on initial join (previousServer is null)
        if (event.getPreviousServer() != null) return;

        String url = plugin.getPackUrl();
        if (url == null || url.isBlank() || url.startsWith("https://example.com")) return;

        var player = event.getPlayer();

        try {
            ResourcePackInfo.Builder builder = server.createResourcePackBuilder(url);

            byte[] hash = plugin.getPackHash();
            if (hash.length > 0) builder.setHash(hash);

            builder.setShouldForce(plugin.isRequired());

            Component prompt = LemonResourcePack.MM.deserialize(plugin.getPromptRaw());
            builder.setPrompt(prompt);

            player.sendResourcePackOffer(builder.build());
        } catch (Exception e) {
            logger.error("Failed to send resource pack to {}: {}", player.getUsername(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Handle accept / decline
    // -------------------------------------------------------------------------

    @Subscribe
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        var status = event.getStatus();

        switch (status) {
            case DECLINED -> {
                if (plugin.isRequired()) {
                    Component msg = LemonResourcePack.MM.deserialize(
                            "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>"
                            + " <red>ʏᴏᴜ ᴍᴜꜱᴛ ᴀᴄᴄᴇᴘᴛ ᴛᴏ ᴘʟᴀʏ.</red>");
                    event.getPlayer().disconnect(msg);
                }
            }
            case FAILED_DOWNLOAD -> {
                if (plugin.isRequired()) {
                    Component msg = LemonResourcePack.MM.deserialize(
                            "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>"
                            + " <red>ᴅᴏᴡɴʟᴏᴀᴅ ꜰᴀɪʟᴇᴅ. ᴘʟᴇᴀꜱᴇ ʀᴇᴊᴏɪɴ ᴀɴᴅ ᴛʀʏ ᴀɢᴀɪɴ.</red>");
                    event.getPlayer().disconnect(msg);
                } else {
                    logger.warn("Resource pack download failed for {}", event.getPlayer().getUsername());
                }
            }
            case ACCEPTED, SUCCESSFUL -> {
                // Normal join — no action needed
            }
            default -> {}
        }
    }
}
