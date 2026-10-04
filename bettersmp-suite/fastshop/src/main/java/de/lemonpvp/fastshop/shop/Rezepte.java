package de.lemonpvp.fastshop.shop;

import io.papermc.paper.registry.TypedKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.TransmuteRecipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Rezepte {

    private Rezepte() {
    }

    public static List<WertRechner.Rezept> ausDemServer() {
        List<WertRechner.Rezept> liste = new ArrayList<>();
        Iterator<Recipe> alle;
        try {
            alle = Bukkit.recipeIterator();
        } catch (RuntimeException fehler) {
            return liste;
        }
        if (alle == null) {
            return liste;
        }
        while (alle.hasNext()) {
            try {
                WertRechner.Rezept rezept = umwandeln(alle.next());
                if (rezept != null) {
                    liste.add(rezept);
                }
            } catch (RuntimeException uebersprungen) {
                continue;
            }
        }
        return liste;
    }

    static WertRechner.Rezept umwandeln(Recipe recipe) {
        if (recipe == null) {
            return null;
        }
        ItemStack ergebnis = recipe.getResult();
        if (ergebnis == null || ergebnis.getType().isAir() || ergebnis.getAmount() <= 0) {
            return null;
        }
        List<RecipeChoice> zutaten = new ArrayList<>();
        if (recipe instanceof ShapedRecipe geformt) {
            Map<Character, RecipeChoice> karte = geformt.getChoiceMap();
            for (String zeile : geformt.getShape()) {
                for (char zeichen : zeile.toCharArray()) {
                    RecipeChoice auswahl = karte.get(zeichen);
                    if (auswahl != null) {
                        zutaten.add(auswahl);
                    }
                }
            }
        } else if (recipe instanceof ShapelessRecipe formlos) {
            zutaten.addAll(formlos.getChoiceList());
        } else if (recipe instanceof CookingRecipe<?> kochen) {
            zutaten.add(kochen.getInputChoice());
        } else if (recipe instanceof StonecuttingRecipe saege) {
            zutaten.add(saege.getInputChoice());
        } else if (recipe instanceof SmithingTransformRecipe schmied) {
            zutaten.add(schmied.getTemplate());
            zutaten.add(schmied.getBase());
            zutaten.add(schmied.getAddition());
        } else if (recipe instanceof TransmuteRecipe umwandlung) {
            zutaten.add(umwandlung.getInput());
            zutaten.add(umwandlung.getMaterial());
        } else {
            return null;
        }
        List<List<Material>> optionen = new ArrayList<>();
        for (RecipeChoice auswahl : zutaten) {
            if (auswahl == null) {
                continue;
            }
            List<Material> materialien = optionen(auswahl);
            if (materialien == null) {
                continue;
            }
            if (materialien.isEmpty()) {
                return null;
            }
            optionen.add(materialien);
        }
        if (optionen.isEmpty()) {
            return null;
        }
        return new WertRechner.Rezept(ergebnis.getType(), ergebnis.getAmount(), optionen);
    }

    static List<Material> optionen(RecipeChoice auswahl) {
        List<Material> liste = new ArrayList<>();
        if (auswahl instanceof RecipeChoice.ItemTypeChoice typen) {
            for (TypedKey<ItemType> schluessel : typen.itemTypes().values()) {
                Material material = Material.matchMaterial(schluessel.key().asString());
                if (material != null) {
                    liste.add(material);
                }
            }
        } else if (auswahl instanceof RecipeChoice.MaterialChoice material) {
            liste.addAll(material.getChoices());
        } else if (auswahl instanceof RecipeChoice.ExactChoice genau) {
            for (ItemStack stack : genau.getChoices()) {
                liste.add(stack.getType());
            }
        } else {
            return null;
        }
        liste.removeIf(material -> material == null || material.isAir());
        return liste;
    }
}
