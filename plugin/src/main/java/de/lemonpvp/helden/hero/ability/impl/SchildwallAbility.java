package de.lemonpvp.helden.hero.ability.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Waechter: stoesst alles weg und haelt danach kurz alles aus. */
public final class SchildwallAbility extends Ability {

    private static final int DURATION_TICKS = 20 * 8;
    private static final double RADIUS = 5.0;

    public SchildwallAbility(HeldenPlugin plugin) {
        super(plugin, "schildwall", "&7Schildwall", 30);
    }

    @Override
    public boolean execute(Player caster) {
        applyEffect(caster, "RESISTANCE", DURATION_TICKS, 2);
        applyEffect(caster, "ABSORPTION", DURATION_TICKS, 1);
        applyEffect(caster, "SLOWNESS", DURATION_TICKS, 0);

        for (Entity entity : caster.getNearbyEntities(RADIUS, 3.0, RADIUS)) {
            if (isValidTarget(caster, entity)) {
                knockBack(caster, (LivingEntity) entity, 1.2, 0.45);
            }
        }

        drawWall(caster.getLocation());
        Compat.sound(caster, "item.shield.block", 1.0f, 0.6f);
        Compat.sound(caster, "block.anvil_land", 0.5f, 1.4f);
        return true;
    }

    private void drawWall(Location center) {
        Particle cloud = Compat.particle("CLOUD");
        for (int step = 0; step < 36; step++) {
            double angle = Math.PI * 2 * step / 36.0;
            for (double height = 0.2; height <= 2.2; height += 0.5) {
                Location point = center.clone().add(Math.cos(angle) * 2.2, height, Math.sin(angle) * 2.2);
                Compat.spawnParticle(center.getWorld(), cloud, point, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        Compat.spawnParticle(center.getWorld(), Compat.particle("EXPLOSION", "EXPLOSION_NORMAL"),
                center.clone().add(0, 0.1, 0), 3, 1.4, 0.1, 1.4, 0.0);
    }

    private void applyEffect(Player player, String effectName, int durationTicks, int amplifier) {
        PotionEffectType type = Compat.effect(effectName);
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, true, true));
        }
    }
}
