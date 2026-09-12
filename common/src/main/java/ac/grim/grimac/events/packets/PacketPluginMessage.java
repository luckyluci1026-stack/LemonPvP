package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.ProxyDetection;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import ac.grim.grimac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import org.jetbrains.annotations.NotNull;

public class PacketPluginMessage extends PacketListenerAbstract {

    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            checkChannel(event.getUser(), packet.getChannelName());
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            checkChannel(event.getUser(), packet.getChannelName());
        }
    }

    private void checkChannel(User user, String channelName) {
        if (!"vv:proxy_details".equals(channelName)) return;

        final boolean usingProxy = ProxyAlertMessenger.isUsingProxy();
        final boolean viaOnBackend = ViaVersionUtil.isAvailable;

        // warn if they are using a proxy
        if (usingProxy) {
            LogUtil.warn(
                    user.getName() + " seems to have connected through a proxy running ViaVersion. "
                            + "Having ViaVersion installed on the proxy is incompatible with GrimAC and causes many issues. "
                            + "Please remove ViaVersion from your proxy server and install it on your backend servers instead."
            );
        }

        if (!CommonGrimArguments.KICK_ON_VIA_PROXY.value()) return;

        // Two very different situations end up here, and the old log line named
        // neither, so an operator could not tell a misconfiguration from a
        // genuinely unsupported setup. Say which one it is.
        final String reason;
        if (viaOnBackend) {
            reason = "ViaVersion is installed on BOTH this backend server and the proxy in front of it. "
                    + "The packet stream would be translated twice and nothing downstream can follow it. "
                    + "Remove ViaVersion from the proxy and keep it here on the backend.";
        } else if (!usingProxy) {
            reason = "this server is not configured to accept a proxy, so proxy data arriving from an "
                    + "unverified source is not trusted. Proxy detection says: " + ProxyDetection.describe() + ". "
                    + "If you DO run a proxy, enable its forwarding in the server's own config "
                    + "(Paper: proxies.velocity.enabled, or settings.bungeecord on Spigot), or declare it "
                    + "explicitly with 'proxy.behind-proxy: true' in the config.yml.";
        } else {
            // Proxy is configured and ViaVersion only runs there. Unsupported and
            // warned about above, but not a reason to disconnect the player.
            return;
        }

        LogUtil.warn(user.getName() + " is being disconnected for sending ViaVersion proxy data: " + reason);

        try {
            // A dedicated message: the generic packet-error text sent people
            // hunting for a crash that never happened.
            WrapperPlayServerDisconnect disconnect = new WrapperPlayServerDisconnect(
                    MessageUtil.miniMessage(GrimAPI.INSTANCE.getConfigManager().getDisconnectViaProxy())
            );
            user.sendPacket(disconnect);
        } catch (Exception e) {
            LogUtil.warn("Failed to send disconnect packet to kick " + user.getName() + "!");
        }
        user.closeConnection();
    }

}
