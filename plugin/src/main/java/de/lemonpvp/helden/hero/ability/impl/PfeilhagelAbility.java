package de.lemonpvp.helden.hero.ability.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Bogenschuetze: Salve aus mehreren Pfeilen. */
public final class PfeilhagelAbility extends Ability {

    private static final int ARROWS = 9;
    private static final double SPREAD = 0.16;
    private static final double SPEED = 2.2;

    public PfeilhagelAbility(HeldenPlugin plugin) {
        super(plugin, "pfeilhagel", "&bPfeilhagel", 25);
    }

    @Override
    public boolean execute(Player caster) {
        Vector direction = caster.getLocation().getDirection().normalize();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < ARROWS; i++) {
            Vector velocity = direction.clone()
                    .add(new Vector(
                            random.nextDouble(-SPREAD, SPREAD),
                            random.nextDouble(-SPREAD / 2, SPREAD),
                            random.nextDouble(-SPREAD, SPREAD)))
                    .normalize()
                    .multiply(SPEED);

            Arrow arrow = caster.launchProjectile(Arrow.class, velocity);
            arrow.setShooter(caster);
            arrow.setCritical(true);
            arrow.setDamage(arrow.getDamage() * 0.8);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }

        Compat.sound(caster, "entity.arrow.shoot", 1.0f, 0.7f);
        Compat.sound(caster, "entity.player.attack_strong", 0.6f, 1.4f);
        return true;
    }
}
