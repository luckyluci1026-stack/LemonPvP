package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.database.PracticeDatabase;
import com.lemonpvp.lemonpractice.model.Gamemode;
import com.lemonpvp.lemonpractice.model.RankTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Network leaderboard, FoxPvP/FlowPvP style. Tabs along the top row switch
 * between an ELO ranking per gamemode and global stat rankings (kills,
 * killstreak, coins). The top 10 are shown as player heads with medals for
 * the podium. All queries are async; the GUI fills in on the main thread.
 */
public class LeaderboardGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** A selectable ranking. Either an ELO board for a gamemode, or a stat column. */
    private record Category(String label, String icon, Material material, boolean isElo,
                            String gamemode, String statColumn) {}

    private static final int[] TAB_SLOTS    = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] RESULT_SLOTS = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
    private static final int   CLOSE_SLOT   = 49;

    private final LemonPractice plugin;
    private final Player viewer;
    private final List<Category> categories = new ArrayList<>();
    private final Map<Integer, Integer> tabSlotToCategory = new HashMap<>();
    private int selected;
    private Inventory inv;
    private boolean registered;
    private int loadToken;

    public LeaderboardGUI(LemonPractice plugin, Player viewer) {
        this(plugin, viewer, 0);
    }

    public LeaderboardGUI(LemonPractice plugin, Player viewer, int selected) {
        this.plugin = plugin;
        this.viewer = viewer;
        buildCategories();
        this.selected = Math.max(0, Math.min(selected, categories.size() - 1));
    }

    private void buildCategories() {
        for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
            if (!gm.isEnabled()) continue;
            categories.add(new Category(gm.getDisplayName(), "<gradient:#40c4ff:#2962ff>", gm.getMaterial(),
                    true, gm.getId(), null));
            if (categories.size() >= 6) break; // leave room for global stat tabs
        }
        categories.add(new Category("Kills", "<gradient:#ff5252:#b71c1c>", Material.DIAMOND_SWORD, false, null, "kills"));
        categories.add(new Category("Killstreak", "<gradient:#ff9800:#ff5722>", Material.BLAZE_POWDER, false, null, "best_killstreak"));
        categories.add(new Category("Coins", "<gradient:#fff176:#f9a825>", Material.GOLD_INGOT, false, null, "coins"));
    }

    public void open() {
        inv = Bukkit.createInventory(null, 54,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ</bold></gradient>"
                        + " <dark_gray>» <white>" + categories.get(selected).label()));

        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray  = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int s = 0; s < 54; s++) inv.setItem(s, gray);
        for (int s = 9; s <= 17; s++) inv.setItem(s, black);
        for (int s = 45; s <= 53; s++) inv.setItem(s, black);

        // Category tabs
        tabSlotToCategory.clear();
        for (int i = 0; i < categories.size() && i < TAB_SLOTS.length; i++) {
            int slot = TAB_SLOTS[i];
            tabSlotToCategory.put(slot, i);
            inv.setItem(slot, tabItem(categories.get(i), i == selected));
        }
        for (int i = categories.size(); i < TAB_SLOTS.length; i++) inv.setItem(TAB_SLOTS[i], black);

        // Loading placeholders
        for (int s : RESULT_SLOTS) {
            inv.setItem(s, simpleItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    "<!italic><gray>Lädt…", List.of()));
        }

        inv.setItem(CLOSE_SLOT, simpleItem(Material.BARRIER,
                "<!italic><red><bold>Sᴄʜʟɪᴇssᴇɴ", List.of("<!italic><gray>Menü schließen")));

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.4f);

        if (!registered) {
            registered = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        loadResults();
    }

    private void loadResults() {
        Category cat = categories.get(selected);
        final int token = ++loadToken;

        var future = cat.isElo()
                ? plugin.getDatabase().getTopElo(cat.gamemode(), plugin.getEloManager().getPlacementCount(), RESULT_SLOTS.length)
                : plugin.getDatabase().getTopStat(cat.statColumn(), RESULT_SLOTS.length);

        future.thenAccept(entries -> Bukkit.getScheduler().runTask(plugin, () -> {
            // Stale load (player switched tabs / closed) — ignore.
            if (token != loadToken || inv == null || !viewer.isOnline()) return;
            for (int i = 0; i < RESULT_SLOTS.length; i++) {
                if (i < entries.size()) {
                    inv.setItem(RESULT_SLOTS[i], entryHead(i + 1, entries.get(i), cat));
                } else {
                    inv.setItem(RESULT_SLOTS[i], simpleItem(Material.GRAY_STAINED_GLASS_PANE,
                            "<!italic><dark_gray>#" + (i + 1) + " <gray>—",
                            List.of("<!italic><dark_gray>Noch kein Eintrag")));
                }
            }
        }));
    }

    private ItemStack tabItem(Category cat, boolean active) {
        ItemStack item = new ItemStack(cat.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + cat.icon() + "<bold>"
                    + smallCaps(cat.label())));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>" + (cat.isElo() ? "ELO-Rangliste" : "Globale Rangliste")));
            lore.add(Component.empty());
            if (active) {
                lore.add(MM.deserialize("<!italic><green>▶ Ausgewählt"));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MM.deserialize("<!italic><yellow>▶ Klicken zum Anzeigen"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack entryHead(int rank, PracticeDatabase.LeaderEntry entry, Category cat) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.uuid());
            meta.setOwningPlayer(op);

            String medal = switch (rank) {
                case 1 -> "<gradient:#ffd700:#ff8f00>① ";
                case 2 -> "<gradient:#eceff1:#90a4ae>② ";
                case 3 -> "<gradient:#bcaaa4:#6d4c41>③ ";
                default -> "<gray>#" + rank + " ";
            };
            meta.displayName(MM.deserialize("<!italic>" + medal + "<white><bold>" + entry.name()));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (cat.isElo()) {
                RankTier tier = RankTier.fromElo(entry.value());
                lore.add(MM.deserialize("<!italic><gray>ELO: <yellow><bold>" + entry.value()));
                lore.add(MM.deserialize("<!italic><gray>Division: <!italic>" + tier.getDisplay()));
            } else {
                String label = switch (cat.statColumn()) {
                    case "kills" -> "Kills";
                    case "best_killstreak" -> "Beste Killstreak";
                    case "coins" -> "Coins";
                    default -> cat.label();
                };
                lore.add(MM.deserialize("<!italic><gray>" + label + ": <yellow><bold>" + entry.value()));
            }
            lore.add(Component.empty());
            meta.lore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack simpleItem(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) lore.add(MM.deserialize(l));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Maps ASCII letters to their Unicode small-caps equivalent (no texture pack). */
    private static String smallCaps(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            sb.append(switch (c) {
                case 'a' -> "ᴀ"; case 'b' -> "ʙ"; case 'c' -> "ᴄ"; case 'd' -> "ᴅ";
                case 'e' -> "ᴇ"; case 'f' -> "ꜰ"; case 'g' -> "ɢ"; case 'h' -> "ʜ";
                case 'i' -> "ɪ"; case 'j' -> "ᴊ"; case 'k' -> "ᴋ"; case 'l' -> "ʟ";
                case 'm' -> "ᴍ"; case 'n' -> "ɴ"; case 'o' -> "ᴏ"; case 'p' -> "ᴘ";
                case 'q' -> "Q"; case 'r' -> "ʀ"; case 's' -> "s"; case 't' -> "ᴛ";
                case 'u' -> "ᴜ"; case 'v' -> "ᴠ"; case 'w' -> "ᴡ"; case 'x' -> "x";
                case 'y' -> "ʏ"; case 'z' -> "ᴢ";
                default -> String.valueOf(c);
            });
        }
        return sb.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(viewer.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            p.closeInventory();
            return;
        }
        Integer catIdx = tabSlotToCategory.get(slot);
        if (catIdx != null && catIdx != selected) {
            selected = catIdx;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.3f);
            open();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!p.getUniqueId().equals(viewer.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        // Reopen happens via open() which reuses the same inventory object; only
        // unregister when the player genuinely closed (no pending reopen).
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (viewer.getOpenInventory() != null
                    && viewer.getOpenInventory().getTopInventory().equals(inv)) return;
            if (registered) {
                registered = false;
                HandlerList.unregisterAll(this);
            }
        });
    }
}
