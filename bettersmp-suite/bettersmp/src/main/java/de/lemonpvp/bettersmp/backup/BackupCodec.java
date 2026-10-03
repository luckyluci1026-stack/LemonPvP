package de.lemonpvp.bettersmp.backup;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * ItemStack[]/ItemStack <-> Text, fuer die Backup-Datenbank-Spalten.
 * Gleiches Prinzip wie bei jedem Kits-/Rucksack-Plugin: Bukkits eigene
 * BukkitObjectOutputStream/-InputStream serialisieren ItemStacks
 * verlustfrei (NBT, Verzauberungen, Anzeigenamen, alles), Base64 macht
 * daraus reinen Text.
 */
public final class BackupCodec {

    private BackupCodec() {
    }

    public static String kodiereArray(ItemStack[] items) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeInt(items.length);
            for (ItemStack item : items) {
                out.writeObject(item);
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    public static ItemStack[] dekodiereArray(String daten) throws IOException, ClassNotFoundException {
        if (daten == null || daten.isEmpty()) {
            return new ItemStack[0];
        }
        byte[] bytes = Base64.getDecoder().decode(daten);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            int laenge = in.readInt();
            ItemStack[] items = new ItemStack[laenge];
            for (int i = 0; i < laenge; i++) {
                items[i] = (ItemStack) in.readObject();
            }
            return items;
        }
    }

    public static String kodiereEinzeln(ItemStack item) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    public static ItemStack dekodiereEinzeln(String daten) throws IOException, ClassNotFoundException {
        if (daten == null || daten.isEmpty()) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(daten);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ItemStack) in.readObject();
        }
    }
}
