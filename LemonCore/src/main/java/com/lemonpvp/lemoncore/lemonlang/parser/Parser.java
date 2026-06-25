package com.lemonpvp.lemoncore.lemonlang.parser;

import com.lemonpvp.lemoncore.lemonlang.LemonLangError;
import com.lemonpvp.lemoncore.lemonlang.ast.*;
import com.lemonpvp.lemoncore.lemonlang.token.ArgScanner;
import com.lemonpvp.lemoncore.lemonlang.token.Line;

import java.util.*;

/**
 * Parses a flat list of {@link Line} objects into a {@link Program}.
 *
 * <p>Entry point: {@link #parse(List, String)} — takes the output of
 * {@link com.lemonpvp.lemoncore.lemonlang.token.LineReader} and the script file
 * name (for error messages). Returns a fully-formed Program; throws
 * {@link LemonLangError} on syntax errors.
 */
public final class Parser {

    private final String scriptFile;
    private List<Line> lines;
    private int pos;

    private Parser(List<Line> lines, String scriptFile) {
        this.lines = lines;
        this.scriptFile = scriptFile;
        this.pos = 0;
    }

    /** Parse the given lines into a Program. Throws LemonLangError on error. */
    public static Program parse(List<Line> lines, String scriptFile) {
        return new Parser(lines, scriptFile).parseProgram();
    }

    // ---- Top-level -----------------------------------------------------------

    private Program parseProgram() {
        List<Node> nodes = new ArrayList<>();
        while (pos < lines.size()) {
            Line line = lines.get(pos);
            if (line.indent() != 0) {
                throw new LemonLangError(scriptFile, line.number(),
                        "Unerwartete Einrueckung auf der obersten Ebene.",
                        "Nur set, define, on, every, item, command, gui duerfen auf Ebene 0 stehen.");
            }
            Node node = parseTopLevel(line);
            if (node != null) nodes.add(node);
        }
        return new Program(Collections.unmodifiableList(nodes), 1);
    }

    private Node parseTopLevel(Line line) {
        String content = line.content();
        List<String> words = ArgScanner.words(content);
        if (words.isEmpty()) { pos++; return null; }

        String kw = words.get(0).toLowerCase();
        switch (kw) {
            case "set":    return parseSet(line);
            case "define": return parseDefine(line);
            case "on":     return parseTrigger(line);
            case "every":  return parseEvery(line);
            case "item":   return parseItem(line);
            case "command":return parseCommand(line);
            case "gui":    return parseGui(line);
            default:
                throw new LemonLangError(scriptFile, line.number(),
                        "Unbekanntes Schluessewort auf der obersten Ebene: '" + kw + "'.",
                        "Erlaubt: set, define, on, every, item, command, gui");
        }
    }

    // ---- set -----------------------------------------------------------------

