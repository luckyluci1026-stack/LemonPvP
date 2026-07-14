package com.lemonpvp.lemonevents.managers;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.HostedEvent;
import com.lemonpvp.lemonevents.util.WinAnimation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs simple host-driven events: someone with the host permission builds a kit,
 * broadcasts, players join, everyone is teleported into a vanilla world with the
 * kit, and the last player standing wins with the {@link WinAnimation}. Exactly
 * one hosted event runs at a time. Kept deliberately separate from the scripted
 * {@code AbstractGame} events so content creators can spin one up in seconds.
 */
public class HostedEventManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** The host's inventory/gamemode before kit-building, restored afterwards. */
    private record HostSnapshot(ItemStack[] storage, ItemStack[] armor, ItemStack offhand, GameMode mode) {}

    private final LemonEvents plugin;
    private HostedEvent active;
    private HostSnapshot hostSnapshot;

    public HostedEventManager(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public HostedEvent getActive() { return active; }

    /** Returns everyone home on shutdown so nobody is stranded in creative/spectator. */
    public void shutdown() {
        if (active == null) return;
        if (active.getState() != HostedEvent.State.SETUP) returnEveryone();
        active = null;
        hostSnapshot = null;
    }

    // -- Lifecycle -----------------------------------------------------------

    /** Starts hosting: the host enters kit-build mode (creative, empty inventory). */
    public boolean startHosting(Player host, String name) {
        if (active != null) {
            msg(host, "<red>An event is already running. Use <white>/host cancel<red> first.");
            return false;
        }
        active = new HostedEvent(host.getUniqueId(), host.getName(), name);
        hostSnapshot = new HostSnapshot(
                host.getInventory().getStorageContents().clone(),
                host.getInventory().getArmorContents().clone(),
                host.getInventory().getItemInOffHand().clone(),
                host.getGameMode());
        host.getInventory().clear();
        host.setGameMode(GameMode.CREATIVE);
        msg(host, "<gradient:#fffb00:#00ff00><bold>Kit builder</bold></gradient> <dark_gray>» "
                + "<gray>Place the items every player should get, then run <white>/host open<gray>.");
        return true;
    }

    /** Finishes the kit, restores the host, and opens joins with a broadcast. */
    public boolean openJoins(Player host) {
        if (notHostOf(host, HostedEvent.State.SETUP)) return false;

        active.setKit(
                host.getInventory().getStorageContents().clone(),
                host.getInventory().getArmorContents().clone(),
                host.getInventory().getItemInOffHand().clone());
        restoreHost(host);
        active.setState(HostedEvent.State.OPEN);
        active.getParticipants().add(host.getUniqueId()); // the host plays too

        broadcastJoin();
        msg(host, "<green>Event <white>" + active.getName() + " <green>is open! Players can join. "
                + "Run <white>/host begin <green>when ready.");
        return true;
    }

    /** A player joins the open event. */
    public boolean join(Player player) {
        if (active == null || active.getState() != HostedEvent.State.OPEN) {
            msg(player, "<red>There is no event to join right now.");
            return false;
        }
        if (!active.getParticipants().add(player.getUniqueId())) {
            msg(player, "<gray>You already joined.");
            return true;
        }
        msg(player, "<green>You joined <white>" + active.getName() + "<green>! Sit tight for the start.");
        Player host = Bukkit.getPlayer(active.getHost());
        if (host != null) msg(host, "<gray>" + player.getName() + " joined <dark_gray>("
                + active.getParticipants().size() + " total)");
        return true;
    }

    /** Teleports everyone into the vanilla arena, applies the kit and counts down. */
    public boolean begin(Player host) {
        if (notHostOf(host, HostedEvent.State.OPEN)) return false;
        if (active.getParticipants().size() < 2) {
            msg(host, "<red>Need at least 2 players to begin.");
            return false;
        }

        active.setState(HostedEvent.State.RUNNING);
        active.setFrozen(true);
        active.getAlive().clear();

        World world = eventWorld();
        List<UUID> ids = new ArrayList<>(active.getParticipants());
        for (int i = 0; i < ids.size(); i++) {
            Player p = Bukkit.getPlayer(ids.get(i));
            if (p == null || !p.isOnline()) continue;
            active.getAlive().add(p.getUniqueId());
            preparePlayer(p);
            p.teleport(spreadSpawn(world, i, ids.size()));
            applyKit(p);
            p.setWalkSpeed(0f); // frozen during the countdown
        }
        runCountdown();
        return true;
    }

    /** Called by the listener when a participant dies while the event runs. */
    public void onDeath(UUID uuid) {
        if (active == null || active.getState() != HostedEvent.State.RUNNING) return;
        if (!active.getAlive().remove(uuid)) return;

        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.setGameMode(GameMode.SPECTATOR);
            msg(p, "<red>You were eliminated! <gray>" + active.getAlive().size() + " left.");
        }
        checkWin();
    }

    /** Called by the listener when a participant disconnects. */
    public void handleQuit(UUID uuid) {
        if (active == null) return;
        active.getParticipants().remove(uuid);
        if (active.getState() == HostedEvent.State.RUNNING && active.getAlive().remove(uuid)) {
            checkWin();
        }
    }

    /** Host aborts the event; everyone is returned and state is cleared. */
    public boolean cancel(Player host) {
        if (active == null) { msg(host, "<red>No event is running."); return false; }
        if (!host.getUniqueId().equals(active.getHost()) && !host.hasPermission("lemonevents.admin.host")) {
            msg(host, "<red>Only the host can cancel this event.");
            return false;
        }
        if (active.getState() == HostedEvent.State.SETUP) {
            restoreHost(host);
        } else {
            returnEveryone();
        }
        broadcast("<gray>The event <white>" + active.getName() + " <gray>was cancelled.");
        active = null;
        hostSnapshot = null;
        return true;
    }

    // -- Internals -----------------------------------------------------------

    private void checkWin() {
        if (active.getAlive().size() > 1) return;
        UUID winnerId = active.getAlive().stream().findFirst().orElse(null);
        active.setState(HostedEvent.State.ENDED);

        String winnerName = winnerId != null ? nameOf(winnerId) : "Nobody";
        broadcast("<bold><gradient:#fffb00:#00ff00>🏆 " + winnerName + " won " + active.getName() + "!</gradient></bold>");

        Player winner = winnerId != null ? Bukkit.getPlayer(winnerId) : null;
        if (winner != null) {
            WinAnimation.celebrate(plugin, winner, active.getName());
            long coins = plugin.getConfig().getLong("hosted-events.win-coins", 0);
            if (coins > 0) {
                var lc = lemonCore();
                if (lc != null) lc.getPlayerDataManager().addCoins(winner.getUniqueId(), coins,
                        "events:hosted:" + active.getName(), null);
            }
        }
        // Let the celebration play, then send everyone back.
        new BukkitRunnable() {
            @Override public void run() {
                returnEveryone();
                active = null;
                hostSnapshot = null;
            }
        }.runTaskLater(plugin, 140L);
    }

    private void runCountdown() {
        new BukkitRunnable() {
            int count = 5;
            @Override
            public void run() {
                if (active == null || active.getState() != HostedEvent.State.RUNNING) { cancel(); return; }
                if (count <= 0) {
                    active.setFrozen(false);
                    for (UUID id : active.getAlive()) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) {
                            p.setWalkSpeed(0.2f);
                            p.showTitle(Title.title(MM.deserialize("<gradient:#fffb00:#00ff00><bold>GO!</bold></gradient>"),
                                    MM.deserialize(""), Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(300))));
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
                        }
                    }
                    cancel();
                    return;
                }
                for (UUID id : active.getAlive()) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        p.showTitle(Title.title(MM.deserialize("<yellow><bold>" + count + "</bold>"),
                                MM.deserialize("<gray>Get ready..."),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1f);
                    }
                }
                count--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        var maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        p.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        for (var e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
    }

    private void applyKit(Player p) {
        p.getInventory().setStorageContents(cloneArr(active.getKitContents()));
        p.getInventory().setArmorContents(cloneArr(active.getKitArmor()));
        p.getInventory().setItemInOffHand(active.getKitOffhand() != null ? active.getKitOffhand().clone() : null);
        p.updateInventory();
    }

    private void returnEveryone() {
        Location ret = returnSpawn();
        for (UUID id : new ArrayList<>(active.getParticipants())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.setWalkSpeed(0.2f);
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            if (ret != null) p.teleport(ret);
        }
    }

    private void restoreHost(Player host) {
        if (hostSnapshot != null) {
            host.getInventory().setStorageContents(hostSnapshot.storage());
            host.getInventory().setArmorContents(hostSnapshot.armor());
            host.getInventory().setItemInOffHand(hostSnapshot.offhand());
            host.setGameMode(hostSnapshot.mode());
            host.updateInventory();
        }
    }

    private World eventWorld() {
        String name = plugin.getConfig().getString("hosted-events.world", "event_arena");
        World w = Bukkit.getWorld(name);
        if (w == null) {
            try {
                w = new WorldCreator(name).environment(World.Environment.NORMAL).createWorld();
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load event world '" + name + "': " + e.getMessage());
            }
        }
        return w != null ? w : Bukkit.getWorlds().get(0);
    }

    private Location spreadSpawn(World world, int index, int total) {
        Location base = world.getSpawnLocation();
        double radius = plugin.getConfig().getDouble("hosted-events.spread-radius", 30);
        double angle = (2 * Math.PI * index) / Math.max(1, total);
        double x = base.getX() + Math.cos(angle) * radius;
        double z = base.getZ() + Math.sin(angle) * radius;
        int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        Vector toCentre = base.toVector().subtract(loc.toVector()).setY(0);
        if (toCentre.lengthSquared() > 1.0e-4) loc.setDirection(toCentre); // face the centre
        return loc;
    }

    private Location returnSpawn() {
        String world = plugin.getConfig().getString("hosted-events.return.world", null);
        if (world != null) {
            World w = Bukkit.getWorld(world);
            if (w != null) return new Location(w,
                    plugin.getConfig().getDouble("hosted-events.return.x", 0.5),
                    plugin.getConfig().getDouble("hosted-events.return.y", 64),
                    plugin.getConfig().getDouble("hosted-events.return.z", 0.5));
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    private void broadcastJoin() {
        String join = "<click:run_command:'/host join'><hover:show_text:'<green>Click to join'>"
                + "<dark_gray>[<gradient:#fffb00:#00ff00><bold>➜ JOIN</bold></gradient><dark_gray>]</hover></click>";
        Bukkit.broadcast(MM.deserialize("<gradient:#fffb00:#ffa751><bold>EVENT</bold></gradient> <dark_gray>» "
                + "<white>" + active.getHostName() + " <gray>is hosting <white>" + active.getName() + "<gray>!  " + join));
        // Best-effort cross-server notice (players still join on this server).
        try {
            plugin.getMessaging().broadcastToAll("§e" + active.getHostName() + " is hosting " + active.getName()
                    + " — join on the events server!");
        } catch (Throwable ignored) {}
    }

    private void broadcast(String mini) {
        Bukkit.broadcast(MM.deserialize(mini));
    }

    private boolean notHostOf(Player p, HostedEvent.State required) {
        if (active == null || active.getState() != required) {
            msg(p, "<red>No event in the right state for that.");
            return true;
        }
        if (!p.getUniqueId().equals(active.getHost()) && !p.hasPermission("lemonevents.admin.host")) {
            msg(p, "<red>Only the host can do that.");
            return true;
        }
        return false;
    }

    private String nameOf(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }

    private com.lemonpvp.lemoncore.LemonCore lemonCore() {
        org.bukkit.plugin.Plugin lc = Bukkit.getPluginManager().getPlugin("LemonCore");
        return lc instanceof com.lemonpvp.lemoncore.LemonCore core ? core : null;
    }

    private static ItemStack[] cloneArr(ItemStack[] src) {
        if (src == null) return null;
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) out[i] = src[i] != null ? src[i].clone() : null;
        return out;
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
