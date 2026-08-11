package de.lemonpvp.helden.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Erkennt Bedrock-Spieler, die ueber Geyser/Floodgate verbunden sind.
 *
 * <p>Floodgate wird per Reflection angesprochen. Das Plugin hat dadurch keine
 * Compile-Dependency auf Floodgate und startet auch auf einem reinen
 * Java-Server ohne Geyser sauber - dort ist einfach nie jemand "Bedrock".</p>
 */
public final class BedrockSupport {

    private final Logger logger;

    private boolean detectFloodgate = true;
    private String usernamePrefix = ".";
    private boolean stripHexColors = true;

    private Object floodgateApi;
    private Method isFloodgatePlayerMethod;

    public BedrockSupport(Logger logger) {
        this.logger = logger;
    }

    public void configure(boolean detectFloodgate, String usernamePrefix, boolean stripHexColors) {
        this.detectFloodgate = detectFloodgate;
        this.usernamePrefix = usernamePrefix == null ? "" : usernamePrefix;
        this.stripHexColors = stripHexColors;
        if (detectFloodgate) {
            hookFloodgate();
        } else {
            floodgateApi = null;
            isFloodgatePlayerMethod = null;
        }
    }

    private void hookFloodgate() {
        if (floodgateApi != null) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null) {
                return;
            }
            isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            floodgateApi = api;
            logger.info("Floodgate erkannt - Bedrock-Spieler werden gesondert behandelt.");
        } catch (ClassNotFoundException ignored) {
            // Kein Floodgate installiert, wir nutzen den Namens-/UUID-Fallback.
        } catch (Throwable throwable) {
            logger.warning("Floodgate gefunden, aber die API antwortet nicht: " + throwable.getMessage());
        }
    }

    public boolean isFloodgateHooked() {
        return floodgateApi != null && isFloodgatePlayerMethod != null;
    }

    /** {@code true}, wenn der Spieler von einem Bedrock-Client kommt. */
    public boolean isBedrock(Player player) {
        if (player == null) {
            return false;
        }
        if (isFloodgateHooked()) {
            try {
                Object result = isFloodgatePlayerMethod.invoke(floodgateApi, player.getUniqueId());
                if (result instanceof Boolean value) {
                    return value;
                }
            } catch (Throwable throwable) {
                logger.warning("Floodgate-Abfrage fehlgeschlagen, nutze Fallback: " + throwable.getMessage());
                floodgateApi = null;
                isFloodgatePlayerMethod = null;
            }
        }
        return matchesFallback(player.getUniqueId(), player.getName());
    }

    /**
     * Fallback ohne Floodgate-API: Floodgate vergibt UUIDs, deren obere 64 Bit
     * null sind, und haengt optional ein Prefix an den Namen.
     */
    private boolean matchesFallback(UUID uuid, String name) {
        if (uuid != null && uuid.getMostSignificantBits() == 0L) {
            return true;
        }
        return !usernamePrefix.isEmpty() && name != null && name.startsWith(usernamePrefix);
    }

    /**
     * Faerbt einen Text passend zum Empfaenger ein: Bedrock-Clients bekommen
     * keine Hex-Farben, sondern die naechste Legacy-Farbe.
     */
    public String format(CommandSender receiver, String message) {
        if (stripHexColors && receiver instanceof Player player && isBedrock(player)) {
            return Text.colorLegacy(message);
        }
        return Text.color(message);
    }
}
