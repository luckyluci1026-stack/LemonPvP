package de.lemonpvp.fastshop;

import de.lemonpvp.fastshop.auktion.AuktionsCommand;
import de.lemonpvp.fastshop.auktion.AuktionsDialoge;
import de.lemonpvp.fastshop.auktion.AuktionsHaus;
import de.lemonpvp.fastshop.auktion.AuktionsListener;
import de.lemonpvp.fastshop.auktion.AuktionsMenus;
import de.lemonpvp.fastshop.command.FastShopCommand;
import de.lemonpvp.fastshop.command.SellCommand;
import de.lemonpvp.fastshop.command.ShopCommand;
import de.lemonpvp.fastshop.command.WorthCommand;
import de.lemonpvp.fastshop.economy.EconomyHook;
import de.lemonpvp.fastshop.gui.ShopListener;
import de.lemonpvp.fastshop.gui.ShopMenus;
import de.lemonpvp.fastshop.shop.ShopConfig;
import de.lemonpvp.fastshop.shop.ShopService;
import de.lemonpvp.fastshop.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FastShop - einfaches, customizables /shop und /sell auf Basis der
 * EssentialsX-/Vault-Economy.
 */
public final class FastShop extends JavaPlugin {

    private Msgs msgs;
    private EconomyHook economy;
    private ShopConfig shop;
    private ShopService service;
    private ShopMenus menus;
    private AuktionsHaus auktionen;
    private AuktionsMenus auktionsMenus;
    private AuktionsDialoge auktionsDialoge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (AuktionsHaus.alteGrenzeAnheben(getConfig())) {
            saveConfig();
            getLogger().info("Auktionshaus: max-angebote vom alten Standard 10 auf 50 angehoben.");
        }
        this.msgs = new Msgs(this);
        this.economy = new EconomyHook();
        this.shop = new ShopConfig(this);
        if (shop.katalogUmgestellt() && getConfig().getInt("settings.menu-rows", 5) == 5) {
            getConfig().set("settings.menu-rows", 3);
            saveConfig();
            getLogger().info("Shop-Hauptmenü auf 3 Reihen verkleinert (wie auf DonutSMP).");
        }
        this.service = new ShopService(this);
        this.menus = new ShopMenus(this);
        this.auktionen = new AuktionsHaus(this);
        this.auktionsMenus = new AuktionsMenus(this);
        this.auktionsDialoge = new AuktionsDialoge(this);

        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AuktionsListener(this), this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("worth").setExecutor(new WorthCommand(this));
        getCommand("fastshop").setExecutor(new FastShopCommand(this));
        AuktionsCommand auktionsCommand = new AuktionsCommand(this);
        getCommand("ah").setExecutor(auktionsCommand);
        getCommand("ah").setTabCompleter(auktionsCommand);

        if (!economy.isEnabled()) {
            getLogger().warning("Kein Vault-Economy gefunden - /shop und /sell brauchen Vault + EssentialsX.");
        }
        getLogger().info("FastShop aktiviert (" + shop.categories().size() + " Kategorien).");
    }

    @Override
    public void onDisable() {
        if (auktionen != null) {
            auktionen.speichernSofort();
        }
    }

    public Msgs msgs() {
        return msgs;
    }


    public EconomyHook economy() {
        return economy;
    }

    public ShopConfig shop() {
        return shop;
    }

    public ShopService service() {
        return service;
    }

    public ShopMenus menus() {
        return menus;
    }

    public AuktionsHaus auktionen() {
        return auktionen;
    }

    public AuktionsMenus auktionsMenus() {
        return auktionsMenus;
    }

    public AuktionsDialoge auktionsDialoge() {
        return auktionsDialoge;
    }
}
