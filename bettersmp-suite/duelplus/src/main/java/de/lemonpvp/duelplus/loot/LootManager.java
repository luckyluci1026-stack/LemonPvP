package de.lemonpvp.duelplus.loot;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packt beim Verlust alles, was der Verlierer GERADE bei sich traegt,
 * in Shulker-Kisten und wirft sie an Ort und Stelle ab - dem Gewinner
 * gehoeren sie exklusiv fuer loot.schutz-sekunden (Standard 60s,
 * ueber eine Markierung direkt am Item selbst, nicht im Speicher -
 * ueberlebt also auch einen Neustart), danach frei fuer alle.
 */
public final class LootManager implements Listener {

    private final DuelPlus plugin;
    private final NamespacedKey gewinnerKey;
    private final NamespacedKey ablaufKey;

    public LootManager(DuelPlus plugin) {
        this.plugin = plugin;
        this.gewinnerKey = new NamespacedKey(plugin, "duell-gewinner");
        this.ablaufKey = new NamespacedKey(plugin, "duell-schutz-ablauf");
    }

    /** Liest das AKTUELLE Inventar des Verlierers, leert es und wirft es als Shulker an "ort" ab. */
    public void verliererLootAbwerfen(Player verlierer, UUID gewinner, Location ort) {
        List<ItemStack> alles = new ArrayList<>();
        sammeln(alles, verlierer.getInventory().getContents());
        sammeln(alles, verlierer.getInventory().getArmorContents());
        ItemStack offhand = verlierer.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            alles.add(offhand.clone());
        }

        verlierer.getInventory().clear();
        verlierer.getInventory().setItemInOffHand(null);

        if (alles.isEmpty()) {
            return;
        }

        int schutzSekunden = plugin.getConfig().getInt("loot.schutz-sekunden", 60);
        long ablauf = System.currentTimeMillis() + schutzSekunden * 1000L;

        for (ItemStack shulker : baueShulker(alles)) {
            Item entity = ort.getWorld().dropItemNaturally(ort, shulker);
            entity.getPersistentDataContainer().set(gewinnerKey, PersistentDataType.STRING, gewinner.toString());
            entity.getPersistentDataContainer().set(ablaufKey, PersistentDataType.LONG, ablauf);
            entity.setGlowing(true);
        }

        Player gewinnerSpieler = plugin.getServer().getPlayer(gewinner);
        if (gewinnerSpieler != null) {
            plugin.msgs().send(gewinnerSpieler, "loot-dropped", "sekunden", String.valueOf(schutzSekunden));
        }
    }

    private void sammeln(List<ItemStack> ziel, ItemStack[] quelle) {
        for (ItemStack item : quelle) {
            if (item != null && item.getType() != Material.AIR) {
                ziel.add(item.clone());
            }
        }
    }

    /** Packt eine Liste Items in so viele Shulker, wie noetig (27 Plaetze je Shulker). */
    private List<ItemStack> baueShulker(List<ItemStack> items) {
        List<ItemStack> shulkerListe = new ArrayList<>();
        for (int start = 0; start < items.size(); start += 27) {
            List<ItemStack> teil = items.subList(start, Math.min(start + 27, items.size()));
            ItemStack shulkerItem = new ItemStack(Material.SHULKER_BOX);
            BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
            ShulkerBox box = (ShulkerBox) meta.getBlockState();
            for (int i = 0; i < teil.size(); i++) {
                box.getInventory().setItem(i, teil.get(i));
            }
            meta.setBlockState(box);
            shulkerItem.setItemMeta(meta);
            shulkerListe.add(shulkerItem);
        }
        return shulkerListe;
    }

    @EventHandler(ignoreCancelled = true)
    public void beimAufheben(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player spieler)) {
            return;
        }
        Item item = event.getItem();
        String gewinnerRoh = item.getPersistentDataContainer().get(gewinnerKey, PersistentDataType.STRING);
        if (gewinnerRoh == null) {
            return;
        }
        Long ablauf = item.getPersistentDataContainer().get(ablaufKey, PersistentDataType.LONG);
        boolean nochGeschuetzt = ablauf != null && System.currentTimeMillis() < ablauf;
        if (nochGeschuetzt && !gewinnerRoh.equals(spieler.getUniqueId().toString())) {
            event.setCancelled(true);
        }
    }
}
