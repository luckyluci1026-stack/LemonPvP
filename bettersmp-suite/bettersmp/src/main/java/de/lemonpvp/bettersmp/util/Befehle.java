package de.lemonpvp.bettersmp.util;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Befehle {

    private Befehle() {
    }

    public static boolean gesperrt(String nachricht, List<String> liste) {
        if (nachricht == null || nachricht.length() < 2 || liste.isEmpty()) {
            return false;
        }
        String wort = nachricht.substring(1).trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        Set<String> namen = new HashSet<>();
        namen.add(ohneNamensraum(wort));
        Command befehl = befehl(wort);
        if (befehl != null) {
            namen.add(ohneNamensraum(befehl.getName().toLowerCase(Locale.ROOT)));
            for (String alias : befehl.getAliases()) {
                namen.add(ohneNamensraum(alias.toLowerCase(Locale.ROOT)));
            }
        }
        for (String eintrag : liste) {
            if (namen.contains(eintrag.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static Command befehl(String wort) {
        try {
            return Bukkit.getCommandMap().getCommand(wort);
        } catch (RuntimeException keineKarte) {
            return null;
        }
    }

    private static String ohneNamensraum(String wort) {
        int doppelpunkt = wort.indexOf(':');
        return doppelpunkt >= 0 ? wort.substring(doppelpunkt + 1) : wort;
    }
}
