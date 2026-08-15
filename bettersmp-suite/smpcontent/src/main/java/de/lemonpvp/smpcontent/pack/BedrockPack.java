package de.lemonpvp.smpcontent.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Baut das Bedrock-Pack, damit Bedrock-Spieler dieselben Items sehen wie
 * Java-Spieler - inklusive der 3D-Modelle aus Blockbench.
 *
 * Geyser braucht dafür zwei Dinge, und beide entstehen hier:
 *
 *   output/bedrock/SMP-Bedrock-Pack.mcpack  ->  Geyser/packs/
 *   output/geyser/smp_items.json            ->  Geyser/custom_mappings/
 *
 * Bedrock benutzt ein anderes Koordinatensystem als Java: die X-Achse ist
 * gespiegelt und der Nullpunkt liegt in der Mitte des Blocks. Genau das
 * rechnet {@link #geometry} um, dazu die UV-Koordinaten auf die echte
 * Texturgröße und die Drehungen mit passendem Vorzeichen.
 *
 * Eigene BLÖCKE gehen nicht: Note-Block-Zustände lassen sich nicht als
 * Geyser-Custom-Item abbilden. Bedrock zeigt dort einen normalen Notenblock.
 */
public final class BedrockPack {

    /** Java-Seite -> Bedrock-Seite. Ost und West tauschen wegen der Spiegelung. */
    private static final Map<String, String> FACES = Map.of(
            "north", "north", "south", "south", "east", "west",
            "west", "east", "up", "up", "down", "down");

    private final SMPContent plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public BedrockPack(SMPContent plugin) {
        this.plugin = plugin;
    }

    /** Wie viele Items dabei herauskamen. */
    public record Result(int solid, int flat, boolean ok, String message) {
    }

    public Result build(Path outputDir, Path texturesDir, Path modelsDir) {
        Path work = plugin.getDataFolder().toPath().resolve("build-bedrock");
        int solid = 0;
        int flat = 0;
        try {
            deleteRecursively(work);
            Files.createDirectories(work.resolve("textures/items"));

            JsonObject textureData = new JsonObject();
            Map<String, JsonArray> mappings = new LinkedHashMap<>();

            for (CustomEntry entry : plugin.registry().entries().values()) {
                if (entry.block()) {
                    continue;
                }
                Path texture = texturesDir.resolve("item").resolve(entry.id() + ".png");
                if (!Files.exists(texture)) {
                    continue;
                }
                Files.copy(texture, work.resolve("textures/items/" + entry.id() + ".png"),
                        StandardCopyOption.REPLACE_EXISTING);
                JsonObject tex = new JsonObject();
                tex.addProperty("textures", "textures/items/" + entry.id());
                textureData.add(entry.id(), tex);

                JsonObject model = readModel(modelsDir.resolve("item")
                        .resolve(entry.id() + ".json"));
                boolean has3d = model != null && model.has("elements")
                        && !model.getAsJsonArray("elements").isEmpty();
                if (has3d) {
                    writeThreeD(work, entry.id(), model, texture);
                    solid++;
                } else {
                    flat++;
                }
                mappings.computeIfAbsent("minecraft:" + entry.material().getKey().getKey(),
                        key -> new JsonArray()).add(mapping(entry, has3d));
            }

            JsonObject itemTexture = new JsonObject();
            itemTexture.addProperty("resource_pack_name", namespace());
            itemTexture.addProperty("texture_name", "atlas.items");
            itemTexture.add("texture_data", textureData);
            write(work.resolve("textures/item_texture.json"), itemTexture);
            write(work.resolve("manifest.json"), manifest());

            Files.createDirectories(outputDir.resolve("bedrock"));
            Path mcpack = outputDir.resolve("bedrock/SMP-Bedrock-Pack.mcpack");
            PackGenerator.zip(work, mcpack);
            deleteRecursively(work);

            JsonObject items = new JsonObject();
            mappings.forEach(items::add);
            JsonObject root = new JsonObject();
            root.addProperty("format_version", "2");
            root.add("items", items);
            write(outputDir.resolve("geyser/smp_items.json"), root);

            return new Result(solid, flat, true, mcpack.getFileName().toString());
        } catch (Exception ex) {
            plugin.getLogger().warning("Bedrock-Pack fehlgeschlagen: " + ex);
            return new Result(solid, flat, false, String.valueOf(ex.getMessage()));
        }
    }

    // ------------------------------------------------------------------

    private void writeThreeD(Path work, String id, JsonObject model, Path texture)
            throws IOException {
        String identifier = namespace() + "_" + id;
        int[] size = textureSize(model, texture);
        write(work.resolve("models/entity/" + identifier + ".geo.json"),
                geometry(model, identifier, size[0], size[1]));
        write(work.resolve("attachables/" + identifier + ".attachable.json"),
                attachable(id, identifier));
        write(work.resolve("animations/" + identifier + ".animation.json"),
                animations(identifier, model.getAsJsonObject("display")));
    }

    /** Rechnet die Quader eines Java-Modells in Bedrock-Geometrie um. */
    private JsonObject geometry(JsonObject model, String identifier, int texW, int texH) {
        JsonArray cubes = new JsonArray();
        for (var element : model.getAsJsonArray("elements")) {
            JsonObject el = element.getAsJsonObject();
            double[] from = triple(el.getAsJsonArray("from"));
            double[] to = triple(el.getAsJsonArray("to"));

            JsonObject cube = new JsonObject();
            // Bedrock spiegelt X und hat den Nullpunkt in der Mitte
            cube.add("origin", array(-to[0] + 8, from[1], from[2] - 8));
            cube.add("size", array(to[0] - from[0], to[1] - from[1], to[2] - from[2]));

            JsonObject rot = el.getAsJsonObject("rotation");
            if (rot != null && rot.has("angle") && rot.get("angle").getAsDouble() != 0) {
                double angle = rot.get("angle").getAsDouble();
                String axis = rot.has("axis") ? rot.get("axis").getAsString() : "y";
                double[] pivot = rot.has("origin")
                        ? triple(rot.getAsJsonArray("origin")) : new double[]{8, 8, 8};
                cube.add("pivot", array(-pivot[0] + 8, pivot[1], pivot[2] - 8));
                cube.add("rotation", switch (axis) {
                    case "x" -> array(angle, 0, 0);
                    case "z" -> array(0, 0, -angle);
                    default -> array(0, -angle, 0);
                });
            }

            JsonObject faces = el.getAsJsonObject("faces");
            if (faces != null) {
                JsonObject uvOut = new JsonObject();
                for (String javaFace : faces.keySet()) {
                    String target = FACES.get(javaFace);
                    JsonObject face = faces.getAsJsonObject(javaFace);
                    if (target == null || !face.has("uv")) {
                        continue;
                    }
                    JsonArray uv = face.getAsJsonArray("uv");
                    double u1 = uv.get(0).getAsDouble();
                    double v1 = uv.get(1).getAsDouble();
                    double u2 = uv.get(2).getAsDouble();
                    double v2 = uv.get(3).getAsDouble();
                    double sx = texW / 16.0;
                    double sy = texH / 16.0;
                    JsonObject out = new JsonObject();
                    out.add("uv", array(Math.min(u1, u2) * sx, Math.min(v1, v2) * sy));
                    out.add("uv_size", array(Math.abs(u2 - u1) * sx, Math.abs(v2 - v1) * sy));
                    uvOut.add(target, out);
                }
                if (!uvOut.isEmpty()) {
                    cube.add("uv", uvOut);
                }
            }
            cubes.add(cube);
        }

        JsonObject bone = new JsonObject();
        bone.addProperty("name", "root");
        bone.add("pivot", array(0, 0, 0));
        bone.add("cubes", cubes);

        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry." + identifier);
        description.addProperty("texture_width", texW);
        description.addProperty("texture_height", texH);
        description.addProperty("visible_bounds_width", 3);
        description.addProperty("visible_bounds_height", 3);
        description.add("visible_bounds_offset", array(0, 0.75, 0));

        JsonObject geo = new JsonObject();
        geo.add("description", description);
        JsonArray bones = new JsonArray();
        bones.add(bone);
        geo.add("bones", bones);

        JsonArray list = new JsonArray();
        list.add(geo);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.16.0");
        root.add("minecraft:geometry", list);
        return root;
    }

    /** Damit Bedrock das Modell in der Hand zeigt statt eines flachen Bildes. */
    private JsonObject attachable(String id, String identifier) {
        return parse("""
                {
                  "format_version": "1.10.0",
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "geyser_custom:%s",
                      "materials": {"default": "entity_alphatest",
                                    "enchanted": "entity_alphatest_glint"},
                      "textures": {"default": "textures/items/%s",
                                   "enchanted": "textures/misc/enchanted_item_glint"},
                      "geometry": {"default": "geometry.%s"},
                      "render_controllers": [
                        {"controllers.render.item_default": "query.owner_identifier"}
                      ],
                      "scripts": {"pre_animation": [
                        "v.main_hand = c.item_slot == 'main_hand';",
                        "v.off_hand = c.item_slot == 'off_hand';"
                      ]},
                      "animations": {
                        "thirdperson_main_hand": "animation.%s.thirdperson_main_hand",
                        "thirdperson_off_hand": "animation.%s.thirdperson_off_hand",
                        "firstperson_main_hand": "animation.%s.firstperson_main_hand",
                        "firstperson_off_hand": "animation.%s.firstperson_off_hand"
                      },
                      "animate": [
                        {"thirdperson_main_hand": "v.main_hand && !c.is_first_person"},
                        {"thirdperson_off_hand": "v.off_hand && !c.is_first_person"},
                        {"firstperson_main_hand": "v.main_hand && c.is_first_person"},
                        {"firstperson_off_hand": "v.off_hand && c.is_first_person"}
                      ]
                    }
                  }
                }""".formatted(id, id, identifier, identifier, identifier,
                identifier, identifier));
    }

    /** Übernimmt die Display-Werte aus Blockbench, damit es in der Hand sitzt. */
    private JsonObject animations(String identifier, JsonObject display) {
        JsonObject anims = new JsonObject();
        anims.add("animation." + identifier + ".thirdperson_main_hand",
                pose(display, "thirdperson_righthand"));
        anims.add("animation." + identifier + ".thirdperson_off_hand",
                pose(display, "thirdperson_lefthand"));
        anims.add("animation." + identifier + ".firstperson_main_hand",
                pose(display, "firstperson_righthand"));
        anims.add("animation." + identifier + ".firstperson_off_hand",
                pose(display, "firstperson_lefthand"));
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", anims);
        return root;
    }

    private JsonObject pose(JsonObject display, String key) {
        JsonObject part = display == null ? null : display.getAsJsonObject(key);
        double[] rot = part != null && part.has("rotation")
                ? triple(part.getAsJsonArray("rotation")) : new double[]{0, 0, 0};
        double[] tr = part != null && part.has("translation")
                ? triple(part.getAsJsonArray("translation")) : new double[]{0, 0, 0};
        double[] sc = part != null && part.has("scale")
                ? triple(part.getAsJsonArray("scale")) : new double[]{1, 1, 1};

        JsonObject root = new JsonObject();
        // Bedrock spiegelt X, darum die Vorzeichen
        root.add("rotation", array(rot[0], -rot[1], -rot[2]));
        root.add("position", array(-tr[0], tr[1], tr[2]));
        root.add("scale", array(sc[0], sc[1], sc[2]));
        JsonObject bones = new JsonObject();
        bones.add("root", root);
        JsonObject anim = new JsonObject();
        anim.addProperty("loop", true);
        anim.add("bones", bones);
        return anim;
    }

    private JsonObject mapping(CustomEntry entry, boolean has3d) {
        JsonObject item = new JsonObject();
        item.addProperty("name", entry.id());
        item.addProperty("custom_model_data",
                plugin.registry().modelDataByItem().getOrDefault(entry.id(), entry.modelData()));
        item.addProperty("icon", entry.id());
        item.addProperty("allow_offhand", true);
        item.addProperty("display_handheld", has3d);
        return item;
    }

    private JsonObject manifest() {
        UUID header = UUID.nameUUIDFromBytes(("smp.bedrock.header." + namespace())
                .getBytes(StandardCharsets.UTF_8));
        UUID module = UUID.nameUUIDFromBytes(("smp.bedrock.module." + namespace())
                .getBytes(StandardCharsets.UTF_8));
        return parse("""
                {
                  "format_version": 2,
                  "header": {
                    "name": "SMP 3D-Pack",
                    "description": "Eigene Items mit 3D-Modellen",
                    "uuid": "%s", "version": [1,0,0], "min_engine_version": [1,21,0]
                  },
                  "modules": [
                    {"type": "resources", "uuid": "%s", "version": [1,0,0]}
                  ]
                }""".formatted(header, module));
    }

    // ------------------------------------------------------------------

    private String namespace() {
        return plugin.getConfig().getString("texturepack.namespace", "smp");
    }

    private JsonObject readModel(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception ex) {
            plugin.getLogger().warning("Modell " + path.getFileName() + " nicht lesbar: "
                    + ex.getMessage());
            return null;
        }
    }

    /** texture_size aus dem Modell, sonst die echte Größe der PNG. */
    private int[] textureSize(JsonObject model, Path texture) {
        if (model.has("texture_size")) {
            JsonArray size = model.getAsJsonArray("texture_size");
            return new int[]{size.get(0).getAsInt(), size.get(1).getAsInt()};
        }
        try (InputStream in = Files.newInputStream(texture)) {
            byte[] head = in.readNBytes(24);
            if (head.length == 24) {
                int w = ((head[16] & 0xFF) << 24) | ((head[17] & 0xFF) << 16)
                        | ((head[18] & 0xFF) << 8) | (head[19] & 0xFF);
                int h = ((head[20] & 0xFF) << 24) | ((head[21] & 0xFF) << 16)
                        | ((head[22] & 0xFF) << 8) | (head[23] & 0xFF);
                if (w > 0 && h > 0) {
                    return new int[]{w, h};
                }
            }
        } catch (IOException ignored) {
            // Standardgröße benutzen
        }
        return new int[]{16, 16};
    }

    private static double[] triple(JsonArray array) {
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(),
                array.size() > 2 ? array.get(2).getAsDouble() : 0};
    }

    private static JsonArray array(double... values) {
        JsonArray out = new JsonArray();
        for (double value : values) {
            out.add(Math.round(value * 10000.0) / 10000.0);
        }
        return out;
    }

    private JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private void write(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, gson.toJson(json), StandardCharsets.UTF_8);
    }

    private void deleteRecursively(Path path) throws IOException {
        PackGenerator.deleteTree(path);
    }
}
