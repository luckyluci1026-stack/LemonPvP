package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player parties: invites, membership, leadership, and chat.
 * All members of a party share a reference to the same {@link Party} object.
 */
public class PartyManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final long INVITE_TIMEOUT_MS = 60_000L;
    private static final String PREFIX = "<gradient:#fffb00:#00ff00><bold>Pᴀʀᴛʏ</bold></gradient> <dark_gray>» ";

    private final LemonPractice plugin;

    /** Every party member (including leader) maps to the same Party instance. */
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();

    /** Pending invites: target uuid → (inviter uuid → expiry epoch ms) */
    private final Map<UUID, Map<UUID, Long>> invites = new ConcurrentHashMap<>();

    public PartyManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Invite
    // -----------------------------------------------------------------------

    public void invite(Player sender, Player target) {
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red>You can't invite yourself."));
            return;
        }

        Party senderParty = parties.get(sender.getUniqueId());
        if (senderParty != null && !senderParty.isLeader(sender.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red>Only the party leader can invite players."));
            return;
        }
        if (parties.containsKey(target.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red><yellow>" + target.getName()
                    + "</yellow> is already in a party."));
            return;
        }

        // Respect target's party-invite setting
        org.bukkit.plugin.Plugin lcPlugin = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lcPlugin instanceof com.lemonpvp.lemoncore.LemonCore lc) {
            com.lemonpvp.lemoncore.managers.PlayerData pd =
                    lc.getPlayerDataManager().getCached(target.getUniqueId());
            if (pd != null && !pd.isPartyInvites()) {
                sender.sendMessage(MM.deserialize("<red><yellow>" + target.getName()
                        + "</yellow> isn't accepting party invites."));
                return;
            }
        }

        invites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(sender.getUniqueId(), System.currentTimeMillis() + INVITE_TIMEOUT_MS);

        sender.sendMessage(MM.deserialize(PREFIX + "<yellow>" + target.getName()
                + "</yellow> <gray>has been invited."));
        sender.playSound(sender.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

        Component accept = MM.deserialize("<green><bold>[Accept]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/party accept " + sender.getName()))
                .hoverEvent(HoverEvent.showText(MM.deserialize("<green>Click to accept")));
        Component deny = MM.deserialize("<red><bold>[Decline]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/party deny " + sender.getName()))
                .hoverEvent(HoverEvent.showText(MM.deserialize("<red>Click to decline")));

        target.sendMessage(MM.deserialize("<dark_gray><st>                                </st>"));
        target.sendMessage(MM.deserialize(PREFIX + "<gradient:#fffb00:#00ff00>Invitation</gradient>"));
        target.sendMessage(MM.deserialize("<yellow>" + sender.getName()
                + "</yellow> <gray>has invited you to their party."));
        target.sendMessage(Component.text("  ").append(accept).append(Component.text("   ")).append(deny));
        target.sendMessage(MM.deserialize("<dark_gray>Expires in 60 seconds."));
        target.sendMessage(MM.deserialize("<dark_gray><st>                                </st>"));
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.3f);
    }

    // -----------------------------------------------------------------------
    // Accept
    // -----------------------------------------------------------------------

    public void accept(Player target, String senderName) {
        Player sender = Bukkit.getPlayerExact(senderName);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(MM.deserialize("<red><yellow>" + senderName
                    + "</yellow> is no longer online."));
            return;
        }

        Map<UUID, Long> forTarget = invites.get(target.getUniqueId());
        Long expiry = forTarget != null ? forTarget.get(sender.getUniqueId()) : null;
        if (expiry == null || System.currentTimeMillis() > expiry) {
            target.sendMessage(MM.deserialize("<red>No pending invite from <yellow>"
                    + senderName + "</yellow>."));
            if (forTarget != null) forTarget.remove(sender.getUniqueId());
            return;
        }
        forTarget.remove(sender.getUniqueId());

        if (parties.containsKey(target.getUniqueId())) {
            target.sendMessage(MM.deserialize("<red>You're already in a party."));
            return;
        }

        Party party = parties.computeIfAbsent(sender.getUniqueId(),
                k -> new Party(sender.getUniqueId()));
        party.addMember(target.getUniqueId());
        parties.put(target.getUniqueId(), party);

        broadcast(party, PREFIX + "<yellow>" + target.getName()
                + "</yellow> <gray>has joined the party.");
        for (UUID m : party.getMembers()) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.4f);
        }
    }

    // -----------------------------------------------------------------------
    // Deny
    // -----------------------------------------------------------------------

    public void deny(Player target, String senderName) {
        Player sender = Bukkit.getPlayerExact(senderName);
        Map<UUID, Long> forTarget = invites.get(target.getUniqueId());
        UUID senderUuid = sender != null
                ? sender.getUniqueId()
                : findByName(forTarget != null ? forTarget.keySet() : null, senderName);

        if (forTarget == null || senderUuid == null || forTarget.remove(senderUuid) == null) {
            target.sendMessage(MM.deserialize("<red>No pending invite from <yellow>"
                    + senderName + "</yellow>."));
            return;
        }

        target.sendMessage(MM.deserialize(
                "<gray>You declined the invite from <yellow>" + senderName + "</yellow>."));
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(MM.deserialize(PREFIX + "<yellow>" + target.getName()
                    + "</yellow> <gray>declined the invite."));
        }
    }

    // -----------------------------------------------------------------------
    // Leave
    // -----------------------------------------------------------------------

    public void leave(Player player) {
        Party party = parties.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MM.deserialize("<red>You're not in a party."));
            return;
        }

        removePlayerFromParty(player.getUniqueId(), party, true);
        player.sendMessage(MM.deserialize("<gray>You left the party."));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    // -----------------------------------------------------------------------
    // Kick
    // -----------------------------------------------------------------------

    public void kick(Player leader, String targetName) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(MM.deserialize("<red>You're not in a party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize("<red>Only the leader can kick players."));
            return;
        }

        UUID targetUuid = findByName(party.getMembers(), targetName);
        if (targetUuid == null || !party.isMember(targetUuid)) {
            leader.sendMessage(MM.deserialize("<red><yellow>" + targetName
                    + "</yellow> isn't in your party."));
            return;
        }
        if (party.isLeader(targetUuid)) {
            leader.sendMessage(MM.deserialize(
                    "<red>You can't kick yourself. Use /party disband."));
            return;
        }

        party.removeMember(targetUuid);
        parties.remove(targetUuid);

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            target.sendMessage(MM.deserialize("<red>You were kicked from the party."));
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
        }
        broadcast(party, PREFIX + "<yellow>" + targetName + "</yellow> <gray>was kicked.");
    }

    // -----------------------------------------------------------------------
    // Disband
    // -----------------------------------------------------------------------

    public void disband(Player leader) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(MM.deserialize("<red>You're not in a party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize(
                    "<red>Only the leader can disband the party."));
            return;
        }

        Component msg = MM.deserialize("<red>The party has been disbanded.");
        for (UUID m : new ArrayList<>(party.getMembers())) {
            parties.remove(m);
            Player p = Bukkit.getPlayer(m);
            if (p != null) {
                p.sendMessage(msg);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Promote
    // -----------------------------------------------------------------------

    public void promote(Player leader, String targetName) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(MM.deserialize("<red>You're not in a party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize("<red>Only the leader can appoint a new leader."));
            return;
        }

        UUID targetUuid = findByName(party.getMembers(), targetName);
        if (targetUuid == null || targetUuid.equals(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize("<red><yellow>" + targetName
                    + "</yellow> isn't in your party."));
            return;
        }

        party.setLeader(targetUuid);
        Player target = Bukkit.getPlayer(targetUuid);
        String tName = target != null ? target.getName() : targetName;
        broadcast(party, PREFIX + "<yellow>" + tName
                + "</yellow> <gray>is now the party leader.");
        for (UUID m : party.getMembers()) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }

    // -----------------------------------------------------------------------
    // Chat
    // -----------------------------------------------------------------------

    public void chat(Player player, String message) {
        Party party = parties.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MM.deserialize("<red>You're not in a party."));
            return;
        }
        broadcast(party, "<gradient:#fffb00:#00ff00><bold>Pᴀʀᴛʏ</bold></gradient> <dark_gray>» "
                + "<gray>" + player.getName() + ": <white>" + message);
    }

    // -----------------------------------------------------------------------
    // List
    // -----------------------------------------------------------------------

    public void list(Player player) {
        Party party = parties.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MM.deserialize("<red>You're not in a party."));
            return;
        }

        player.sendMessage(MM.deserialize(
                "<dark_gray><st>                                        </st>"));
        player.sendMessage(MM.deserialize(
                PREFIX + "<gradient:#fffb00:#00ff00><bold>Your Party</bold></gradient>"
                        + " <dark_gray>(" + party.size() + " players)"));
        player.sendMessage(Component.empty());
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            String name = p != null ? p.getName() : member.toString();
            String online = p != null ? "<green>●" : "<red>●";
            String role = party.isLeader(member) ? "<gold>[Leader] </gold>" : "<gray>";
            player.sendMessage(MM.deserialize(online + " " + role + name));
        }
        player.sendMessage(MM.deserialize(
                "<dark_gray><st>                                        </st>"));
    }

    // -----------------------------------------------------------------------
    // Queue integration — queue all party members together
    // -----------------------------------------------------------------------

    public void queueParty(Player leader, String gamemode) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            plugin.getQueueManager().addToQueue(leader.getUniqueId(), gamemode);
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize(
                    "<red>Only the party leader can queue the party."));
            return;
        }

        int queued = 0;
        for (UUID member : party.getMembers()) {
            if (plugin.getQueueManager().isQueued(member)) continue;
            if (plugin.getDuelManager().isInDuel(member)) continue;
            plugin.getQueueManager().addToQueue(member, gamemode);
            queued++;
            Player p = Bukkit.getPlayer(member);
            if (p != null && !p.getUniqueId().equals(leader.getUniqueId())) {
                p.sendMessage(MM.deserialize(PREFIX
                        + "<gray>You were queued for <gold>" + gamemode
                        + "</gold>."));
            }
        }
        broadcast(party, PREFIX + "<gray>" + queued
                + " members were queued for <gold>" + gamemode + "</gold>.");
    }

    // -----------------------------------------------------------------------
    // Cleanup on disconnect
    // -----------------------------------------------------------------------

    public void handleQuit(UUID uuid) {
        // Clean up pending invites
        invites.remove(uuid);
        invites.values().forEach(m -> m.remove(uuid));

        Party party = parties.get(uuid);
        if (party == null) return;

        removePlayerFromParty(uuid, party, false);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public Party getParty(UUID uuid)     { return parties.get(uuid); }
    public boolean isInParty(UUID uuid)  { return parties.containsKey(uuid); }

    public List<String> getPendingInviterNames(UUID targetUuid) {
        Map<UUID, Long> forTarget = invites.get(targetUuid);
        if (forTarget == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> e : forTarget.entrySet()) {
            if (now > e.getValue()) continue;
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) names.add(p.getName());
        }
        return names;
    }

    public List<String> getMemberNames(UUID leaderUuid) {
        Party party = parties.get(leaderUuid);
        if (party == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (UUID m : party.getMembers()) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) names.add(p.getName());
        }
        return names;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void removePlayerFromParty(UUID uuid, Party party, boolean announce) {
        party.removeMember(uuid);
        parties.remove(uuid);

        if (party.size() == 0) return;

        if (party.isLeader(uuid)) {
            // Transfer leadership to the next remaining member
            UUID newLeader = party.getMembers().iterator().next();
            party.setLeader(newLeader);
            Player newLP = Bukkit.getPlayer(newLeader);
            String newName = newLP != null ? newLP.getName() : newLeader.toString();
            if (announce) {
                broadcast(party, PREFIX + "<yellow>" + resolveNameOrUuid(uuid)
                        + "</yellow> <gray>left the party. New leader: <yellow>" + newName + "</yellow>.");
            } else {
                broadcast(party, PREFIX + "<yellow>" + resolveNameOrUuid(uuid)
                        + "</yellow> <gray>left the party. New leader: <yellow>" + newName + "</yellow>.");
            }
        } else if (announce) {
            broadcast(party, PREFIX + "<yellow>" + resolveNameOrUuid(uuid)
                    + "</yellow> <gray>left the party.");
        }
    }

    private void broadcast(Party party, String miniMsg) {
        Component msg = MM.deserialize(miniMsg);
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) p.sendMessage(msg);
        }
    }

    private String resolveNameOrUuid(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString().substring(0, 8);
    }

    private UUID findByName(Collection<UUID> uuids, String name) {
        if (uuids == null) return null;
        for (UUID uuid : uuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getName().equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }
}
