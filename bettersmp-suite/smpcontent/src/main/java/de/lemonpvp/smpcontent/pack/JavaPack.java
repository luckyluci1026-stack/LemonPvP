package de.lemonpvp.smpcontent.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Liest ein fertiges Java-Resourcepack (die ZIP aus der server.properties) und
 * sucht darin alles heraus, was eigene Inhalte sind:
 *
 *   Items   - jedes Modell hinter einem custom_model_data
 *             (neues Format assets/minecraft/items/*.json ab 1.21.4
 *              und das alte "overrides" in assets/minecraft/models/item/*.json)
 *   Blöcke  - jede Variante in assets/minecraft/blockstates/note_block.json,
 *             die nicht auf das Vanilla-Modell zeigt
 *
 * Elternmodelle werden mitgelesen und zusammengeführt, damit auch Modelle
 * funktionieren, die ihre Quader oder Texturen von einem parent erben.
 */
public final class JavaPack {

    /**
     * Ein Item aus dem Pack.
     *
     * @param material Grundmaterial, oder <b>null</b> bei Items, die über
     *                 "item_model" laufen - dort steht es nicht im Pack und
     *                 wird aus der config.yml nachgeschlagen.
     */
    public record Item(String id, String material, int modelData,
                       JsonObject model, Path texture) {
    }

    /** Ein eigener Block: der Java-Blockzustand, das Modell und die Textur. */
    public record Block(String state, String id, JsonObject model, Path texture) {
    }

    private final Path root;
    private final Map<String, JsonObject> modelCache = new HashMap<>();
    private final List<String> notes = new ArrayList<>();

    private JavaPack(Path root) {
        this.root = root;
    }

    /** Entpackt die ZIP und öffnet sie zum Lesen. */
    public static JavaPack open(Path zip, Path work) throws IOException {
        PackGenerator.deleteTree(work);
        Files.createDirectories(work);
        extract(zip, work);
        // Manche Packs liegen in einem Unterordner ("MeinPack/assets/...")
        Path base = work;
        if (!Files.isDirectory(base.resolve("assets"))) {
            try (var list = Files.list(work)) {
                for (Path candidate : list.toList()) {
                    if (Files.isDirectory(candidate.resolve("assets"))) {
                        base = candidate;
                        break;
                    }
                }
            }
        }
        if (!Files.isDirectory(base.resolve("assets"))) {
            throw new IOException("Das ist kein Resourcepack - assets/ fehlt");
        }
        return new JavaPack(base);
    }

    /** Was beim Lesen aufgefallen ist (fehlende Texturen usw.). */
    public List<String> notes() {
        return notes;
    }

    // ---------------- Items ----------------

    public List<Item> items() {
        List<Item> out = new ArrayList<>();
        Map<String, Integer> used = new HashMap<>();
        // Packs für 1.21.4+ haben oft beide Formate, damit ältere Clients auch
        // etwas sehen. Dann darf jedes Item trotzdem nur einmal herauskommen.
        Set<String> seen = new HashSet<>();

        // Neues Format ab 1.21.4: assets/minecraft/items/<material>.json
        forEachJson(root.resolve("assets/minecraft/items"), path -> {
            String material = stripJson(path.getFileName().toString());
            JsonObject definition = readJson(path);
            if (definition == null) {
                return;
            }
            Map<Integer, String> found = new TreeMap<>();
            collectRangeDispatch(definition, found);
            found.forEach((data, ref) -> add(out, used, seen, material, data, ref));
        });

        // Der moderne Weg ab 1.21.4: das Item zeigt über die Komponente
        // "item_model" auf assets/<namespace>/items/<id>.json. Da steht dann
        // kein custom_model_data mehr drin - welches Grundmaterial dahinter
        // steckt, weiß nur das Plugin. Darum bleibt material hier null und
        // wird später aus der config.yml nachgeschlagen.
        Path assets = root.resolve("assets");
        if (Files.isDirectory(assets)) {
            try (var namespaces = Files.list(assets)) {
                for (Path namespace : namespaces.filter(Files::isDirectory).sorted().toList()) {
                    if (namespace.getFileName().toString().equals("minecraft")) {
                        continue;
                    }
                    forEachJson(namespace.resolve("items"), path -> {
                        String name = stripJson(path.getFileName().toString());
                        JsonObject definition = readJson(path);
                        if (definition == null || !seen.add("model#" + name)) {
                            return;
                        }
                        String ref = firstModelRef(definition, 0);
                        if (ref == null) {
                            return;
                        }
                        JsonObject model = model(ref);
                        Path texture = model == null ? null : texture(model);
                        if (texture == null) {
                            notes.add("Textur fehlt: " + ref);
                            return;
                        }
                        out.add(new Item(name(ref), null, 0, model, texture));
                    });
                }
            } catch (IOException ignored) {
                // Kein assets/ lesbar - dann gibt es hier eben nichts
            }
        }

        // Altes Format: overrides in assets/minecraft/models/item/<material>.json
        forEachJson(root.resolve("assets/minecraft/models/item"), path -> {
            String material = stripJson(path.getFileName().toString());
            JsonObject model = readJson(path);
            if (model == null || !model.has("overrides")) {
                return;
            }
            for (JsonElement element : model.getAsJsonArray("overrides")) {
                JsonObject override = element.getAsJsonObject();
                JsonObject predicate = override.getAsJsonObject("predicate");
                if (predicate == null || !predicate.has("custom_model_data")
                        || !override.has("model")) {
                    continue;
                }
                int data = predicate.get("custom_model_data").getAsInt();
                add(out, used, seen, material, data, override.get("model").getAsString());
            }
        });
        return out;
    }

    /**
     * Sucht im Item-Definitionsbaum jede Weiche auf custom_model_data. Die kann
     * auch tief drin stecken (z.B. unter minecraft:condition), darum rekursiv.
     */
    private void collectRangeDispatch(JsonElement element, Map<Integer, String> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectRangeDispatch(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        String property = object.has("property") ? object.get("property").getAsString() : "";
        if (type.endsWith("range_dispatch") && property.endsWith("custom_model_data")
                && object.has("entries")) {
            for (JsonElement entry : object.getAsJsonArray("entries")) {
                JsonObject on = entry.getAsJsonObject();
                if (!on.has("threshold") || !on.has("model")) {
                    continue;
                }
                String ref = firstModelRef(on.get("model"), 0);
                if (ref != null) {
                    out.putIfAbsent(on.get("threshold").getAsInt(), ref);
                }
            }
        }
        for (String key : object.keySet()) {
            collectRangeDispatch(object.get(key), out);
        }
    }

    /** Aus einem verschachtelten Modell-Knoten den ersten echten Modellpfad holen. */
    private String firstModelRef(JsonElement element, int depth) {
        if (depth > 8) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String ref = firstModelRef(child, depth + 1);
                if (ref != null) {
                    return ref;
                }
            }
            return null;
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("model") && object.get("model").isJsonPrimitive()) {
            return object.get("model").getAsString();
        }
        for (String key : object.keySet()) {
            String ref = firstModelRef(object.get(key), depth + 1);
            if (ref != null) {
                return ref;
            }
        }
        return null;
    }

    private void add(List<Item> out, Map<String, Integer> used, Set<String> seen,
                     String material, int data, String ref) {
        if (ref == null || ref.isBlank() || ref.startsWith("builtin/")) {
            return;
        }
        if (!seen.add(material + "#" + data)) {
            return;
        }
        JsonObject model = model(ref);
        if (model == null) {
            notes.add("Modell fehlt: " + ref);
            return;
        }
        Path texture = texture(model);
        if (texture == null) {
            notes.add("Textur fehlt: " + ref);
            return;
        }
        out.add(new Item(unique(used, name(ref)), material, data, model, texture));
    }

    // ---------------- Blöcke ----------------

    public List<Block> blocks() {
        List<Block> out = new ArrayList<>();
        Map<String, Integer> used = new HashMap<>();
        JsonObject states = readJson(root.resolve("assets/minecraft/blockstates/note_block.json"));
        if (states == null || !states.has("variants")) {
            return out;
        }
        JsonObject variants = states.getAsJsonObject("variants");
        for (String key : variants.keySet()) {
            JsonElement value = variants.get(key);
            if (value.isJsonArray()) {
                JsonArray array = value.getAsJsonArray();
                if (array.isEmpty()) {
                    continue;
                }
                value = array.get(0);
            }
            if (!value.isJsonObject() || !value.getAsJsonObject().has("model")) {
                continue;
            }
            String ref = value.getAsJsonObject().get("model").getAsString();
            // Vanilla-Notenblöcke bleiben Notenblöcke
            if (ref.endsWith("block/note_block") && namespaceOf(ref).equals("minecraft")) {
                continue;
            }
            String state = state(key);
            if (state == null) {
                continue;
            }
            JsonObject model = model(ref);
            if (model == null) {
                notes.add("Blockmodell fehlt: " + ref);
                continue;
            }
            Path texture = texture(model);
            if (texture == null) {
                notes.add("Blocktextur fehlt: " + ref);
                continue;
            }
            out.add(new Block(state, unique(used, name(ref)), model, texture));
        }
        return out;
    }

    /**
     * "note=1,instrument=bit" -&gt; "minecraft:note_block[instrument=bit,note=1,powered=false]".
     * Die Eigenschaften müssen sortiert und vollständig sein, sonst findet
     * Geyser den Zustand nicht.
     */
    static String state(String key) {
        Map<String, String> properties = new TreeMap<>();
        for (String part : key.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && !kv[0].isBlank()) {
                properties.put(kv[0].trim(), kv[1].trim());
            }
        }
        if (!properties.containsKey("instrument") || !properties.containsKey("note")) {
            return null;
        }
        properties.putIfAbsent("powered", "false");
        StringBuilder out = new StringBuilder("minecraft:note_block[");
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.append(']').toString();
    }

    // ---------------- Modelle & Texturen ----------------

    /** Modell samt aller Elternmodelle, zusammengeführt. */
    public JsonObject model(String ref) {
        return model(ref, 0);
    }

    private JsonObject model(String ref, int depth) {
        if (depth > 8) {
            return null;
        }
        JsonObject cached = modelCache.get(ref);
        if (cached != null) {
            return cached;
        }
        JsonObject model = readJson(pathOf(ref, "models", ".json"));
        if (model == null) {
            return null;
        }
        if (model.has("parent")) {
            String parent = model.get("parent").getAsString();
            if (!parent.contains("builtin/")) {
                JsonObject base = model(parent, depth + 1);
                if (base != null) {
                    model = merge(base, model);
                }
            }
        }
        modelCache.put(ref, model);
        return model;
    }

    /** Kind gewinnt, geerbte Quader und Texturen bleiben erhalten. */
    private static JsonObject merge(JsonObject parent, JsonObject child) {
        JsonObject out = parent.deepCopy();
        for (String key : child.keySet()) {
            if (key.equals("textures") && out.has("textures")) {
                JsonObject textures = out.getAsJsonObject("textures");
                JsonObject own = child.getAsJsonObject("textures");
                for (String texture : own.keySet()) {
                    textures.add(texture, own.get(texture));
                }
            } else {
                out.add(key, child.get(key));
            }
        }
        out.remove("parent");
        return out;
    }

    /** Die Textur, die das Modell am meisten benutzt - die zeigt Bedrock an. */
    public Path texture(JsonObject model) {
        JsonObject textures = model.getAsJsonObject("textures");
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        String key = null;
        if (model.has("elements")) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (JsonElement element : model.getAsJsonArray("elements")) {
                JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                if (faces == null) {
                    continue;
                }
                for (String face : faces.keySet()) {
                    JsonObject data = faces.getAsJsonObject(face);
                    if (data.has("texture")) {
                        counts.merge(data.get("texture").getAsString(), 1, Integer::sum);
                    }
                }
            }
            key = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
        if (key == null) {
            key = textures.has("layer0") ? "#layer0"
                    : "#" + textures.keySet().iterator().next();
        }

        String value = key;
        for (int i = 0; i < 8 && value != null && value.startsWith("#"); i++) {
            JsonElement next = textures.get(value.substring(1));
            value = next != null && next.isJsonPrimitive() ? next.getAsString() : null;
        }
        if (value == null || value.startsWith("#")) {
            return null;
        }
        Path path = pathOf(value, "textures", ".png");
        return Files.isRegularFile(path) ? path : null;
    }

    /** "smp:item/foo" -&gt; assets/smp/&lt;kind&gt;/item/foo&lt;suffix&gt; */
    private Path pathOf(String ref, String kind, String suffix) {
        String namespace = namespaceOf(ref);
        String path = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        return root.resolve("assets").resolve(namespace).resolve(kind)
                .resolve(path.replace('\\', '/') + suffix);
    }

    private static String namespaceOf(String ref) {
        int colon = ref.indexOf(':');
        return colon > 0 ? ref.substring(0, colon) : "minecraft";
    }

    /** Aus "smp:item/laserschwert_rot" wird "laserschwert_rot". */
    static String name(String ref) {
        String path = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        int slash = path.lastIndexOf('/');
        String plain = slash >= 0 ? path.substring(slash + 1) : path;
        StringBuilder out = new StringBuilder();
        for (char c : plain.toLowerCase(Locale.ROOT).toCharArray()) {
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '-' ? c : '_');
        }
        String result = out.toString();
        return result.isBlank() ? "item" : result;
    }

    private static String unique(Map<String, Integer> used, String id) {
        int count = used.merge(id, 1, Integer::sum);
        return count == 1 ? id : id + "_" + count;
    }

    // ---------------- Dateien ----------------

    private void forEachJson(Path folder, Consumer<Path> action) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (var list = Files.list(folder)) {
            list.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(action);
        } catch (IOException ignored) {
            // Ordner nicht lesbar: dann gibt es hier eben nichts
        }
    }

    private JsonObject readJson(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(
                    Files.readString(path, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception ex) {
            notes.add(path.getFileName() + " ist kaputt: " + ex.getMessage());
            return null;
        }
    }

    private static String stripJson(String fileName) {
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    /** Entpackt eine ZIP; Einträge, die aus dem Ordner ausbrechen, fliegen raus. */
    static void extract(Path zip, Path target) throws IOException {
        Path base = target.toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path out = base.resolve(entry.getName()).normalize();
                if (!out.startsWith(base)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                copy(in, out);
            }
        }
    }

    private static void copy(InputStream in, Path out) throws IOException {
        try (var stream = Files.newOutputStream(out)) {
            in.transferTo(stream);
        }
    }
}
