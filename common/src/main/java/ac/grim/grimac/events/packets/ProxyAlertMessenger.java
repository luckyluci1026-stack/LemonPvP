package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.ProxyDetection;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.io.*;

// TODO (Cross-Platform) ensure this is correct, and modify to only check appropriate files for each platform
public class ProxyAlertMessenger extends PacketListenerAbstract {
    @Getter private static boolean usingProxy;

    public ProxyAlertMessenger() {
        ProxyDetection.detect();
        usingProxy = ProxyDetection.isBehindProxy();
        // Say it out loud at boot. A wrong answer here disconnects legitimate
        // players over proxy plugin messages, so it must not be silent.
        LogUtil.info("Proxy detection: this server is " + ProxyDetection.describe());

        if (usingProxy) {
            LogUtil.info("Registering an outgoing plugin channel...");
            GrimAPI.INSTANCE.getPlatformServer().registerOutgoingPluginChannel("BungeeCord");
        }
    }

    public static void sendPluginMessage(String message) {
        if (!canSendAlerts())
            return;

        ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE");
        out.writeUTF("GRIMAC");

        try {
            new DataOutputStream(messageBytes).writeUTF(message);
        } catch (IOException exception) {
            LogUtil.error("Something went wrong whilst forwarding an alert to other servers!", exception);
            return;
        }

        out.writeShort(messageBytes.toByteArray().length);
        out.write(messageBytes.toByteArray());

        Iterables.getFirst(GrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers(), null).sendPluginMessage("BungeeCord", out.toByteArray());
    }

    public static boolean canSendAlerts() {
        return usingProxy && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.proxy.send", false) && !GrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers().isEmpty();
    }

    public static boolean canReceiveAlerts() {
        return usingProxy && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.proxy.receive", false) && GrimAPI.INSTANCE.getAlertManager().hasAlertListeners();
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE || !ProxyAlertMessenger.canReceiveAlerts())
            return;

        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

        if (!wrapper.getChannelName().equals("BungeeCord") && !wrapper.getChannelName().equals("bungeecord:main"))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(wrapper.getData());

        if (!in.readUTF().equals("GRIMAC")) return;

        final String alert;
        byte[] messageBytes = new byte[in.readShort()];
        in.readFully(messageBytes);

        try {
            alert = new DataInputStream(new ByteArrayInputStream(messageBytes)).readUTF();
        } catch (IOException exception) {
            LogUtil.error("Something went wrong whilst reading an alert forwarded from another server!", exception);
            return;
        }
        Component message = MessageUtil.miniMessage(alert);
        GrimAPI.INSTANCE.getAlertManager().sendAlert(message, null);
    }
}
