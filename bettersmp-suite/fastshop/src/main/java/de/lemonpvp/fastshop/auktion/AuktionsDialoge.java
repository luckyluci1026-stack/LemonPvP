package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AuktionsDialoge {

    private static final ClickCallback.Options EINMAL = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(10))
            .build();
    private static final String BEGRIFF = "begriff";
    private static final String PREIS = "preis";
    private static final int MAX_BEGRIFF = 32;

    private final FastShop plugin;

    public AuktionsDialoge(FastShop plugin) {
        this.plugin = plugin;
    }

    public void suche(Player spieler, AuktionsMenus.Ansicht ansicht) {
        DialogBase basis = DialogBase.builder(text("ah-dialog-search-title"))
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(text("ah-dialog-search-text"))))
                .inputs(List.of(DialogInput.text(BEGRIFF, text("ah-dialog-search-label"))
                        .initial(begriffBereinigen(ansicht.suche()))
                        .maxLength(MAX_BEGRIFF)
                        .build()))
                .build();
        ActionButton suchen = knopf("ah-dialog-search-button", (antwort, wer) -> spaeter(spieler, () ->
                plugin.auktionsMenus().uebersichtOeffnen(spieler, new AuktionsMenus.Ansicht(0, ansicht.sortierung(),
                        ansicht.filter(), begriffBereinigen(antwort.getText(BEGRIFF))))));
        ActionButton abbrechen = knopf("ah-dialog-cancel", (antwort, wer) -> spaeter(spieler, () ->
                plugin.auktionsMenus().uebersichtOeffnen(spieler, ansicht)));
        zeigen(spieler, basis, suchen, abbrechen);
    }

    public void preis(Player spieler, AuktionsMenus.Verkaufen menue) {
        AuktionsMenus.Ansicht ansicht = menue.ansicht();
        int slot = menue.slot();
        double alterPreis = menue.preis();
        boolean gesetzt = menue.preisGesetzt();
        ItemStack vorschau = menue.vorschau();
        List<DialogBody> inhalt = new ArrayList<>();
        if (vorschau != null) {
            inhalt.add(DialogBody.item(vorschau).build());
        }
        inhalt.add(DialogBody.plainMessage(text("ah-dialog-price-text")));
        DialogBase basis = DialogBase.builder(text("ah-dialog-price-title"))
                .canCloseWithEscape(true)
                .body(inhalt)
                .inputs(List.of(DialogInput.text(PREIS, text("ah-dialog-price-label"))
                        .initial(eingabeText(alterPreis))
                        .maxLength(20)
                        .build()))
                .build();
        ActionButton uebernehmen = knopf("ah-dialog-price-button", (antwort, wer) -> spaeter(spieler, () -> {
            String eingabe = antwort.getText(PREIS);
            double neu = eingabe == null ? -1 : AuktionsCommand.preisLesen(eingabe);
            AuktionsHaus haus = plugin.auktionen();
            if (neu <= 0) {
                haus.senden(spieler, "ah-invalid-price");
                plugin.auktionsMenus().verkaufenOeffnen(spieler, ansicht, slot, alterPreis, gesetzt);
                return;
            }
            if (neu < haus.minPreis() || neu > haus.maxPreis()) {
                haus.senden(spieler, "ah-price-range", "min", plugin.economy().format(haus.minPreis()),
                        "max", plugin.economy().format(haus.maxPreis()));
            }
            plugin.auktionsMenus().verkaufenOeffnen(spieler, ansicht, slot, neu, true);
        }));
        ActionButton abbrechen = knopf("ah-dialog-cancel", (antwort, wer) -> spaeter(spieler, () ->
                plugin.auktionsMenus().verkaufenOeffnen(spieler, ansicht, slot, alterPreis, gesetzt)));
        zeigen(spieler, basis, uebernehmen, abbrechen);
    }

    public static String begriffBereinigen(String eingabe) {
        if (eingabe == null) {
            return "";
        }
        String text = eingabe.replace("<", "").replace(">", "").replace("&", "").trim().replaceAll("\\s+", " ");
        return text.length() > MAX_BEGRIFF ? text.substring(0, MAX_BEGRIFF).trim() : text;
    }

    static String eingabeText(double preis) {
        if (preis == Math.rint(preis) && Math.abs(preis) < 1e15) {
            return String.valueOf((long) preis);
        }
        return String.format(Locale.ROOT, "%.2f", preis);
    }

    private void zeigen(Player spieler, DialogBase basis, ActionButton ja, ActionButton nein) {
        Dialog dialog = Dialog.create(fabrik -> fabrik.empty()
                .base(basis)
                .type(DialogType.confirmation(ja, nein)));
        spieler.closeInventory();
        spieler.showDialog(dialog);
    }

    private ActionButton knopf(String schluessel, DialogActionCallback aktion) {
        return ActionButton.builder(text(schluessel))
                .width(150)
                .action(DialogAction.customClick(aktion, EINMAL))
                .build();
    }

    private void spaeter(Player spieler, Runnable aufgabe) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (spieler.isOnline()) {
                aufgabe.run();
            }
        });
    }

    private Component text(String schluessel) {
        return plugin.msgs().format(schluessel);
    }
}
