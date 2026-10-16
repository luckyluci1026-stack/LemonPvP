package dev.lemonpvp.test.stress;

import dev.lemonpvp.runtime.api.ConfigSnapshot;
import dev.lemonpvp.runtime.api.ConfigurableModule;
import dev.lemonpvp.runtime.api.ModuleContext;
import dev.lemonpvp.runtime.api.RuntimeModule;
import dev.lemonpvp.runtime.api.TrackedResource;
import dev.lemonpvp.runtime.api.paper.CommandSpec;
import dev.lemonpvp.runtime.api.paper.ModuleCommandHandler;
import dev.lemonpvp.runtime.api.paper.ModuleScheduler;
import dev.lemonpvp.runtime.api.paper.PaperModulePlatform;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.PluginManager;

/**
 * Stress test for the module runtime, driven by {@code /stress} (console or operators).
 *
 * <p>Scenarios register thousands of listeners, timers and commands through the module
 * context, fire event storms, hold memory and hop between threads. Everything stays
 * registered until {@code /stress clear}, disable, unload or a hot swap, which must release
 * all of it ({@code /fork module diagnostics test-stress} shows what is tracked). Each
 * scenario ends with one line {@code STRESS <scenario>: ... - OK}, {@code - FAILED} or
 * {@code - REFUSED} (a limit of this module was reached; nothing was changed).</p>
 *
 * <p>{@code generation} in {@code /stress status} counts enables across hot swaps and
 * restarts. It is stored in the module data directory, so after {@code /fork module reload}
 * it shows that the new instance found the state the old one left behind.</p>
 */
public final class StressModule implements RuntimeModule, ConfigurableModule {

    private static final String PERMISSION = "test-stress.use";
    private static final String STATE_FILE = "state.properties";
    private static final String USAGE = "/stress <status|all|clear|listeners|events|tasks|commands|memory|async> [amount]";
    private static final List<String> SUBCOMMANDS = List.of("status", "all", "clear", "listeners", "events", "tasks",
            "commands", "memory", "async");
    private static final long MIB = 1024L * 1024L;
    /** Upper bound of handler calls in one {@code /stress events} (blocks the main thread). */
    private static final long MAX_HANDLER_CALLS = 50_000_000L;
    private static final long WARMUP_TICKS = 10;
    private static final long MEASURE_TICKS = 40;

    /** A scenario with an amount: name, key in config.yml, default and hard upper bound. */
    private enum Scenario {
        LISTENERS("listeners", "listeners", 2_000, 50_000),
        EVENTS("events", "events", 10_000, 10_000_000),
        TASKS("tasks", "tasks", 500, 10_000),
        COMMANDS("commands", "commands", 200, 2_000),
        MEMORY("memory", "memory-mb", 64, 1_024),
        ASYNC("async", "async", 500, 2_000);

        private final String command;
        private final String configKey;
        private final int defaultAmount;
        private final int max;

        Scenario(String command, String configKey, int defaultAmount, int max) {
            this.command = command;
            this.configKey = configKey;
            this.defaultAmount = defaultAmount;
            this.max = max;
        }

        static Optional<Scenario> byCommand(String command) {
            return Arrays.stream(values()).filter(s -> s.command.equals(command)).findFirst();
        }
    }

