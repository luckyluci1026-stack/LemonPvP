package de.lemonpvp.smpcontent.pack;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Baut aus der Konfiguration, deinen PNG-Dateien und deinen Blockbench-Modellen
 * ein fertiges Java-Resourcepack - inklusive Item-Modellen, Blockmodellen und
 * der kompletten note_block.json für die eigenen Blöcke.
 *
 * Ablauf:
 *   1. PNG nach   plugins/SMPContent/textures/item/&lt;id&gt;.png   (bzw. block/)
 *   2. Optional ein Blockbench-Modell nach
 *                 plugins/SMPContent/models/item/&lt;id&gt;.json    (bzw. block/)
 *   3. Eintrag in der config.yml
 *   4. /smpcontent pack   ->   plugins/SMPContent/output/&lt;Name&gt;.zip
 */
public final class PackGenerator {

    /** Alle Notenblock-Instrumente in 1.21 - für die vollständige Blockstate-Datei. */
    private static final String[] INSTRUMENTS = {
            "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar",
            "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit",
            "banjo", "pling", "zombie", "skeleton", "creeper", "dragon",
            "wither_skeleton", "piglin", "custom_head",
    };

    private final SMPContent plugin;

    public PackGenerator(SMPContent plugin) {
        this.plugin = plugin;
    }

    /** Ergebnis eines Pack-Baus. */
    public record Result(boolean ok, int items, int blocks, int customModels,
                         int missing, String message) {
    }

    private String namespace() {
        return plugin.getConfig().getString("texturepack.namespace", "smp");
    }

    private Path texturesDir() {
        return plugin.getDataFolder().toPath().resolve("textures");
    }

    /** Hier legst du deine Blockbench-Modelle ab. */
    private Path modelsDir() {
        return plugin.getDataFolder().toPath().resolve("models");
    }

    private Path outputDir() {
        return plugin.getDataFolder().toPath().resolve("output");
    }

    // ---------------- Ordner & Anleitungen ----------------

    /** Legt die Ordnerstruktur an, in die du Texturen und Modelle kopierst. */
    public void ensureFolders() {
        try {
            Files.createDirectories(texturesDir().resolve("item"));
            Files.createDirectories(texturesDir().resolve("block"));
            Files.createDirectories(modelsDir().resolve("item"));
            Files.createDirectories(modelsDir().resolve("block"));
            writeIfAbsent(texturesDir().resolve("LIESMICH.txt"), """
                    Hier kommen deine eigenen Texturen rein.

                      item/<id>.png    Textur fuer ein Item aus dem Abschnitt "items"
                      block/<id>.png   Textur fuer einen Block aus dem Abschnitt "blocks"

                    Der Dateiname muss exakt der Id aus der config.yml entsprechen,
                    also z.B. item/ruby.png fuer das Item "ruby".

                    Empfohlene Groesse: 16x16 Pixel (32x32 oder 64x64 gehen auch).
                    Zusaetzliche PNGs in diesen Ordnern werden mitkopiert - praktisch,
                    wenn ein Blockbench-Modell mehrere Texturen benutzt.

                    Danach im Spiel:  /smpcontent pack
                    Das fertige Pack liegt dann in  output/
                    """);
            writeIfAbsent(modelsDir().resolve("BLOCKBENCH.txt"), """
                    Eigene 3D-Modelle aus Blockbench
                    ================================

                    Lege deine Blockbench-Exporte hier ab:

                      item/<id>.json     Modell fuer ein Item, eine Waffe, ein Werkzeug
                      block/<id>.json    Modell fuer einen Block

                    Der Dateiname muss der Id aus der config.yml entsprechen. Liegt
                    hier ein Modell, benutzt das Pack DEIN Modell statt des
                    automatisch erzeugten Standardmodells.

                    So gehst du in Blockbench vor:

                    1. Neu  ->  "Java Block/Item Model"
                    2. Modell bauen und texturieren
                    3. Textur-Eigenschaften oeffnen und den Namen so setzen, dass
                       der Pfad passt:   %NS%:item/<id>     (bzw. %NS%:block/<id>)
                    4. Datei -> Exportieren -> "Java-Block/Item-Modell",
                       hier in models/item/ bzw. models/block/ speichern
                    5. Die PNG dazu nach ../textures/item/<id>.png legen
                    6. Im Spiel:  /smpcontent pack

                    Tipp: Fuer Waffen und Werkzeuge lohnt sich in Blockbench der
                    Reiter "Display" - damit sitzt das Modell in der Hand richtig.
                    Diese Einstellungen bleiben beim Export erhalten.
                    """.replace("%NS%", namespace()));
        } catch (IOException e) {
            plugin.getLogger().warning("Ordner nicht anlegbar: " + e.getMessage());
        }
    }

    // ---------------- Pack bauen ----------------

