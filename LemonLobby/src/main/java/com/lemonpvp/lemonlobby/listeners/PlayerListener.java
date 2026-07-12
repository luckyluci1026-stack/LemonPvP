package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.gui.TrainingGUI;
import com.lemonpvp.lemonlobby.managers.HotbarManager;
import com.lemonpvp.lemonlobby.messaging.LobbyMessaging;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final LemonLobby plugin;
    private final HotbarManager hotbarManager;
    private final LobbyMessaging lobbyMessaging;
    private final TrainingGUI trainingGUI;
    /** Boost-rocket cooldowns (uuid -> last-use epoch ms). */
    private final java.util.Map<java.util.UUID, Long> boostCooldown =
            new java.util.concurrent.ConcurrentHashMap<>();

    public PlayerListener(LemonLobby plugin, HotbarManager hotbarManager,
                          LobbyMessaging lobbyMessaging, TrainingGUI trainingGUI) {
        this.plugin = plugin;
        this.hotbarManager = hotbarManager;
        this.lobbyMessaging = lobbyMessaging;
        this.trainingGUI = trainingGUI;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        hotbarManager.giveHotbar(player);
        plugin.getGoldenHourManager().show(player);
        plugin.getTreeUpgradeManager().load(player.getUniqueId());

        // Daily reward: load state, then notify if claimable.
        java.util.UUID dailyUuid = player.getUniqueId();
        plugin.getDailyRewardManager().load(dailyUuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(dailyUuid);
            if (p == null || !p.isOnline()) return;
            if (plugin.getDailyRewardManager().canClaim(dailyUuid)) {
                p.sendMessage(MINI_MESSAGE.deserialize(
                        "<gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray> "
                        + "<gold>Your daily reward is ready! "
                        + "<click:run_command:'/daily'><hover:show_text:'<green>Click to open'>"
                        + "<yellow><underlined>/daily</underlined></hover></click>"));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
        }, 60L);
        plugin.getBoosterManager().load(player.getUniqueId())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getBoosterManager().showBoosterBar(player)));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        hotbarManager.removeHotbar(player);
        plugin.getTreeUpgradeManager().unload(player.getUniqueId());
        plugin.getBoosterManager().unload(player.getUniqueId());
        plugin.getDailyRewardManager().unload(player.getUniqueId());
        plugin.getAppleTreeListener().cleanupPlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDropItem(PlayerDropItemEvent event) {
        // Creative players (builders/admins) may manage items freely.
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        // Block all item drops in the lobby
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Delegate to TrainingGUI first
        if (trainingGUI.handleClick(event)) return;

        // Creative players (builders/admins) may rearrange, remove and add items.
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        // Cancel all clicks in the player's own inventory — prevents moving any items
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        InventoryType type = event.getInventory().getType();

        // Allow all custom chest-based GUIs (shop, cosmetics, training, etc.)
        if (type == InventoryType.CHEST) return;

        // Block player inventory and crafting to prevent item manipulation
        if (type == InventoryType.CRAFTING || type == InventoryType.PLAYER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // Only handle right-click actions
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Defer to the apple tree: right-clicking a tree block should harvest it,
        // not fire the held hotbar item's action (e.g. the Practice queue compass).
        if (action == Action.RIGHT_CLICK_BLOCK
                && plugin.getAppleTreeListener().isTreeBlock(event.getClickedBlock())) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !hotbarManager.isHotbarItem(item)) return;

        event.setCancelled(true);

        int slot = player.getInventory().getHeldItemSlot();

        switch (slot) {
            case 0 -> {
                // Queue sword: open the gamemode picker — choosing a mode writes
                // the pending-queue marker and sends the player to duels, where
                // LemonPractice auto-queues them on join.
                new com.lemonpvp.lemonlobby.gui.QueueSelectGUI(plugin, player).open();
            }
            case 1 -> {
                // Training Compass - open Training GUI
                trainingGUI.open(player);
            }
            case 2 -> {
                // Boost rocket: launch up + forward with a short cooldown.
                long now = System.currentTimeMillis();
                Long last = boostCooldown.get(player.getUniqueId());
                if (last != null && now - last < 5000L) {
                    double remaining = (5000L - (now - last)) / 1000.0;
                    player.sendActionBar(MINI_MESSAGE.deserialize("<!italic><gray>Boost ready in <yellow>"
                            + String.format(java.util.Locale.US, "%.1f", remaining) + "s"));
                    return;
                }
                boostCooldown.put(player.getUniqueId(), now);
                org.bukkit.util.Vector dir = player.getLocation().getDirection().normalize()
                        .multiply(1.3).setY(1.0);
                player.setVelocity(dir);
                player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK,
                        player.getLocation(), 30, 0.3, 0.2, 0.3, 0.08);
                player.getWorld().playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.1f);
            }
            case 3 -> {
                // Events server
                lobbyMessaging.connectToServer(player,
                        plugin.getServersConfig().getString("servers.events.name", "events"));
            }
            case 4 -> {
                // Kit editor: opens RIGHT HERE in the lobby when LemonPractice is
                // installed alongside (its /kiteditor command); only players on a
                // lobby without it get sent to the duels server as a fallback.
                if (Bukkit.getPluginManager().isPluginEnabled("LemonPractice")) {
                    player.performCommand("kiteditor");
                } else {
                    lobbyMessaging.connectToServer(player,
                            plugin.getServersConfig().getString("servers.duels.name", "duels"));
                }
            }
            case 6 -> {
                // Cosmetics
                org.bukkit.plugin.Plugin cosmeticsPlugin = Bukkit.getPluginManager().getPlugin("LemonCosmetics");
                if (cosmeticsPlugin instanceof com.lemonpvp.lemoncosmetics.LemonCosmetics lemonCosmetics) {
                    new com.lemonpvp.lemoncosmetics.gui.CosmeticsMainGUI(lemonCosmetics, player).open();
                } else {
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Cosmetics is not available right now."));
                }
            }
            case 7 -> {
                // Settings → open settings GUI
                org.bukkit.plugin.Plugin settingsPlugin = Bukkit.getPluginManager().getPlugin("LemonCore");
                if (settingsPlugin instanceof com.lemonpvp.lemoncore.LemonCore lemonCore) {
                    new com.lemonpvp.lemoncore.gui.SettingsGUI(lemonCore, player).open();
                } else {
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Settings are not available right now."));
                }
            }
            case 8 -> {
                // Leave - connect to hub
                lobbyMessaging.connectToServer(player, plugin.getServersConfig().getString("servers.lobby.name", "lobby"));
            }
            default -> {
                // Not a handled slot
            }
        }
    }
}
