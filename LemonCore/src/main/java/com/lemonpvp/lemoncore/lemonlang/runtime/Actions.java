package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.LemonLangError;
import com.lemonpvp.lemoncore.lemonlang.LemonLangManager;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.managers.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Static dispatcher for all LemonLang action verbs.
 * Each verb is handled in the {@link #execute} method.
 */
public final class Actions {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Logger LOG = Logger.getLogger("LemonLang");

    private Actions() {}

    /**
     * Execute a single action verb with its args/text inside the given context.
     *
     * @param verb   the action verb (lowercase)
     * @param args   structured arguments list (may be empty)
     * @param text   rest-of-line text for text verbs (may be null)
     * @param ctx    the execution context
     */
    public static void execute(String verb, List<String> args, String text, ScriptContext ctx) {
        String scriptFile = ctx.scriptName();
        Player player = ctx.player();

        switch (verb.toLowerCase()) {

            // ---- message --------------------------------------------------------
            case "message": {
                if (player == null) return;
                String resolved = ctx.resolveString(text);
                player.sendMessage(parse(resolved));
                break;
            }

            // ---- actionbar ------------------------------------------------------
            case "actionbar": {
                if (player == null) return;
                String resolved = ctx.resolveString(text);
                player.sendActionBar(parse(resolved));
                break;
            }

            // ---- title ----------------------------------------------------------
            case "title": {
                if (player == null) return;
                String resolved = ctx.resolveString(text);
                // Format: "main title | subtitle | fadeInSecs"  or just "main title"
                String[] parts = resolved.split("\\|");
                String mainText = parts.length > 0 ? parts[0].trim() : resolved;
                String subText  = parts.length > 1 ? parts[1].trim() : "";
                int fadeIn = 10, stay = 70, fadeOut = 20;
                if (parts.length > 2) {
                    try {
                        String durStr = parts[2].trim().replace("s", "").trim();
                        int secs = Integer.parseInt(durStr);
                        stay = secs * 20;
                    } catch (NumberFormatException ignored) {}
                }
                Component mainC = parse(mainText);
                Component subC  = parse(subText);
                Title title = Title.title(mainC, subC,
                        Title.Times.times(
                                Duration.ofMillis(fadeIn * 50L),
                                Duration.ofMillis(stay * 50L),
                                Duration.ofMillis(fadeOut * 50L)));
                player.showTitle(title);
                break;
            }

            // ---- subtitle -------------------------------------------------------
            case "subtitle": {
                if (player == null) return;
                String resolved = ctx.resolveString(text);
                player.showTitle(Title.title(Component.empty(), parse(resolved)));
                break;
            }

            // ---- broadcast ------------------------------------------------------
            case "broadcast": {
                String resolved = ctx.resolveString(text);
                Component msg = parse(resolved);
                Bukkit.broadcast(msg);
                break;
            }

            // ---- kick -----------------------------------------------------------
            case "kick": {
                if (player == null) return;
                String resolved = ctx.resolveString(text);
                player.kick(parse(resolved));
                break;
            }

            // ---- give -----------------------------------------------------------
            case "give": {
                if (args.isEmpty()) throw error(scriptFile, 0, "give braucht Argumente.", "Beispiel: give apples 10");
                String what = args.get(0).toLowerCase();
                switch (what) {
                    case "apples": {
                        long amount = parseLong(args, 1, scriptFile);
                        if (player != null) economy(ctx).addApples(player.getUniqueId(), amount);
                        break;
                    }
                    case "planks": {
                        long amount = parseLong(args, 1, scriptFile);
                        if (player != null) economy(ctx).addPlanks(player.getUniqueId(), amount);
                        break;
                    }
                    case "coins": {
                        long amount = parseLong(args, 1, scriptFile);
                        if (player != null) economy(ctx).addCoins(player.getUniqueId(), amount, "lemonlang", null);
                        break;
                    }
                    case "item": {
                        if (args.size() < 2) throw error(scriptFile, 0, "give item braucht einen Namen.", "Beispiel: give item KIRSCHE");
                        String itemName = args.get(1);
                        LemonLangManager mgr = ctx.manager();
                        if (mgr != null && player != null) {
                            ItemStack stack = mgr.getItemRegistry().get(itemName);
                            if (stack != null) {
                                player.getInventory().addItem(stack.clone());
                            } else {
                                LOG.warning("[LemonLang] Unbekanntes Item '" + itemName + "' in 'give item'.");
                            }
                        }
                        break;
                    }
                    default:
                        throw error(scriptFile, 0, "Unbekanntes give-Ziel: '" + what + "'.",
                                "Erlaubt: apples, planks, coins, item NAME");
                }
                break;
            }

            // ---- take -----------------------------------------------------------
            case "take": {
                if (args.isEmpty()) throw error(scriptFile, 0, "take braucht Argumente.", "Beispiel: take apples 10");
                String what = args.get(0).toLowerCase();
                switch (what) {
                    case "apples": {
                        long amount = parseLong(args, 1, scriptFile);
                        if (player != null) economy(ctx).removeApples(player.getUniqueId(), amount);
                        break;
                    }
                    case "planks": {
                        long amount = parseLong(args, 1, scriptFile);
                        if (player != null) economy(ctx).removePlanks(player.getUniqueId(), amount);
                        break;
                    }
                    case "coins": {
                        long amount = parseLong(args, 1, scriptFile);
                        if (player != null) economy(ctx).removeCoins(player.getUniqueId(), amount, "lemonlang", null);
                        break;
                    }
                    case "item": {
                        if (args.size() < 2) throw error(scriptFile, 0, "take item braucht einen Namen.", "Beispiel: take item KIRSCHE");
                        String itemName = args.get(1);
                        LemonLangManager mgr = ctx.manager();
                        if (mgr != null && player != null) {
                            ItemStack stack = mgr.getItemRegistry().get(itemName);
                            if (stack != null) {
                                player.getInventory().removeItem(stack);
                            }
                        }
                        break;
                    }
                    default:
                        throw error(scriptFile, 0, "Unbekanntes take-Ziel: '" + what + "'.",
                                "Erlaubt: apples, planks, coins, item NAME");
                }
                break;
            }

            // ---- sound ----------------------------------------------------------
            case "sound": {
                if (args.isEmpty()) throw error(scriptFile, 0, "sound braucht einen Sound-Namen.", "Beispiel: sound ENTITY_PLAYER_LEVELUP");
                String soundName = args.get(0).toUpperCase();
                try {
                    Sound sound = Sound.valueOf(soundName);
                    if (player != null) player.playSound(player.getLocation(), sound, 1f, 1f);
                } catch (IllegalArgumentException e) {
                    throw error(scriptFile, 0, "Unbekannter Sound: '" + soundName + "'.",
                            "Bitte einen gueltigen Bukkit-Sound-Namen verwenden.");
                }
                break;
            }

            // ---- particle -------------------------------------------------------
            case "particle": {
                if (args.isEmpty()) throw error(scriptFile, 0, "particle braucht einen Partikel-Namen.", "Beispiel: particle FLAME");
                String particleName = args.get(0).toUpperCase();
                try {
                    Particle particle = Particle.valueOf(particleName);
                    if (player != null) {
                        player.getWorld().spawnParticle(particle, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                } catch (IllegalArgumentException e) {
                    throw error(scriptFile, 0, "Unbekannter Partikel: '" + particleName + "'.",
                            "Bitte einen gueltigen Bukkit-Partikel-Namen verwenden.");
                }
                break;
            }

            // ---- firework -------------------------------------------------------
            case "firework": {
                if (player == null) return;
                Color color = Color.YELLOW;
                if (!args.isEmpty()) {
                    try {
                        // Try hex or named color
                        String colorStr = args.get(0).toUpperCase();
                        switch (colorStr) {
                            case "RED":    color = Color.RED;    break;
                            case "GREEN":  color = Color.GREEN;  break;
                            case "BLUE":   color = Color.BLUE;   break;
                            case "YELLOW": color = Color.YELLOW; break;
                            case "PURPLE": color = Color.PURPLE; break;
                            case "AQUA":   color = Color.AQUA;   break;
                            case "WHITE":  color = Color.WHITE;  break;
                            case "ORANGE": color = Color.ORANGE; break;
                            default:
                                if (colorStr.startsWith("#") && colorStr.length() == 7) {
                                    int r = Integer.parseInt(colorStr.substring(1,3), 16);
                                    int g = Integer.parseInt(colorStr.substring(3,5), 16);
                                    int b = Integer.parseInt(colorStr.substring(5,7), 16);
                                    color = Color.fromRGB(r, g, b);
                                }
                        }
                    } catch (Exception ignored) {}
                }
                Firework fw = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL)
                        .withColor(color)
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
                break;
            }

            // ---- effect ---------------------------------------------------------
            case "effect": {
                if (player == null) return;
                if (args.size() < 3) throw error(scriptFile, 0, "effect braucht 3 Argumente.",
                        "Syntax: effect EFFECT_NAME DAUER_SEKUNDEN AMPLIFIER");
                String effectName = args.get(0).toUpperCase();
                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type == null) {
                    throw error(scriptFile, 0, "Unbekannter Effekt: '" + effectName + "'.",
                            "Bitte einen gueltigen PotionEffect-Namen verwenden.");
                }
                int durationSecs = 30;
                int amplifier = 0;
                try { durationSecs = Integer.parseInt(args.get(1)); } catch (NumberFormatException ignored) {}
                try { amplifier = Integer.parseInt(args.get(2)); } catch (NumberFormatException ignored) {}
                player.addPotionEffect(new PotionEffect(type, durationSecs * 20, amplifier));
                break;
            }

            // ---- teleport -------------------------------------------------------
            case "teleport": {
                if (player == null || args.size() < 3) {
                    if (args.size() < 3) throw error(scriptFile, 0, "teleport braucht X Y Z.", "Syntax: teleport X Y Z [WORLD]");
                    return;
                }
                try {
                    double x = Double.parseDouble(args.get(0));
                    double y = Double.parseDouble(args.get(1));
                    double z = Double.parseDouble(args.get(2));
                    World world = player.getWorld();
                    if (args.size() >= 4) {
                        World w2 = Bukkit.getWorld(args.get(3));
                        if (w2 != null) world = w2;
                    }
                    player.teleport(new Location(world, x, y, z));
                } catch (NumberFormatException e) {
                    throw error(scriptFile, 0, "Ungueltige Koordinaten in teleport.", "Syntax: teleport X Y Z");
                }
                break;
            }

            // ---- heal -----------------------------------------------------------
            case "heal": {
                if (player == null) return;
                double health = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                if (!args.isEmpty()) {
                    try { health = Double.parseDouble(args.get(0)); } catch (NumberFormatException ignored) {}
                }
                player.setHealth(Math.min(health, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
                break;
            }

            // ---- feed -----------------------------------------------------------
            case "feed": {
                if (player == null) return;
                int food = 20;
                if (!args.isEmpty()) {
                    try { food = Integer.parseInt(args.get(0)); } catch (NumberFormatException ignored) {}
                }
                player.setFoodLevel(Math.min(food, 20));
                break;
            }

            // ---- perm -----------------------------------------------------------
            case "perm": {
                if (args.size() < 2) throw error(scriptFile, 0, "perm braucht add/remove und eine Permission.",
                        "Syntax: perm add lemon.vip  oder  perm remove lemon.vip");
                String action = args.get(0).toLowerCase();
                String permNode = args.get(1);
                LemonCore core = (LemonCore) ctx.manager().getPlugin();
                LuckPerms lp = core != null ? core.getLuckPerms() : null;
                if (lp == null) {
                    LOG.warning("[LemonLang] LuckPerms nicht verfuegbar fuer perm-Aktion.");
                    break;
                }
                UUID uuid = player != null ? player.getUniqueId() : null;
                if (uuid == null) break;
                lp.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
                    if (user == null) return;
                    if (action.equals("add")) {
                        user.data().add(Node.builder(permNode).build());
                    } else if (action.equals("remove")) {
                        user.data().remove(Node.builder(permNode).build());
                    }
                    lp.getUserManager().saveUser(user);
                });
                break;
            }

            // ---- run (macro) ----------------------------------------------------
            case "run": {
                if (args.isEmpty()) throw error(scriptFile, 0, "run braucht einen Makro-Namen.", "Syntax: run meinMakro");
                String macroName = args.get(0);
                var macroBody = ctx.env().getMacro(macroName);
                if (macroBody == null && ctx.manager() != null) {
                    macroBody = ctx.manager().getEnvironment().getMacro(macroName);
                }
                if (macroBody == null) {
                    throw error(scriptFile, 0, "Unbekanntes Makro: '" + macroName + "'.",
                            "Definiere es mit: define " + macroName);
                }
                Interpreter interp = new Interpreter(scriptFile);
                interp.run(macroBody, ctx);
                break;
            }

            // ---- console --------------------------------------------------------
            case "console": {
                String cmd = ctx.resolveString(text != null ? text : String.join(" ", args));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                break;
            }

            // ---- command (player executes) --------------------------------------
            case "command": {
                if (player == null) return;
                String cmd = ctx.resolveString(text != null ? text : String.join(" ", args));
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                player.performCommand(cmd);
                break;
            }

            // ---- gui ------------------------------------------------------------
            case "gui": {
                if (args.isEmpty()) throw error(scriptFile, 0, "gui braucht einen Namen.", "Syntax: gui MEIN_GUI");
                String guiName = args.get(0);
                LemonLangManager mgr = ctx.manager();
                if (mgr != null && player != null) {
                    LemonCore core = (LemonCore) mgr.getPlugin();
                    mgr.getGuiRegistry().open(guiName, player, core);
                }
                break;
            }

            // ---- cooldown -------------------------------------------------------
            case "cooldown": {
                if (args.size() < 2) throw error(scriptFile, 0, "cooldown braucht NAME und DAUER.",
                        "Syntax: cooldown meinKey 5s");
                String key = args.get(0);
                long ms = com.lemonpvp.lemoncore.lemonlang.token.ArgScanner.parseDuration(args.get(1), scriptFile, 0);
                LemonLangManager mgr = ctx.manager();
                if (mgr != null && player != null) {
                    mgr.getCooldownRegistry().setCooldown(player.getUniqueId(), key, ms);
                }
                break;
            }

            // ---- wait -----------------------------------------------------------
            case "wait": {
                LOG.warning("[LemonLang] 'wait' ist in diesem Kontext nicht blockierend und wird ignoriert. " +
                        "Asynchrones Wait wird nicht unterstuetzt.");
                break;
            }

            // ---- stop / cancel --------------------------------------------------
            case "stop":
            case "cancel": {
                throw new StopExecution();
            }

            // ---- db (write/read/delete) -----------------------------------------
            case "db": {
                // Format: db write [sync] TABLE KEY FIELD VALUE
                //         db read  [sync] TABLE KEY FIELD -> VARIABLE
                //         db delete [sync] TABLE KEY FIELD
                if (args.isEmpty()) {
                    LOG.warning("[LemonLang] db: Kein Unterbefehl angegeben. Erlaubt: write, read, delete");
                    break;
                }
                LemonCore lc = (LemonCore) ctx.manager().getPlugin();
                String sub = args.get(0).toLowerCase();
                boolean sync = args.size() > 1 && args.get(1).equalsIgnoreCase("sync");
                int offset = sync ? 2 : 1;
                // Need at least: sub [sync] TABLE KEY FIELD  → offset + 3 total args
                if (args.size() < offset + 3) {
                    LOG.warning("[LemonLang] db " + sub + ": Zu wenige Argumente. "
                            + "Format: db " + sub + " [sync] TABLE KEY FIELD [VALUE]");
                    break;
                }
                // TABLE arg is present but semantically unused (table is always lemonlang_data);
                // we keep it in the syntax for forward-compatibility and user clarity.
                String key   = ctx.resolveString(args.get(offset + 1));  // KEY (resolved)
                String field = args.get(offset + 2);                     // FIELD

                switch (sub) {
                    case "write" -> {
                        if (args.size() < offset + 4) {
                            LOG.warning("[LemonLang] db write: Fehlender Wert. "
                                    + "Format: db write [sync] TABLE KEY FIELD VALUE");
                            break;
                        }
                        String value = ctx.resolveString(
                                String.join(" ", args.subList(offset + 3, args.size())));
                        if (sync) {
                            LemonLangDB.writeSync(lc, key, field, value);
                        } else {
                            LemonLangDB.writeAsync(lc, key, field, value);
                        }
                    }
                    case "read" -> {
                        // Format: db read [sync] TABLE KEY FIELD -> VARIABLE
                        int arrowIdx = args.indexOf("->");
                        if (arrowIdx < 0 || arrowIdx >= args.size() - 1) {
                            LOG.warning("[LemonLang] db read: Fehlendes '->' oder Variablenname. "
                                    + "Format: db read [sync] TABLE KEY FIELD -> VARIABLE");
                            break;
                        }
                        String varName = args.get(arrowIdx + 1);
                        if (sync) {
                            String val = LemonLangDB.readSync(lc, key, field).orElse("");
                            ctx.env().setVar(varName, val);
                        } else {
                            LemonLangDB.readAsync(lc, key, field).thenAccept(optVal -> {
                                String val = optVal.orElse("");
                                org.bukkit.Bukkit.getScheduler().runTask(ctx.manager().getPlugin(), () ->
                                        ctx.env().setVar(varName, val));
                            });
                        }
                    }
                    case "delete" -> {
                        if (sync) {
                            LemonLangDB.deleteSync(lc, key, field);
                        } else {
                            LemonLangDB.deleteAsync(lc, key, field);
                        }
                    }
                    default -> LOG.warning("[LemonLang] db: Unbekannter Unterbefehl '" + sub
                            + "'. Erlaubt: write, read, delete");
                }
                break;
            }

            default: {
                LOG.warning("[LemonLang] Unbekanntes Verb '" + verb + "' in Skript '" + scriptFile + "'.");
            }
        }
    }

    // ---- Helpers ----------------------------------------------------------------

    private static Component parse(String miniMessage) {
        return MM.deserialize("<!italic>" + miniMessage);
    }

    private static long parseLong(List<String> args, int idx, String scriptFile) {
        if (idx >= args.size()) throw error(scriptFile, 0, "Fehlende Zahl.", "Beispiel: give apples 10");
        try {
            return Long.parseLong(args.get(idx));
        } catch (NumberFormatException e) {
            throw error(scriptFile, 0, "Ungueltige Zahl '" + args.get(idx) + "'.", "Bitte eine ganze Zahl angeben.");
        }
    }

    private static LemonLangError error(String scriptFile, int line, String msg, String hint) {
        return new LemonLangError(scriptFile, line, msg, hint);
    }

    private static PlayerDataManager economy(ScriptContext ctx) {
        LemonCore core = (LemonCore) ctx.manager().getPlugin();
        return core.getPlayerDataManager();
    }
}
