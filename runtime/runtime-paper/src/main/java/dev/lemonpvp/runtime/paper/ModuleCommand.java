package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.api.paper.CommandSpec;
import dev.lemonpvp.runtime.api.paper.ModuleCommandHandler;
import dev.lemonpvp.runtime.platform.CallbackGuard;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/** Bukkit command that delegates to a module handler through its callback guard. */
final class ModuleCommand extends Command {

    private final ModuleCommandHandler handler;
    private final CallbackGuard guard;

    ModuleCommand(CommandSpec spec, List<String> aliases, ModuleCommandHandler handler, CallbackGuard guard) {
        super(spec.label(), spec.description(), spec.usage(), aliases);
        this.handler = handler;
        this.guard = guard;
        if (spec.permission() != null) {
            setPermission(spec.permission());
        }
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (!this.guard.isActive()) {
            sender.sendMessage(Component.text("This command is currently unavailable.", NamedTextColor.RED));
            return true;
        }
        boolean ok = this.guard.call("command /" + commandLabel, () -> this.handler.execute(sender, commandLabel, args), Boolean.TRUE);
        if (!ok) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!testPermissionSilent(sender)) {
            return List.of();
        }
        List<String> out = this.guard.call("tab completion /" + alias, () -> this.handler.complete(sender, alias, args), List.of());
        return out == null ? List.of() : out;
    }
}
