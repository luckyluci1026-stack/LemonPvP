package de.lemonpvp.bettersmp.gui;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Schoenes /settings-GUI: Admins schalten die BetterSMP-Module per Klick
 * an/aus. Aenderungen werden sofort in config.yml gespeichert und live wirksam.
 */
public final class SettingsGUI implements InventoryHolder {

    /** Ein umschaltbarer Konfigurations-Eintrag. */
    public record Toggle(int slot, String path, Material icon, String title, String description) {
    }

    /** Aktions-Buttons (kein Toggle). */
    public enum Action {
        RELOAD, INSTALL, RANKS, CLOSE
    }

    public static final List<Toggle> TOGGLES = List.of(
            new Toggle(10, "chat.enabled", Material.WRITABLE_BOOK,
                    "<#FFD75A>Chat-Formatierung", "LPC-MiniMessage-Chat mit Prefixen"),
            new Toggle(11, "chat.no-chat-reports", Material.SHIELD,
                    "<#FFD75A>NoChatReports", "Chat als System-Nachricht (nicht meldbar)"),
            new Toggle(12, "chat.mentions.enabled", Material.NAME_TAG,
                    "<#FFD75A>Erwaehnungen (@Name)", "Ping mit Sound bei @Spielername"),
            new Toggle(13, "chat.clickable-links", Material.COMPASS,
                    "<#FFD75A>Klickbare Links", "URLs im Chat anklickbar machen"),
            new Toggle(14, "combat.enabled", Material.DIAMOND_SWORD,
                    "<#FFD75A>AntiCombatLog", "Kampf-Tag + Bestrafung bei Combat-Log"),
            new Toggle(19, "combat.block-elytra", Material.ELYTRA,
                    "<#FFD75A>Elytra im Kampf sperren", "Kein Elytra-Start waehrend des Kampfes"),
            new Toggle(20, "combat.actionbar", Material.CLOCK,
                    "<#FFD75A>Kampf-Actionbar", "Countdown-Anzeige waehrend des Kampfes"),
            new Toggle(21, "join-quit.enabled", Material.OAK_SIGN,
                    "<#FFD75A>Join/Quit-Nachrichten", "Schoene Beitritts-/Verlassen-Meldungen"),
            new Toggle(22, "join-quit.motd.enabled", Material.FILLED_MAP,
                    "<#FFD75A>MOTD beim Join", "Begruessungstext nach dem Beitreten"),
            new Toggle(23, "join-quit.first-join-title.enabled", Material.NETHER_STAR,
                    "<#FFD75A>Erst-Join-Titel", "Grosser Willkommens-Titel fuer Neue"),
            new Toggle(24, "installer.enabled", Material.HOPPER,
                    "<#FFD75A>Auto-Installer", "Begleit-Plugins beim Start nachladen")
    );

    private final BetterSMP plugin;
    private final Inventory inventory;

    public SettingsGUI(BetterSMP plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54,
                Text.mm("<gradient:#FFD75A:#FFB02E><bold>BetterSMP</bold></gradient> <dark_gray>» <gray>Einstellungen"));
        render();
    }

    public void render() {
        ItemStack border = GuiItems.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border);
            } else {
                inventory.setItem(i, null);
            }
        }

        for (Toggle toggle : TOGGLES) {
            boolean on = plugin.getConfig().getBoolean(toggle.path(), true);
            List<String> lore = List.of(
                    "<gray>" + toggle.description(),
                    "",
                    on ? "<green>▶ Aktiviert" : "<red>▶ Deaktiviert",
                    "<dark_gray>Config: <gray>" + toggle.path(),
                    "",
                    "<yellow>Klick <gray>zum Umschalten");
            inventory.setItem(toggle.slot(),
                    GuiItems.item(on ? toggle.icon() : Material.GRAY_DYE, toggle.title(), lore, on));
        }

        int inCombat = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.combat().isTagged(online.getUniqueId())) {
                inCombat++;
            }
        }
        inventory.setItem(4, GuiItems.item(Material.NETHER_STAR,
                "<gradient:#FFD75A:#FFB02E><bold>BetterSMP</bold></gradient>",
                List.of("<gray>Version <white>" + plugin.getPluginMeta().getVersion(),
                        "<gray>Spieler im Kampf: <white>" + inCombat,
                        "",
                        "<gray>Schalte die Module unten per Klick um."), true));

        inventory.setItem(48, GuiItems.item(Material.LIME_CONCRETE,
                "<green><bold>Config neu laden", List.of("<gray>Liest config.yml & messages.yml neu ein"), false));
        inventory.setItem(49, GuiItems.item(Material.ENDER_CHEST,
                "<#FFD75A><bold>Begleit-Plugins installieren",
                List.of("<gray>Laedt fehlende Plugins von den",
                        "<gray>offiziellen Quellen (Neustart noetig)"), false));
        inventory.setItem(50, GuiItems.item(Material.WRITTEN_BOOK,
                "<#FFD75A><bold>Standard-Raenge anlegen",
                List.of("<gray>Erstellt LuckPerms-Raenge",
                        "<gray>(default, vip, mod, admin, owner)"), false));
        inventory.setItem(53, GuiItems.item(Material.BARRIER,
                "<red><bold>Schliessen", List.of("<gray>GUI schliessen"), false));
    }

    /** Liefert das Toggle fuer einen Slot oder null. */
    public Toggle toggleAt(int slot) {
        for (Toggle toggle : TOGGLES) {
            if (toggle.slot() == slot) {
                return toggle;
            }
        }
        return null;
    }

    /** Liefert die Aktion fuer einen Slot oder null. */
    public Action actionAt(int slot) {
        return switch (slot) {
            case 48 -> Action.RELOAD;
            case 49 -> Action.INSTALL;
            case 50 -> Action.RANKS;
            case 53 -> Action.CLOSE;
            default -> null;
        };
    }

    public Component title() {
        return Text.mm("<gray>BetterSMP-Einstellungen");
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
