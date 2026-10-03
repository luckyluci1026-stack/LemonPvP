package de.lemonpvp.duelplus.db;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Was ein Spieler beim Betreten der Arena "riskiert": Hauptinventar
 * (Hotbar + Rucksack, 36 Plaetze), Ruestung (4) und Offhand (1).
 * Bewusst OHNE Enderkiste - die zaehlt nicht zur "Kampfausruestung".
 */
public record SpielerSnapshot(ItemStack[] hauptinventar, ItemStack[] ruestung, ItemStack offhand, List<ItemStack> nachlieferung) {

    public SpielerSnapshot {
        List<ItemStack> ohneLuecken = new ArrayList<>();
        if (nachlieferung != null) {
            for (ItemStack item : nachlieferung) {
                if (item != null && !item.getType().isAir()) {
                    ohneLuecken.add(item);
                }
            }
        }
        nachlieferung = List.copyOf(ohneLuecken);
    }

    public SpielerSnapshot(ItemStack[] hauptinventar, ItemStack[] ruestung, ItemStack offhand) {
        this(hauptinventar, ruestung, offhand, List.of());
    }

    public static SpielerSnapshot von(PlayerInventory inv) {
        // getStorageContents() statt getContents() - eindeutig NUR Hotbar+
        // Rucksack, ohne jede Unklarheit ueber eine moegliche Ueberschneidung
        // mit den separat erfassten Ruestungs-Slots.
        return new SpielerSnapshot(kopie(inv.getStorageContents()), kopie(inv.getArmorContents()),
                cloneOrNull(inv.getItemInOffHand()));
    }

    private static ItemStack[] kopie(ItemStack[] original) {
        ItemStack[] kopie = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            kopie[i] = cloneOrNull(original[i]);
        }
        return kopie;
    }

    public void anwenden(PlayerInventory inv) {
        inv.setStorageContents(hauptinventar);
        inv.setArmorContents(ruestung);
        inv.setItemInOffHand(offhand);
    }

    public SpielerSnapshot mitZusatz(List<ItemStack> zusatz) {
        ItemStack[] haupt = hauptinventar.clone();
        List<ItemStack> rest = new ArrayList<>(nachlieferung);
        for (ItemStack item : zusatz) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            int frei = freierPlatz(haupt);
            if (frei >= 0) {
                haupt[frei] = item.clone();
            } else {
                rest.add(item.clone());
            }
        }
        return new SpielerSnapshot(haupt, ruestung, offhand, rest);
    }

    public SpielerSnapshot ohneNachlieferung() {
        return new SpielerSnapshot(hauptinventar, ruestung, offhand, List.of());
    }

    public static SpielerSnapshot leer() {
        return new SpielerSnapshot(new ItemStack[36], new ItemStack[4], null);
    }

    private static int freierPlatz(ItemStack[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null || items[i].getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
