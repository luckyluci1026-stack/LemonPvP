package dev.paperswap.example.greeter;

import dev.paperswap.runtime.api.ConfigSnapshot;
import dev.paperswap.runtime.api.ConfigurableModule;
import dev.paperswap.runtime.api.ModuleContext;
import dev.paperswap.runtime.api.RuntimeModule;
import dev.paperswap.runtime.api.paper.CommandSpec;
import dev.paperswap.runtime.api.paper.PaperModulePlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Example runtime module.
 *
 * <ul>
 *     <li>greets joining players (event listener),</li>
 *     <li>provides {@code /greet} (command; also reachable as {@code /example-greeter:greet}),</li>
 *     <li>counts minutes since it was enabled (repeating task),</li>
 *     <li>applies {@code config.yml} changes without a restart ({@link ConfigurableModule}).</li>
 * </ul>
 * Everything is registered through the module context, so disabling or hot-swapping the
 * module removes it again. No static state is kept.
 */
public final class GreeterModule implements RuntimeModule, ConfigurableModule {

    private static final int MAX_MESSAGE_LENGTH = 256;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicLong minutesEnabled = new AtomicLong();
    private volatile Messages messages;

    private record Messages(String join, String greet) {
        static Messages from(ConfigSnapshot config) {
            return new Messages(config.getString("join-message", "<green>Welcome, <player>!</green>"),
                    config.getString("greet-message", "<yellow>Hello <player>!</yellow>"));
        }
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.messages = Messages.from(context.config());
        this.minutesEnabled.set(0);
        PaperModulePlatform paper = context.platform(PaperModulePlatform.class);

        paper.events().listen(PlayerJoinEvent.class,
                event -> event.getPlayer().sendMessage(render(this.messages.join(), event.getPlayer().getName())));

        paper.commands().register(CommandSpec.of("greet")
                        .withDescription("Greets you")
                        .withPermission("example-greeter.use"),
                (CommandSender sender, String label, String[] args) -> {
                    sender.sendMessage(render(this.messages.greet(), sender.getName()));
                    return true;
                });

        paper.scheduler().runTimer(this.minutesEnabled::incrementAndGet, 20L * 60, 20L * 60);
        context.logger().info("Example greeter enabled on " + context.runtime().serverBrand() + " "
                + context.runtime().minecraftVersion());
    }

    @Override
    public void onDisable(ModuleContext context) {
        context.logger().info("Example greeter disabled after " + this.minutesEnabled.get() + " minute(s)");
    }

    @Override
    public List<String> validateConfig(ConfigSnapshot candidate) {
        List<String> problems = new ArrayList<>();
        for (String key : List.of("join-message", "greet-message")) {
            String value = candidate.getString(key, null);
            if (value == null || value.isBlank()) {
                problems.add(key + " must be a non-empty string");
            } else if (value.length() > MAX_MESSAGE_LENGTH) {
                problems.add(key + " is longer than " + MAX_MESSAGE_LENGTH + " characters");
            }
        }
        return problems;
    }

    @Override
    public void onConfigReload(ConfigSnapshot config) {
        this.messages = Messages.from(config);
    }

    /** Player names are inserted as unparsed text, never interpreted as MiniMessage. */
    private Component render(String template, String playerName) {
        return this.miniMessage.deserialize(template, Placeholder.unparsed("player", playerName));
    }
}
