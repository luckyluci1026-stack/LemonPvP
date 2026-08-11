package de.lemonpvp.helden.hero.ability.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Assassine: Sprung nach vorn, kurz unsichtbar. */
public final class SchattenschrittAbility extends Ability {

    private static final int DURATION_TICKS = 20 * 4;

    public SchattenschrittAbility(HeldenPlugin plugin) {
        super(plugin, "schattenschritt", "&5Schattenschritt", 18);
    }

    @Override
    public boolean execute(Player caster) {
        Location from = caster.getLocation();

        Vector dash = from.getDirection().normalize().multiply(1.7);
        dash.setY(Math.max(0.42, dash.getY()));
        caster.setVelocity(dash);

        applyEffect(caster, "INVISIBILITY", DURATION_TICKS, 0);
        applyEffect(caster, "SPEED", DURATION_TICKS, 1);

        Particle smoke = Compat.particle("LARGE_SMOKE", "SMOKE_LARGE");
        Particle portal = Compat.particle("PORTAL");
        Compat.spawnParticle(from.getWorld(), smoke, from.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
        Compat.spawnParticle(from.getWorld(), portal, from.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.4);
        Compat.sound(from, "entity.enderman.teleport", 1.0f, 1.4f);
        return true;
    }

    private void applyEffect(Player player, String effectName, int durationTicks, int amplifier) {
        PotionEffectType type = Compat.effect(effectName);
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, true, true));
        }
    }
}
