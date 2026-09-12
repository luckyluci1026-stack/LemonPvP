package de.lemonpvp.punishplus;

import de.lemonpvp.punishplus.api.PunishPlusApi;
import de.lemonpvp.punishplus.command.PunishCommands;
import de.lemonpvp.punishplus.listener.LoginListener;
import de.lemonpvp.punishplus.store.Gruende;
import de.lemonpvp.punishplus.store.MariaDbPunishRepository;
import de.lemonpvp.punishplus.store.PunishRepository;
import de.lemonpvp.punishplus.store.YamlPunishRepository;
import de.lemonpvp.punishplus.util.Msgs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * /offend (temporaer) und /punish (immer dauerhaft), Gruende samt eigener
 * Dauer aus bans.yml. Eigenstaendig, mit statischer API (PunishPlusApi)
 * fuer andere Plugins.
 *
 * database.enabled in der config.yml entscheidet, ob Sperren nur lokal
 * (gesperrt.yml) oder netzwerkweit ueber eine geteilte MariaDB gelten -
 * siehe PunishRepository.
 */
public final class PunishPlus extends JavaPlugin implements CommandExecutor {

    private Msgs msgs;
    private Gruende gruende;
    private PunishRepository repository;
    private PunishManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.gruende = new Gruende(this);
        gruende.load();
        this.repository = getConfig().getBoolean("database.enabled", false)
                ? new MariaDbPunishRepository(this)
                : new YamlPunishRepository(this);
        repository.load();
        this.manager = new PunishManager(this);
        PunishPlusApi.init(manager);

        getServer().getPluginManager().registerEvents(new LoginListener(this), this);

        PunishCommands commands = new PunishCommands(this);
        getCommand("offend").setExecutor(commands);
        getCommand("offend").setTabCompleter(commands);
        getCommand("punish").setExecutor(commands);
        getCommand("punish").setTabCompleter(commands);
        getCommand("punishplus").setExecutor(this);

        getLogger().info("PunishPlus aktiviert.");
    }

    @Override
    public void onDisable() {
        if (repository != null) {
            repository.shutdown();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        reloadConfig();
        gruende.load();
        msgs.send(sender, "reloaded");
        return true;
    }

    public Msgs msgs() {
        return msgs;
    }

    public Gruende gruende() {
        return gruende;
    }

    public PunishRepository repository() {
        return repository;
    }

    public PunishManager manager() {
        return manager;
    }
}
