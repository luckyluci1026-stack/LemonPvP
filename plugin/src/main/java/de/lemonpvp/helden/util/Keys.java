package de.lemonpvp.helden.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Zentrale NamespacedKeys fuer den PersistentDataContainer. */
public final class Keys {

    private static NamespacedKey itemId;
    private static NamespacedKey menuAction;
    private static NamespacedKey dummyOwner;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        itemId = new NamespacedKey(plugin, "item_id");
        menuAction = new NamespacedKey(plugin, "menu_action");
        dummyOwner = new NamespacedKey(plugin, "dummy_owner");
    }

    /** ID des Custom-Items aus items.yml. */
    public static NamespacedKey itemId() {
        return itemId;
    }

    /** Aktion, die ein Menue-Icon ausloest. */
    public static NamespacedKey menuAction() {
        return menuAction;
    }

    /** Besitzer eines Combat-Log-Dummys, damit er auch nach einem Absturz erkennbar bleibt. */
    public static NamespacedKey dummyOwner() {
        return dummyOwner;
    }
}
