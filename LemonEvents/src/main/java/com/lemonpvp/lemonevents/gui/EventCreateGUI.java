package com.lemonpvp.lemonevents.gui;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.EventType;
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
 * Wizard for creating a scripted {@code GameEvent} without the command: cycle
 * the discipline, type a name in chat, cycle the three prize tiers, then create.
 * The discipline maps to an {@link EventType} plus, for the PvP/Horror variants,
 * the keyword the backend routes on — appended to the name automatically so the
 * right game class is picked.
 */
public class EventCreateGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** A pickable discipline: label, its EventType and the routing keyword (or null). */
    private record Discipline(String label, EventType type, String keyword, Material icon) {}

    private static final Discipline[] DISCIPLINES = {
            new Discipline("FFA", EventType.PvP, "FFA", Material.DIAMOND_SWORD),
            new Discipline("Sumo", EventType.PvP, "SUMO", Material.SLIME_BALL),
            new Discipline("TeamFight", EventType.PvP, "TEAMFIGHT", Material.SHIELD),
            new Discipline("PvP Tournament", EventType.PvP, "TOURNAMENT", Material.GOLDEN_SWORD),
            new Discipline("LemonRoyale", EventType.LemonRoyale, null, Material.ELYTRA),
            new Discipline("HungerGames", EventType.HungerGames, null, Material.CHEST),
            new Discipline("Cinema", EventType.Cinema, null, Material.PAINTING),
            new Discipline("Escape", EventType.Horror, "ESCAPE", Material.IRON_DOOR),
            new Discipline("Mafia", EventType.Horror, "MAFIA", Material.CROSSBOW),
            new Discipline("Hunt", EventType.Horror, "HUNT", Material.BOW),
    };

    private static final int[] PRIZE_PRESETS = {0, 100, 250, 500, 1000, 2500, 5000, 10000};

    private static final int DISCIPLINE_SLOT = 10, NAME_SLOT = 12,
            PRIZE1_SLOT = 14, PRIZE2_SLOT = 15, PRIZE3_SLOT = 16, CREATE_SLOT = 22;

    private final LemonEvents plugin;
    private final Player owner;

    private int disciplineIdx = 0;
    private String name;
    private int prize1Idx = 4; // 1000
    private int prize2Idx = 3; // 500
    private int prize3Idx = 2; // 250

    private Inventory inv;
    private boolean registered;

    public EventCreateGUI(LemonEvents plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27, MM.deserialize(
                "<!italic><gradient:#fffb00:#ffa751><bold>Create Event</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        owner.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        Discipline d = DISCIPLINES[disciplineIdx];
        inv.setItem(DISCIPLINE_SLOT, named(d.icon(), "<yellow><bold>Discipline", List.of(
                "<gray>Current: <white>" + d.label(),
                "<dark_gray>Type: " + d.type(),
                "", "<green>► Click to cycle")));

        inv.setItem(NAME_SLOT, named(Material.NAME_TAG, "<aqua><bold>Name", List.of(
                "<gray>Current: <white>" + (name == null ? "<dark_gray>not set" : name),
                "", "<green>► Click to type it in chat")));

        inv.setItem(PRIZE1_SLOT, prizeItem("1st", PRIZE_PRESETS[prize1Idx]));
        inv.setItem(PRIZE2_SLOT, prizeItem("2nd", PRIZE_PRESETS[prize2Idx]));
        inv.setItem(PRIZE3_SLOT, prizeItem("3rd", PRIZE_PRESETS[prize3Idx]));

        inv.setItem(CREATE_SLOT, named(Material.LIME_DYE, "<green><bold>✔ Create Event", List.of(
                "<gray>Discipline: <white>" + d.label(),
                "<gray>Name: <white>" + (name == null ? "<dark_gray>not set" : name),
                "<gray>Prizes: <white>" + PRIZE_PRESETS[prize1Idx] + "/" + PRIZE_PRESETS[prize2Idx] + "/" + PRIZE_PRESETS[prize3Idx],
                "", "<green>► Click to create")));
    }

    private ItemStack prizeItem(String place, int amount) {
        return named(Material.GOLD_NUGGET, "<gold><bold>" + place + " Prize", List.of(
                "<gray>Coins: <white>" + amount, "", "<green>► Click to cycle"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(owner.getUniqueId())) return;
        e.setCancelled(true);
        switch (e.getRawSlot()) {
            case DISCIPLINE_SLOT -> { disciplineIdx = (disciplineIdx + 1) % DISCIPLINES.length; click(p); render(); }
            case PRIZE1_SLOT -> { prize1Idx = (prize1Idx + 1) % PRIZE_PRESETS.length; click(p); render(); }
            case PRIZE2_SLOT -> { prize2Idx = (prize2Idx + 1) % PRIZE_PRESETS.length; click(p); render(); }
            case PRIZE3_SLOT -> { prize3Idx = (prize3Idx + 1) % PRIZE_PRESETS.length; click(p); render(); }
            case NAME_SLOT -> promptName(p);
            case CREATE_SLOT -> create(p);
            default -> { /* border */ }
        }
    }

    private void promptName(Player p) {
        unregister();
        p.closeInventory();
        p.sendMessage(MM.deserialize("<!italic><yellow>Type the event name in chat <gray>(or <white>cancel<gray>)."));
        plugin.getChatInputManager().await(p, input -> {
            if (input != null && !input.isBlank()) name = input;
            open();
        });
    }

    private void create(Player p) {
        if (name == null || name.isBlank()) { msg(p, "<red>Set a name first."); return; }
        Discipline d = DISCIPLINES[disciplineIdx];
        // Ensure the routing keyword is present so the correct game class is built.
        String finalName = name;
        if (d.keyword() != null && !finalName.toUpperCase().contains(d.keyword())) {
            finalName = name + " (" + d.label() + ")";
        }
        int prize1 = PRIZE_PRESETS[prize1Idx], prize2 = PRIZE_PRESETS[prize2Idx], prize3 = PRIZE_PRESETS[prize3Idx];
        unregister();
        p.closeInventory();
        plugin.getEventManager().createEvent(finalName, d.type(), prize3, prize2, prize1)
                .thenAccept(ev -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player pl = Bukkit.getPlayer(p.getUniqueId());
                    if (pl == null) return;
                    if (ev == null) { msg(pl, "<red>Failed to create the event (name already taken?)."); return; }
                    msg(pl, "<gradient:#fffb00:#00ff00><bold>Event created:</bold></gradient> <white>" + ev.getName());
                    msg(pl, "<gray>Start it with <white>/aowstartevent " + ev.getName()
                            + " <gray>once players have joined.");
                    pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
                }));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(owner.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }
    private void click(Player p) { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f); }
    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

    private ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + name));
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
