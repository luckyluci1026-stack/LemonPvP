package dev.paperswap.test.library;

import dev.paperswap.runtime.api.ModuleContext;
import dev.paperswap.runtime.api.RuntimeModule;
import dev.paperswap.runtime.api.paper.CommandSpec;
import dev.paperswap.runtime.api.paper.PaperModulePlatform;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Provides {@link CounterService} to other modules and shows the counters with
 * {@code /testlib}. Counters live in memory: a reload of this module starts from zero.
 */
public final class LibraryModule implements RuntimeModule {

    @Override
    public void onEnable(ModuleContext context) {
        String version = context.descriptor().version();
        Counters counters = new Counters(version);
        context.services().provide(CounterService.class, counters);
        context.platform(PaperModulePlatform.class).commands().register(
                new CommandSpec("testlib", "Shows the counters of test-library", "/testlib", List.of(), "test-library.use"),
                (sender, label, args) -> {
                    sender.sendMessage(Component.text("TESTLIB " + version + ": counters " + counters.snapshot(),
                            NamedTextColor.AQUA));
                    return true;
                });
        context.logger().info("Test library " + version + " enabled, CounterService published");
    }

    @Override
    public void onDisable(ModuleContext context) {
        context.logger().info("Test library " + context.descriptor().version() + " disabled, CounterService withdrawn");
    }

    private static final class Counters implements CounterService {
        private final String version;
        private final Map<String, AtomicLong> values = new ConcurrentHashMap<>();

        Counters(String version) {
            this.version = version;
        }

        @Override
        public long increment(String name) {
            return this.values.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
        }

        @Override
        public String providerVersion() {
            return this.version;
        }

        Map<String, Long> snapshot() {
            Map<String, Long> out = new TreeMap<>();
            this.values.forEach((k, v) -> out.put(k, v.get()));
            return out;
        }
    }
}
