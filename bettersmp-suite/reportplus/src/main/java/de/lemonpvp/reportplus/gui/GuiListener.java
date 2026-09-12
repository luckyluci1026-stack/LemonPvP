package de.lemonpvp.reportplus.gui;

import de.lemonpvp.reportplus.ReportPlus;
import de.lemonpvp.reportplus.store.BugEntry;
import de.lemonpvp.reportplus.store.ReportEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;
import java.util.UUID;

/** Ein Klick-Verteiler fuer alle GUI-Fenster aus {@link Guis}. */
public final class GuiListener implements Listener {

    private final ReportPlus plugin;

    public GuiListener(ReportPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(event.getWhoClicked() instanceof Player spieler)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        if (holder instanceof Guis.PlayerPickerHolder h) {
            event.setCancelled(true);
            UUID ziel = h.ziele.get(event.getSlot());
            if (ziel != null) {
                plugin.reportFlow().starteMitZiel(spieler, ziel);
            }
        } else if (holder instanceof Guis.KategoriePickerHolder h) {
            event.setCancelled(true);
            String option = h.optionen.get(event.getSlot());
            if (option != null) {
                spieler.closeInventory();
                h.beiAuswahl.accept(option);
            }
        } else if (holder instanceof Guis.ReportListHolder h) {
            event.setCancelled(true);
            handleReportListe(spieler, h, event.getSlot());
        } else if (holder instanceof Guis.ReportDetailHolder h) {
            event.setCancelled(true);
            handleReportDetail(spieler, h, event.getSlot());
        } else if (holder instanceof Guis.BugListHolder h) {
            event.setCancelled(true);
            handleBugListe(spieler, h, event.getSlot());
        } else if (holder instanceof Guis.BugDetailHolder h) {
            event.setCancelled(true);
            handleBugDetail(spieler, h, event.getSlot());
        }
    }

    private void handleReportListe(Player spieler, Guis.ReportListHolder holder, int slot) {
        if (slot == Guis.NAV_PREV) {
            Guis.oeffneReportListe(plugin, spieler, holder.page - 1);
            return;
        }
        if (slot == Guis.NAV_NEXT) {
            Guis.oeffneReportListe(plugin, spieler, holder.page + 1);
            return;
        }
        Integer id = holder.eintraege.get(slot);
        if (id == null) {
            return;
        }
        Optional<ReportEntry> eintrag = plugin.reports().get(id);
        eintrag.ifPresent(e -> Guis.oeffneReportDetail(plugin, spieler, e, holder.page));
    }

    private void handleReportDetail(Player spieler, Guis.ReportDetailHolder holder, int slot) {
        Optional<ReportEntry> vorhanden = plugin.reports().get(holder.id);
        if (vorhanden.isEmpty()) {
            plugin.msgs().send(spieler, "report.no-such-report");
            spieler.closeInventory();
            return;
        }
        ReportEntry eintrag = vorhanden.get();
        if (slot == Guis.ACT_BACK) {
            Guis.oeffneReportListe(plugin, spieler, holder.zurueckZuSeite);
        } else if (slot == Guis.ACT_TELEPORT) {
            Player ziel = Bukkit.getPlayer(eintrag.ziel());
            if (ziel == null) {
                plugin.msgs().send(spieler, "report.target-offline", "spieler", eintrag.zielName());
            } else {
                spieler.teleport(ziel.getLocation());
                plugin.msgs().send(spieler, "report.teleported", "spieler", eintrag.zielName());
            }
        } else if (slot == Guis.ACT_RESOLVE) {
            plugin.reports().setzeStatus(holder.id, "ERLEDIGT");
            plugin.msgs().send(spieler, "report.resolved", "id", String.valueOf(holder.id));
            Guis.oeffneReportListe(plugin, spieler, holder.zurueckZuSeite);
        } else if (slot == Guis.ACT_DISMISS) {
            plugin.reports().setzeStatus(holder.id, "VERWORFEN");
            plugin.msgs().send(spieler, "report.dismissed", "id", String.valueOf(holder.id));
            Guis.oeffneReportListe(plugin, spieler, holder.zurueckZuSeite);
        }
    }

    private void handleBugListe(Player spieler, Guis.BugListHolder holder, int slot) {
        if (slot == Guis.NAV_PREV) {
            Guis.oeffneBugListe(plugin, spieler, holder.page - 1);
            return;
        }
        if (slot == Guis.NAV_NEXT) {
            Guis.oeffneBugListe(plugin, spieler, holder.page + 1);
            return;
        }
        Integer id = holder.eintraege.get(slot);
        if (id == null) {
            return;
        }
        Optional<BugEntry> eintrag = plugin.bugs().get(id);
        eintrag.ifPresent(e -> Guis.oeffneBugDetail(plugin, spieler, e, holder.page));
    }

    private void handleBugDetail(Player spieler, Guis.BugDetailHolder holder, int slot) {
        if (plugin.bugs().get(holder.id).isEmpty()) {
            plugin.msgs().send(spieler, "bugreport.no-such-report");
            spieler.closeInventory();
            return;
        }
        if (slot == Guis.ACT_BACK) {
            Guis.oeffneBugListe(plugin, spieler, holder.zurueckZuSeite);
        } else if (slot == Guis.ACT_RESOLVE) {
            plugin.bugs().setzeStatus(holder.id, "ERLEDIGT");
            plugin.msgs().send(spieler, "bugreport.resolved", "id", String.valueOf(holder.id));
            Guis.oeffneBugListe(plugin, spieler, holder.zurueckZuSeite);
        } else if (slot == Guis.ACT_DISMISS) {
            plugin.bugs().setzeStatus(holder.id, "VERWORFEN");
            plugin.msgs().send(spieler, "bugreport.dismissed", "id", String.valueOf(holder.id));
            Guis.oeffneBugListe(plugin, spieler, holder.zurueckZuSeite);
        }
    }
}
