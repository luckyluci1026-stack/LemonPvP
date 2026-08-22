package de.lemonpvp.helden.ui;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Teilnehmeruebersicht: wer hat noch wie viele Herzen, wer haengt an wem, wer
 * ist raus. Bewusst ein Kisten-GUI - Geyser uebersetzt das automatisch fuer
 * Bedrock-Spieler.
 */
public final class PlayerListMenu extends Menu {

    private final List<HeldenProfile> profiles;

    public PlayerListMenu(HeldenPlugin plugin) {
        super(plugin);
        this.profiles = plugin.profiles().ranked();
    }

    @Override
    public String title() {
        return plugin.messages().raw("list.menu-title");
    }

    @Override
    public int size() {
        int rows = Math.max(1, Math.min(6, (profiles.size() + 8) / 9));
        return rows * 9;
    }

    @Override
    protected void build(Player viewer) {
        int slot = 0;
        for (HeldenProfile profile : profiles) {
            if (slot >= size()) {
                break;
            }
            set(slot++, iconFor(profile));
        }
    }

    private ItemStack iconFor(HeldenProfile profile) {
        HeldenProfile partner = plugin.links().partnerOf(profile);
        String link = partner == null
                ? plugin.messages().raw("link.none")
                : Text.replace(plugin.messages().raw("link.partner"), "%partner%", partner.name());

        List<String> lore = new ArrayList<>();
        lore.add(Text.replace(plugin.messages().raw("list.lore-hearts"), "%hearts%", profile.hearts()));
        lore.add(Text.replace(plugin.messages().raw("list.lore-kills"), "%kills%", profile.kills()));
        lore.add(Text.replace(plugin.messages().raw("list.lore-deaths"), "%deaths%", profile.pvpDeaths()));
        lore.add(Text.replace(plugin.messages().raw("list.lore-link"), "%link%", link));
        lore.add(Text.replace(plugin.messages().raw("list.lore-status"),
                "%status%", plugin.game().statusOf(profile)));

        String display = (profile.eliminated() ? "&8&m" : "&f") + profile.name();
        return icon(materialFor(profile), display, lore, null);
    }

    /**
     * Ein Material je Zustand - so sieht man auf einen Blick, wer knapp dran
     * ist, ohne die Lore lesen zu muessen.
     */
    private Material materialFor(HeldenProfile profile) {
        if (profile.eliminated()) {
            return Compat.material("BLACK_STAINED_GLASS_PANE");
        }
        if (plugin.hearts().isOnLinkHeart(profile)) {
            return Compat.material("MAGENTA_STAINED_GLASS_PANE");
        }
        return Compat.material("RED_STAINED_GLASS_PANE");
    }

    @Override
    public void onClick(Player viewer, int slot, ItemStack clicked, String action) {
        // Reine Anzeige - nichts zu klicken.
    }
}
