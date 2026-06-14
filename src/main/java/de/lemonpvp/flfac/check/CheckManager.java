package de.lemonpvp.flfac.check;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.impl.combat.AutoClickerCheck;
import de.lemonpvp.flfac.check.impl.combat.HitboxCheck;
import de.lemonpvp.flfac.check.impl.combat.KillAuraCheck;
import de.lemonpvp.flfac.check.impl.combat.ReachCheck;
import de.lemonpvp.flfac.check.impl.combat.VelocityCheck;
import de.lemonpvp.flfac.check.impl.movement.FlyCheck;
import de.lemonpvp.flfac.check.impl.movement.JesusCheck;
import de.lemonpvp.flfac.check.impl.movement.NoFallCheck;
import de.lemonpvp.flfac.check.impl.movement.SpeedCheck;
import de.lemonpvp.flfac.check.impl.movement.TimerCheck;
import de.lemonpvp.flfac.check.impl.player.BadPacketsCheck;
import de.lemonpvp.flfac.check.impl.world.FastBreakCheck;
import de.lemonpvp.flfac.check.impl.world.NukerCheck;
import de.lemonpvp.flfac.check.impl.world.ScaffoldCheck;

import java.util.EnumMap;
import java.util.Map;

/** Holds the singleton instances of every registered check. */
public final class CheckManager {

    private final Map<CheckType, Check> checks = new EnumMap<>(CheckType.class);

    // Combat
    private final ReachCheck reach;
    private final HitboxCheck hitbox;
    private final KillAuraCheck killAura;
    private final AutoClickerCheck autoClicker;
    private final VelocityCheck velocity;

    // Movement
    private final SpeedCheck speed;
    private final FlyCheck fly;
    private final NoFallCheck noFall;
    private final JesusCheck jesus;
    private final TimerCheck timer;

    // World
    private final ScaffoldCheck scaffold;
    private final NukerCheck nuker;
    private final FastBreakCheck fastBreak;

    // Player
    private final BadPacketsCheck badPackets;

    public CheckManager(FLFAC plugin) {
        this.reach = register(new ReachCheck(plugin));
        this.hitbox = register(new HitboxCheck(plugin));
        this.killAura = register(new KillAuraCheck(plugin));
        this.autoClicker = register(new AutoClickerCheck(plugin));
        this.velocity = register(new VelocityCheck(plugin));

        this.speed = register(new SpeedCheck(plugin));
        this.fly = register(new FlyCheck(plugin));
        this.noFall = register(new NoFallCheck(plugin));
        this.jesus = register(new JesusCheck(plugin));
        this.timer = register(new TimerCheck(plugin));

        this.scaffold = register(new ScaffoldCheck(plugin));
        this.nuker = register(new NukerCheck(plugin));
        this.fastBreak = register(new FastBreakCheck(plugin));

        this.badPackets = register(new BadPacketsCheck(plugin));
    }

    private <T extends Check> T register(T check) {
        checks.put(check.getType(), check);
        return check;
    }

    public Check get(CheckType type) {
        return checks.get(type);
    }

    public int enabledCount() {
        int count = 0;
        for (Check check : checks.values()) {
            if (check.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public int totalCount() {
        return checks.size();
    }

    public ReachCheck reach() { return reach; }
    public HitboxCheck hitbox() { return hitbox; }
    public KillAuraCheck killAura() { return killAura; }
    public AutoClickerCheck autoClicker() { return autoClicker; }
    public VelocityCheck velocity() { return velocity; }

    public SpeedCheck speed() { return speed; }
    public FlyCheck fly() { return fly; }
    public NoFallCheck noFall() { return noFall; }
    public JesusCheck jesus() { return jesus; }
    public TimerCheck timer() { return timer; }

    public ScaffoldCheck scaffold() { return scaffold; }
    public NukerCheck nuker() { return nuker; }
    public FastBreakCheck fastBreak() { return fastBreak; }

    public BadPacketsCheck badPackets() { return badPackets; }
}
