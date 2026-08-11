package de.lemonpvp.helden.hero;

import de.lemonpvp.helden.util.Text;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** Eine Heldenklasse aus heroes.yml. */
public final class Hero {

    /** Ein passiver Dauereffekt des Helden. */
    public record Passive(PotionEffectType type, int amplifier) {

        public PotionEffect toEffect(int durationTicks) {
            return new PotionEffect(type, durationTicks, amplifier, true, false, false);
        }
    }

    private final String id;
    private final String display;
    private final Material icon;
    private final int order;
    private final List<String> description;
    private final String abilityId;
    private final String weaponId;
    private final List<Passive> passives;
    private final double damageDealtMultiplier;
    private final double damageTakenMultiplier;
    private final double projectileDamagePercent;
    private final List<KitEntry> kit;

    public Hero(String id,
                String display,
                Material icon,
                int order,
                List<String> description,
                String abilityId,
                String weaponId,
                List<Passive> passives,
                double damageDealtMultiplier,
                double damageTakenMultiplier,
                double projectileDamagePercent,
                List<KitEntry> kit) {
        this.id = id;
        this.display = display;
        this.icon = icon;
        this.order = order;
        this.description = List.copyOf(description);
        this.abilityId = abilityId;
        this.weaponId = weaponId;
        this.passives = List.copyOf(passives);
        this.damageDealtMultiplier = damageDealtMultiplier;
        this.damageTakenMultiplier = damageTakenMultiplier;
        this.projectileDamagePercent = projectileDamagePercent;
        this.kit = List.copyOf(kit);
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public String coloredDisplay() {
        return Text.color(display);
    }

    public Material icon() {
        return icon;
    }

    public int order() {
        return order;
    }

    public List<String> description() {
        return description;
    }

    public String abilityId() {
        return abilityId;
    }

    public boolean hasAbility() {
        return abilityId != null && !abilityId.isEmpty();
    }

    public String weaponId() {
        return weaponId;
    }

    public List<Passive> passives() {
        return passives;
    }

    public double damageDealtMultiplier() {
        return damageDealtMultiplier;
    }

    public double damageTakenMultiplier() {
        return damageTakenMultiplier;
    }

    public double projectileDamagePercent() {
        return projectileDamagePercent;
    }

    public List<KitEntry> kit() {
        return kit;
    }
}
