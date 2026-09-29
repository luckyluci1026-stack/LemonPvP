package de.lemonpvp.fastshop.auktion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public final class Angebot {

    public enum Status {
        AKTIV,
        ABGELAUFEN
    }

    private final UUID id;
    private final UUID verkaeufer;
    private final String verkaeuferName;
    private final ItemStack item;
    private final double preis;
    private final long erstellt;
    private final long endet;
    private final String suchText;
    private Status status;

    public Angebot(UUID id, UUID verkaeufer, String verkaeuferName, ItemStack item, double preis,
                   long erstellt, long endet, Status status) {
        this.id = id;
        this.verkaeufer = verkaeufer;
        this.verkaeuferName = verkaeuferName;
        this.item = item.clone();
        this.preis = preis;
        this.erstellt = erstellt;
        this.endet = endet;
        this.status = status;
        this.suchText = Suchbegriffe.normalisieren(item.getType().name() + " " + anzeigeName(item) + " " + verkaeuferName);
    }

    private static String anzeigeName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "";
        }
        Component name = meta.displayName();
        return name == null ? "" : PlainTextComponentSerializer.plainText().serialize(name);
    }

    public UUID id() {
        return id;
    }

    public UUID verkaeufer() {
        return verkaeufer;
    }

    public String verkaeuferName() {
        return verkaeuferName;
    }

    public ItemStack item() {
        return item.clone();
    }

    public Material typ() {
        return item.getType();
    }

    public int menge() {
        return item.getAmount();
    }

    public boolean passtZu(String begriff) {
        return Suchbegriffe.passt(suchText, begriff);
    }

    public double preis() {
        return preis;
    }

    public long erstellt() {
        return erstellt;
    }

    public long endet() {
        return endet;
    }

    public Status status() {
        return status;
    }

    public void ablaufen() {
        this.status = Status.ABGELAUFEN;
    }

    public boolean kaufbar(long jetzt) {
        return status == Status.AKTIV && jetzt < endet;
    }
}
