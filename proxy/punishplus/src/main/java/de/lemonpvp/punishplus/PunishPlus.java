package de.lemonpvp.punishplus;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.lemonpvp.punishplus.api.PunishPlusApi;
import de.lemonpvp.punishplus.command.PunishCommand;
import de.lemonpvp.punishplus.command.PunishPlusCommand;
import de.lemonpvp.punishplus.listener.PunishListener;
import de.lemonpvp.punishplus.store.Gruende;
import de.lemonpvp.punishplus.store.PunishStore;
import de.lemonpvp.punishplus.util.Msg;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * /offend (temporaer) und /punish (immer dauerhaft) - direkt am Proxy,
 * damit eine Sperre sofort das GANZE Netzwerk betrifft (gleiches Prinzip
 * wie SMPProxys eigenes /netban), aber mit vorgefertigten Gruenden samt
 * eigener Dauer aus bans.yml statt freier Dauer-Eingabe.
 *
 * Bewusst ein eigenes, neues Plugin statt eine Erweiterung von SMPProxy -
 * bestehende Plugins werden nicht angefasst, nur neue hinzugefuegt. Eine
 * fruehere Fassung lief als Paper-Plugin (mit optionaler MariaDB, um
 * mehrere Server zu verbinden) - hier am Proxy braucht es dafuer gar
 * keine Datenbank: Es gibt nur den einen Proxy-Prozess.
 */
@Plugin(
        id = "punishplus",
        name = "PunishPlus",
        version = "1.0.0",
        description = "/offend (temporaer) und /punish (immer dauerhaft) mit vorgefertigten Gruenden - netzwerkweit",
        authors = {"SMP"}
)
public final class PunishPlus {

    private final ProxyServer proxy;
    private final Logger log;

    private final Config config;
    private final Gruende gruende;
    private final PunishStore store;
    private final PunishManager manager;

    @Inject
    public PunishPlus(ProxyServer proxy, Logger log, @DataDirectory Path folder) {
        this.proxy = proxy;
        this.log = log;
        this.config = new Config(folder, log);
        this.gruende = new Gruende(folder, log);
        this.store = new PunishStore(folder, log);
        this.manager = new PunishManager(this);
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        config.load();
        gruende.load();
        store.load();
        PunishPlusApi.init(manager);

        proxy.getEventManager().register(this, new PunishListener(this));
        registerCommands();

        log.info("PunishPlus aktiviert - Sperren gelten sofort netzwerkweit.");
    }

    private void registerCommands() {
        CommandManager commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder("offend").build(), new PunishCommand(this, false));
        commands.register(commands.metaBuilder("punish").build(), new PunishCommand(this, true));
        commands.register(commands.metaBuilder("punishplus").build(), new PunishPlusCommand(this));
    }

    /** Von /punishplus reload aufgerufen. */
    public void reload() {
        config.load();
        gruende.load();
    }

    public Component message(String key, String... placeholders) {
        return Msg.of(config.message(key), config.prefix(), placeholders);
    }

    /** Nachricht ohne Prefix - fuer Trennbildschirme beim Kick. */
    public Component screen(String key, String... placeholders) {
        return Msg.of(config.message(key), "", placeholders);
    }

    public ProxyServer proxy() {
        return proxy;
    }

    public Config config() {
        return config;
    }

    public Gruende gruende() {
        return gruende;
    }

    public PunishStore store() {
        return store;
    }

    public PunishManager manager() {
        return manager;
    }

    public Logger log() {
        return log;
    }
}
