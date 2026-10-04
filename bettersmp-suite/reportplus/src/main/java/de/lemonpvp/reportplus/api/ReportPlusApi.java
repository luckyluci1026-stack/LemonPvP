package de.lemonpvp.reportplus.api;

import de.lemonpvp.reportplus.ReportPlus;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class ReportPlusApi {

    private ReportPlusApi() {
    }

    public static void gegnerMerken(UUID spieler, UUID gegner, String gegnerName) {
        ReportPlus plugin = JavaPlugin.getPlugin(ReportPlus.class);
        if (plugin.isEnabled() && plugin.kontakte() != null) {
            plugin.kontakte().merken(spieler, gegner, gegnerName);
        }
    }
}
