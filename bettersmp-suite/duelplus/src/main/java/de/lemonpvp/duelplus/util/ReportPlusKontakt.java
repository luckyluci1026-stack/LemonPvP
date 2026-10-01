package de.lemonpvp.duelplus.util;

import de.lemonpvp.reportplus.api.ReportPlusApi;

import java.util.UUID;

public final class ReportPlusKontakt {

    private ReportPlusKontakt() {
    }

    public static void merken(UUID spieler, UUID gegner, String gegnerName) {
        ReportPlusApi.gegnerMerken(spieler, gegner, gegnerName);
    }
}