    public Result build() {
        String ns = namespace();
        String packName = plugin.getConfig().getString("texturepack.pack-name", "SMPPack");
        Path work = plugin.getDataFolder().toPath().resolve("build");

        int items = 0;
        int blocks = 0;
        int customModels = 0;
        List<String> missing = new ArrayList<>();

        try {
            deleteRecursively(work);
            Path assets = work.resolve("assets");
            Path nsRoot = assets.resolve(ns);
            Files.createDirectories(nsRoot.resolve("textures/item"));
            Files.createDirectories(nsRoot.resolve("textures/block"));
            Files.createDirectories(nsRoot.resolve("models/item"));
            Files.createDirectories(nsRoot.resolve("models/block"));
            Files.createDirectories(nsRoot.resolve("items"));
            Files.createDirectories(assets.resolve("minecraft/blockstates"));

            writePackMeta(work);

            for (CustomEntry entry : plugin.registry().entries().values()) {
                String kind = entry.block() ? "block" : "item";

                boolean hasTexture = copyIfPresent(
                        texturesDir().resolve(kind).resolve(entry.id() + ".png"),
                        nsRoot.resolve("textures/" + kind + "/" + entry.id() + ".png"));

                // Eigenes Blockbench-Modell schlägt das Standardmodell
                boolean hasOwnModel = copyIfPresent(
                        modelsDir().resolve(kind).resolve(entry.id() + ".json"),
                        nsRoot.resolve("models/" + kind + "/" + entry.id() + ".json"));
                if (hasOwnModel) {
                    customModels++;
                }
                if (!hasTexture && !hasOwnModel) {
                    missing.add(kind + "/" + entry.id() + ".png");
                }

                if (entry.block()) {
                    if (!hasOwnModel) {
                        write(nsRoot.resolve("models/block/" + entry.id() + ".json"), """
                                {
                                  "parent": "minecraft:block/cube_all",
                                  "textures": { "all": "%s:block/%s" }
                                }""".formatted(ns, entry.id()));
                    }
                    // Das Item in der Hand zeigt denselben Block
                    write(nsRoot.resolve("models/item/" + entry.id() + ".json"),
                            "{ \"parent\": \"%s:block/%s\" }".formatted(ns, entry.id()));
                    write(nsRoot.resolve("items/" + entry.id() + ".json"), """
                            {
                              "model": { "type": "minecraft:model", "model": "%s:block/%s" }
                            }""".formatted(ns, entry.id()));
                    blocks++;
                } else {
                    if (!hasOwnModel) {
                        write(nsRoot.resolve("models/item/" + entry.id() + ".json"), """
                                {
                                  "parent": "minecraft:item/generated",
                                  "textures": { "layer0": "%s:item/%s" }
                                }""".formatted(ns, entry.id()));
                    }
                    write(nsRoot.resolve("items/" + entry.id() + ".json"), """
                            {
                              "model": { "type": "minecraft:model", "model": "%s:item/%s" }
                            }""".formatted(ns, entry.id()));
                    items++;
                }
            }

            // Zusätzliche Texturen mitnehmen (Blockbench-Modelle nutzen oft mehrere)
            copyExtraTextures(nsRoot);
            writeBlockStates(assets.resolve("minecraft/blockstates/note_block.json"), ns);

            Files.createDirectories(outputDir());
            Path zip = outputDir().resolve(packName + ".zip");
            zipDirectory(work, zip);
            deleteRecursively(work);

            String message = missing.isEmpty()
                    ? "Pack gebaut: " + zip.getFileName()
                    : "Es fehlen: " + String.join(", ", missing);
            return new Result(true, items, blocks, customModels, missing.size(), message);

        } catch (Exception e) {
            plugin.getLogger().warning("Pack-Bau fehlgeschlagen: " + e);
            return new Result(false, 0, 0, 0, 0, String.valueOf(e.getMessage()));
        }
    }

    private void writePackMeta(Path work) throws IOException {
        int format = plugin.getConfig().getInt("texturepack.pack-format", 64);
        String description = plugin.getConfig()
                .getString("texturepack.description", "Eigene Bloecke und Items")
                .replace("\"", "'");
        write(work.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": %d,
                    "supported_formats": [46, 99],
                    "description": "%s"
                  }
                }""".formatted(format, description));
    }

    /**
     * Schreibt ALLE Notenblock-Zustände. Nicht belegte zeigen weiterhin den
     * Vanilla-Notenblock - sonst erschiene jeder normale Notenblock als
     * fehlende Textur.
     */
    private void writeBlockStates(Path target, String ns) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String instrument : INSTRUMENTS) {
            for (int note = 0; note <= 24; note++) {
                for (boolean powered : new boolean[]{false, true}) {
                    String state = "instrument=" + instrument + ",note=" + note
                            + ",powered=" + powered;
                    lines.add("    \"" + state + "\": { \"model\": \""
                            + modelForState(state, ns) + "\" }");
                }
            }
        }
        write(target, "{\n  \"variants\": {\n" + String.join(",\n", lines) + "\n  }\n}\n");
    }

    /** Sucht den eigenen Block, dessen Zustand exakt zu dieser Variante passt. */
    private String modelForState(String state, String ns) {
        for (CustomEntry entry : plugin.registry().entries().values()) {
            if (!entry.block() || entry.state() == null) {
                continue;
            }
            String own = entry.state().toLowerCase(Locale.ROOT);
            int open = own.indexOf('[');
            int close = own.lastIndexOf(']');
            if (open < 0 || close < 0) {
                continue;
            }
            if (own.substring(open + 1, close).equals(state)) {
                return ns + ":block/" + entry.id();
            }
        }
        return "minecraft:block/note_block";
    }

    /**
     * Kopiert alle weiteren PNGs aus textures/item und textures/block mit -
     * Blockbench-Modelle verweisen oft auf zusätzliche Texturen.
     */
    private void copyExtraTextures(Path nsRoot) throws IOException {
        for (String kind : new String[]{"item", "block"}) {
            Path dir = texturesDir().resolve(kind);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                        Files.copy(file, nsRoot.resolve("textures/" + kind + "/"
                                        + file.getFileName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    // ---------------- Datei-Helfer ----------------

    private boolean copyIfPresent(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return false;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeIfAbsent(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            write(path, content);
        }
    }

    private void zipDirectory(Path folder, Path target) throws IOException {
        Files.deleteIfExists(target);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(target));
             Stream<Path> walk = Files.walk(folder)) {
            for (Path file : walk.filter(Files::isRegularFile).sorted().toList()) {
                zos.putNextEntry(new ZipEntry(
                        folder.relativize(file).toString().replace('\\', '/')));
                try (InputStream in = Files.newInputStream(file)) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
