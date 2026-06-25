package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.ast.CommandDef;
import com.lemonpvp.lemoncore.lemonlang.ast.TriggerDef;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registers a LemonLang {@link CommandDef} as a live Bukkit {@link Command}
 * via CommandMap reflection. Supports unregistration on reload.
 */
public final class DynamicCommand extends Command {

    private static final Logger LOG = Logger.getLogger("LemonLang");

    private final CommandDef def;
    private final LemonCore plugin;
    private final Interpreter interpreter;
    private final Environment env;

    public DynamicCommand(CommandDef def, LemonCore plugin, Interpreter interpreter, Environment env) {
        super(def.name());
        this.def = def;
        this.plugin = plugin;
        this.interpreter = interpreter;
        this.env = env;

        // Apply props
        Map<String, String> props = def.props();
        String perm = props.get("permission");
        if (perm != null && !perm.isBlank()) {
            setPermission(perm.trim());
        }
        String desc = props.get("description");
        if (desc != null && !desc.isBlank()) {
            setDescription(desc.trim());
        }
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgefuehrt werden.");
            return true;
        }

        // Permission check
        String perm = getPermission();
        if (perm != null && !perm.isBlank() && !player.hasPermission(perm)) {
            net.kyori.adventure.text.Component denied = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<red><!italic>Du hast keine Berechtigung fuer diesen Befehl.");
            player.sendMessage(denied);
            return true;
        }

        // Build context
        Environment childEnv = env.child();
        // Inject arg1, arg2, ... from command args
        for (int i = 0; i < args.length; i++) {
            childEnv.setVar("arg" + (i + 1), args[i]);
        }

        ScriptContext ctx = new ScriptContext(player, def.name(), childEnv, plugin.getLemonLangManager());

        // Find and run "on run" trigger
        for (TriggerDef trigger : def.triggers()) {
            if ("run".equalsIgnoreCase(trigger.event())) {
                interpreter.run(trigger.body(), ctx);
            }
        }

        return true;
    }

    /**
     * Registers this command with Bukkit's CommandMap via reflection.
     * Returns true on success.
     */
    public boolean register(LemonCore plugin) {
        try {
            CommandMap commandMap = getCommandMap(plugin);
            if (commandMap == null) return false;
            commandMap.register(plugin.getName().toLowerCase(), this);
            return true;
        } catch (Exception e) {
            LOG.warning("[LemonLang] Konnte Befehl '" + def.name() + "' nicht registrieren: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregisters this command from the CommandMap. Call on reload/shutdown.
     */
    public void unregister(LemonCore plugin) {
        try {
            CommandMap commandMap = getCommandMap(plugin);
            if (commandMap == null) return;
            // Get known commands map and remove our command
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            knownCommands.remove(def.name().toLowerCase());
            knownCommands.remove(plugin.getName().toLowerCase() + ":" + def.name().toLowerCase());
            this.unregister(commandMap);
        } catch (Exception e) {
            LOG.warning("[LemonLang] Konnte Befehl '" + def.name() + "' nicht deregistrieren: " + e.getMessage());
        }
    }

    private static CommandMap getCommandMap(LemonCore plugin) {
        try {
            Field f = SimplePluginManager.class.getDeclaredField("commandMap");
            f.setAccessible(true);
            return (CommandMap) f.get(plugin.getServer().getPluginManager());
        } catch (Exception e) {
            LOG.warning("[LemonLang] Konnte CommandMap nicht abrufen: " + e.getMessage());
            return null;
        }
    }
}
