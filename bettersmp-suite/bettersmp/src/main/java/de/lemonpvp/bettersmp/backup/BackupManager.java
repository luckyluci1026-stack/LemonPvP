package de.lemonpvp.bettersmp.backup;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sichert Inventar + Enderkiste ALLER Online-Spieler in festem Abstand
 * (backup.interval-seconds, Standard 15) asynchron in die
 * BackupDatabase - ein Sicherheitsnetz, damit im Fall von Data
 * Corruption an der eigentlichen Spielerdatei/Datenbank eine kuerzlich
 * gesicherte Kopie existiert. Reine "letzter Stand"-Sicherung (kein
 * Verlauf/keine Versionen) - passend zum eigentlichen Zweck (schnelle
 * Wiederherstellung), nicht als Audit-Log gedacht.
 */
public final class BackupManager {

    private final BetterSMP plugin;
    private final BackupDatabase db;
    private BukkitTask task;

    public BackupManager(BetterSMP plugin) {
        this.plugin = plugin;
        this.db = new BackupDatabase(plugin);
    }

    public BackupDatabase db() {
        return db;
    }

    public void start() {
        db.init();
        if (!plugin.getConfig().getBoolean("backup.enabled", true)) {
            plugin.getLogger().info("Inventar-Backup deaktiviert (backup.enabled: false).");
            return;
        }
        long intervalTicks = Math.max(5, plugin.getConfig().getInt("backup.interval-seconds", 15)) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::sichereAlle, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        db.shutdown();
    }

    private void sichereAlle() {
        if (!db.bereit()) {
            return;
        }
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            sichereEinen(spieler);
        }
    }

    private void sichereEinen(Player spieler) {
        PlayerInventory inv = spieler.getInventory();
        // Kopien statt der Live-Arrays - die Serialisierung laeuft asynchron
        // weiter, das Original darf sich in der Zwischenzeit ganz normal
        // aendern, ohne die Sicherung zu verfaelschen.
        ItemStack[] inventar = inv.getStorageContents().clone();
        ItemStack[] ruestung = inv.getArmorContents().clone();
        ItemStack offhand = inv.getItemInOffHand().clone();
        ItemStack[] enderkiste = spieler.getEnderChest().getContents().clone();
        db.sichern(spieler.getUniqueId(), spieler.getName(), inventar, ruestung, offhand, enderkiste);
    }

    /**
     * Wendet die zuletzt gesicherte Momentaufnahme auf einen ONLINE Spieler
     * an - ueberschreibt sein aktuelles Inventar + seine Enderkiste
     * komplett. Absichtlich nur fuer online Spieler (kein OfflinePlayer-
     * Zugriff auf die Spielerdatei - das waere blockierend und
     * fehleranfaelliger). Das Ergebnis (true = angewendet) kommt bereits
     * auf dem Haupt-Thread an, sicher fuer direkte Folge-Bukkit-Aufrufe.
     */
    public CompletableFuture<Boolean> wiederherstellen(UUID ziel) {
        CompletableFuture<Boolean> ergebnis = new CompletableFuture<>();
        db.lesen(ziel).thenAccept(snapshotOpt -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player spieler = Bukkit.getPlayer(ziel);
            if (snapshotOpt.isEmpty() || spieler == null) {
                ergebnis.complete(false);
                return;
            }
            BackupSnapshot snapshot = snapshotOpt.get();
            PlayerInventory inv = spieler.getInventory();
            inv.setStorageContents(snapshot.inventar());
            inv.setArmorContents(snapshot.ruestung());
            inv.setItemInOffHand(snapshot.offhand());
            spieler.getEnderChest().setContents(snapshot.enderkiste());
            ergebnis.complete(true);
        }));
        return ergebnis;
    }

    public CompletableFuture<Optional<BackupSnapshot>> status(UUID spieler) {
        return db.lesen(spieler);
    }
}
