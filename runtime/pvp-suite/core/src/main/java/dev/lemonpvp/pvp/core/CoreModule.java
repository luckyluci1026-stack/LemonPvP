package dev.lemonpvp.pvp.core;

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
import dev.lemonpvp.runtime.api.paper.ModuleCommandHandler;
import dev.lemonpvp.runtime.api.paper.ModuleTask;
import dev.lemonpvp.runtime.api.paper.PaperModulePlatform;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * pvp-core: counts kills and deaths, keeps streaks and an Elo rating, saves everything to
 * {@code lemon/module-data/pvp-core/stats.tsv} and offers {@code /stats}, {@code /top} and
 * {@code /pvp} (admin). Other modules of the suite use its {@link PvpStatsService} and
 * listen to {@link PvpKillEvent}.
 */
public final class CoreModule implements RuntimeModule, ConfigurableModule {

    private static final String STATS_FILE = "stats.tsv";
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    /** Settings from config.yml. */
    private record Settings(int kFactor, int autosaveMinutes, String killMessage, String deathMessage) {
        static Settings from(ConfigSnapshot c) {
            return new Settings(Math.clamp(c.getInt("k-factor", 32), 1, 100), Math.clamp(c.getInt("autosave-minutes", 5), 1, 60),
                    c.getString("messages.kill", "<green>+<change> Rating</green> <gray>für den Kill an <victim> (jetzt <rating>)"),
                    c.getString("messages.death", "<red>-<change> Rating</red> <gray>(<killer> hat dich getötet, jetzt <rating>)"));
        }
    }

    private ModuleContext context;
    private PaperModulePlatform paper;
    private StatsBook book;
    private volatile Settings settings;
    private ModuleTask autosave;

    // Saving: snapshots are numbered on the main thread; a writer never replaces a newer
    // file with an older snapshot (an async autosave can still run while onDisable saves).
    private final Object saveLock = new Object();
    private long snapshots;
    private long written = -1;

    @Override
    public void onEnable(ModuleContext context) throws IOException {
        this.context = context;
        this.paper = context.platform(PaperModulePlatform.class);
        this.settings = Settings.from(context.config());
        this.book = new StatsBook(this.settings.kFactor());
        StatsFile.Loaded loaded = StatsFile.read(statsFile()); // a few KB: fine on the main thread
        this.book.load(loaded.players());
        loaded.problems().forEach(p -> context.logger().warning(p));

        context.services().provide(PvpStatsService.class, new StatsService());
        this.paper.events().listen(PlayerDeathEvent.class, EventPriority.MONITOR, true, this::onDeath);
        this.paper.events().listen(PlayerJoinEvent.class,
                e -> this.book.seen(e.getPlayer().getUniqueId(), e.getPlayer().getName()));
        this.paper.commands().register(new CommandSpec("stats", "Zeigt PvP-Statistiken", "/stats [Spieler]", List.of(),
                "pvp-core.use"), new StatsCommand());
        this.paper.commands().register(new CommandSpec("top", "Die besten PvP-Spieler", "/top [Anzahl]", List.of(),
                "pvp-core.use"), new TopCommand());
        this.paper.commands().register(new CommandSpec("pvp", "PvP-Verwaltung",
                "/pvp <save|reset <Spieler|all confirm>|sim kill <Killer> <Opfer>|sim death <Spieler>>", List.of(),
                "pvp-core.admin"), new AdminCommand());
        this.autosave = scheduleAutosave();
        context.logger().info("PvP core enabled: " + this.book.size() + " players loaded from " + STATS_FILE);
    }

    @Override
    public void onDisable(ModuleContext context) {
        if (this.book.dirty()) {
            try {
                save(this.book.snapshot(), ++this.snapshots);
                this.book.markSaved();
            } catch (IOException e) {
                context.logger().warning("Could not save " + STATS_FILE + ": " + e);
            }
        }
        context.logger().info("PvP core disabled, statistics of " + this.book.size() + " players saved");
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot c) {
        List<String> problems = new ArrayList<>();
        ConfigChecks.wholeNumber(c, "k-factor", 1, 100, problems);
        ConfigChecks.wholeNumber(c, "autosave-minutes", 1, 60, problems);
        ConfigChecks.text(c, "messages.kill", problems);
        ConfigChecks.text(c, "messages.death", problems);
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        Settings next = Settings.from(config);
        this.book.kFactor(next.kFactor());
        if (next.autosaveMinutes() != this.settings.autosaveMinutes()) {
            this.autosave.close(); // a timer is a tracked resource: closing it stops it right away
            this.settings = next;
            this.autosave = scheduleAutosave();
        }
        this.settings = next;
    }

