package dev.paperswap.test.chaos;

import dev.paperswap.runtime.api.ConfigSnapshot;
import dev.paperswap.runtime.api.ConfigurableModule;
import dev.paperswap.runtime.api.ModuleContext;
import dev.paperswap.runtime.api.RuntimeModule;
import dev.paperswap.runtime.api.TrackedResource;
import dev.paperswap.runtime.api.paper.CommandSpec;
import dev.paperswap.runtime.api.paper.ModuleCommandHandler;
import dev.paperswap.runtime.api.paper.PaperModulePlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventPriority;

/**
 * Misbehaves on purpose so that the runtime's fault handling can be tested on a real server.
 *
 * <p>Lifecycle faults come from {@code config.yml} (see the comments there). Runtime faults
 * come from {@code /chaos}: a command, a listener and a task that throw, an async task that
 * blocks the unload, and a thread that is deliberately not stopped (a leak the leak detector
 * must report). Result lines start with {@code CHAOS <subcommand>:}.</p>
 *
 * <p>{@code enable #N of this instance} in {@code /chaos ping} tells a rolled-back version
 * (same instance, enabled again) from a new one (enable #1).</p>
 */
public final class ChaosModule implements RuntimeModule, ConfigurableModule {

    static final String LEAK_THREAD_NAME = "test-chaos-leaked-thread";
    private static final int MAX_DELAY_MS = 10_000;
    private static final String USAGE = "/chaos <ping|status|throw-command|throw-listener [n]|throw-task"
            + "|block-async <seconds>|leak-thread <seconds>>";
    private static final List<String> SUBCOMMANDS = List.of("ping", "status", "throw-command", "throw-listener",
            "throw-task", "block-async", "leak-thread");

    /** Faults configured in config.yml. */
    private record Faults(boolean failOnLoad, boolean failOnEnable, boolean failOnDisable, int enableDelayMs) {
        static Faults from(ConfigSnapshot c) {
            return new Faults(c.getBoolean("fail-on-load", false), c.getBoolean("fail-on-enable", false),
                    c.getBoolean("fail-on-disable", false), Math.clamp(c.getInt("enable-delay-ms", 0), 0, MAX_DELAY_MS));
        }

        @Override
        public String toString() {
            return "fail-on-load=" + this.failOnLoad + ", fail-on-enable=" + this.failOnEnable + ", fail-on-disable="
                    + this.failOnDisable + ", enable-delay-ms=" + this.enableDelayMs;
        }
    }

    private ModuleContext context;
    private PaperModulePlatform paper;
    private volatile Faults faults = Faults.from(ConfigSnapshot.empty());
    private int enableCount;
    private long observedEvents;
    private TrackedResource throwingListener;

    @Override
    public void onLoad(ModuleContext context) {
        if (Faults.from(context.config()).failOnLoad()) {
            throw new IllegalStateException("deliberate failure in onLoad (fail-on-load: true)");
        }
    }

    @Override
    public void onEnable(ModuleContext context) throws InterruptedException {
        this.context = context;
        this.faults = Faults.from(context.config());
        this.enableCount++;
        this.observedEvents = 0;
        this.throwingListener = null;
        if (this.faults.enableDelayMs() > 0) {
            context.logger().info("Blocking the main thread for " + this.faults.enableDelayMs()
                    + " ms in onEnable (enable-delay-ms)");
            Thread.sleep(this.faults.enableDelayMs());
        }
        this.paper = context.platform(PaperModulePlatform.class);
        // Registered before the deliberate failure below: the runtime must release both again.
        this.paper.events().listen(ChaosEvent.class, event -> this.observedEvents++);
        this.paper.commands().register(new CommandSpec("chaos", "PaperSwap module runtime fault test", USAGE, List.of(),
                "test-chaos.use"), new Handler());
        if (this.faults.failOnEnable()) {
            throw new IllegalStateException("deliberate failure in onEnable (fail-on-enable: true), after registering"
                    + " a listener and /chaos");
        }
        context.logger().info("Chaos test enabled: enable #" + this.enableCount + " of this instance, version "
                + context.descriptor().version());
    }

