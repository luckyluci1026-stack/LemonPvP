package de.lemonpvp.helden.heart;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Das Link-Herz.
 *
 * <p>Wer auf sein letztes Herz faellt, bekommt einen zufaelligen Partner
 * zugelost und teilt dieses Herz mit ihm. Geht es verloren, kostet das den
 * Partner ebenfalls ein Herz - was wiederum dessen Link-Herz ausloesen kann.</p>
 *
 * <p>Die Bindung ist einseitig: sie kostet den Partner nur dann etwas, wenn der
 * Besitzer faellt, nicht umgekehrt.</p>
 */
public final class LinkHeartManager {

    private final HeldenPlugin plugin;

    public LinkHeartManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public HeldenProfile partnerOf(HeldenProfile profile) {
        return profile == null ? null : plugin.profiles().get(profile.linkPartner());
    }

    public void clear(HeldenProfile profile) {
        if (profile != null) {
            profile.linkPartner(null);
        }
    }

    /** Lost einen Partner aus, sobald jemand auf dem Link-Herz steht. */
    public void assignIfNeeded(HeldenProfile profile) {
        if (!plugin.settings().linkHeartEnabled() || profile == null || profile.eliminated()) {
            return;
        }
        if (profile.hearts() != 1) {
            return;
        }
        if (profile.hasLinkPartner() && isUsablePartner(partnerOf(profile))) {
            return;
        }

        HeldenProfile partner = drawPartner(profile);
        if (partner == null) {
            Player owner = Bukkit.getPlayer(profile.uuid());
            if (owner != null) {
                plugin.messages().send(owner, "link.no-partner");
            }
            profile.linkPartner(null);
            return;
        }

        profile.linkPartner(partner.uuid());
        announce(profile, partner);
    }

    private void announce(HeldenProfile profile, HeldenProfile partner) {
        Player owner = Bukkit.getPlayer(profile.uuid());
        if (owner != null) {
            plugin.messages().send(owner, "link.assigned-owner", "%partner%", partner.name());
            Compat.sound(owner, "block.beacon.power_select", 1.0f, 0.8f);
        }
        Player partnerPlayer = Bukkit.getPlayer(partner.uuid());
        if (partnerPlayer != null) {
            plugin.messages().send(partnerPlayer, "link.assigned-partner", "%player%", profile.name());
            Compat.sound(partnerPlayer, "block.beacon.power_select", 1.0f, 0.6f);
        }
        if (plugin.settings().linkAnnounce()) {
            plugin.messages().broadcastRaw("link.broadcast",
                    "%player%", profile.name(),
                    "%partner%", partner.name());
        }
    }

    /**
     * Zieht das Link-Herz beim Partner ein, nachdem der Besitzer gefallen ist.
     *
     * @param visited bereits behandelte Spieler - verhindert eine Endlosschleife,
     *                wenn sich zwei Link-Herzen gegenseitig treffen
     */
    public void applyChainLoss(HeldenProfile owner, Set<UUID> visited) {
        if (!plugin.settings().linkHeartEnabled() || owner == null) {
            return;
        }
        HeldenProfile partner = partnerOf(owner);
        if (partner == null || partner.eliminated()) {
            return;
        }

        plugin.messages().broadcastRaw("link.chain-broadcast",
                "%player%", owner.name(),
                "%partner%", partner.name());
        Player partnerPlayer = Bukkit.getPlayer(partner.uuid());
        if (partnerPlayer != null) {
            plugin.messages().send(partnerPlayer, "link.chain-loss", "%player%", owner.name());
            Compat.sound(partnerPlayer, "entity.wither.hurt", 1.0f, 0.5f);
        }

        owner.linkPartner(null);
        plugin.hearts().loseHeart(partner, null, visited);
    }

    /**
     * Alle, die durch das Ausscheiden dieses Spielers ihren Partner verlieren,
     * bekommen einen neuen zugelost.
     */
    public void reassignPartnersOf(HeldenProfile gone) {
        if (!plugin.settings().linkReassignOnPartnerOut() || gone == null) {
            return;
        }
        for (HeldenProfile profile : plugin.profiles().all()) {
            if (profile.eliminated() || !gone.uuid().equals(profile.linkPartner())) {
                continue;
            }
            profile.linkPartner(null);
            assignIfNeeded(profile);
        }
    }

    private boolean isUsablePartner(HeldenProfile partner) {
        return partner != null && !partner.eliminated();
    }

    /**
     * Zieht einen zufaelligen Mitspieler. Online-Spieler haben Vorrang - ein
     * Link-Herz an jemandem, der seit Wochen nicht da war, waere sinnlos.
     */
    private HeldenProfile drawPartner(HeldenProfile owner) {
        Set<UUID> alreadyLinked = new HashSet<>();
        if (!plugin.settings().linkAllowMultiple()) {
            for (HeldenProfile profile : plugin.profiles().all()) {
                if (profile.hasLinkPartner() && !profile.uuid().equals(owner.uuid())) {
                    alreadyLinked.add(profile.linkPartner());
                }
            }
        }

        List<HeldenProfile> online = new ArrayList<>();
        List<HeldenProfile> offline = new ArrayList<>();
        for (HeldenProfile candidate : plugin.profiles().all()) {
            if (candidate.uuid().equals(owner.uuid()) || candidate.eliminated()) {
                continue;
            }
            if (alreadyLinked.contains(candidate.uuid())) {
                continue;
            }
            if (Bukkit.getPlayer(candidate.uuid()) != null) {
                online.add(candidate);
            } else {
                offline.add(candidate);
            }
        }

        List<HeldenProfile> pool = online.isEmpty() ? offline : online;
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
