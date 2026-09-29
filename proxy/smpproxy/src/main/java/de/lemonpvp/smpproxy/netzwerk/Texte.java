package de.lemonpvp.smpproxy.netzwerk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Texte {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Texte() {
    }

    public static Component mitSpielertext(String vorlage, String prefix, String... ersetzungen) {
        if (vorlage == null || vorlage.isEmpty()) {
            return Component.empty();
        }
        String text = vorlage.replace("%prefix%", prefix == null ? "" : prefix);
        for (int i = 0; i + 1 < ersetzungen.length; i += 2) {
            text = text.replace(ersetzungen[i], MM.escapeTags(ersetzungen[i + 1]));
        }
        return MM.deserialize(text);
    }
}