    @Override
    public void onDisable(ModuleContext context) {
        if (this.faults.failOnDisable()) {
            throw new IllegalStateException("deliberate failure in onDisable (fail-on-disable: true)");
        }
        context.logger().info("Chaos test disabled after enable #" + this.enableCount + " of this instance");
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot candidate) {
        List<String> problems = new ArrayList<>();
        for (String key : List.of("fail-on-load", "fail-on-enable", "fail-on-disable")) {
            Optional<Object> value = candidate.get(key);
            if (value.isPresent() && !(value.get() instanceof Boolean)) {
                problems.add(key + " must be true or false");
            }
        }
        Optional<Object> delay = candidate.get("enable-delay-ms");
        if (delay.isPresent() && !(delay.get() instanceof Integer ms && ms >= 0 && ms <= MAX_DELAY_MS)) {
            problems.add("enable-delay-ms must be a whole number from 0 to " + MAX_DELAY_MS);
        }
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        this.faults = Faults.from(config);
    }

    private final class Handler implements ModuleCommandHandler {

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (args.length < 1 || args.length > 2) {
                return false;
            }
            String sub = args[0].toLowerCase(Locale.ROOT);
            String arg = args.length == 2 ? args[1] : null;
            switch (sub) {
                case "ping" -> reply(sender, "ping", "alive, version " + ChaosModule.this.context.descriptor().version()
                        + ", enable #" + ChaosModule.this.enableCount + " of this instance");
                case "status" -> reply(sender, "status", "faults from config.yml: " + ChaosModule.this.faults);
                case "throw-command" -> throw new IllegalStateException("deliberate exception in /chaos throw-command");
                case "throw-listener" -> {
                    Integer n = number(sender, sub, arg, 3, 1, 1_000);
                    if (n != null) {
                        throwListener(sender, n);
                    }
                }
                case "throw-task" -> {
                    ChaosModule.this.paper.scheduler().run(() -> {
                        throw new IllegalStateException("deliberate exception in a task of /chaos throw-task");
                    });
                    reply(sender, "throw-task", "scheduled a task that throws on the next tick");
                }
                case "block-async" -> {
                    Integer seconds = number(sender, sub, arg, null, 1, 120);
                    if (seconds != null) {
                        blockAsync(sender, seconds);
                    }
                }
                case "leak-thread" -> {
                    Integer seconds = number(sender, sub, arg, null, 1, 600);
                    if (seconds != null) {
                        leakThread(sender, seconds);
                    }
                }
                default -> {
                    return false;
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

    /** A listener that throws runs before the normal one; the normal one must still see every event. */
    private void throwListener(CommandSender sender, int n) {
        if (this.throwingListener == null) {
            this.throwingListener = this.paper.events().listen(ChaosEvent.class, EventPriority.LOW, false, event -> {
                throw new IllegalStateException("deliberate exception in a ChaosEvent listener");
            });
        }
        long before = this.observedEvents;
        for (int i = 0; i < n; i++) {
            this.paper.server().getPluginManager().callEvent(new ChaosEvent());
        }
        long seen = this.observedEvents - before;
        String text = "fired " + n + " events; a throwing listener ran first each time and the normal listener saw "
                + seen + "/" + n;
        reply(sender, "throw-listener", text + (seen == n ? " - OK" : " - FAILED"));
    }

    /** Blocks a scheduler worker; unloading must be refused until it returns. */
    private void blockAsync(CommandSender sender, int seconds) {
        ModuleContext ctx = this.context;
        this.paper.scheduler().runAsync(() -> {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ctx.logger().info("block-async finished after " + seconds + " s (module active: " + ctx.isActive() + ")");
        });
        reply(sender, "block-async", "an async task now blocks for " + seconds + " s; /fork module unload test-chaos"
                + " must be refused until it has returned");
    }

    /** Starts a thread that is deliberately neither tracked nor stopped on disable. */
    private void leakThread(CommandSender sender, int seconds) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, LEAK_THREAD_NAME);
        thread.setDaemon(true);
        thread.setContextClassLoader(ChaosModule.class.getClassLoader());
        thread.start();
        reply(sender, "leak-thread", "started thread '" + LEAK_THREAD_NAME + "' for " + seconds + " s, deliberately NOT"
                + " stopped on disable; after /fork module unload test-chaos the leak detector must name it until it ends");
    }

    /** @return the number, or {@code null} after telling the sender why it is invalid */
    private static Integer number(CommandSender sender, String sub, String text, Integer def, int min, int max) {
        if (text == null && def != null) {
            return def;
        }
        try {
            int value = Integer.parseInt(String.valueOf(text));
            if (value >= min && value <= max) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        reply(sender, sub, "expects a whole number from " + min + " to " + max);
        return null;
    }

    private static void reply(CommandSender sender, String sub, String text) {
        sender.sendMessage(Component.text("CHAOS " + sub + ": " + text, NamedTextColor.LIGHT_PURPLE));
    }
}
