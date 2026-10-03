package de.lemonpvp.duelplus.util;

import de.lemonpvp.bettersmp.api.BetterSMPApi;

import java.util.UUID;

public final class BetterSmpKontakt {

    private BetterSmpKontakt() {
    }

    public static void merken(UUID spieler, UUID gegner, String gegnerName) {
        BetterSMPApi.gegnerMerken(spieler, gegner, gegnerName);
    }
}
