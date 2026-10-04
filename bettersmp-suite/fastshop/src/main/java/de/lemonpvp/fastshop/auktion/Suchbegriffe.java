package de.lemonpvp.fastshop.auktion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Suchbegriffe {

    private static final Map<String, List<String>> WOERTER = new HashMap<>();
    private static final int LAENGSTES;

    static {
        eintragen("diamant", "diamond");
        eintragen("smaragd", "emerald");
        eintragen("netherit", "netherite");
        eintragen("eisen", "iron");
        eintragen("kupfer", "copper");
        eintragen("kohle", "coal");
        eintragen("erz", "ore");
        eintragen("barren", "ingot");
        eintragen("klumpen", "nugget");
        eintragen("roh", "raw");
        eintragen("quarz", "quartz");
        eintragen("antik", "ancient");
        eintragen("schrott", "scrap");
        eintragen("schwert", "sword");
        eintragen("spitzhacke", "pickaxe");
        eintragen("axt", "axe");
        eintragen("schaufel", "shovel");
        eintragen("hacke", "hoe");
        eintragen("bogen", "bow");
        eintragen("boegen", "bow");
        eintragen("armbrust", "crossbow");
        eintragen("pfeil", "arrow");
        eintragen("dreizack", "trident");
        eintragen("streitkolben", "mace");
        eintragen("schild", "shield", "sign");
        eintragen("namensschild", "name tag");
        eintragen("angel", "fishing rod");
        eintragen("schere", "shears");
        eintragen("feuerzeug", "flint and steel");
        eintragen("kompass", "compass");
        eintragen("uhr", "clock");
        eintragen("fernrohr", "spyglass");
        eintragen("pinsel", "brush");
        eintragen("eimer", "bucket");
        eintragen("wasser", "water");
        eintragen("milch", "milk");
        eintragen("helm", "helmet");
        eintragen("brustpanzer", "chestplate");
        eintragen("brustplatte", "chestplate");
        eintragen("hose", "leggings");
        eintragen("beinschutz", "leggings");
        eintragen("stiefel", "boots");
        eintragen("schuhe", "boots");
        eintragen("ruestung", "helmet", "chestplate", "leggings", "boots");
        eintragen("elytren", "elytra");
        eintragen("fluegel", "elytra");
        eintragen("schildkroete", "turtle");
        eintragen("apfel", "apple");
        eintragen("aepfel", "apple");
        eintragen("karotte", "carrot");
        eintragen("moehre", "carrot");
        eintragen("kartoffel", "potato");
        eintragen("brot", "bread");
        eintragen("rind", "beef");
        eintragen("steak", "beef");
        eintragen("schwein", "porkchop");
        eintragen("kotelett", "porkchop");
        eintragen("huhn", "chicken");
        eintragen("haehnchen", "chicken");
        eintragen("hammel", "mutton");
        eintragen("kaninchen", "rabbit");
        eintragen("hase", "rabbit");
        eintragen("fisch", "cod", "salmon", "fish");
        eintragen("lachs", "salmon");
        eintragen("kabeljau", "cod");
        eintragen("kuchen", "cake");
        eintragen("keks", "cookie");
        eintragen("melone", "melon");
        eintragen("kuerbis", "pumpkin");
        eintragen("beere", "berries");
        eintragen("suppe", "stew", "soup");
        eintragen("gebraten", "cooked");
        eintragen("zucker", "sugar");
        eintragen("zuckerrohr", "sugar cane");
        eintragen("weizen", "wheat");
        eintragen("samen", "seeds");
        eintragen("honig", "honey");
        eintragen("wabe", "honeycomb");
        eintragen("trank", "potion");
        eintragen("traenke", "potion");
        eintragen("verzauber", "enchant");
        eintragen("buch", "book");
        eintragen("buecher", "book");
        eintragen("erfahrung", "experience");
        eintragen("flasche", "bottle");
        eintragen("perle", "pearl");
        eintragen("auge", "eye");
        eintragen("lohen", "blaze");
        eintragen("rute", "rod");
        eintragen("staub", "powder", "dust");
        eintragen("traene", "tear");
        eintragen("schleim", "slime");
        eintragen("knochen", "bone");
        eintragen("faden", "string");
        eintragen("leder", "leather");
        eintragen("feder", "feather");
        eintragen("spinne", "spider");
        eintragen("schwarzpulver", "gunpowder");
        eintragen("feuerwerk", "firework");
        eintragen("rakete", "firework rocket");
        eintragen("stern", "star");
        eintragen("drache", "dragon");
        eintragen("kopf", "head");
        eintragen("koepfe", "head");
        eintragen("schaedel", "skull");
        eintragen("membran", "membrane");
        eintragen("tinte", "ink");
        eintragen("sattel", "saddle");
        eintragen("leine", "lead");
        eintragen("block", "block");
        eintragen("bloeck", "block");
        eintragen("fass", "barrel");
        eintragen("faesser", "barrel");
        eintragen("truhe", "chest");
        eintragen("kiste", "chest", "box");
        eintragen("shulker", "shulker");
        eintragen("ofen", "furnace");
        eintragen("schmelzofen", "blast furnace");
        eintragen("raeucherofen", "smoker");
        eintragen("werkbank", "crafting table");
        eintragen("amboss", "anvil");
        eintragen("zaubertisch", "enchanting table");
        eintragen("buecherregal", "bookshelf");
        eintragen("fackel", "torch");
        eintragen("laterne", "lantern");
        eintragen("glas", "glass");
        eintragen("wolle", "wool");
        eintragen("bett", "bed");
        eintragen("teppich", "carpet");
        eintragen("beton", "concrete");
        eintragen("keramik", "terracotta");
        eintragen("terrakotta", "terracotta");
        eintragen("bruchstein", "cobblestone");
        eintragen("stein", "stone");
        eintragen("kies", "gravel");
        eintragen("erde", "dirt");
        eintragen("gras", "grass");
        eintragen("holz", "log", "planks", "wood");
        eintragen("stamm", "log");
        eintragen("bretter", "planks");
        eintragen("eiche", "oak");
        eintragen("schwarzeiche", "dark oak");
        eintragen("birke", "birch");
        eintragen("fichte", "spruce");
        eintragen("tropenbaum", "jungle");
        eintragen("dschungel", "jungle");
        eintragen("akazie", "acacia");
        eintragen("kirsch", "cherry");
        eintragen("bambus", "bamboo");
        eintragen("endstein", "end stone");
        eintragen("tiefenschiefer", "deepslate");
        eintragen("granit", "granite");
        eintragen("diorit", "diorite");
        eintragen("andesit", "andesite");
        eintragen("schwarzstein", "blackstone");
        eintragen("leuchtstein", "glowstone");
        eintragen("prismarin", "prismarine");
        eintragen("rotstein", "redstone");
        eintragen("schnee", "snow");
        eintragen("eis", "ice");
        eintragen("packeis", "packed ice");
        eintragen("ton", "clay");
        eintragen("ziegel", "brick");
        eintragen("farbstoff", "dye");
        eintragen("farbe", "dye");
        eintragen("leuchtfeuer", "beacon");
        eintragen("trichter", "hopper");
        eintragen("spender", "dispenser");
        eintragen("werfer", "dropper");
        eintragen("kolben", "piston");
        eintragen("beobachter", "observer");
        eintragen("hebel", "lever");
        eintragen("knopf", "button");
        eintragen("druckplatte", "pressure plate");
        eintragen("falltuer", "trapdoor");
        eintragen("tuer", "door");
        eintragen("zaun", "fence");
        eintragen("zauntor", "fence gate");
        eintragen("treppe", "stairs");
        eintragen("stufe", "slab");
        eintragen("mauer", "wall");
        eintragen("leiter", "ladder");
        eintragen("schiene", "rail");
        eintragen("lore", "minecart");
        eintragen("boot", "boat");
        eintragen("kerze", "candle");
        eintragen("glocke", "bell");
        eintragen("fahne", "banner");
        eintragen("schallplatte", "music disc");
        eintragen("musikplatte", "music disc");
        eintragen("setzling", "sapling");
        eintragen("laub", "leaves");
        eintragen("pilz", "mushroom");
        eintragen("kaktus", "cactus");
        eintragen("seerose", "lily pad");
        eintragen("ranke", "vine");
        eintragen("rot", "red");
        eintragen("blau", "blue");
        eintragen("hellblau", "light blue");
        eintragen("gruen", "green");
        eintragen("hellgruen", "lime");
        eintragen("gelb", "yellow");
        eintragen("schwarz", "black");
        eintragen("weiss", "white");
        eintragen("grau", "gray");
        eintragen("hellgrau", "light gray");
        eintragen("lila", "purple");
        eintragen("violett", "purple");
        eintragen("rosa", "pink");
        eintragen("braun", "brown");
        eintragen("tuerkis", "cyan");
        LAENGSTES = WOERTER.keySet().stream().mapToInt(String::length).max().orElse(0);
    }

    private Suchbegriffe() {
    }

    private static void eintragen(String deutsch, String... englisch) {
        WOERTER.put(deutsch, List.of(englisch));
    }

    public static String normalisieren(String text) {
        return text.toLowerCase(Locale.ROOT).replace('_', ' ')
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
    }

    public static boolean passt(String suchText, String anfrage) {
        for (String wort : normalisieren(anfrage).trim().split("\\s+")) {
            if (wort.isEmpty() || suchText.contains(wort)) {
                continue;
            }
            List<List<String>> teile = zerlegen(wort);
            if (teile.isEmpty()) {
                return false;
            }
            for (List<String> moeglich : teile) {
                if (moeglich.stream().noneMatch(suchText::contains)) {
                    return false;
                }
            }
        }
        return true;
    }

    static List<List<String>> zerlegen(String wort) {
        List<List<String>> teile = new ArrayList<>();
        int position = 0;
        while (position < wort.length()) {
            List<String> treffer = null;
            int gefunden = 0;
            for (int laenge = Math.min(LAENGSTES, wort.length() - position); laenge >= 3; laenge--) {
                treffer = WOERTER.get(wort.substring(position, position + laenge));
                if (treffer != null) {
                    gefunden = laenge;
                    break;
                }
            }
            if (treffer == null) {
                position++;
            } else {
                teile.add(treffer);
                position += gefunden;
            }
        }
        return teile;
    }
}
