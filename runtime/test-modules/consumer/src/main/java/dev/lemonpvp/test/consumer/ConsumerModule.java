package dev.lemonpvp.test.consumer;

import dev.lemonpvp.runtime.api.ModuleContext;
import dev.lemonpvp.runtime.api.RuntimeModule;
import dev.lemonpvp.runtime.api.paper.CommandSpec;
import dev.lemonpvp.runtime.api.paper.PaperModulePlatform;
import dev.lemonpvp.test.library.CounterService;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Uses {@link CounterService} from test-library: {@code /testcount [name]} increments a
 * counter there. Declares {@code test-library >=1.0.0 <2.0.0}, so the runtime refuses to
 * enable it without the library, refuses to disable or reload the library while this
 * module is loaded, and refuses library 2.0.0.
 */
public final class ConsumerModule implements RuntimeModule {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    @Override
    public void onEnable(ModuleContext context) {
        // Cached for this enable cycle only; the runtime keeps the provider enabled meanwhile.
        CounterService counters = context.services().find(CounterService.class)
                .orElseThrow(() -> new IllegalStateException("CounterService is not available; enable test-library first"));
        context.platform(PaperModulePlatform.class).commands().register(
                new CommandSpec("testcount", "Increments a counter in test-library", "/testcount [name]", List.of(),
                        "test-consumer.use"),
                (sender, label, args) -> {
                    String name = args.length > 0 ? args[0] : "default";
                    if (!NAME.matcher(name).matches()) {
                        sender.sendMessage(Component.text("TESTCOUNT: name must be 1-32 letters, digits, - or _",
                                NamedTextColor.RED));
                        return true;
                    }
                    long value = counters.increment(name);
                    sender.sendMessage(Component.text("TESTCOUNT " + name + " = " + value + " (CounterService of test-library "
                            + counters.providerVersion() + ")", NamedTextColor.AQUA));
                    return true;
                });
        context.logger().info("Test consumer enabled, using the CounterService of test-library " + counters.providerVersion());
    }
}
