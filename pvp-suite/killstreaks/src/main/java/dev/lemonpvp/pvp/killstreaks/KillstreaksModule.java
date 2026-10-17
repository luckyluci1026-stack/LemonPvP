package dev.lemonpvp.pvp.killstreaks;

import dev.lemonpvp.pvp.api.ConfigChecks;
import dev.lemonpvp.pvp.api.PlayerStats;
import dev.lemonpvp.pvp.api.PvpKillEvent;
import dev.lemonpvp.pvp.api.Text;
import dev.paperswap.runtime.api.ConfigSnapshot;
import dev.paperswap.runtime.api.ConfigurableModule;
import dev.paperswap.runtime.api.ModuleContext;
import dev.paperswap.runtime.api.RuntimeModule;
import dev.paperswap.runtime.api.paper.PaperModulePlatform;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * pvp-killstreaks: announces streaks with a broadcast, a title and a sound, gives potion
 * effects as rewards, strikes harmless lightning where the victim died and announces who
 * ended a long streak. Reacts only to {@link PvpKillEvent} from pvp-core.
 */
public final class KillstreaksModule implements RuntimeModule, ConfigurableModule {

    private static final Sound STREAK_SOUND = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 1f, 1f);
    private static final Sound BIG_STREAK_SOUND = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1f);
    private static final Sound SHUTDOWN_SOUND = Sound.sound(Key.key("entity.wither.spawn"), Sound.Source.MASTER, 0.6f, 1.4f);
    private static final Title.Times TITLE_TIMES = Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000),
            Duration.ofMillis(500));

    /** Settings from config.yml; the rules are already parsed and checked. */
    private record Settings(StreakRules rules, int bigStreak, boolean lightning, String announce, String title,
                            String subtitle, String shutdown) {
        static Settings from(ConfigSnapshot c) {
            ConfigSnapshot rewards = c.getSection("rewards");
            Map<String, List<String>> specs = new LinkedHashMap<>();
            for (String key : rewards.keys()) {
                specs.put(key, rewards.getStringList(key));
            }
            StreakRules rules = StreakRules.of(c.getStringList("milestones"), c.getInt("repeat-every", 5),
                    c.getInt("shutdown-min-streak", 5), specs);
            return new Settings(rules, c.getInt("big-streak", 10), c.getBoolean("lightning", true),
                    c.getString("messages.announce", "<gold>✪ <yellow><player></yellow> hat eine <red><streak>er-Killstreak</red>!"),
                    c.getString("messages.title", "<gold><bold><streak>er SERIE!"),
                    c.getString("messages.subtitle", "<yellow>Weiter so!"),
                    c.getString("messages.shutdown", "<aqua>⚡ <killer> hat die <streak>er-Serie von <victim> beendet!"));
        }
    }

    private PaperModulePlatform paper;
    private volatile Settings settings;

    @Override
    public void onEnable(ModuleContext context) {
        // Invalid rules (after editing config.yml and reloading) stop the enable with a clear
        // message; during a hot swap the runtime then keeps the previous version running.
        this.settings = Settings.from(context.config());
        this.paper = context.platform(PaperModulePlatform.class);
        this.paper.events().listen(PvpKillEvent.class, this::onKill);
        context.logger().info("Killstreaks enabled");
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot c) {
        List<String> problems = new ArrayList<>();
        ConfigChecks.wholeNumber(c, "big-streak", 1, 1000, problems);
        ConfigChecks.bool(c, "lightning", problems);
        for (String key : List.of("announce", "title", "subtitle", "shutdown")) {
            ConfigChecks.text(c, "messages." + key, problems);
        }
        try {
            for (String effect : Settings.from(c).rules().effectNames()) {
                if (effectType(effect) == null) {
                    problems.add("unknown potion effect '" + effect + "' in rewards");
                }
            }
        } catch (IllegalArgumentException e) {
            problems.add(e.getMessage());
        }
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        this.settings = Settings.from(config);
    }

    private void onKill(PvpKillEvent event) {
        Settings s = this.settings;
        Server server = this.paper.server();
        PlayerStats killer = event.killer();
        int streak = killer.streak();
        if (s.rules().isMilestone(streak)) {
            server.broadcast(Text.render(s.announce(), "player", killer.name(), "streak", streak));
            Player player = server.getPlayer(killer.id());
            if (player != null) {
                player.showTitle(Title.title(Text.render(s.title(), "streak", streak), Text.render(s.subtitle(), "streak", streak),
                        TITLE_TIMES));
                player.playSound(STREAK_SOUND);
                for (StreakRules.Reward r : s.rules().rewardsFor(streak)) {
                    PotionEffectType type = effectType(r.effect());
                    if (type != null) {
                        player.addPotionEffect(new PotionEffect(type, r.seconds() * 20, r.amplifier()));
                    }
                }
            }
            if (streak >= s.bigStreak()) {
                server.playSound(BIG_STREAK_SOUND); // the server is an audience: every player hears it
            }
        }
        if (s.rules().isShutdown(event.endedStreak())) {
            server.broadcast(Text.render(s.shutdown(), "killer", killer.name(), "victim", event.victim().name(),
                    "streak", event.endedStreak()));
            server.playSound(SHUTDOWN_SOUND);
        }
        Player victim = server.getPlayer(event.victim().id());
        if (s.lightning() && victim != null) {
            victim.getWorld().strikeLightningEffect(victim.getLocation()); // only the effect, no damage or fire
        }
    }

    private static PotionEffectType effectType(String name) {
        return Registry.MOB_EFFECT.get(NamespacedKey.minecraft(name));
    }
}
