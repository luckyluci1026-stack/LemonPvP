package de.lemonpvp.helden.heart;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Setzt die maximale Gesundheit versionsunabhaengig.
 *
 * <p>Das Attribut fuer die Maximalgesundheit heisst je nach Minecraft-Version
 * {@code GENERIC_MAX_HEALTH} oder {@code MAX_HEALTH}, und {@code Attribute} war
 * mal ein Enum und ist heute eine Registry. Statt uns auf eine Variante
 * festzulegen, suchen wir sie zur Laufzeit und fallen sonst auf das alte
 * {@code setMaxHealth} zurueck.</p>
 */
public final class HealthCompat {

    public static final double VANILLA_MAX_HEALTH = 20.0;
    /** Ein Herz sind zwei Lebenspunkte. */
    public static final double HEALTH_PER_HEART = 2.0;

    private static boolean resolved;
    private static Object maxHealthAttribute;
    private static Method getAttributeMethod;
    private static Method setBaseValueMethod;

    private HealthCompat() {
    }

    /** Setzt die Maximalgesundheit auf die uebergebene Herzzahl. */
    public static void applyHearts(Player player, int hearts) {
        setMaxHealth(player, Math.max(HEALTH_PER_HEART, hearts * HEALTH_PER_HEART));
    }

    /** Stellt die normalen 10 Herzen wieder her (Ausgeschiedene, Zuschauer). */
    public static void reset(Player player) {
        setMaxHealth(player, VANILLA_MAX_HEALTH);
    }

    private static void setMaxHealth(Player player, double value) {
        if (!applyViaAttribute(player, value)) {
            player.setMaxHealth(value);
        }
        if (player.getHealth() > value) {
            player.setHealth(value);
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean applyViaAttribute(Player player, double value) {
        resolve();
        if (maxHealthAttribute == null || getAttributeMethod == null || setBaseValueMethod == null) {
            return false;
        }
        try {
            Object instance = getAttributeMethod.invoke(player, maxHealthAttribute);
            if (instance == null) {
                return false;
            }
            setBaseValueMethod.invoke(instance, value);
            return true;
        } catch (Throwable throwable) {
            // Einmal fehlgeschlagen heisst: ab jetzt immer der Fallback.
            maxHealthAttribute = null;
            return false;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
            for (String fieldName : new String[]{"MAX_HEALTH", "GENERIC_MAX_HEALTH"}) {
                try {
                    maxHealthAttribute = attributeClass.getField(fieldName).get(null);
                    break;
                } catch (NoSuchFieldException ignored) {
                    // naechster Kandidat
                }
            }
            if (maxHealthAttribute == null) {
                return;
            }
            Class<?> instanceClass = Class.forName("org.bukkit.attribute.AttributeInstance");
            getAttributeMethod = LivingEntity.class.getMethod("getAttribute", attributeClass);
            setBaseValueMethod = instanceClass.getMethod("setBaseValue", double.class);
        } catch (Throwable throwable) {
            maxHealthAttribute = null;
        }
    }
}
