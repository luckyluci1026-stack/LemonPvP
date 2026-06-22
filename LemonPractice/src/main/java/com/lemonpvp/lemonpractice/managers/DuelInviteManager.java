package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles direct {@code /duel <player>} challenges: storing invites with a
 * timeout, validating them, and — once accepted — recording a cross-server
 * pending duel and sending both players to the duels server.
 *
 * <p>Invites live in memory on the server where the challenge is issued
 * (normally the lobby). The actual match starts on the duels server via the
 * {@code lp_pending_duels} table, claimed on join by {@link DuelInviteListener}.</p>
 */
public class DuelInviteManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final long INVITE_TIMEOUT_MS = 60_000L;

    private final LemonPractice plugin;

    /** target uuid -> (sender uuid -> invite) */
    private final Map<UUID, Map<UUID, Invite>> invites = new ConcurrentHashMap<>();

    public DuelInviteManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Sending
    // -----------------------------------------------------------------------

    public void sendInvite(Player sender, Player target, String gamemodeId) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red>Du kannst dich nicht selbst herausfordern."));
            return;
        }

        Gamemode gm = plugin.getGamemodeManager().getGamemode(gamemodeId);
        if (gm == null) {
            sender.sendMessage(MM.deserialize("<red>Unbekannter Modus: <yellow>" + gamemodeId));
            return;
        }
        if (!gm.isEnabled()) {
            sender.sendMessage(MM.deserialize("<red>Dieser Modus ist derzeit deaktiviert."));
            return;
        }

        if (plugin.getDuelManager().isInDuel(sender.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red>Du bist bereits in einem Duell."));
            return;
        }
        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            sender.sendMessage(MM.deserialize("<red><yellow>" + target.getName()
                    + "</yellow> ist gerade in einem Duell."));
            return;
        }

        invites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(sender.getUniqueId(), new Invite(sender.getUniqueId(), gm.getId(),
                        System.currentTimeMillis() + INVITE_TIMEOUT_MS));

        // Confirmation to the sender
        sender.sendMessage(MM.deserialize("<green>Du hast <yellow>" + target.getName()
                + "</yellow> zu einem <gold>" + gm.getName() + "</gold>-Duell herausgefordert."));
        sender.playSound(sender.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

        // Clickable invite to the target
        Component accept = MM.deserialize("<green><bold>[Annehmen]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/duel accept " + sender.getName()))
                .hoverEvent(HoverEvent.showText(MM.deserialize("<green>Klicke, um anzunehmen")));
        Component deny = MM.deserialize("<red><bold>[Ablehnen]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/duel deny " + sender.getName()))
                .hoverEvent(HoverEvent.showText(MM.deserialize("<red>Klicke, um abzulehnen")));

        target.sendMessage(MM.deserialize("<dark_gray><st>                                </st>"));
        target.sendMessage(MM.deserialize("<gradient:#fffb00:#00ff00><bold>Duell-Herausforderung</bold></gradient>"));
        target.sendMessage(MM.deserialize("<yellow>" + sender.getName() + "</yellow> <gray>fordert dich heraus: <gold>"
                + gm.getName()));
        target.sendMessage(Component.text("  ").append(accept).append(Component.text("   ")).append(deny));
        target.sendMessage(MM.deserialize("<dark_gray>Läuft in 60 Sekunden ab."));
        target.sendMessage(MM.deserialize("<dark_gray><st>                                </st>"));
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.3f);
    }

    // -----------------------------------------------------------------------
    // Accept / deny
    // -----------------------------------------------------------------------

    public void acceptInvite(Player target, String senderName) {
        Player sender = Bukkit.getPlayerExact(senderName);
        if (sender == null) {
            target.sendMessage(MM.deserialize("<red><yellow>" + senderName + "</yellow> ist nicht mehr online."));
            return;
        }

        Map<UUID, Invite> forTarget = invites.get(target.getUniqueId());
        Invite invite = (forTarget != null) ? forTarget.get(sender.getUniqueId()) : null;
        if (invite == null || invite.isExpired()) {
            target.sendMessage(MM.deserialize("<red>Du hast keine offene Herausforderung von <yellow>"
                    + sender.getName() + "</yellow>."));
            if (forTarget != null) forTarget.remove(sender.getUniqueId());
            return;
        }

        // Re-validate both players are free
        if (plugin.getDuelManager().isInDuel(target.getUniqueId())
                || plugin.getDuelManager().isInDuel(sender.getUniqueId())) {
            target.sendMessage(MM.deserialize("<red>Einer von euch ist bereits in einem Duell."));
            forTarget.remove(sender.getUniqueId());
            return;
        }

        Gamemode gm = plugin.getGamemodeManager().getGamemode(invite.gamemode);
        if (gm == null || !gm.isEnabled()) {
            target.sendMessage(MM.deserialize("<red>Dieser Modus ist nicht mehr verfügbar."));
            forTarget.remove(sender.getUniqueId());
            return;
        }

        forTarget.remove(sender.getUniqueId());

        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        String duelsServer = plugin.getServersConfig().getString("servers.duels.name", "duels");

        Component starting = MM.deserialize("<green>Duell wird gestartet... <gray>Du wirst zum Duell-Server geschickt.");
        sender.sendMessage(starting);
        target.sendMessage(starting);
        sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);

        // If we're already on the duels server, start immediately; otherwise hand
        // off through the DB and transfer both players there.
        if (plugin.getServerType().equals("DUELS")) {
            plugin.getDuelManager().startDuel(senderUuid, targetUuid, gm.getId());
            return;
        }

        plugin.getDatabase().insertPendingDuel(senderUuid, targetUuid, gm.getId())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player s = Bukkit.getPlayer(senderUuid);
                    Player t = Bukkit.getPlayer(targetUuid);
                    if (s != null) plugin.getVelocityMessaging().sendToServer(s, duelsServer);
                    if (t != null) plugin.getVelocityMessaging().sendToServer(t, duelsServer);
                }));
    }

    public void denyInvite(Player target, String senderName) {
        Player sender = Bukkit.getPlayerExact(senderName);
        Map<UUID, Invite> forTarget = invites.get(target.getUniqueId());
        UUID senderUuid = (sender != null) ? sender.getUniqueId() : findSenderByName(forTarget, senderName);

        if (forTarget == null || senderUuid == null || forTarget.remove(senderUuid) == null) {
            target.sendMessage(MM.deserialize("<red>Du hast keine offene Herausforderung von <yellow>"
                    + senderName + "</yellow>."));
            return;
        }

        target.sendMessage(MM.deserialize("<gray>Du hast die Herausforderung von <yellow>"
                + senderName + "</yellow> abgelehnt."));
        if (sender != null) {
            sender.sendMessage(MM.deserialize("<red><yellow>" + target.getName()
                    + "</yellow> hat deine Duell-Herausforderung abgelehnt."));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns the sender UUIDs of all unexpired invites for a target (for tab-completion). */
    public java.util.List<String> getPendingSenderNames(UUID targetUuid) {
        Map<UUID, Invite> forTarget = invites.get(targetUuid);
        if (forTarget == null) return java.util.Collections.emptyList();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Invite> e : forTarget.entrySet()) {
            if (e.getValue().isExpired()) continue;
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) names.add(p.getName());
        }
        return names;
    }

    private UUID findSenderByName(Map<UUID, Invite> forTarget, String name) {
        if (forTarget == null) return null;
        for (UUID uuid : forTarget.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getName().equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }

    public void clearInvitesFor(UUID uuid) {
        invites.remove(uuid);
        invites.values().forEach(m -> m.remove(uuid));
    }

    // -----------------------------------------------------------------------

    private static final class Invite {
        final UUID sender;
        final String gamemode;
        final long expiryMs;

        Invite(UUID sender, String gamemode, long expiryMs) {
            this.sender = sender;
            this.gamemode = gamemode;
            this.expiryMs = expiryMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryMs;
        }
    }
}
