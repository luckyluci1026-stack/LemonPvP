package de.lemonpvp.fastshop;

import de.lemonpvp.fastshop.command.FastShopCommand;
import de.lemonpvp.fastshop.command.SellCommand;
import de.lemonpvp.fastshop.command.ShopCommand;
import de.lemonpvp.fastshop.command.WorthCommand;
import de.lemonpvp.fastshop.economy.EconomyHook;
import de.lemonpvp.fastshop.gui.ShopListener;
import de.lemonpvp.fastshop.gui.ShopMenus;
import de.lemonpvp.fastshop.shop.ShopConfig;
import de.lemonpvp.fastshop.shop.ShopService;
import de.lemonpvp.fastshop.util.Glyphs;
import de.lemonpvp.fastshop.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FastShop - einfaches, customizables /shop und /sell auf Basis der
 * EssentialsX-/Vault-Economy.
 */
public final class FastShop extends JavaPlugin {

    private Msgs msgs;
    private Glyphs glyphs;
    private EconomyHook economy;
    private ShopConfig shop;
    private ShopService service;
    private ShopMenus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.glyphs = new Glyphs(this);
        msgs.setGlyphs(glyphs);
        this.economy = new EconomyHook();
        this.shop = new ShopConfig(this);
        this.service = new ShopService(this);
        this.menus = new ShopMenus(this);

        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("worth").setExecutor(new WorthCommand(this));
        getCommand("fastshop").setExecutor(new FastShopCommand(this));

        if (!economy.isEnabled()) {
            getLogger().warning("Kein Vault-Economy gefunden - /shop und /sell brauchen Vault + EssentialsX.");
        }
        getLogger().info("FastShop aktiviert (" + shop.categories().size() + " Kategorien).");
    }

    public Msgs msgs() {
        return msgs;
    }

    public Glyphs glyphs() {
        return glyphs;
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
}