    // ------------------------------------------------------------------ kills

    private void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            countKill(killer.getUniqueId(), killer.getName(), victim.getUniqueId(), victim.getName(), false);
        } else {
            this.book.recordDeath(victim.getUniqueId(), victim.getName());
        }
    }

    private void countKill(UUID killerId, String killerName, UUID victimId, String victimName, boolean simulated) {
        StatsBook.Kill kill = this.book.recordKill(killerId, killerName, victimId, victimName);
        Settings s = this.settings;
        Server server = this.paper.server();
        Player killer = server.getPlayer(killerId);
        if (killer != null) {
            killer.sendMessage(Text.render(s.killMessage(), "change", kill.ratingChange(), "victim", victimName,
                    "rating", kill.killer().rating()));
        }
        Player victim = server.getPlayer(victimId);
        if (victim != null) {
            victim.sendMessage(Text.render(s.deathMessage(), "change", kill.ratingChange(), "killer", killerName,
                    "rating", kill.victim().rating()));
        }
        server.getPluginManager().callEvent(new PvpKillEvent(kill.killer(), kill.victim(), kill.ratingChange(),
                kill.endedStreak(), simulated));
    }

    // ------------------------------------------------------------------ saving

    private Path statsFile() {
        return this.context.dataDirectory().resolve(STATS_FILE);
    }

    private ModuleTask scheduleAutosave() {
        long period = 20L * 60 * this.settings.autosaveMinutes();
        return this.paper.scheduler().runTimer(this::autosave, period, period);
    }

    /** Takes a snapshot on the main thread and writes it on a worker thread. */
    private void autosave() {
        if (!this.book.dirty()) {
            return;
        }
        List<PlayerStats> snapshot = this.book.snapshot();
        long number = ++this.snapshots;
        this.book.markSaved();
        this.paper.scheduler().runAsync(() -> {
            try {
                save(snapshot, number);
            } catch (IOException e) {
                this.context.logger().warning("Could not save " + STATS_FILE + ": " + e);
                if (this.context.isActive()) {
                    this.paper.scheduler().run(this.book::markDirty);
                }
            }
        });
    }

    private void save(List<PlayerStats> snapshot, long number) throws IOException {
        synchronized (this.saveLock) {
            if (number <= this.written) {
                return; // a newer snapshot is already on disk
            }
            StatsFile.write(statsFile(), snapshot);
            this.written = number;
        }
    }

    // ------------------------------------------------------------------ commands

    private final class StatsService implements PvpStatsService {
        @Override
        public Optional<PlayerStats> stats(UUID player) {
            return CoreModule.this.book.stats(player);
        }

        @Override
        public Optional<PlayerStats> statsByName(String name) {
            return CoreModule.this.book.statsByName(name);
        }

        @Override
        public List<PlayerStats> top(int limit) {
            return CoreModule.this.book.top(limit);
        }

        @Override
        public int rank(UUID player) {
            return CoreModule.this.book.rank(player);
        }
    }

    private final class StatsCommand implements ModuleCommandHandler {
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (args.length > 1) {
                return false;
            }
            Optional<PlayerStats> stats;
            if (args.length == 1) {
                stats = CoreModule.this.book.statsByName(args[0]);
            } else if (sender instanceof Player player) {
                stats = CoreModule.this.book.stats(player.getUniqueId());
            } else {
                sender.sendMessage(Text.render("<red>Von der Konsole: /stats <name>", "name", "Spieler"));
                return true;
            }
            if (stats.isEmpty()) {
                sender.sendMessage(Text.render("<gray>Noch keine PvP-Statistik für <name>.", "name",
                        args.length == 1 ? args[0] : sender.getName()));
                return true;
            }
            PlayerStats s = stats.get();
            sender.sendMessage(Text.render("<gold>━━ PvP-Statistik: <yellow><name></yellow> ━━", "name", s.name()));
            sender.sendMessage(Text.render("<white>Kills <green><kills></green> · Tode <red><deaths></red> · K/D <yellow><kd>",
                    "kills", s.kills(), "deaths", s.deaths(), "kd", String.format(Locale.ROOT, "%.2f", s.kd())));
            sender.sendMessage(Text.render("<white>Serie <gold><streak></gold> · beste <gold><best></gold> · Rating <aqua><rating></aqua> <gray>(#<rank>)",
                    "streak", s.streak(), "best", s.bestStreak(), "rating", s.rating(), "rank", CoreModule.this.book.rank(s.id())));
            return true;
        }

        @Override
        public List<String> complete(CommandSender sender, String alias, String[] args) {
            return args.length == 1 ? onlineNames(args[0]) : List.of();
        }
    }

    private final class TopCommand implements ModuleCommandHandler {
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            int limit = 10;
            if (args.length == 1) {
                try {
                    limit = Math.clamp(Integer.parseInt(args[0]), 1, 20);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (args.length > 1) {
                return false;
            }
            List<PlayerStats> top = CoreModule.this.book.top(limit);
            if (top.isEmpty()) {
                sender.sendMessage(Text.render("<gray>Noch keine PvP-Kämpfe gezählt."));
                return true;
            }
            sender.sendMessage(Text.render("<gold>━━ Top <n> (Rating) ━━", "n", top.size()));
            for (int i = 0; i < top.size(); i++) {
                PlayerStats s = top.get(i);
                sender.sendMessage(Text.render("<yellow>#<pos></yellow> <white><name></white> <gray>·</gray> <aqua><rating></aqua> <gray>· <kills>/<deaths>",
                        "pos", i + 1, "name", s.name(), "rating", s.rating(), "kills", s.kills(), "deaths", s.deaths()));
            }
            return true;
        }
    }

    /** {@code /pvp}: saving, resetting and simulating kills to try the suite without players. */
    private final class AdminCommand implements ModuleCommandHandler {
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            StatsBook book = CoreModule.this.book;
            if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
                List<PlayerStats> snapshot = book.snapshot();
                try {
                    save(snapshot, ++CoreModule.this.snapshots);
                    book.markSaved();
                    sender.sendMessage(Text.render("<green>Gespeichert: <n> Spieler in " + STATS_FILE, "n", snapshot.size()));
                } catch (IOException e) {
                    sender.sendMessage(Text.render("<red>Speichern fehlgeschlagen: <error>", "error", e.getMessage()));
                }
                return true;
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
                if (args[1].equalsIgnoreCase("all")) {
                    if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
                        sender.sendMessage(Text.render("<yellow>Alle Statistiken gelöscht (<n> Spieler).", "n", book.resetAll()));
                    } else {
                        sender.sendMessage(Text.render("<red>Löscht ALLE Statistiken. Bestätigen: /pvp reset all confirm"));
                    }
                    return true;
                }
                Optional<PlayerStats> s = book.statsByName(args[1]);
                if (s.isPresent() && book.reset(s.get().id())) {
                    sender.sendMessage(Text.render("<yellow>Statistik von <name> gelöscht.", "name", s.get().name()));
                } else {
                    sender.sendMessage(Text.render("<gray>Keine Statistik für <name>.", "name", args[1]));
                }
                return true;
            }
            if (args.length >= 3 && args[0].equalsIgnoreCase("sim")) {
                return simulate(sender, args);
            }
            return false;
        }

        /**
         * {@code sim kill <killer> <victim>} and {@code sim death <player>}: the same code path as a
         * real kill, for trying the suite without players. Offline names get offline-mode UUIDs.
         */
        private boolean simulate(CommandSender sender, String[] args) {
            for (int i = 2; i < args.length; i++) {
                if (!PLAYER_NAME.matcher(args[i]).matches()) {
                    sender.sendMessage(Text.render("<red>Ungültiger Spielername: <name>", "name", args[i]));
                    return true;
                }
            }
            if (args[1].equalsIgnoreCase("kill") && args.length == 4) {
                if (args[2].equalsIgnoreCase(args[3])) {
                    sender.sendMessage(Text.render("<red>Killer und Opfer müssen verschieden sein."));
                    return true;
                }
                countKill(uuidOf(args[2]), args[2], uuidOf(args[3]), args[3], true);
                sender.sendMessage(Text.render("<gray>[sim] <killer> hat <victim> getötet.", "killer", args[2], "victim", args[3]));
                return true;
            }
            if (args[1].equalsIgnoreCase("death") && args.length == 3) {
                int ended = CoreModule.this.book.recordDeath(uuidOf(args[2]), args[2]);
                sender.sendMessage(Text.render("<gray>[sim] <name> ist gestorben (Serie <ended> beendet).", "name", args[2],
                        "ended", ended));
                return true;
            }
            return false;
        }

        @Override
        public List<String> complete(CommandSender sender, String alias, String[] args) {
            if (args.length == 1) {
                return startingWith(List.of("save", "reset", "sim"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("sim")) {
                return startingWith(List.of("kill", "death"), args[1]);
            }
            return onlineNames(args[args.length - 1]);
        }
    }

    private UUID uuidOf(String name) {
        Player online = this.paper.server().getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        // The UUID an offline-mode server gives this name.
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private List<String> onlineNames(String prefix) {
        return startingWith(this.paper.server().getOnlinePlayers().stream().map(Player::getName).toList(), prefix);
    }

    private static List<String> startingWith(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
