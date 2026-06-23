package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.FriendRequestManager;
import com.lemonpvp.lemoncore.managers.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * /friend — manage the friends list.
 *
 * <ul>
 *   <li>/friend (no args) / list / gui → opens FriendsGUI</li>
 *   <li>/friend add {@literal <player>} → sends a friend request; does NOT add
 *       immediately — the target must accept first</li>
 *   <li>/friend accept {@literal <player>} → accepts a pending request</li>
 *   <li>/friend deny {@literal <player>} → denies a pending request</li>
 *   <li>/friend remove {@literal <player>} → removes an existing friend</li>
 * </ul>
 */
public class FriendCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public FriendCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.friend")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }

        // No args or explicit list/gui sub-command → open GUI
        if (args.length == 0
                || args[0].equalsIgnoreCase("list")
                || args[0].equalsIgnoreCase("gui")) {
            new com.lemonpvp.lemoncore.gui.FriendsGUI(plugin, player).open();
            return true;
        }

        String action = args[0].toLowerCase();

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String targetName = args[1];
        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(MM.deserialize("<red>Du kannst dir selbst keine Freundschaftsanfrage senden."));
            return true;
        }

        resolveUuid(targetName, targetUuid -> {
            if (targetUuid == null) {
                player.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName));
                return;
            }

            PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
            FriendRequestManager req = plugin.getFriendRequestManager();

            switch (action) {

                case "add" -> {
                    if (data != null && data.isFriend(targetUuid)) {
                        player.sendMessage(MM.deserialize(
                                "<red>Du bist bereits mit <white>" + targetName + "<red> befreundet."));
                        return;
                    }
                    if (req.hasPending(player.getUniqueId(), targetUuid)) {
                        player.sendMessage(MM.deserialize(
                                "<yellow>Du hast <white>" + targetName
                                + "<yellow> bereits eine Anfrage gesendet. Warte auf die Antwort."));
                        return;
                    }
                    // Check if target already sent us a request → auto-accept
                    if (req.consume(targetUuid, player.getUniqueId())) {
                        addFriendship(player.getUniqueId(), targetUuid, player.getName(), targetName, data);
                        return;
                    }
                    req.send(player.getUniqueId(), targetUuid);
                    player.sendMessage(MM.deserialize(
                            "<gray>Freundschaftsanfrage an <white>" + targetName + "<gray> gesendet."));
                    notifyTarget(targetUuid, player.getName());
                }

                case "accept" -> {
                    // "targetName" is the sender whose request we accept
                    if (!req.consume(targetUuid, player.getUniqueId())) {
                        player.sendMessage(MM.deserialize(
                                "<red>Keine ausstehende Freundschaftsanfrage von <white>" + targetName + "<red>."));
                        return;
                    }
                    PlayerData targetData = plugin.getPlayerDataManager().getCached(targetUuid);
                    addFriendship(player.getUniqueId(), targetUuid, player.getName(), targetName, data);
                    player.sendMessage(MM.deserialize(
                            "<green>Du bist jetzt mit <white>" + targetName + "<green> befreundet! 🎉"));
                    Player targetPlayer = Bukkit.getPlayer(targetUuid);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(MM.deserialize(
                                "<green><white>" + player.getName()
                                + "<green> hat deine Freundschaftsanfrage angenommen! 🎉"));
                        targetPlayer.playSound(targetPlayer.getLocation(),
                                Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                }

                case "deny" -> {
                    if (!req.consume(targetUuid, player.getUniqueId())) {
                        player.sendMessage(MM.deserialize(
                                "<red>Keine ausstehende Freundschaftsanfrage von <white>" + targetName + "<red>."));
                        return;
                    }
                    player.sendMessage(MM.deserialize(
                            "<gray>Freundschaftsanfrage von <white>" + targetName + "<gray> abgelehnt."));
                    Player targetPlayer = Bukkit.getPlayer(targetUuid);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(MM.deserialize(
                                "<red><white>" + player.getName()
                                + "<red> hat deine Freundschaftsanfrage abgelehnt."));
                    }
                }

                case "remove" -> {
                    if (data != null && !data.isFriend(targetUuid)) {
                        player.sendMessage(MM.deserialize(
                                "<red><white>" + targetName + "<red> ist nicht in deiner Freundesliste."));
                        return;
                    }
                    removeFriendship(player.getUniqueId(), targetUuid, data, targetName);
                    player.sendMessage(MM.deserialize(
                            "<gray><white>" + targetName + "<gray> aus der Freundesliste entfernt."));
                    Player targetPlayer = Bukkit.getPlayer(targetUuid);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(MM.deserialize(
                                "<gray><white>" + player.getName()
                                + "<gray> hat dich aus seiner Freundesliste entfernt."));
                    }
                }

                default -> sendUsage(player);
            }
        });
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Writes the friendship to the DB and updates both in-memory caches. */
    private void addFriendship(UUID uuid1, UUID uuid2,
                               String name1, String name2,
                               PlayerData data1) {
        plugin.getDatabaseManager().executeAsync(conn -> {
            try (var ps = conn.prepareStatement(
                    "INSERT IGNORE INTO lc_friends (uuid1, uuid2) VALUES (?,?)")) {
                ps.setString(1, uuid1.toString());
                ps.setString(2, uuid2.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Friend add DB error: " + e.getMessage());
            }
        });
        if (data1 != null) data1.addFriend(uuid2);
        PlayerData data2 = plugin.getPlayerDataManager().getCached(uuid2);
        if (data2 != null) data2.addFriend(uuid1);
    }

    /** Removes the friendship from the DB and updates both in-memory caches. */
    private void removeFriendship(UUID uuid1, UUID uuid2, PlayerData data1, String targetName) {
        plugin.getDatabaseManager().executeAsync(conn -> {
            try (var ps = conn.prepareStatement(
                    "DELETE FROM lc_friends WHERE (uuid1=? AND uuid2=?) OR (uuid1=? AND uuid2=?)")) {
                ps.setString(1, uuid1.toString()); ps.setString(2, uuid2.toString());
                ps.setString(3, uuid2.toString()); ps.setString(4, uuid1.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Friend remove DB error: " + e.getMessage());
            }
        });
        if (data1 != null) data1.removeFriend(uuid2);
        PlayerData data2 = plugin.getPlayerDataManager().getCached(uuid2);
        if (data2 != null) data2.removeFriend(uuid1);
    }

    /**
     * Sends a clickable friend-request notification to the target if they are
     * online. The [Akzeptieren] and [Ablehnen] buttons run the corresponding
     * sub-commands via chat, so no custom event plumbing is needed.
     */
    private void notifyTarget(UUID targetUuid, String senderName) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) return;

        Component accept = MM.deserialize("<green><bold>[✔ Akzeptieren]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/friend accept " + senderName))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        MM.deserialize("<green>Freundschaftsanfrage annehmen")));
        Component deny = MM.deserialize("<red><bold>[✘ Ablehnen]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/friend deny " + senderName))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        MM.deserialize("<red>Freundschaftsanfrage ablehnen")));

        target.sendMessage(Component.empty());
        target.sendMessage(MM.deserialize(
                "<gradient:#69f0ae:#00b0ff><bold>Freundschaftsanfrage</bold></gradient>"));
        target.sendMessage(MM.deserialize(
                "<gray><white>" + senderName + "<gray> möchte dich als Freund hinzufügen."));
        target.sendMessage(accept.append(Component.text(" ")).append(deny));
        target.sendMessage(MM.deserialize(
                "<dark_gray><i>Verfällt in 2 Minuten.</i>"));
        target.sendMessage(Component.empty());
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.6f);
    }

    private void sendUsage(Player player) {
        player.sendMessage(MM.deserialize(
                "<gray>Verwendung: <white>/friend add|accept|deny|remove <Spieler>"
                + " <dark_gray>| <white>/friend list"));
    }

    private void resolveUuid(String name, Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(uuid ->
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(uuid)));
    }
}
