package de.lemonpvp.helden.hero.ability.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Krieger: Rundumschlag mit Rueckstoss. */
public final class WirbelsturmAbility extends Ability {

    private static final double RADIUS = 4.0;
    private static final double DAMAGE = 7.0;

    public WirbelsturmAbility(HeldenPlugin plugin) {
        super(plugin, "wirbelsturm", "&cWirbelsturm", 20);
    }

    @Override
    public boolean execute(Player caster) {
        Location center = caster.getLocation();
        int hits = 0;

        for (Entity entity : caster.getNearbyEntities(RADIUS, 3.0, RADIUS)) {
            if (!isValidTarget(caster, entity)) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;
            target.damage(DAMAGE, caster);
            knockBack(caster, target, 0.9, 0.4);
            hits++;
        }

        drawSpiral(center);
        Compat.sound(center, "entity.player.attack_sweep", 1.0f, 0.8f);
        Compat.sound(center, "item.trident.riptide_1", 0.8f, 1.2f);

        // Der Wirbel wirkt auch ohne Treffer - so laeuft der Cooldown fair mit.
        return hits >= 0;
    }

    private void drawSpiral(Location center) {
        Particle sweep = Compat.particle("SWEEP_ATTACK");
        Particle crit = Compat.particle("CRIT");
        for (int step = 0; step < 24; step++) {
            double angle = Math.PI * 2 * step / 24.0;
            Location point = center.clone().add(
                    Math.cos(angle) * RADIUS * 0.75,
                    0.6 + (step % 4) * 0.2,
                    Math.sin(angle) * RADIUS * 0.75);
            Compat.spawnParticle(center.getWorld(), sweep, point, 1, 0, 0, 0, 0);
            Compat.spawnParticle(center.getWorld(), crit, point, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }
}
