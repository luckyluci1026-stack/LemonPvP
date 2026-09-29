package de.lemonpvp.antiswear.voice;

import de.lemonpvp.antiswear.AntiSwear;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;

public final class VoiceAnbindung implements VoicechatPlugin {

    private final VoiceModeration voice;

    private VoiceAnbindung(VoiceModeration voice) {
        this.voice = voice;
    }

    public static boolean starten(AntiSwear plugin) {
        BukkitVoicechatService dienst = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (dienst == null) {
            return false;
        }
        dienst.registerPlugin(new VoiceAnbindung(plugin.voice()));
        return true;
    }

    @Override
    public String getPluginId() {
        return "antiswear";
    }

    @Override
    public void registerEvents(EventRegistration registrierung) {
        registrierung.registerEvent(MicrophonePacketEvent.class, this::beimSprechen, 100);
        registrierung.registerEvent(CreateGroupEvent.class, this::beimGruppeErstellen, 100);
        registrierung.registerEvent(PlayerConnectedEvent.class, this::beimVerbinden);
        voice.setVerbunden(true);
    }

    public void beimSprechen(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender != null && !voice.darfSprechen(sender.getPlayer().getUuid())) {
            event.cancel();
        }
    }

    public void beimGruppeErstellen(CreateGroupEvent event) {
        VoicechatConnection verbindung = event.getConnection();
        Group gruppe = event.getGroup();
        if (verbindung == null || gruppe == null) {
            return;
        }
        if (!voice.gruppennameErlaubt(verbindung.getPlayer().getUuid(), gruppe.getName())) {
            event.cancel();
        }
    }

    public void beimVerbinden(PlayerConnectedEvent event) {
        VoicechatConnection verbindung = event.getConnection();
        if (verbindung != null) {
            voice.voiceVerbunden(verbindung.getPlayer().getUuid());
        }
    }
}
