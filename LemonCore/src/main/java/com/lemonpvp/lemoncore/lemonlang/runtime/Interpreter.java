package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.lemonlang.LemonLangError;
import com.lemonpvp.lemoncore.lemonlang.ast.*;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Executes a list of {@link Stmt} objects given a {@link ScriptContext}.
 */
public final class Interpreter {

    private final String scriptFile;

    public Interpreter(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    /**
     * Run a list of statements in the given context.
     * Catches {@link StopExecution} to abort the body cleanly.
     */
    public void run(List<Stmt> stmts, ScriptContext ctx) {
        try {
            execAll(stmts, ctx);
        } catch (StopExecution ignored) {
            // stop/cancel verb intentionally aborts execution
        }
    }

    private void execAll(List<Stmt> stmts, ScriptContext ctx) {
        for (Stmt stmt : stmts) {
            exec(stmt, ctx);
        }
    }

    private void exec(Stmt stmt, ScriptContext ctx) {
        switch (stmt) {
            case ActionStmt a -> Actions.execute(a.verb(), a.args(), a.text(), ctx);

            case IfStmt iff -> {
                boolean result = evalCondition(iff.condition(), ctx);
                if (result) {
                    execAll(iff.thenBody(), ctx);
                } else if (!iff.elseBody().isEmpty()) {
                    execAll(iff.elseBody(), ctx);
                }
            }

            case SetStmt s -> {
                String resolved = ctx.resolveString(s.expression());
                ctx.env().setVar(s.name(), resolved);
            }

            case DefineStmt d -> {
                // Macros are registered at load time; if encountered inline, register locally
                ctx.env().defineMacro(d.name(), d.body());
            }

            case RepeatStmt r -> {
                for (int i = 0; i < r.times(); i++) {
                    Environment child = ctx.env().child();
                    child.setVar("loop", String.valueOf(i));
                    ScriptContext childCtx = ctx.fork(child);
                    execAll(r.body(), childCtx);
                }
            }
        }
    }

    // ---- Condition evaluation -----------------------------------------------

    private boolean evalCondition(Condition cond, ScriptContext ctx) {
        return switch (cond) {
            case Compare c    -> evalCompare(c, ctx);
            case Predicate p  -> evalPredicate(p, ctx);
            case And a        -> evalCondition(a.left(), ctx) && evalCondition(a.right(), ctx);
            case Or o         -> evalCondition(o.left(), ctx) || evalCondition(o.right(), ctx);
            case Not n        -> !evalCondition(n.inner(), ctx);
        };
    }

    private boolean evalCompare(Compare c, ScriptContext ctx) {
        Player player = ctx.player();
        String var = c.var().toLowerCase();
        double lhs;

        switch (var) {
            case "apples":   lhs = getCachedApples(player, ctx);    break;
            case "planks":   lhs = getCachedPlanks(player, ctx);    break;
            case "coins":    lhs = getCachedCoins(player, ctx);     break;
            case "health":   lhs = player != null ? player.getHealth() : 0; break;
            case "food":     lhs = player != null ? player.getFoodLevel() : 0; break;
            case "x":        lhs = player != null ? player.getLocation().getX() : 0; break;
            case "y":        lhs = player != null ? player.getLocation().getY() : 0; break;
            case "z":        lhs = player != null ? player.getLocation().getZ() : 0; break;
            case "online":   lhs = org.bukkit.Bukkit.getOnlinePlayers().size(); break;
            default: {
                // Try env variable as number
                String val = ctx.env().getVar(var);
                if (val != null) {
                    try { lhs = Double.parseDouble(val); }
                    catch (NumberFormatException e) { return compareStrings(val, c.op(), c.value()); }
                } else {
                    lhs = 0;
                }
            }
        }

        double rhs;
        try {
            rhs = Double.parseDouble(c.value());
        } catch (NumberFormatException e) {
            // String compare
            String lhsStr = ctx.resolveString("{" + c.var() + "}");
            return compareStrings(lhsStr, c.op(), c.value());
        }

        return switch (c.op()) {
            case ">="  -> lhs >= rhs;
            case "<="  -> lhs <= rhs;
            case ">"   -> lhs > rhs;
            case "<"   -> lhs < rhs;
            case "=="  -> lhs == rhs;
            case "!="  -> lhs != rhs;
            case "="   -> lhs == rhs;
            default    -> false;
        };
    }

    private boolean compareStrings(String lhs, String op, String rhs) {
        int cmp = lhs.compareTo(rhs);
        return switch (op) {
            case "=", "==" -> lhs.equalsIgnoreCase(rhs);
            case "!="       -> !lhs.equalsIgnoreCase(rhs);
            case ">"        -> cmp > 0;
            case "<"        -> cmp < 0;
            case ">="       -> cmp >= 0;
            case "<="       -> cmp <= 0;
            default         -> false;
        };
    }

    private boolean evalPredicate(Predicate p, ScriptContext ctx) {
        Player player = ctx.player();
        List<String> args = p.args();

        return switch (p.name().toLowerCase()) {
            case "at" -> {
                if (player == null || args.size() < 3) yield false;
                try {
                    double tx = Double.parseDouble(args.get(0));
                    double ty = Double.parseDouble(args.get(1));
                    double tz = Double.parseDouble(args.get(2));
                    Location loc = player.getLocation();
                    yield Math.abs(loc.getX() - tx) <= 2 &&
                          Math.abs(loc.getY() - ty) <= 2 &&
                          Math.abs(loc.getZ() - tz) <= 2;
                } catch (NumberFormatException e) { yield false; }
            }
            case "near" -> {
                if (player == null || args.size() < 4) yield false;
                try {
                    double tx = Double.parseDouble(args.get(0));
                    double ty = Double.parseDouble(args.get(1));
                    double tz = Double.parseDouble(args.get(2));
                    double r  = Double.parseDouble(args.get(3));
                    Location loc = player.getLocation();
                    double dx = loc.getX() - tx;
                    double dy = loc.getY() - ty;
                    double dz = loc.getZ() - tz;
                    yield (dx*dx + dy*dy + dz*dz) <= r*r;
                } catch (NumberFormatException e) { yield false; }
            }
            case "chance" -> {
                if (args.isEmpty()) yield false;
                try {
                    double n = Double.parseDouble(args.get(0));
                    yield Math.random() * 100 < n;
                } catch (NumberFormatException e) { yield false; }
            }
            case "has" -> {
                if (player == null || args.isEmpty()) yield false;
                String kind = args.get(0).toLowerCase();
                if (kind.equals("perm")) {
                    if (args.size() < 2) yield false;
                    yield player.hasPermission(args.get(1));
                } else if (kind.equals("item")) {
                    if (args.size() < 2) yield false;
                    try {
                        Material mat = Material.valueOf(args.get(1).toUpperCase());
                        yield player.getInventory().contains(mat);
                    } catch (IllegalArgumentException e) { yield false; }
                }
                yield false;
            }
            default -> {
                throw new LemonLangError(scriptFile, p.line(),
                        "Unbekanntes Predikat: '" + p.name() + "'.",
                        "Bekannte Praedikate: at, near, chance, has");
            }
        };
    }

    // ---- Economy helpers ----------------------------------------------------

    private long getCachedApples(Player player, ScriptContext ctx) {
        if (player == null) return 0;
        PlayerData data = getPlayerData(player, ctx);
        return data != null ? data.getApples() : 0;
    }

    private long getCachedPlanks(Player player, ScriptContext ctx) {
        if (player == null) return 0;
        PlayerData data = getPlayerData(player, ctx);
        return data != null ? data.getPlanks() : 0;
    }

    private long getCachedCoins(Player player, ScriptContext ctx) {
        if (player == null) return 0;
        PlayerData data = getPlayerData(player, ctx);
        return data != null ? data.getCoins() : 0;
    }

    private PlayerData getPlayerData(Player player, ScriptContext ctx) {
        if (ctx.manager() == null) return null;
        LemonCore core = (LemonCore) ctx.manager().getPlugin();
        if (core == null) return null;
        return core.getPlayerDataManager().getCached(player.getUniqueId());
    }
}
