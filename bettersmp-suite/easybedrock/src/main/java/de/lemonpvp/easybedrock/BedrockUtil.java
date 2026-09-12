package de.lemonpvp.easybedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Erkennt Bedrock-Spieler ohne harte Floodgate-Abhängigkeit: Floodgate
 * vergibt UUIDs, deren obere 64 Bit 0 sind.
 */
public final class BedrockUtil {

    private BedrockUtil() {
    }

    public static boolean isBedrock(Player player) {
        return player.getUniqueId().getMostSignificantBits() == 0L;
    }

    public static int countBedrock() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBedrock(player)) {
                count++;
            }
        }
        return count;
    }
}