    private Node parseSet(Line line) {
        pos++;
        String content = line.content();
        // set NAME = VALUE
        int eqIdx = content.indexOf('=');
        if (eqIdx < 0) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlende '=' in set-Anweisung.",
                    "Syntax: set name = wert");
        }
        String namePart = content.substring("set".length(), eqIdx).trim();
        String valuePart = content.substring(eqIdx + 1).trim();
        if (namePart.isEmpty()) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlender Variablenname in set-Anweisung.",
                    "Syntax: set name = wert");
        }
        return new SetStmt(namePart, valuePart, line.number());
    }

    // ---- define --------------------------------------------------------------

    private Node parseDefine(Line line) {
        pos++;
        List<String> words = ArgScanner.words(line.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlender Name nach 'define'.",
                    "Syntax: define macroName");
        }
        String name = words.get(1);
        int baseIndent = line.indent();
        List<Stmt> body = parseBody(baseIndent);
        return new DefineStmt(name, body, line.number());
    }

    // ---- on <trigger> --------------------------------------------------------

    private TriggerDef parseTrigger(Line line) {
        pos++;
        List<String> words = ArgScanner.words(line.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlender Trigger-Name nach 'on'.",
                    "Beispiel: on join");
        }
        String event = words.get(1).toLowerCase();
        List<String> params = words.size() > 2 ? words.subList(2, words.size()) : List.of();
        int baseIndent = line.indent();
        List<Stmt> body = parseBody(baseIndent);
        return new TriggerDef(event, params, body, line.number());
    }

    // ---- every <duration> ----------------------------------------------------

    private Node parseEvery(Line line) {
        pos++;
        List<String> words = ArgScanner.words(line.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlende Zeitangabe nach 'every'.",
                    "Beispiel: every 30s");
        }
        long intervalMs = ArgScanner.parseDuration(words.get(1), scriptFile, line.number());
        int baseIndent = line.indent();
        List<Stmt> body = parseBody(baseIndent);
        return new EveryDef(intervalMs, body, line.number());
    }

    // ---- item <NAME> ---------------------------------------------------------

    private Node parseItem(Line line) {
        pos++;
        List<String> words = ArgScanner.words(line.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlender Item-Name nach 'item'.",
                    "Beispiel: item KIRSCHE");
        }
        String name = words.get(1);
        int baseIndent = line.indent();
        int childIndent = peekIndent();

        Map<String, String> props = new LinkedHashMap<>();
        List<String> lore = new ArrayList<>();
        List<TriggerDef> triggers = new ArrayList<>();

        while (pos < lines.size() && lines.get(pos).indent() > baseIndent) {
            Line child = lines.get(pos);
            List<String> cw = ArgScanner.words(child.content());
            if (cw.isEmpty()) { pos++; continue; }
            String key = cw.get(0).toLowerCase();
            if (key.equals("lore")) {
                pos++;
                // collect sub-indented lore lines
                while (pos < lines.size() && lines.get(pos).indent() > child.indent()) {
                    lore.add(lines.get(pos).content().trim());
                    pos++;
                }
            } else if (key.equals("on")) {
                triggers.add(parseTrigger(child));
            } else if (cw.size() >= 2) {
                // e.g. material APPLE, name <red>Kirsche, cooldown 5s
                String val = child.content().substring(key.length()).trim();
                props.put(key, val);
                pos++;
            } else {
                pos++;
            }
        }
        return new ItemDef(name, Collections.unmodifiableMap(props),
                Collections.unmodifiableList(lore),
                Collections.unmodifiableList(triggers), line.number());
    }

    // ---- command <name> ------------------------------------------------------

    private Node parseCommand(Line line) {
        pos++;
        List<String> words = ArgScanner.words(line.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlender Command-Name nach 'command'.",
                    "Beispiel: command kirsche");
        }
        String name = words.get(1);
        int baseIndent = line.indent();

        Map<String, String> props = new LinkedHashMap<>();
        List<TriggerDef> triggers = new ArrayList<>();

        while (pos < lines.size() && lines.get(pos).indent() > baseIndent) {
            Line child = lines.get(pos);
            List<String> cw = ArgScanner.words(child.content());
            if (cw.isEmpty()) { pos++; continue; }
            String key = cw.get(0).toLowerCase();
            if (key.equals("on")) {
                triggers.add(parseTrigger(child));
            } else {
                String val = child.content().substring(key.length()).trim();
                props.put(key, val);
                pos++;
            }
        }
        return new CommandDef(name, Collections.unmodifiableMap(props),
                Collections.unmodifiableList(triggers), line.number());
    }

    // ---- gui <NAME> ----------------------------------------------------------

    private Node parseGui(Line line) {
        pos++;
        List<String> words = ArgScanner.words(line.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, line.number(),
                    "Fehlender GUI-Name nach 'gui'.",
                    "Beispiel: gui SHOP");
        }
        String name = words.get(1);
        int baseIndent = line.indent();

        Map<String, String> props = new LinkedHashMap<>();
        List<SlotDef> slots = new ArrayList<>();

        while (pos < lines.size() && lines.get(pos).indent() > baseIndent) {
            Line child = lines.get(pos);
            List<String> cw = ArgScanner.words(child.content());
            if (cw.isEmpty()) { pos++; continue; }
            String key = cw.get(0).toLowerCase();
            if (key.equals("slot")) {
                slots.add(parseSlot(child));
            } else {
                String val = child.content().substring(key.length()).trim();
                props.put(key, val);
                pos++;
            }
        }
        return new GuiDef(name, Collections.unmodifiableMap(props),
                Collections.unmodifiableList(slots), line.number());
    }

    private SlotDef parseSlot(Line slotLine) {
        pos++;
        List<String> words = ArgScanner.words(slotLine.content());
        if (words.size() < 2) {
            throw new LemonLangError(scriptFile, slotLine.number(),
                    "Fehlende Slot-Nummer nach 'slot'.",
                    "Beispiel: slot 13");
        }
        int slotNum;
        try {
            slotNum = Integer.parseInt(words.get(1));
        } catch (NumberFormatException e) {
            throw new LemonLangError(scriptFile, slotLine.number(),
                    "Ungueltige Slot-Nummer '" + words.get(1) + "'.",
                    "Slot muss eine ganze Zahl sein, z.B.: slot 13");
        }
        int baseIndent = slotLine.indent();
        Map<String, String> props = new LinkedHashMap<>();
        List<String> lore = new ArrayList<>();
        TriggerDef onClick = null;

        while (pos < lines.size() && lines.get(pos).indent() > baseIndent) {
            Line child = lines.get(pos);
            List<String> cw = ArgScanner.words(child.content());
            if (cw.isEmpty()) { pos++; continue; }
            String key = cw.get(0).toLowerCase();
            if (key.equals("lore")) {
                pos++;
                while (pos < lines.size() && lines.get(pos).indent() > child.indent()) {
                    lore.add(lines.get(pos).content().trim());
                    pos++;
                }
            } else if (key.equals("on")) {
                onClick = parseTrigger(child);
            } else {
                String val = child.content().substring(key.length()).trim();
                props.put(key, val);
                pos++;
            }
        }
        return new SlotDef(slotNum, Collections.unmodifiableMap(props),
                Collections.unmodifiableList(lore), onClick, slotLine.number());
    }

    // ---- Body (indented block) -----------------------------------------------

    /** Collects Stmt lines that are strictly more indented than baseIndent. */
    private List<Stmt> parseBody(int baseIndent) {
        List<Stmt> stmts = new ArrayList<>();
        while (pos < lines.size()) {
            Line line = lines.get(pos);
            if (line.indent() <= baseIndent) break;
            Stmt stmt = parseStmt(line, baseIndent);
            if (stmt != null) stmts.add(stmt);
        }
        return Collections.unmodifiableList(stmts);
    }

    private Stmt parseStmt(Line line, int parentIndent) {
        List<String> words = ArgScanner.words(line.content());
        if (words.isEmpty()) { pos++; return null; }
        String kw = words.get(0).toLowerCase();

        switch (kw) {
            case "if":
                return parseIf(line);
            case "else":
            case "else if":
                // Should be consumed by parseIf; if we see it standalone it's an error
                throw new LemonLangError(scriptFile, line.number(),
                        "'else' ohne vorheriges 'if'.",
                        "else muss direkt nach einem if-Block stehen.");
            case "repeat": {
                pos++;
                if (words.size() < 2) {
                    throw new LemonLangError(scriptFile, line.number(),
                            "Fehlende Anzahl nach 'repeat'.",
                            "Syntax: repeat 3");
                }
                int times;
                try { times = Integer.parseInt(words.get(1)); }
                catch (NumberFormatException e) {
                    throw new LemonLangError(scriptFile, line.number(),
                            "Ungueltige Zahl '" + words.get(1) + "' nach 'repeat'.",
                            "Syntax: repeat 3");
                }
                List<Stmt> body = parseBody(line.indent());
                return new RepeatStmt(times, body, line.number());
            }
            case "set": {
                pos++;
                String content = line.content();
                int eqIdx = content.indexOf('=');
                if (eqIdx < 0) {
                    throw new LemonLangError(scriptFile, line.number(),
                            "Fehlende '=' in set-Anweisung.",
                            "Syntax: set name = wert");
                }
                String namePart = content.substring("set".length(), eqIdx).trim();
                String valuePart = content.substring(eqIdx + 1).trim();
                return new SetStmt(namePart, valuePart, line.number());
            }
            case "define": {
                pos++;
                if (words.size() < 2) {
                    throw new LemonLangError(scriptFile, line.number(),
                            "Fehlender Name nach 'define'.",
                            "Syntax: define macroName");
                }
                String name = words.get(1);
                List<Stmt> body = parseBody(line.indent());
                return new DefineStmt(name, body, line.number());
            }
            default: {
                // Action statement
                pos++;
                // Text verbs: the "text" is the rest of the line after the verb
                String verb = words.get(0);
                String rest = line.content().substring(verb.length()).trim();
                Set<String> textVerbs = Set.of("message", "actionbar", "title", "subtitle",
                        "broadcast", "kick");
                if (textVerbs.contains(kw)) {
                    return new ActionStmt(verb, List.of(), rest, line.number());
                } else {
                    // Structured verbs: args are the words, text is null
                    List<String> args = words.size() > 1 ? words.subList(1, words.size()) : List.of();
                    return new ActionStmt(verb, Collections.unmodifiableList(new ArrayList<>(args)), rest, line.number());
                }
            }
        }
    }

    // ---- if / else -----------------------------------------------------------

    private IfStmt parseIf(Line ifLine) {
        pos++;
        String content = ifLine.content();
        // Extract condition text after "if "
        String condText = content.substring("if".length()).trim();
        Condition condition = parseCondition(condText, ifLine.number());
        List<Stmt> thenBody = parseBody(ifLine.indent());

        // Look ahead for else / else if
        List<Stmt> elseBody = new ArrayList<>();
        if (pos < lines.size()) {
            Line next = lines.get(pos);
            if (next.indent() == ifLine.indent()) {
                String nc = next.content().trim();
                if (nc.equals("else")) {
                    pos++;
                    elseBody = parseBody(next.indent());
                } else if (nc.startsWith("else if ")) {
                    // Rewrite as "if ..." at same indent and recurse
                    String newContent = nc.substring("else ".length()); // "if ..."
                    Line elseIfLine = new Line(next.indent(), newContent, next.number());
                    // Replace in our list temporarily
                    List<Line> saved = lines;
                    List<Line> modified = new ArrayList<>(lines);
                    modified.set(pos, elseIfLine);
                    lines = modified;
                    IfStmt nestedIf = parseIf(elseIfLine);
                    lines = saved;
                    elseBody = List.of(nestedIf);
                }
            }
        }
        return new IfStmt(condition, thenBody, Collections.unmodifiableList(elseBody), ifLine.number());
    }

    // ---- Condition parsing ---------------------------------------------------

    private Condition parseCondition(String text, int lineNum) {
        text = text.trim();
        if (text.isEmpty()) {
            throw new LemonLangError(scriptFile, lineNum,
                    "Leere Bedingung nach 'if'.",
                    "Beispiele: if chance 50 / if has perm lemon.vip / if apples >= 10");
        }

        // Check for 'not'
        if (text.startsWith("not ")) {
            Condition inner = parseCondition(text.substring(4), lineNum);
            return new Not(inner, lineNum);
        }

        // Check for 'and' / 'or' (simple left-right split, right-to-left binding)
        // We split on " and " and " or " at the top level only
        int andIdx = indexOfKeyword(text, " and ");
        int orIdx = indexOfKeyword(text, " or ");
        if (andIdx >= 0 && (orIdx < 0 || andIdx < orIdx)) {
            Condition left = parseCondition(text.substring(0, andIdx), lineNum);
            Condition right = parseCondition(text.substring(andIdx + 5), lineNum);
            return new And(left, right, lineNum);
        }
        if (orIdx >= 0) {
            Condition left = parseCondition(text.substring(0, orIdx), lineNum);
            Condition right = parseCondition(text.substring(orIdx + 4), lineNum);
            return new Or(left, right, lineNum);
        }

        List<String> words = ArgScanner.words(text);
        if (words.isEmpty()) {
            throw new LemonLangError(scriptFile, lineNum, "Leere Bedingung.", null);
        }

        String first = words.get(0).toLowerCase();

        // Predicate conditions
        switch (first) {
            case "at": {
                // at X Y Z
                return new Predicate("at", words.subList(1, words.size()), lineNum);
            }
            case "near": {
                // near X Y Z R
                return new Predicate("near", words.subList(1, words.size()), lineNum);
            }
            case "chance": {
                // chance N
                return new Predicate("chance", words.subList(1, words.size()), lineNum);
            }
            case "has": {
                // has perm node / has item MAT
                return new Predicate("has", words.subList(1, words.size()), lineNum);
            }
            default: {
                // Compare: var op value
                // e.g.: apples >= 100
                if (words.size() < 3) {
                    throw new LemonLangError(scriptFile, lineNum,
                            "Ungueltige Bedingung: '" + text + "'.",
                            "Vergleich-Syntax: variable operator wert (z.B. apples >= 100)");
                }
                String var = words.get(0);
                String op = words.get(1);
                String value = words.get(2);
                return new Compare(var, op, value, lineNum);
            }
        }
    }

    /** Returns the index of the first occurrence of keyword in s, or -1. */
    private static int indexOfKeyword(String s, String keyword) {
        int idx = 0;
        while (true) {
            int found = s.indexOf(keyword, idx);
            if (found < 0) return -1;
            return found;
        }
    }

    // ---- Helpers -------------------------------------------------------------

    private int peekIndent() {
        if (pos >= lines.size()) return -1;
        return lines.get(pos).indent();
    }
}
