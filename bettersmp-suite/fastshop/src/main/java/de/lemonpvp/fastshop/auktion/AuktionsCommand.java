package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AuktionsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> UNTERBEFEHLE = List.of("sell", "search", "meine");

    private final FastShop plugin;

    public AuktionsCommand(FastShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        AuktionsMenus menus = plugin.auktionsMenus();
        if (args.length == 0) {
            menus.uebersichtOeffnen(spieler, AuktionsMenus.Ansicht.start());
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell", "verkaufen", "list" -> verkaufen(spieler, args);
            case "search", "suche", "suchen" -> {
                String begriff = AuktionsDialoge.begriffBereinigen(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                if (begriff.isEmpty()) {
                    plugin.auktionsDialoge().suche(spieler, AuktionsMenus.Ansicht.start());
                } else {
                    menus.uebersichtOeffnen(spieler, suche(begriff));
                }
            }
            case "meine", "angebote", "own" -> menus.eigeneOeffnen(spieler, AuktionsMenus.Ansicht.start());
            default -> menus.uebersichtOeffnen(spieler, suche(AuktionsDialoge.begriffBereinigen(String.join(" ", args))));
        }
        return true;
    }

    private static AuktionsMenus.Ansicht suche(String begriff) {
        return new AuktionsMenus.Ansicht(0, Sortierung.NEUESTE, Filter.ALLE, begriff);
    }

    private void verkaufen(Player spieler, String[] args) {
        AuktionsHaus haus = plugin.auktionen();
        int slot = spieler.getInventory().getHeldItemSlot();
        if (args.length < 2) {
            plugin.auktionsMenus().verkaufenOeffnen(spieler, AuktionsMenus.Ansicht.start(), slot, 0, false);
            return;
        }
        double preis = preisLesen(args[1]);
        if (preis <= 0) {
            haus.senden(spieler, "ah-invalid-price");
            plugin.auktionsMenus().verkaufenOeffnen(spieler, AuktionsMenus.Ansicht.start(), slot, 0, false);
            return;
        }
        if (preis < haus.minPreis() || preis > haus.maxPreis()) {
            haus.senden(spieler, "ah-price-range",
                    "min", plugin.economy().format(haus.minPreis()), "max", plugin.economy().format(haus.maxPreis()));
        }
        plugin.auktionsMenus().verkaufenOeffnen(spieler, AuktionsMenus.Ansicht.start(), slot, preis, true);
    }

    public static double preisLesen(String eingabe) {
        String text = eingabe.trim().toLowerCase(Locale.ROOT)
                .replace("$", "").replace("€", "").replace("_", "").replace(" ", "");
        if (text.isEmpty()) {
            return -1;
        }
        double faktor = 1;
        char letztes = text.charAt(text.length() - 1);
        if (letztes == 'k' || letztes == 'm' || letztes == 'b') {
            faktor = letztes == 'k' ? 1_000 : letztes == 'm' ? 1_000_000 : 1_000_000_000;
            text = text.substring(0, text.length() - 1);
        }
        text = trennzeichenBereinigen(text, faktor == 1);
        try {
            double wert = Double.parseDouble(text) * faktor;
            return Double.isFinite(wert) && wert > 0 ? Math.round(wert * 100.0) / 100.0 : -1;
        } catch (NumberFormatException fehler) {
            return -1;
        }
    }

    private static String trennzeichenBereinigen(String text, boolean ohneEinheit) {
        int kommas = text.length() - text.replace(",", "").length();
        int punkte = text.length() - text.replace(".", "").length();
        if (kommas > 0 && punkte > 0) {
            return text.lastIndexOf('.') > text.lastIndexOf(',')
                    ? text.replace(",", "")
                    : text.replace(".", "").replace(',', '.');
        }
        if (kommas > 1) {
            return text.replace(",", "");
        }
        if (punkte > 1) {
            return text.replace(".", "");
        }
        String trenner = kommas == 1 ? "," : punkte == 1 ? "." : null;
        if (trenner == null) {
            return text;
        }
        int nachkommastellen = text.length() - text.indexOf(trenner) - 1;
        if (ohneEinheit && nachkommastellen == 3) {
            return text.replace(trenner, "");
        }
        return text.replace(trenner, ".");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> vorschlaege = new ArrayList<>();
        if (args.length == 1) {
            for (String unterbefehl : UNTERBEFEHLE) {
                if (unterbefehl.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    vorschlaege.add(unterbefehl);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            vorschlaege.addAll(List.of("1000", "10k", "1m"));
        }
        return vorschlaege;
    }
}
