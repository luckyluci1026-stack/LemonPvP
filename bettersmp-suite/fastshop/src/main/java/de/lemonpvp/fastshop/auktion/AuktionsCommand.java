package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        AuktionsHaus haus = plugin.auktionen();
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (args.length == 0) {
            plugin.auktionsMenus().uebersichtOeffnen(spieler, AuktionsMenus.Ansicht.start());
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell", "verkaufen", "list" -> anbieten(spieler, args);
            case "search", "suche", "suchen" -> {
                if (args.length < 2) {
                    haus.senden(spieler, "ah-search-hint");
                    return true;
                }
                String begriff = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                int treffer = haus.kaufbare(Sortierung.NEUESTE, Filter.ALLE, begriff).size();
                haus.senden(spieler, "ah-search", "query", begriff.replace("<", "").replace(">", ""),
                        "count", String.valueOf(treffer));
                plugin.auktionsMenus().uebersichtOeffnen(spieler, new AuktionsMenus.Ansicht(0, Sortierung.NEUESTE, Filter.ALLE, begriff));
            }
            case "meine", "angebote", "own" -> plugin.auktionsMenus().eigeneOeffnen(spieler, AuktionsMenus.Ansicht.start());
            default -> haus.senden(spieler, "ah-usage");
        }
        return true;
    }

    private void anbieten(Player spieler, String[] args) {
        AuktionsHaus haus = plugin.auktionen();
        if (args.length < 2) {
            haus.senden(spieler, "ah-sell-hint", "hours", String.valueOf(haus.dauerStunden()));
            return;
        }
        double preis = preisLesen(args[1]);
        if (preis <= 0) {
            haus.senden(spieler, "ah-invalid-price");
            return;
        }
        ItemStack hand = spieler.getInventory().getItemInMainHand().clone();
        switch (haus.anbieten(spieler, preis)) {
            case ANGEBOTEN -> haus.senden(spieler, "ah-listed", "amount", String.valueOf(hand.getAmount()),
                    "item", AuktionsHaus.itemText(hand), "price", plugin.economy().format(preis),
                    "hours", String.valueOf(haus.dauerStunden()));
            case KEINE_WIRTSCHAFT -> haus.senden(spieler, "ah-no-economy");
            case NICHTS_IN_DER_HAND -> haus.senden(spieler, "ah-nothing-in-hand");
            case PREIS_AUSSERHALB -> haus.senden(spieler, "ah-price-range",
                    "min", plugin.economy().format(haus.minPreis()), "max", plugin.economy().format(haus.maxPreis()));
            case ZU_VIELE -> haus.senden(spieler, "ah-limit", "max", String.valueOf(haus.maxAngebote()));
        }
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
