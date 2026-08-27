package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard for {@code /aowcode}: build a reward code without typing arguments.
 * Cycle the reward type, value, max-uses and duration with clicks (the value
 * for rank / kill-effect is typed in chat), then Create — a random 26-char code
 * is generated and handed back as a clickable chip.
 */
public class AowCodeGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String[] TYPES = {"coins", "rank", "killeffect"};
    private static final long[] COIN_PRESETS = {100, 500, 1000, 5000, 10000, 50000};
    private static final int[] USE_PRESETS = {1, 5, 10, 25, 100, 0}; // 0 = unlimited
    private static final int[] DURATION_DAYS = {1, 7, 30, 0};        // 0 = permanent

    private static final int TYPE_SLOT = 10, VALUE_SLOT = 12, USES_SLOT = 14, DURATION_SLOT = 16, CREATE_SLOT = 22;

    private final LemonCore plugin;
    private final Player admin;

    private int typeIdx = 0;
    private String value = "100";     // sensible default for coins
    private int coinIdx = 0;
    private int usesIdx = 0;
    private int durationIdx = 1;      // 7d default

    private Inventory inv;
    private boolean registered;

    public AowCodeGUI(LemonCore plugin, Player admin) {
        this.plugin = plugin;
        this.admin = admin;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27, MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>Create Code</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        admin.playSound(admin.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        admin.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        String type = TYPES[typeIdx];
        inv.setItem(TYPE_SLOT, named(
                type.equals("coins") ? Material.GOLD_NUGGET : type.equals("rank") ? Material.NAME_TAG : Material.BLAZE_POWDER,
                "<yellow><bold>Reward Type", List.of(
                        "<gray>Current: <white>" + type,
                        "",
                        "<green>► Click to cycle (coins / rank / killeffect)")));

        inv.setItem(VALUE_SLOT, named(Material.PAPER, "<aqua><bold>Value", List.of(
                "<gray>Current: <white>" + value,
                "",
                type.equals("coins")
                        ? "<green>► Click to cycle coin amounts"
                        : "<green>► Click to type the " + type + " value in chat")));

        String uses = USE_PRESETS[usesIdx] == 0 ? "Unlimited" : String.valueOf(USE_PRESETS[usesIdx]);
        inv.setItem(USES_SLOT, named(Material.REPEATER, "<gold><bold>Max Uses", List.of(
                "<gray>Current: <white>" + uses, "", "<green>► Click to cycle")));

        String dur = DURATION_DAYS[durationIdx] == 0 ? "Permanent" : DURATION_DAYS[durationIdx] + " days";
        inv.setItem(DURATION_SLOT, named(Material.CLOCK, "<light_purple><bold>Duration", List.of(
                "<gray>Current: <white>" + dur, "", "<green>► Click to cycle")));

        inv.setItem(CREATE_SLOT, named(Material.LIME_DYE, "<green><bold>✔ Create Code", List.of(
                "<gray>Type: <white>" + type,
                "<gray>Value: <white>" + value,
                "<gray>Uses: <white>" + uses + " <dark_gray>| <gray>Duration: <white>" + dur,
                "", "<green>► Click to create")));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(admin.getUniqueId())) return;
        e.setCancelled(true);

        switch (e.getRawSlot()) {
            case TYPE_SLOT -> {
                typeIdx = (typeIdx + 1) % TYPES.length;
                if (TYPES[typeIdx].equals("coins")) value = String.valueOf(COIN_PRESETS[coinIdx]);
                else value = "…"; // needs chat input
                click(p); render();
            }
            case VALUE_SLOT -> {
                if (TYPES[typeIdx].equals("coins")) {
                    coinIdx = (coinIdx + 1) % COIN_PRESETS.length;
                    value = String.valueOf(COIN_PRESETS[coinIdx]);
                    click(p); render();
                } else {
                    promptValue(p);
                }
            }
            case USES_SLOT -> { usesIdx = (usesIdx + 1) % USE_PRESETS.length; click(p); render(); }
            case DURATION_SLOT -> { durationIdx = (durationIdx + 1) % DURATION_DAYS.length; click(p); render(); }
            case CREATE_SLOT -> create(p);
            default -> { /* border */ }
        }
    }

    private void promptValue(Player p) {
        unregister();
        p.closeInventory();
        p.sendMessage(MM.deserialize("<!italic><yellow>Type the " + TYPES[typeIdx]
                + " value in chat <gray>(e.g. a rank group or kill-effect id), or <white>cancel<gray>."));
        plugin.getChatInputManager().await(p, input -> {
            if (input != null && !input.isBlank()) value = input;
            open(); // reopen with the value set
        });
    }

    private void create(Player p) {
        String type = TYPES[typeIdx];
        if (!type.equals("coins") && (value == null || value.isBlank() || value.equals("…"))) {
            msg(p, "<red>Set a value first — click <white>Value<red> and type it in chat.");
            return;
        }
        int maxUses = USE_PRESETS[usesIdx];
        long durationSecs = DURATION_DAYS[durationIdx] * 86_400L;
        unregister();
        p.closeInventory();
        plugin.getCodeManager().createCode("random26", type, value, maxUses, durationSecs, p.getUniqueId())
                .thenAccept(code -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player pl = Bukkit.getPlayer(p.getUniqueId());
                    if (pl == null) return;
                    if (code == null) { msg(pl, "<red>Failed to create the code — check the console."); return; }
                    pl.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Code created!</bold></gradient> "
                            + "<click:copy_to_clipboard:'" + code + "'>"
                            + "<hover:show_text:'<green>Click to copy'><yellow><u>" + code + "</u></yellow></hover></click>"));
                    pl.sendMessage(MM.deserialize("<!italic><gray>Reward: <white>" + type + " " + value
                            + " <dark_gray>| <gray>Uses: <white>" + (maxUses == 0 ? "∞" : maxUses)
                            + " <dark_gray>| <gray>Redeem with <white>/code " + code));
                    pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
                }));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(admin.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }
    private void click(Player p) { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f); }
    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            if (!lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
}
