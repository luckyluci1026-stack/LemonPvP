package de.lemonpvp.reportplus;

import de.lemonpvp.reportplus.command.BugFlow;
import de.lemonpvp.reportplus.command.BugReportCommand;
import de.lemonpvp.reportplus.command.ReportCommand;
import de.lemonpvp.reportplus.command.ReportFlow;
import de.lemonpvp.reportplus.command.StaffCommands;
import de.lemonpvp.reportplus.gui.GuiListener;
import de.lemonpvp.reportplus.listener.ChatCaptureListener;
import de.lemonpvp.reportplus.store.BugStore;
import de.lemonpvp.reportplus.store.ReportStore;
import de.lemonpvp.reportplus.util.Msgs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * GUI-gestuetztes /report (ersetzt BetterSMPs Text-Version, siehe
 * "loadbefore" in plugin.yml) und neues /bugreport - beides mit
 * Warteschlangen-GUIs fuers Team (/reports, /bugreports).
 *
 * Eigenstaendig: kein Abhaengen von BetterSMP im Code, nur dieselben
 * Rechte-Namen (bettersmp.report*) wiederverwendet, damit in LuckPerms
 * nichts nachgezogen werden muss.
 */
public final class ReportPlus extends JavaPlugin implements CommandExecutor {

    private Msgs msgs;
    private ReportStore reports;
    private BugStore bugs;
    private ReportFlow reportFlow;
    private BugFlow bugFlow;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.reports = new ReportStore(this);
        reports.load();
        this.bugs = new BugStore(this);
        bugs.load();
        this.reportFlow = new ReportFlow(this);
        this.bugFlow = new BugFlow(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new ChatCaptureListener(this), this);

        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("bugreport").setExecutor(new BugReportCommand(this));
        StaffCommands staffCommands = new StaffCommands(this);
        getCommand("reports").setExecutor(staffCommands);
        getCommand("bugreports").setExecutor(staffCommands);
        getCommand("reportplus").setExecutor(this);

        getLogger().info("ReportPlus aktiviert.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        reloadConfig();
        msgs.send(sender, "reloaded");
        return true;
    }

    public Msgs msgs() {
        return msgs;
    }

    public ReportStore reports() {
        return reports;
    }

    public BugStore bugs() {
        return bugs;
    }

    public ReportFlow reportFlow() {
        return reportFlow;
    }

    public BugFlow bugFlow() {
        return bugFlow;
    }
}
