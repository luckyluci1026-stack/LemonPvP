package de.lemonpvp.bettersmp.backup;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Eine gesicherte Momentaufnahme aus bsmp_backup_inventar - siehe BackupDatabase/BackupManager. */
public record BackupSnapshot(
        UUID uuid, String name,
        ItemStack[] inventar, ItemStack[] ruestung, ItemStack offhand, ItemStack[] enderkiste,
        long gespeichert) {
}
