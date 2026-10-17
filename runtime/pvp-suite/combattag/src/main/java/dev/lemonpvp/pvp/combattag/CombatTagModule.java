package dev.lemonpvp.pvp.combattag;

import dev.lemonpvp.pvp.api.CombatTagService;
import dev.lemonpvp.pvp.api.ConfigChecks;
import dev.lemonpvp.pvp.api.Text;
import dev.lemonpvp.runtime.api.ConfigSnapshot;
import dev.lemonpvp.runtime.api.ConfigurableModule;
import dev.lemonpvp.runtime.api.ModuleContext;
import dev.lemonpvp.runtime.api.RuntimeModule;
import dev.lemonpvp.runtime.api.paper.CommandSpec;
import dev.lemonpvp.runtime.api.paper.PaperModulePlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * pvp-combattag: a player who hits or is hit by another player is "in combat" for a few
 * seconds (countdown in the action bar). Logging out in combat kills the player, some
 * commands (spawn, home, ...) are blocked. Publishes {@link CombatTagService} for other modules.
 */
public final class CombatTagModule implements RuntimeModule, ConfigurableModule {

    private static final String BYPASS = "pvp-combattag.bypass";

    /** Settings from config.yml. */
    private record Settings(int tagSeconds, boolean punishLogout, Set<String> blockedCommands, String tagged,
                            String actionBar, String safe, String blocked, String logout) {
        static Settings from(ConfigSnapshot c) {
            return new Settings(Math.clamp(c.getInt("tag-seconds", 15), 1, 120), c.getBoolean("punish-logout", true),
                    c.getStringList("blocked-commands").stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()),
                    c.getString("messages.tagged", "<red>⚔ Du bist im Kampf mit <enemy>! <gray>Nicht ausloggen (<seconds> s)."),
                    c.getString("messages.action-bar", "<red>⚔ Im Kampf: <white><seconds> s"),
                    c.getString("messages.safe", "<green>✔ Du bist nicht mehr im Kampf."),
                    c.getString("messages.blocked", "<red>/<command> ist im Kampf gesperrt <gray>(noch <seconds> s)."),
                    c.getString("messages.logout", "<red>☠ <player> hat sich im Kampf ausgeloggt und ist gestorben."));
        }
    }

    private PaperModulePlatform paper;
    private CombatTags tags;
    private volatile Settings settings;

    @Override
    public void onEnable(ModuleContext context) {
        this.paper = context.platform(PaperModulePlatform.class);
        this.settings = Settings.from(context.config());
        this.tags = new CombatTags(System::currentTimeMillis);
        // CombatTagService has one abstract method, so a lambda (method reference) implements it.
        context.services().provide(CombatTagService.class, this.tags::remaining);
        this.paper.events().listen(EntityDamageByEntityEvent.class, EventPriority.MONITOR, true, this::onDamage);
        this.paper.events().listen(PlayerQuitEvent.class, this::onQuit);
        this.paper.events().listen(PlayerDeathEvent.class, EventPriority.MONITOR, true,
                e -> this.tags.untag(e.getPlayer().getUniqueId()));
        this.paper.events().listen(PlayerCommandPreprocessEvent.class, EventPriority.LOW, true, this::onCommand);
        this.paper.scheduler().runTimer(this::tick, 10L, 10L);
        this.paper.commands().register(new CommandSpec("combat", "Zeigt, ob du im Kampf bist", "/combat [Spieler]",
                List.of("ct"), "pvp-combattag.use"), this::combatCommand);
        context.logger().info("Combat tag enabled: " + this.settings.tagSeconds() + " s per hit, logout punishment "
                + (this.settings.punishLogout() ? "on" : "off"));
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot c) {
        List<String> problems = new ArrayList<>();
        ConfigChecks.wholeNumber(c, "tag-seconds", 1, 120, problems);
        ConfigChecks.bool(c, "punish-logout", problems);
        for (String key : List.of("tagged", "action-bar", "safe", "blocked", "logout")) {
            ConfigChecks.text(c, "messages." + key, problems);
        }
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        this.settings = Settings.from(config);
    }

    private void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        Settings s = this.settings;
        long millis = s.tagSeconds() * 1000L;
        if (this.tags.tag(victim.getUniqueId(), millis)) {
            victim.sendMessage(Text.render(s.tagged(), "enemy", attacker.getName(), "seconds", s.tagSeconds()));
        }
        if (this.tags.tag(attacker.getUniqueId(), millis)) {
            attacker.sendMessage(Text.render(s.tagged(), "enemy", victim.getName(), "seconds", s.tagSeconds()));
        }
    }

    /** @return the player behind a hit: the damager itself or the shooter of an arrow, trident, ... */
    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (this.tags.remaining(id) == 0) {
            return;
        }
        this.tags.untag(id);
        Settings s = this.settings;
        if (s.punishLogout() && !player.hasPermission(BYPASS)) {
            player.setHealth(0.0); // a normal death: pvp-core counts it like any other
            this.paper.server().broadcast(Text.render(s.logout(), "player", player.getName()));
        }
    }

    private void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        long remaining = this.tags.remaining(player.getUniqueId());
        if (remaining == 0 || player.hasPermission(BYPASS)) {
            return;
        }
        String label = CommandLabels.of(event.getMessage());
        if (this.settings.blockedCommands().contains(label)) {
            event.setCancelled(true);
            player.sendMessage(Text.render(this.settings.blocked(), "command", label, "seconds", seconds(remaining)));
        }
    }

    /** Every half second: countdown in the action bar, "safe" message when the tag ends. */
    private void tick() {
        Server server = this.paper.server();
        Settings s = this.settings;
        for (UUID id : this.tags.expire()) {
            Player player = server.getPlayer(id);
            if (player != null) {
                player.sendActionBar(Text.render(s.safe()));
            }
        }
        for (UUID id : this.tags.tagged()) {
            Player player = server.getPlayer(id);
            if (player != null) {
                player.sendActionBar(Text.render(s.actionBar(), "seconds", seconds(this.tags.remaining(id))));
            }
        }
    }

    private boolean combatCommand(CommandSender sender, String label, String[] args) {
        Player target;
        if (args.length == 1) {
            target = this.paper.server().getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Text.render("<gray><name> ist nicht online.", "name", args[0]));
                return true;
            }
        } else if (args.length == 0 && sender instanceof Player player) {
            target = player;
        } else {
            return false;
        }
        long remaining = this.tags.remaining(target.getUniqueId());
        sender.sendMessage(remaining > 0
                ? Text.render("<red>⚔ <name> ist im Kampf: noch <seconds> s", "name", target.getName(), "seconds", seconds(remaining))
                : Text.render("<green>✔ <name> ist nicht im Kampf.", "name", target.getName()));
        return true;
    }

    private static long seconds(long millis) {
        return (millis + 999) / 1000;
    }
}
