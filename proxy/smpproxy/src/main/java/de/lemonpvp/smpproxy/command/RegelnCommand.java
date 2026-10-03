package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.util.Msg;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class RegelnCommand implements SimpleCommand {

    public static final MinecraftChannelIdentifier KANAL = MinecraftChannelIdentifier.create("smpproxy", "regeln");

    private final SMPProxy plugin;

    public RegelnCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        zeigen(invocation.source());
    }

    public void zeigen(Audience empfaenger) {
        List<Component> zeilen = new ArrayList<>();
        for (String zeile : plugin.config().regeln()) {
            zeilen.add(zeile.isEmpty() ? Component.empty() : Msg.of(zeile, plugin.config().prefix()));
        }
        if (!zeilen.isEmpty()) {
            empfaenger.sendMessage(Component.join(JoinConfiguration.newlines(), zeilen));
        }
    }

    @Subscribe
    public void beimPluginKanal(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(KANAL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (event.getSource() instanceof ServerConnection verbindung && plugin.config().regelnEnabled()) {
            zeigen(verbindung.getPlayer());
        }
    }
}
