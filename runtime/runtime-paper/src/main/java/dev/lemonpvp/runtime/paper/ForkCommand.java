package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.commands.AdminCommand;
import dev.lemonpvp.runtime.commands.CommandActor;
import dev.lemonpvp.runtime.commands.MessageLevel;
import dev.lemonpvp.runtime.commands.RuntimePermissions;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * {@code /fork} command. Translates the Bukkit sender into a {@link CommandActor}; all
 * logic lives in the platform-neutral {@link AdminCommand}.
 *
 * <p>Output uses fixed MiniMessage templates. Dynamic text (module ids, file names,
 * error messages) is inserted with {@link Placeholder#unparsed(String, String)} and is
 * therefore never interpreted as markup.</p>
 */
final class ForkCommand extends Command {

    static final String LABEL = "fork";
    static final String NAMESPACE = "lemon";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final AdminCommand admin;

    ForkCommand(AdminCommand admin) {
        super(LABEL, "Lemon hot-deployment runtime administration",
                "/fork <info|version|status|reload|module>", List.of());
        this.admin = admin;
        setPermission(RuntimePermissions.BASE);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        this.admin.execute(new SenderActor(sender), args);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return this.admin.complete(new SenderActor(sender), args);
    }

    /** Adapts a Bukkit command sender. */
    record SenderActor(CommandSender sender) implements CommandActor {

        @Override
        public String name() {
            return this.sender instanceof ConsoleCommandSender ? "console" : this.sender.getName();
        }

        @Override
        public boolean hasPermission(String node) {
            return this.sender.hasPermission(node);
        }

        @Override
        public void send(MessageLevel level, String text) {
            String template = switch (level) {
                case HEADER -> "<gold><bold><text></bold></gold>";
                case INFO -> "<gray><text></gray>";
                case DETAIL -> "<dark_gray>  <text></dark_gray>";
                case SUCCESS -> "<green><text></green>";
                case WARNING -> "<yellow><text></yellow>";
                case ERROR -> "<red><text></red>";
            };
            this.sender.sendMessage(MINI_MESSAGE.deserialize(template, Placeholder.unparsed("text", text)));
        }
    }
}
