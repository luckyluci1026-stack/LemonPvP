package de.lemonpvp.helden.hero.ability.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Heiler: heilt alle Teamkollegen im Umkreis.
 *
 * <p>Geheilt wird ueber INSTANT_HEALTH statt ueber {@code setHealth} - so
 * kuemmert sich der Server um das Maximum und wir muessen die zwischen den
 * Versionen wandernde Attribut-API nicht anfassen.</p>
 */
public final class HeilkreisAbility extends Ability {

    private static final double RADIUS = 6.0;

    public HeilkreisAbility(HeldenPlugin plugin) {
        super(plugin, "heilkreis", "&aHeilkreis", 30);
    }

    @Override
    public boolean execute(Player caster) {
        Location center = caster.getLocation();

        heal(caster);
        for (Entity entity : caster.getNearbyEntities(RADIUS, 4.0, RADIUS)) {
            if (isAlly(caster, entity)) {
                heal((Player) entity);
            }
        }

        drawCircle(center);
        Compat.sound(center, "block.beacon.activate", 0.8f, 1.6f);
        return true;
    }

    private void heal(Player player) {
        PotionEffectType instant = Compat.effect("INSTANT_HEALTH");
        if (instant != null) {
            player.addPotionEffect(new PotionEffect(instant, 1, 1, true, false, false));
        }
        PotionEffectType regeneration = Compat.effect("REGENERATION");
        if (regeneration != null) {
            player.addPotionEffect(new PotionEffect(regeneration, 20 * 5, 0, true, true, true));
        }
        Compat.spawnParticle(player.getWorld(), Compat.particle("HEART"),
                player.getLocation().add(0, 2.1, 0), 4, 0.3, 0.2, 0.3, 0.0);
    }

    private void drawCircle(Location center) {
        Particle happy = Compat.particle("HAPPY_VILLAGER", "VILLAGER_HAPPY");
        for (int step = 0; step < 40; step++) {
            double angle = Math.PI * 2 * step / 40.0;
            Location point = center.clone().add(Math.cos(angle) * RADIUS, 0.2, Math.sin(angle) * RADIUS);
            Compat.spawnParticle(center.getWorld(), happy, point, 2, 0.05, 0.4, 0.05, 0.0);
        }
    }
}