    private ModuleContext context;
    private PaperModulePlatform paper;
    private long generation;
    private final List<TrackedResource> listeners = new ArrayList<>();
    private final List<TrackedResource> timers = new ArrayList<>();
    private final List<TrackedResource> commands = new ArrayList<>();
    private final List<byte[]> memory = new ArrayList<>();
    private long timerRuns;
    private long commandCalls;
    private int nextCommand;

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        this.paper = context.platform(PaperModulePlatform.class);
        // A re-enabled instance starts empty: the runtime released everything on disable.
        this.listeners.clear();
        this.timers.clear();
        this.commands.clear();
        this.memory.clear();
        this.timerRuns = 0;
        this.commandCalls = 0;
        this.nextCommand = 0;
        this.generation = nextGeneration(context.dataDirectory().resolve(STATE_FILE));
        context.resources().onCleanup("stress memory", this.memory::clear);
        this.paper.commands().register(new CommandSpec("stress", "Lemon module runtime stress test", USAGE, List.of(),
                PERMISSION), new Handler());
        context.logger().info("Stress test enabled: generation " + this.generation + ", version "
                + context.descriptor().version());
    }

    @Override
    public void onDisable(ModuleContext context) {
        context.logger().info("Stress test disabled: generation " + this.generation + ", the runtime now releases "
                + context.resources().size() + " tracked resources (" + this.listeners.size() + " stress listeners, "
                + this.timers.size() + " timers, " + this.commands.size() + " commands, " + this.memory.size() + " MiB)");
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot candidate) {
        List<String> problems = new ArrayList<>();
        for (Scenario s : Scenario.values()) {
            Optional<Object> value = candidate.get(s.configKey);
            if (value.isPresent() && !(value.get() instanceof Integer i && i >= 1 && i <= s.max)) {
                problems.add(s.configKey + " must be a whole number from 1 to " + s.max);
            }
        }
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        // Amounts are read from context.config() whenever a scenario starts.
    }

    /** Reads, increments and stores the generation counter (a few bytes, main thread is fine). */
    private long nextGeneration(Path file) {
        long previous = 0;
        try {
            if (Files.exists(file)) {
                String text = Files.readString(file, StandardCharsets.UTF_8).strip();
                previous = Long.parseLong(text.startsWith("generation=") ? text.substring("generation=".length()) : text);
            }
        } catch (IOException | NumberFormatException e) {
            this.context.logger().warning("Could not read " + file.getFileName() + " (" + e + "); starting at generation 1");
        }
        long next = previous + 1;
        try {
            Files.writeString(file, "generation=" + next + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.context.logger().warning("Could not write " + file.getFileName() + ": " + e);
        }
        return next;
    }

    private final class Handler implements ModuleCommandHandler {

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (args.length < 1 || args.length > 2) {
                return false;
            }
            String name = args[0].toLowerCase(Locale.ROOT);
            switch (name) {
                case "status" -> status(sender);
                case "all" -> all(sender);
                case "clear" -> clear(sender);
                default -> {
                    Optional<Scenario> scenario = Scenario.byCommand(name);
                    if (scenario.isEmpty()) {
                        return false;
                    }
                    Integer amount = amount(sender, scenario.get(), args.length == 2 ? args[1] : null);
                    if (amount != null) {
                        run(sender, scenario.get(), amount);
                    }
                }
            }
            return true;
        }

        @Override
        public List<String> complete(CommandSender sender, String alias, String[] args) {
            if (args.length != 1) {
                return List.of();
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
    }

    /** @return the amount, or {@code null} after telling the sender why it is invalid */
    private Integer amount(CommandSender sender, Scenario s, String text) {
        if (text == null) {
            return configured(s);
        }
        try {
            int value = Integer.parseInt(text);
            if (value >= 1 && value <= s.max) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        refused(sender, s.command, "amount must be a whole number from 1 to " + s.max);
        return null;
    }

    private int configured(Scenario s) {
        return Math.clamp(this.context.config().getInt(s.configKey, s.defaultAmount), 1, s.max);
    }

    private void run(CommandSender sender, Scenario s, int amount) {
        switch (s) {
            case LISTENERS -> listeners(sender, amount);
            case EVENTS -> events(sender, amount);
            case TASKS -> tasks(sender, amount);
            case COMMANDS -> commands(sender, amount);
            case MEMORY -> memory(sender, amount);
            case ASYNC -> async(sender, amount);
        }
    }

    private void all(CommandSender sender) {
        info(sender, "all", "listeners, events, commands, memory, async and tasks with the amounts from config.yml");
        for (Scenario s : List.of(Scenario.LISTENERS, Scenario.EVENTS, Scenario.COMMANDS, Scenario.MEMORY, Scenario.ASYNC,
                Scenario.TASKS)) {
            run(sender, s, configured(s));
        }
    }

    // ------------------------------------------------------------------ scenarios

    private void listeners(CommandSender sender, int n) {
        if (this.listeners.size() + n > Scenario.LISTENERS.max) {
            refused(sender, "listeners", "at most " + Scenario.LISTENERS.max + " stress listeners at once (now "
                    + this.listeners.size() + "); /stress clear releases them");
            return;
        }
        EventPriority[] priorities = EventPriority.values();
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            EventPriority priority = priorities[this.listeners.size() % priorities.length];
            this.listeners.add(this.paper.events().listen(StressEvent.class, priority, false, StressEvent::hit));
        }
        ok(sender, "listeners", "+" + n + " registered in " + ms(System.nanoTime() - start) + " (stress listeners now "
                + this.listeners.size() + ", tracked resources " + this.context.resources().size() + ")");
    }

    private void events(CommandSender sender, int n) {
        long perEvent = this.listeners.size();
        long expected = n * perEvent;
        if (expected > MAX_HANDLER_CALLS) {
            refused(sender, "events", n + " events x " + perEvent + " listeners = " + expected + " handler calls (limit "
                    + MAX_HANDLER_CALLS + ", they block the main thread); lower the amount or /stress clear");
            return;
        }
        PluginManager events = this.paper.server().getPluginManager();
        long hits = 0;
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            StressEvent event = new StressEvent();
            events.callEvent(event);
            hits += event.hits();
        }
        long nanos = System.nanoTime() - start;
        String text = n + " events x " + perEvent + " listeners = " + expected + " handler calls in " + ms(nanos)
                + (expected > 0 ? " (" + nanos / expected + " ns per call)" : "");
        if (hits == expected) {
            ok(sender, "events", text);
        } else {
            failed(sender, "events", text + ", but the handlers ran " + hits + " times");
        }
    }

    private void tasks(CommandSender sender, int n) {
        if (this.timers.size() + n > Scenario.TASKS.max) {
            refused(sender, "tasks", "at most " + Scenario.TASKS.max + " stress timers at once (now " + this.timers.size()
                    + "); /stress clear releases them");
            return;
        }
        ModuleScheduler scheduler = this.paper.scheduler();
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            this.timers.add(scheduler.runTimer(() -> this.timerRuns++, 1L, 1L));
        }
        int active = this.timers.size();
        info(sender, "tasks", "+" + n + " timers (every tick) started in " + ms(System.nanoTime() - start) + ", now "
                + active + "; measuring " + MEASURE_TICKS + " ticks ...");
        Server server = this.paper.server();
        long[] base = new long[2];
        scheduler.runLater(() -> {
            base[0] = this.timerRuns;
            base[1] = server.getCurrentTick();
        }, WARMUP_TICKS);
        scheduler.runLater(() -> {
            if (this.timers.size() != active) {
                info(sender, "tasks", "the number of timers changed during the measurement; no verdict");
                return;
            }
            long runs = this.timerRuns - base[0];
            long ticks = server.getCurrentTick() - base[1];
            long expectedRuns = active * ticks;
            String text = active + " timers ran " + runs + " times in " + ticks + " ticks (expected " + expectedRuns
                    + "), average tick " + String.format(Locale.ROOT, "%.2f", server.getAverageTickTime()) + " ms";
            if (ticks > 0 && Math.abs(runs - expectedRuns) <= active) {
                ok(sender, "tasks", text);
            } else {
                failed(sender, "tasks", text);
            }
        }, WARMUP_TICKS + MEASURE_TICKS);
    }

    private void commands(CommandSender sender, int n) {
        if (this.commands.size() + n > Scenario.COMMANDS.max) {
            refused(sender, "commands", "at most " + Scenario.COMMANDS.max + " stress commands at once (now "
                    + this.commands.size() + "); /stress clear releases them");
            return;
        }
        Server server = this.paper.server();
        String namespace = this.context.descriptor().id();
        int first = this.nextCommand;
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            String label = "stress-cmd-" + this.nextCommand++;
            this.commands.add(this.paper.commands().register(CommandSpec.of(label).withPermission(PERMISSION),
                    (s, l, a) -> {
                        this.commandCalls++;
                        return true;
                    }));
        }
        long registered = System.nanoTime();
        int players = updateCommandTrees(server);
        long synced = System.nanoTime();
        long before = this.commandCalls;
        CommandSender console = server.getConsoleSender();
        for (int i = first; i < this.nextCommand; i++) {
            server.dispatchCommand(console, namespace + ":stress-cmd-" + i);
        }
        long answered = this.commandCalls - before;
        String text = "+" + n + " registered in " + ms(registered - start) + ", command tree sent to " + players
                + " player(s) in " + ms(synced - registered) + ", " + answered + "/" + n + " answered in "
                + ms(System.nanoTime() - synced) + " (stress commands now " + this.commands.size() + ")";
        if (answered == n) {
            ok(sender, "commands", text);
        } else {
            failed(sender, "commands", text);
        }
    }

    private void memory(CommandSender sender, int mib) {
        Runtime rt = Runtime.getRuntime();
        long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        if (this.memory.size() + mib > Scenario.MEMORY.max) {
            refused(sender, "memory", "at most " + Scenario.MEMORY.max + " MiB at once (now " + this.memory.size() + ")");
            return;
        }
        if (mib * MIB > free / 2) {
            refused(sender, "memory", mib + " MiB is more than half of the free heap (" + free / MIB + " MiB free)");
            return;
        }
        long start = System.nanoTime();
        for (int i = 0; i < mib; i++) {
            byte[] block = new byte[(int) MIB];
            block[block.length - 1] = 1;
            this.memory.add(block);
        }
        ok(sender, "memory", "holding " + this.memory.size() + " MiB (+" + mib + " in " + ms(System.nanoTime() - start)
                + "), heap " + heap());
    }

    /**
     * Async tasks that each hash 64 KiB off the main thread and come back with {@code run()}.
     * Also checks that one-shot task handles are released once they have run (they used to
     * stay tracked until the module was disabled).
     */
    private void async(CommandSender sender, int n) {
        ModuleScheduler scheduler = this.paper.scheduler();
        Server server = this.paper.server();
        Queue<TrackedResource> created = new ConcurrentLinkedQueue<>();
        AtomicInteger onMainThread = new AtomicInteger();
        int[] done = {0};
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            int seed = i;
            created.add(scheduler.runAsync(() -> {
                if (server.isPrimaryThread()) {
                    onMainThread.incrementAndGet();
                }
                hash(seed);
                if (!this.context.isActive()) {
                    return; // disabled meanwhile: do not touch the server any more
                }
                try {
                    created.add(scheduler.run(() -> {
                        if (++done[0] == n) {
                            long nanos = System.nanoTime() - start;
                            // A few ticks later every callback has returned and released its handle.
                            scheduler.runLater(() -> asyncVerdict(sender, n, nanos, created, onMainThread.get()), 5L);
                        }
                    }));
                } catch (IllegalStateException disabledMeanwhile) {
                    // the module was disabled between the check above and run()
                }
            }));
        }
        info(sender, "async", n + " async tasks submitted ...");
    }

    private void asyncVerdict(CommandSender sender, int n, long nanos, Queue<TrackedResource> created, int onMainThread) {
        long unreleased = created.stream().filter(r -> !r.isReleased()).count();
        String text = n + " async tasks hashed 64 KiB each and came back to the main thread in " + ms(nanos) + "; "
                + created.size() + " one-shot task handles, " + unreleased + " still tracked after running"
                + (onMainThread > 0 ? ", " + onMainThread + " ran on the main thread" : "");
        if (unreleased == 0 && onMainThread == 0 && created.size() == 2 * n) {
            ok(sender, "async", text);
        } else {
            failed(sender, "async", text);
        }
    }

    private void clear(CommandSender sender) {
        int before = this.context.resources().size();
        boolean hadCommands = !this.commands.isEmpty();
        long start = System.nanoTime();
        int released = release(this.listeners) + release(this.timers) + release(this.commands);
        if (hadCommands) {
            updateCommandTrees(this.paper.server());
        }
        int mib = this.memory.size();
        this.memory.clear();
        int after = this.context.resources().size();
        String text = "released " + released + " registrations early in " + ms(System.nanoTime() - start) + " and dropped "
                + mib + " MiB; tracked resources " + before + " -> " + after;
        if (after == before - released) {
            ok(sender, "clear", text);
        } else {
            failed(sender, "clear", text);
        }
    }

    private void status(CommandSender sender) {
        info(sender, "status", this.context.descriptor().id() + " " + this.context.descriptor().version() + ", generation "
                + this.generation + " (enables across hot swaps and restarts)");
        info(sender, "status", "held: " + this.listeners.size() + " listeners, " + this.timers.size() + " timers ("
                + this.timerRuns + " runs), " + this.commands.size() + " commands (" + this.commandCalls + " calls), "
                + this.memory.size() + " MiB; tracked resources " + this.context.resources().size());
        info(sender, "status", "heap " + heap() + ", average tick "
                + String.format(Locale.ROOT, "%.2f", this.paper.server().getAverageTickTime()) + " ms");
    }

    // ------------------------------------------------------------------ helpers

    private static int release(List<TrackedResource> handles) {
        int n = handles.size();
        handles.forEach(TrackedResource::close);
        handles.clear();
        return n;
    }

    private static int updateCommandTrees(Server server) {
        int players = 0;
        for (Player player : server.getOnlinePlayers()) {
            player.updateCommands();
            players++;
        }
        return players;
    }

    private static void hash(int seed) {
        byte[] data = new byte[64 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 31 + seed);
        }
        try {
            MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String heap() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / MIB + " / " + rt.maxMemory() / MIB + " MiB used";
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.1f ms", nanos / 1_000_000.0);
    }

    private static void ok(CommandSender sender, String scenario, String text) {
        send(sender, "STRESS " + scenario + ": " + text + " - OK", NamedTextColor.GREEN);
    }

    private static void failed(CommandSender sender, String scenario, String text) {
        send(sender, "STRESS " + scenario + ": " + text + " - FAILED", NamedTextColor.RED);
    }

    private static void refused(CommandSender sender, String scenario, String text) {
        send(sender, "STRESS " + scenario + ": " + text + " - REFUSED", NamedTextColor.GOLD);
    }

    private static void info(CommandSender sender, String scenario, String text) {
        send(sender, "STRESS " + scenario + ": " + text, NamedTextColor.GRAY);
    }

    private static void send(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }
}
