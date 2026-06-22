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
            sender.sendMessage(MM.deserialize("<red>Du kannst dich nicht selbst einladen."));
            return;
        }

        Party senderParty = parties.get(sender.getUniqueId());
        if (senderParty != null && !senderParty.isLeader(sender.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red>Nur der Party-Leader kann Spieler einladen."));
            return;
        }
        if (parties.containsKey(target.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red><yellow>" + target.getName()
                    + "</yellow> ist bereits in einer Party."));
            return;
        }

        // Respect target's party-invite setting
        org.bukkit.plugin.Plugin lcPlugin = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lcPlugin instanceof com.lemonpvp.lemoncore.LemonCore lc) {
            com.lemonpvp.lemoncore.managers.PlayerData pd =
                    lc.getPlayerDataManager().getCached(target.getUniqueId());
            if (pd != null && !pd.isPartyInvites()) {
                sender.sendMessage(MM.deserialize("<red><yellow>" + target.getName()
                        + "</yellow> akzeptiert keine Party-Einladungen."));
                return;
            }
        }

        invites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(sender.getUniqueId(), System.currentTimeMillis() + INVITE_TIMEOUT_MS);

        sender.sendMessage(MM.deserialize(PREFIX + "<yellow>" + target.getName()
                + "</yellow> <gray>wurde eingeladen."));
        sender.playSound(sender.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

        Component accept = MM.deserialize("<green><bold>[Annehmen]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/party accept " + sender.getName()))
                .hoverEvent(HoverEvent.showText(MM.deserialize("<green>Klicke, um anzunehmen")));
        Component deny = MM.deserialize("<red><bold>[Ablehnen]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/party deny " + sender.getName()))
                .hoverEvent(HoverEvent.showText(MM.deserialize("<red>Klicke, um abzulehnen")));

        target.sendMessage(MM.deserialize("<dark_gray><st>                                </st>"));
        target.sendMessage(MM.deserialize(PREFIX + "<gradient:#fffb00:#00ff00>Einladung</gradient>"));
        target.sendMessage(MM.deserialize("<yellow>" + sender.getName()
                + "</yellow> <gray>hat dich in seine Party eingeladen."));
        target.sendMessage(Component.text("  ").append(accept).append(Component.text("   ")).append(deny));
        target.sendMessage(MM.deserialize("<dark_gray>Läuft in 60 Sekunden ab."));
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
                    + "</yellow> ist nicht mehr online."));
            return;
        }

        Map<UUID, Long> forTarget = invites.get(target.getUniqueId());
        Long expiry = forTarget != null ? forTarget.get(sender.getUniqueId()) : null;
        if (expiry == null || System.currentTimeMillis() > expiry) {
            target.sendMessage(MM.deserialize("<red>Keine offene Einladung von <yellow>"
                    + senderName + "</yellow>."));
            if (forTarget != null) forTarget.remove(sender.getUniqueId());
            return;
        }
        forTarget.remove(sender.getUniqueId());

        if (parties.containsKey(target.getUniqueId())) {
            target.sendMessage(MM.deserialize("<red>Du bist bereits in einer Party."));
            return;
        }

        Party party = parties.computeIfAbsent(sender.getUniqueId(),
                k -> new Party(sender.getUniqueId()));
        party.addMember(target.getUniqueId());
        parties.put(target.getUniqueId(), party);

        broadcast(party, PREFIX + "<yellow>" + target.getName()
                + "</yellow> <gray>ist der Party beigetreten.");
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
            target.sendMessage(MM.deserialize("<red>Keine offene Einladung von <yellow>"
                    + senderName + "</yellow>."));
            return;
        }

        target.sendMessage(MM.deserialize(
                "<gray>Du hast die Einladung von <yellow>" + senderName + "</yellow> abgelehnt."));
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(MM.deserialize(PREFIX + "<yellow>" + target.getName()
                    + "</yellow> <gray>hat die Einladung abgelehnt."));
        }
    }

    // -----------------------------------------------------------------------
    // Leave
    // -----------------------------------------------------------------------

    public void leave(Player player) {
        Party party = parties.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MM.deserialize("<red>Du bist in keiner Party."));
            return;
        }

        removePlayerFromParty(player.getUniqueId(), party, true);
        player.sendMessage(MM.deserialize("<gray>Du hast die Party verlassen."));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    // -----------------------------------------------------------------------
    // Kick
    // -----------------------------------------------------------------------

    public void kick(Player leader, String targetName) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(MM.deserialize("<red>Du bist in keiner Party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize("<red>Nur der Leader kann Spieler kicken."));
            return;
        }

        UUID targetUuid = findByName(party.getMembers(), targetName);
        if (targetUuid == null || !party.isMember(targetUuid)) {
            leader.sendMessage(MM.deserialize("<red><yellow>" + targetName
                    + "</yellow> ist nicht in deiner Party."));
            return;
        }
        if (party.isLeader(targetUuid)) {
            leader.sendMessage(MM.deserialize(
                    "<red>Du kannst dich nicht selbst kicken. Nutze /party disband."));
            return;
        }

        party.removeMember(targetUuid);
        parties.remove(targetUuid);

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            target.sendMessage(MM.deserialize("<red>Du wurdest aus der Party gekickt."));
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
        }
        broadcast(party, PREFIX + "<yellow>" + targetName + "</yellow> <gray>wurde gekickt.");
    }

    // -----------------------------------------------------------------------
    // Disband
    // -----------------------------------------------------------------------

    public void disband(Player leader) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(MM.deserialize("<red>Du bist in keiner Party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize(
                    "<red>Nur der Leader kann die Party auflösen."));
            return;
        }

        Component msg = MM.deserialize("<red>Die Party wurde aufgelöst.");
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
            leader.sendMessage(MM.deserialize("<red>Du bist in keiner Party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize("<red>Nur der Leader kann einen neuen Leader ernennen."));
            return;
        }

        UUID targetUuid = findByName(party.getMembers(), targetName);
        if (targetUuid == null || targetUuid.equals(leader.getUniqueId())) {
            leader.sendMessage(MM.deserialize("<red><yellow>" + targetName
                    + "</yellow> ist nicht in deiner Party."));
            return;
        }

        party.setLeader(targetUuid);
        Player target = Bukkit.getPlayer(targetUuid);
        String tName = target != null ? target.getName() : targetName;
        broadcast(party, PREFIX + "<yellow>" + tName
                + "</yellow> <gray>ist jetzt der Party-Leader.");
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
            player.sendMessage(MM.deserialize("<red>Du bist in keiner Party."));
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
            player.sendMessage(MM.deserialize("<red>Du bist in keiner Party."));
            return;
        }

        player.sendMessage(MM.deserialize(
                "<dark_gray><st>                                        </st>"));
        player.sendMessage(MM.deserialize(
                PREFIX + "<gradient:#fffb00:#00ff00><bold>Deine Party</bold></gradient>"
                        + " <dark_gray>(" + party.size() + " Spieler)"));
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
                    "<red>Nur der Party-Leader kann die Party in die Queue einreihen."));
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
                        + "<gray>Du wurdest für <gold>" + gamemode
                        + "</gold> in die Queue eingereiht."));
            }
        }
        broadcast(party, PREFIX + "<gray>" + queued
                + " Mitglieder wurden für <gold>" + gamemode + "</gold> gequeued.");
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
                        + "</yellow> <gray>hat die Party verlassen. Neuer Leader: <yellow>" + newName + "</yellow>.");
            } else {
                broadcast(party, PREFIX + "<yellow>" + resolveNameOrUuid(uuid)
                        + "</yellow> <gray>hat die Party verlassen. Neuer Leader: <yellow>" + newName + "</yellow>.");
            }
        } else if (announce) {
            broadcast(party, PREFIX + "<yellow>" + resolveNameOrUuid(uuid)
                    + "</yellow> <gray>hat die Party verlassen.");
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
