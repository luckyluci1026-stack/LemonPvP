package dev.lemonpvp.pvp.scoreboard;

import dev.lemonpvp.pvp.api.CombatTagService;
import dev.lemonpvp.pvp.api.ConfigChecks;
import dev.lemonpvp.pvp.api.PlayerStats;
import dev.lemonpvp.pvp.api.PvpKillEvent;
import dev.lemonpvp.pvp.api.PvpStatsService;
import dev.lemonpvp.pvp.api.Text;
import dev.lemonpvp.runtime.api.ConfigSnapshot;
import dev.lemonpvp.runtime.api.ConfigurableModule;
import dev.lemonpvp.runtime.api.ModuleContext;
import dev.lemonpvp.runtime.api.RuntimeModule;
import dev.lemonpvp.runtime.api.paper.CommandSpec;
import dev.lemonpvp.runtime.api.paper.ModuleTask;
import dev.lemonpvp.runtime.api.paper.PaperModulePlatform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * pvp-scoreboard: a live sidebar with the player's PvP statistics. Shows a combat countdown
 * at the top while pvp-combattag reports the player in combat; that module is optional and
 * looked up on every refresh. {@code /sb} turns the sidebar off and on.
 */
public final class ScoreboardModule implements RuntimeModule, ConfigurableModule {

    /** Settings from config.yml. */
    private record Settings(int refreshTicks, String title, List<String> lines, String combatLine) {
        static Settings from(ConfigSnapshot c) {
            List<String> lines = c.getStringList("lines");
            return new Settings(Math.clamp(c.getInt("refresh-ticks", 10), 5, 100),
                    c.getString("title", "<gold><bold>LemonPvP"),
                    lines.isEmpty() ? List.of("<white>Kills: <green><kills>", "<white>Rating: <aqua><rating>") : lines,
                    c.getString("combat-line", "<red><bold>⚔ IM KAMPF: <seconds>s"));
        }
    }

    private ModuleContext context;
    private PaperModulePlatform paper;
    private PvpStatsService stats;
    private volatile Settings settings;
    private ModuleTask refresher;
    private final Map<UUID, Board> boards = new HashMap<>();
    private final Set<UUID> hidden = new HashSet<>();

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        this.paper = context.platform(PaperModulePlatform.class);
        this.settings = Settings.from(context.config());
        // pvp-core is a declared dependency: it stays enabled as long as this module is.
        this.stats = context.services().find(PvpStatsService.class)
                .orElseThrow(() -> new IllegalStateException("PvpStatsService not available; enable pvp-core first"));
        this.boards.clear();
        this.hidden.clear();
        // On disable (and hot swap) everyone gets the normal scoreboard back.
        context.resources().onCleanup("restore the players' scoreboards", this::removeAll);
        this.paper.events().listen(PlayerJoinEvent.class, e -> show(e.getPlayer()));
        this.paper.events().listen(PlayerQuitEvent.class, e -> this.boards.remove(e.getPlayer().getUniqueId()));
        this.paper.events().listen(PvpKillEvent.class, EventPriority.MONITOR, false, this::onKill);
        this.paper.commands().register(new CommandSpec("sb", "Schaltet die PvP-Seitenleiste an oder aus", "/sb",
                List.of("pvpboard"), "pvp-scoreboard.use"), this::toggleCommand);
        this.refresher = scheduleRefresh();
        this.paper.server().getOnlinePlayers().forEach(this::show);
        context.logger().info("PvP scoreboard enabled for " + this.boards.size() + " online player(s)");
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot c) {
        List<String> problems = new ArrayList<>();
        ConfigChecks.wholeNumber(c, "refresh-ticks", 5, 100, problems);
        ConfigChecks.text(c, "title", problems);
        ConfigChecks.text(c, "combat-line", problems);
        if (c.getStringList("lines").size() > Board.MAX_LINES - 1) {
            problems.add("lines: at most " + (Board.MAX_LINES - 1) + " (one line is kept free for the combat countdown)");
        }
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        Settings next = Settings.from(config);
        boolean newPeriod = next.refreshTicks() != this.settings.refreshTicks();
        this.settings = next;
        if (newPeriod) {
            this.refresher.close();
            this.refresher = scheduleRefresh();
        }
        refreshAll();
    }

    private ModuleTask scheduleRefresh() {
        int ticks = this.settings.refreshTicks();
        return this.paper.scheduler().runTimer(this::refreshAll, ticks, ticks);
    }

    private void show(Player player) {
        if (this.hidden.contains(player.getUniqueId())) {
            return;
        }
        Board board = new Board(this.paper.server(), player);
        this.boards.put(player.getUniqueId(), board);
        refresh(player, board);
    }

    private void onKill(PvpKillEvent event) {
        for (UUID id : List.of(event.killer().id(), event.victim().id())) {
            Board board = this.boards.get(id);
            Player player = this.paper.server().getPlayer(id);
            if (board != null && player != null) {
                refresh(player, board);
            }
        }
    }

    private void refreshAll() {
        Server server = this.paper.server();
        this.boards.forEach((id, board) -> {
            Player player = server.getPlayer(id);
            if (player != null) {
                refresh(player, board);
            }
        });
    }

    private void refresh(Player player, Board board) {
        Settings s = this.settings;
        UUID id = player.getUniqueId();
        PlayerStats st = this.stats.stats(id).orElse(new PlayerStats(id, player.getName(), 0, 0, 0, 0, 1000));
        int rank = this.stats.rank(id);
        List<Component> lines = new ArrayList<>();
        long combat = combatMillis(id);
        if (combat > 0) {
            lines.add(Text.render(s.combatLine(), "seconds", (combat + 999) / 1000));
        }
        for (String template : s.lines()) {
            Component line = Text.render(template, "player", player.getName(), "kills", st.kills(), "deaths", st.deaths(),
                    "kd", String.format(Locale.ROOT, "%.2f", st.kd()), "streak", st.streak(), "best", st.bestStreak(),
                    "rating", st.rating(), "rank", rank == 0 ? "-" : rank, "online", this.paper.server().getOnlinePlayers().size());
            lines.add(template.isBlank() ? Component.text(" ") : line);
        }
        board.update(Text.render(s.title()), lines);
    }

    /** pvp-combattag is optional: no service, no countdown. Looked up fresh, never kept. */
    private long combatMillis(UUID id) {
        return this.context.services().find(CombatTagService.class).map(c -> c.remainingMillis(id)).orElse(0L);
    }

    private boolean toggleCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.render("<red>Nur für Spieler."));
            return true;
        }
        UUID id = player.getUniqueId();
        Board board = this.boards.remove(id);
        if (board != null) {
            board.remove(this.paper.server().getScoreboardManager().getMainScoreboard());
            this.hidden.add(id);
            player.sendMessage(Text.render("<gray>PvP-Seitenleiste aus. <white>/sb</white> schaltet sie wieder ein."));
        } else {
            this.hidden.remove(id);
            show(player);
            player.sendMessage(Text.render("<green>PvP-Seitenleiste an."));
        }
        return true;
    }

    private void removeAll() {
        var main = this.paper.server().getScoreboardManager().getMainScoreboard();
        this.boards.values().forEach(b -> b.remove(main));
        this.boards.clear();
    }
}
