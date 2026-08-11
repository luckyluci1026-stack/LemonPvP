package de.lemonpvp.helden.hero.ability.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.util.Vector;

/**
 * Magier: Feuerball.
 *
 * <p>Bewusst ein {@link SmallFireball} - der richtet keinen Blockschaden an und
 * ist damit projekttauglich.</p>
 */
public final class FeuerballAbility extends Ability {

    public FeuerballAbility(HeldenPlugin plugin) {
        super(plugin, "feuerball", "&6Feuerball", 12);
    }

    @Override
    public boolean execute(Player caster) {
        Vector direction = caster.getLocation().getDirection().normalize().multiply(1.4);

        SmallFireball fireball = caster.launchProjectile(SmallFireball.class, direction);
        fireball.setShooter(caster);
        fireball.setIsIncendiary(true);
        fireball.setYield(0.0f);

        Compat.sound(caster, "entity.blaze.shoot", 1.0f, 0.9f);
        Compat.spawnParticle(caster.getWorld(), Compat.particle("FLAME"),
                caster.getEyeLocation().add(direction), 12, 0.2, 0.2, 0.2, 0.01);
        return true;
    }
}
