package de.lemonpvp.helden.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Alles, was Mojang zwischen zwei Versionen umbenannt hat.
 *
 * <p>Effekte, Verzauberungen, Partikel und Sounds werden bewusst ueber Strings
 * aufgeloest statt ueber Enum-Konstanten. So laeuft dasselbe Jar auf mehreren
 * Serverversionen und ein Tippfehler in der Config wirft eine lesbare Warnung
 * statt einer NoSuchFieldError.</p>
 */
public final class Compat {

    private static final Map<String, String> EFFECT_ALIASES = new HashMap<>();
    private static final Map<String, String> ENCHANT_ALIASES = new HashMap<>();

    static {
        // neu -> alt
        EFFECT_ALIASES.put("RESISTANCE", "DAMAGE_RESISTANCE");
        EFFECT_ALIASES.put("SLOWNESS", "SLOW");
        EFFECT_ALIASES.put("STRENGTH", "INCREASE_DAMAGE");
        EFFECT_ALIASES.put("JUMP_BOOST", "JUMP");
        EFFECT_ALIASES.put("HASTE", "FAST_DIGGING");
        EFFECT_ALIASES.put("MINING_FATIGUE", "SLOW_DIGGING");
        EFFECT_ALIASES.put("INSTANT_HEALTH", "HEAL");
        EFFECT_ALIASES.put("INSTANT_DAMAGE", "HARM");
        EFFECT_ALIASES.put("NAUSEA", "CONFUSION");
        // alt -> neu
        EFFECT_ALIASES.put("DAMAGE_RESISTANCE", "RESISTANCE");
        EFFECT_ALIASES.put("SLOW", "SLOWNESS");
        EFFECT_ALIASES.put("INCREASE_DAMAGE", "STRENGTH");
        EFFECT_ALIASES.put("JUMP", "JUMP_BOOST");
        EFFECT_ALIASES.put("FAST_DIGGING", "HASTE");
        EFFECT_ALIASES.put("SLOW_DIGGING", "MINING_FATIGUE");
        EFFECT_ALIASES.put("HEAL", "INSTANT_HEALTH");
        EFFECT_ALIASES.put("HARM", "INSTANT_DAMAGE");
        EFFECT_ALIASES.put("CONFUSION", "NAUSEA");

        ENCHANT_ALIASES.put("sharpness", "damage_all");
        ENCHANT_ALIASES.put("damage_all", "sharpness");
        ENCHANT_ALIASES.put("power", "arrow_damage");
        ENCHANT_ALIASES.put("arrow_damage", "power");
        ENCHANT_ALIASES.put("punch", "arrow_knockback");
        ENCHANT_ALIASES.put("arrow_knockback", "punch");
        ENCHANT_ALIASES.put("unbreaking", "durability");
        ENCHANT_ALIASES.put("durability", "unbreaking");
        ENCHANT_ALIASES.put("protection", "protection_environmental");
        ENCHANT_ALIASES.put("protection_environmental", "protection");
        ENCHANT_ALIASES.put("efficiency", "dig_speed");
        ENCHANT_ALIASES.put("dig_speed", "efficiency");
        ENCHANT_ALIASES.put("fortune", "loot_bonus_blocks");
        ENCHANT_ALIASES.put("looting", "loot_bonus_mobs");
    }

    private Compat() {
    }

    /** Loest einen Effektnamen auf, egal ob alte oder neue Schreibweise. */
    @SuppressWarnings("deprecation")
    public static PotionEffectType effect(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String key = name.trim().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
        PotionEffectType type = PotionEffectType.getByName(key);
        if (type != null) {
            return type;
        }
        String alias = EFFECT_ALIASES.get(key);
        return alias == null ? null : PotionEffectType.getByName(alias);
    }

    public static Material material(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
    }

    @SuppressWarnings("deprecation")
    public static Enchantment enchantment(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "");
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));
        if (enchantment != null) {
            return enchantment;
        }
        String alias = ENCHANT_ALIASES.get(key);
        return alias == null ? null : Enchantment.getByKey(NamespacedKey.minecraft(alias));
    }

    /**
     * Erster Partikel, den diese Serverversion kennt. Beispiel:
     * {@code particle("DUST", "REDSTONE")}.
     */
    public static Particle particle(String... candidates) {
        for (String candidate : candidates) {
            try {
                return Particle.valueOf(candidate.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // naechster Kandidat
            }
        }
        return null;
    }

    public static void spawnParticle(World world, Particle particle, Location location, int count,
                                     double offsetX, double offsetY, double offsetZ, double extra) {
        if (world == null || particle == null || location == null) {
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    /**
     * Sound ueber den String-Namen abspielen. Die String-Overloads gibt es seit
     * jeher, die {@code Sound}-Konstanten dagegen wandern staendig.
     */
    public static void sound(Player player, String sound, float volume, float pitch) {
        if (player == null || sound == null || sound.isEmpty()) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void sound(Location location, String sound, float volume, float pitch) {
        if (location == null || location.getWorld() == null || sound == null || sound.isEmpty()) {
            return;
        }
        location.getWorld().playSound(location, sound, volume, pitch);
    }
}
