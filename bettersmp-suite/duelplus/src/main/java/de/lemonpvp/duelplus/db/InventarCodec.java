package de.lemonpvp.duelplus.db;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * SpielerSnapshot <-> Text, fuer die Datenbank-Spalte. Gleiches Prinzip
 * wie bei jedem Kits-/Rucksack-Plugin: Bukkits eigene
 * BukkitObjectOutputStream/-InputStream serialisieren ItemStacks
 * verlustfrei (NBT, Verzauberungen, Anzeigenamen, alles), Base64 macht
 * daraus reinen Text fuer die Datenbank.
 */
public final class InventarCodec {

    private InventarCodec() {
    }

    public static String kodieren(SpielerSnapshot snapshot) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            schreibe(out, snapshot.hauptinventar());
            schreibe(out, snapshot.ruestung());
            out.writeObject(snapshot.offhand());
            schreibe(out, snapshot.nachlieferung().toArray(new ItemStack[0]));
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    public static SpielerSnapshot dekodieren(String daten) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(daten);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            ItemStack[] haupt = lese(in);
            ItemStack[] ruestung = lese(in);
            ItemStack offhand = (ItemStack) in.readObject();
            return new SpielerSnapshot(haupt, ruestung, offhand, nachlieferungLesen(in));
        }
    }

    public static String kodierenListe(List<ItemStack> items) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            schreibe(out, items.toArray(new ItemStack[0]));
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    public static List<ItemStack> dekodierenListe(String daten) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(daten);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ohneLuecken(lese(in));
        }
    }

    private static List<ItemStack> nachlieferungLesen(BukkitObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            return ohneLuecken(lese(in));
        } catch (EOFException e) {
            return List.of();
        }
    }

    private static List<ItemStack> ohneLuecken(ItemStack[] items) {
        List<ItemStack> liste = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) {
                liste.add(item);
            }
        }
        return liste;
    }

    private static void schreibe(BukkitObjectOutputStream out, ItemStack[] items) throws IOException {
        out.writeInt(items.length);
        for (ItemStack item : items) {
            out.writeObject(item);
        }
    }

    private static ItemStack[] lese(BukkitObjectInputStream in) throws IOException, ClassNotFoundException {
        int laenge = in.readInt();
        ItemStack[] items = new ItemStack[laenge];
        for (int i = 0; i < laenge; i++) {
            items[i] = (ItemStack) in.readObject();
        }
        return items;
    }
}
