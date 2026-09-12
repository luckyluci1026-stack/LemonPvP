package de.lemonpvp.smpcontent.command;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.pack.BedrockPack;
import de.lemonpvp.smpcontent.pack.PackSource;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /smpcontent give|list|reload
 */
public final class ContentCommand implements TabExecutor {

    private final SMPContent plugin;

    public ContentCommand(SMPContent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smpcontent.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.registry().load();
                plugin.vehicles().load();
                plugin.bosses().load();

                ConfigProblem.Report problem = plugin.configProblem() != null
                        ? plugin.configProblem()
                        : plugin.msgs().problem();
                if (problem == null && !plugin.registry().problems().isEmpty()) {
                    problem = plugin.registry().problems().get(0);
                }
                if (problem == null) {
                    plugin.msgs().send(sender, "reloaded");
                } else {
                    plugin.msgs().send(sender, "config-error",
                            "file", problem.file(),
                            "line", String.valueOf(problem.line()),
                            "hint", ConfigProblem.safeForChat(problem.hint()));
                    for (String line : problem.context()) {
                        sender.sendMessage(Text.mm("<dark_gray>"
                                + ConfigProblem.safeForChat(line) + "</dark_gray>"));
                    }
                }
            }
            case "list", "search", "suche" -> {
                String search = args.length > 1
                        ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                        : "";
                if (sender instanceof Player player) {
                    plugin.gui().open(player, search);
                } else {
                    plugin.msgs().send(sender, "list-console",
                            "list", String.join(", ", plugin.registry().entries().keySet()));
                }
            }
            case "give" -> give(sender, args);
            case "pack" -> {
                if (plugin.pack().busy()) {
                    plugin.msgs().send(sender, "pack-busy");
                    return true;
                }
                plugin.msgs().send(sender, "pack-building");
                var result = plugin.pack().build();
                if (!result.ok()) {
                    plugin.msgs().send(sender, "pack-failed", "error", result.message());
                } else {
                    plugin.msgs().send(sender, "pack-done",
                            "items", String.valueOf(result.items()),
                            "blocks", String.valueOf(result.blocks()),
                            "models", String.valueOf(result.customModels()));
                    if (result.missing() > 0) {
                        plugin.msgs().send(sender, "pack-missing",
                                "count", String.valueOf(result.missing()),
                                "details", result.message());
                    }
                    var bedrock = plugin.pack().bedrockResult();
                    if (bedrock != null && bedrock.ok()) {
                        plugin.msgs().send(sender, "pack-bedrock",
                                "solid", String.valueOf(bedrock.solid()),
                                "flat", String.valueOf(bedrock.flat()),
                                "blocks", String.valueOf(bedrock.blocks()));
                    } else if (bedrock != null) {
                        plugin.msgs().send(sender, "pack-failed", "error", bedrock.message());
                    }
                }
            }
            case "bedrock" -> bedrock(sender, args);
            case "boss" -> boss(sender, args);
            default -> plugin.msgs().send(sender, "usage");
        }
        return true;
    }

    /**
     * /smpcontent boss &lt;id&gt; [x y z] - stellt einen Boss hin.
     *
     * Ohne Koordinaten kommt er dorthin, wohin du schaust; von der Konsole
     * aus müssen die drei Zahlen dabeistehen.
     */
    private void boss(CommandSender sender, String[] args) {
        if (plugin.bosses().types().isEmpty()) {
            plugin.msgs().send(sender, "boss-none");
            return;
        }
        if (args.length < 2) {
            plugin.msgs().send(sender, "boss-usage",
                    "list", String.join(", ", plugin.bosses().types().keySet()));
            return;
        }
        String id = args[1].toLowerCase(java.util.Locale.ROOT);
        if (!plugin.bosses().types().containsKey(id)) {
            plugin.msgs().send(sender, "boss-unknown", "id", id,
                    "list", String.join(", ", plugin.bosses().types().keySet()));
            return;
        }

        org.bukkit.Location wo;
        if (args.length >= 5) {
            try {
                org.bukkit.World welt = sender instanceof Player p
                        ? p.getWorld() : org.bukkit.Bukkit.getWorlds().get(0);
                wo = new org.bukkit.Location(welt, Double.parseDouble(args[2]),
                        Double.parseDouble(args[3]), Double.parseDouble(args[4]));
            } catch (NumberFormatException ex) {
                plugin.msgs().send(sender, "boss-usage",
                        "list", String.join(", ", plugin.bosses().types().keySet()));
                return;
            }
        } else if (sender instanceof Player player) {
            var ziel = player.getTargetBlockExact(60);
            wo = ziel != null ? ziel.getLocation().add(0.5, 1, 0.5)
                    : player.getLocation();
        } else {
            plugin.msgs().send(sender, "boss-usage",
                    "list", String.join(", ", plugin.bosses().types().keySet()));
            return;
        }

        var mob = plugin.bosses().spawn(id, wo);
        if (mob == null) {
            plugin.msgs().send(sender, "boss-failed", "id", id);
            return;
        }
        plugin.msgs().send(sender, "boss-spawned", "id", id,
                "x", String.valueOf(wo.getBlockX()),
                "y", String.valueOf(wo.getBlockY()),
                "z", String.valueOf(wo.getBlockZ()));
    }

    /**
     * /smpcontent bedrock [URL|Datei]
     *
     * Baut das Bedrock-Pack aus dem Java-Pack, das in der server.properties
     * steht - ohne vorher /smpcontent pack zu brauchen. Das Herunterladen und
     * Entpacken läuft auf einem Nebenthread, sonst würde der Server stocken.
     */
    private void bedrock(CommandSender sender, String[] args) {
        String given = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "";
        if (plugin.pack().busy()) {
            plugin.msgs().send(sender, "pack-busy");
            return;
        }
        plugin.msgs().send(sender, "bedrock-building");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PackSource.Found found = null;
            String error = null;
            BedrockPack.Result result = null;
            try {
                found = PackSource.locate(plugin, given);
                result = plugin.pack().buildBedrockFrom(found.zip());
            } catch (Exception ex) {
                error = String.valueOf(ex.getMessage());
            } finally {
                if (found != null && found.temporary()) {
                    try {
                        java.nio.file.Files.deleteIfExists(found.zip());
                    } catch (java.io.IOException ignored) {
                        // Die Datei bleibt eben liegen, das ist kein Beinbruch
                    }
                }
            }
            final PackSource.Found source = found;
            final BedrockPack.Result done = result;
            final String failure = error;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (failure != null) {
                    plugin.msgs().send(sender, "bedrock-missing", "error", failure);
                } else if (done == null || !done.ok()) {
                    plugin.msgs().send(sender, "pack-failed",
                            "error", done == null ? "unbekannt" : done.message());
                } else {
                    plugin.msgs().send(sender, "bedrock-done",
                            "origin", source == null ? "?" : source.origin(),
                            "solid", String.valueOf(done.solid()),
                            "flat", String.valueOf(done.flat()),
                            "blocks", String.valueOf(done.blocks()));
                }
            });
        });
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.msgs().send(sender, "give-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.msgs().send(sender, "player-not-found");
            return;
        }
        CustomEntry entry = plugin.registry().get(args[2]);
        if (entry == null) {
            plugin.msgs().send(sender, "unknown-content", "id", args[2],
                    "list", String.join(", ", plugin.registry().entries().keySet()));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        // Mehr als ein Stapel wird auf mehrere aufgeteilt, Reste fallen zu Boden
        int left = amount;
        while (left > 0) {
            int batch = Math.min(64, left);
            left -= batch;
            plugin.registry().give(target, entry, batch);
        }
        plugin.msgs().send(sender, "given", "amount", String.valueOf(amount),
                "id", entry.id(), "player", target.getName());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("give", "list", "pack", "bedrock", "boss", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("boss")) {
            for (String id : plugin.bosses().types().keySet()) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Bei mehreren hundert Ids nicht alles schicken: erst was mit dem
            // Getippten anfängt, dann was es enthält - und höchstens 60 Stück.
            String typed = args[2].toLowerCase(Locale.ROOT);
            List<String> contains = new ArrayList<>();
            for (String id : plugin.registry().entries().keySet()) {
                if (id.startsWith(typed)) {
                    out.add(id);
                } else if (!typed.isEmpty() && id.contains(typed)) {
                    contains.add(id);
                }
                if (out.size() >= 60) {
                    break;
                }
            }
            for (String id : contains) {
                if (out.size() >= 60) {
                    break;
                }
                out.add(id);
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            out.addAll(List.of("1", "16", "64"));
        }
        return out;
    }
}
