package de.lemonpvp.helden.hero.ability;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Text;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Basis fuer alle aktiven Heldenfaehigkeiten. */
public abstract class Ability {

    protected final HeldenPlugin plugin;

    private final String id;
    private final String display;
    private final int cooldownSeconds;

    protected Ability(HeldenPlugin plugin, String id, String display, int cooldownSeconds) {
        this.plugin = plugin;
        this.id = id;
        this.display = display;
        this.cooldownSeconds = cooldownSeconds;
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

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Fuehrt die Faehigkeit aus.
     *
     * @return {@code false}, wenn nichts passiert ist - dann laeuft auch kein
     *         Cooldown an.
     */
    public abstract boolean execute(Player caster);

    /** Darf der Zauberer dieses Ziel treffen? Teamkollegen und Zuschauer nicht. */
    protected boolean isValidTarget(Player caster, Entity entity) {
        if (!(entity instanceof LivingEntity target) || entity.equals(caster)) {
            return false;
        }
        if (target.isDead()) {
            return false;
        }
        if (target instanceof Player player) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                return false;
            }
            HeldenProfile profile = plugin.profiles().get(player);
            if (profile != null && profile.fallen()) {
                return false;
            }
            if (!plugin.settings().teamFriendlyFire() && plugin.teams().sameTeam(caster, player)) {
                return false;
            }
            return !plugin.combat().isProtected(player);
        }
        return true;
    }

    /** Teamkollegen im Umkreis, den Zauberer eingeschlossen. */
    protected boolean isAlly(Player caster, Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return player.equals(caster) || plugin.teams().sameTeam(caster, player);
    }

    /** Stoesst ein Ziel vom Zauberer weg, ohne durch Nullvektoren zu stolpern. */
    protected void knockBack(Player caster, LivingEntity target, double strength, double lift) {
        Vector direction = target.getLocation().toVector().subtract(caster.getLocation().toVector());
        if (direction.lengthSquared() < 1.0E-4) {
            direction = caster.getLocation().getDirection().clone();
        }
        direction.setY(0);
        if (direction.lengthSquared() < 1.0E-4) {
            direction = new Vector(0, 0, 1);
        }
        target.setVelocity(direction.normalize().multiply(strength).setY(lift));
    }
}
