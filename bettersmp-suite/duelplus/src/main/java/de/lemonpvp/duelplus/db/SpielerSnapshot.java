package de.lemonpvp.duelplus.db;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Was ein Spieler beim Betreten der Arena "riskiert": Hauptinventar
 * (Hotbar + Rucksack, 36 Plaetze), Ruestung (4) und Offhand (1).
 * Bewusst OHNE Enderkiste - die zaehlt nicht zur "Kampfausruestung".
 */
public record SpielerSnapshot(ItemStack[] hauptinventar, ItemStack[] ruestung, ItemStack offhand) {

    public static SpielerSnapshot von(PlayerInventory inv) {
        return new SpielerSnapshot(inv.getContents().clone(), inv.getArmorContents().clone(),
                cloneOrNull(inv.getItemInOffHand()));
    }

    public void anwenden(PlayerInventory inv) {
        inv.setContents(hauptinventar);
        inv.setArmorContents(ruestung);
        inv.setItemInOffHand(offhand);
    }

    public static SpielerSnapshot leer() {
        return new SpielerSnapshot(new ItemStack[36], new ItemStack[4], null);
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
