package de.lemonpvp.smpcontent.ability;

import java.util.Locale;
import java.util.Map;

/**
 * Ein winziger Rechner für eigene Animationen.
 *
 * Damit man Formen selbst bauen kann, ohne Java anzufassen:
 * <pre>
 *   x: "cos(a + t * 0.3) * radius"
 *   y: "p * 2"
 *   z: "sin(a + t * 0.3) * radius"
 * </pre>
 *
 * Erlaubt sind + - * / % ^, Klammern, Zahlen, Variablen und die üblichen
 * Funktionen (sin, cos, sqrt, min, max, abs, floor, round, random ...).
 * Die Formel wird einmal beim Laden zerlegt und danach nur noch gerechnet.
 */
public final class Formula {

    /** Ein Rechenschritt im zerlegten Term. */
    public interface Node {
        double eval(Map<String, Double> vars);
    }

    private final Node root;
    private final String source;

    private Formula(Node root, String source) {
        this.root = root;
        this.source = source;
    }

    public static Formula compile(String expression) {
        String text = expression == null ? "0" : expression.trim();
        if (text.isEmpty()) {
            text = "0";
        }
        return new Formula(new Parser(text).parseAll(), text);
    }

    public double eval(Map<String, Double> vars) {
        double value = root.eval(vars);
        return Double.isFinite(value) ? value : 0;
    }

    public String source() {
        return source;
    }

    // ------------------------------------------------------------------
    //  Zerlegen
    // ------------------------------------------------------------------

    private static final class Parser {

        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        Node parseAll() {
            Node node = parseSum();
            skipSpaces();
            if (pos < text.length()) {
                throw new IllegalArgumentException(
                        "Unerwartetes Zeichen '" + text.charAt(pos) + "' an Stelle " + (pos + 1));
            }
            return node;
        }

        private Node parseSum() {
            Node left = parseProduct();
            while (true) {
                skipSpaces();
                if (eat('+')) {
                    Node right = parseProduct();
                    Node a = left;
                    left = vars -> a.eval(vars) + right.eval(vars);
                } else if (eat('-')) {
                    Node right = parseProduct();
                    Node a = left;
                    left = vars -> a.eval(vars) - right.eval(vars);
                } else {
                    return left;
                }
            }
        }

        private Node parseProduct() {
            Node left = parsePower();
            while (true) {
                skipSpaces();
                if (eat('*')) {
                    Node right = parsePower();
                    Node a = left;
                    left = vars -> a.eval(vars) * right.eval(vars);
                } else if (eat('/')) {
                    Node right = parsePower();
                    Node a = left;
                    left = vars -> {
                        double divisor = right.eval(vars);
                        return divisor == 0 ? 0 : a.eval(vars) / divisor;
                    };
                } else if (eat('%')) {
                    Node right = parsePower();
                    Node a = left;
                    left = vars -> {
                        double divisor = right.eval(vars);
                        return divisor == 0 ? 0 : a.eval(vars) % divisor;
                    };
                } else {
                    return left;
                }
            }
        }

        private Node parsePower() {
            Node base = parseUnary();
            skipSpaces();
            if (eat('^')) {
                Node exponent = parsePower();
                return vars -> Math.pow(base.eval(vars), exponent.eval(vars));
            }
            return base;
        }

        private Node parseUnary() {
            skipSpaces();
            if (eat('-')) {
                Node inner = parseUnary();
                return vars -> -inner.eval(vars);
            }
            eat('+');
            return parsePrimary();
        }

        private Node parsePrimary() {
            skipSpaces();
            if (eat('(')) {
                Node inner = parseSum();
                skipSpaces();
                if (!eat(')')) {
                    throw new IllegalArgumentException("Es fehlt eine schließende Klammer.");
                }
                return inner;
            }
            int start = pos;
            while (pos < text.length()
                    && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
                pos++;
            }
            if (pos > start) {
                double value = Double.parseDouble(text.substring(start, pos));
                return vars -> value;
            }
            while (pos < text.length()
                    && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
                pos++;
            }
            if (pos == start) {
                throw new IllegalArgumentException(
                        "Da fehlt etwas an Stelle " + (pos + 1) + ".");
            }
            String name = text.substring(start, pos).toLowerCase(Locale.ROOT);
            skipSpaces();
            if (eat('(')) {
                Node first = parseSum();
                skipSpaces();
                Node second = null;
                if (eat(',')) {
                    second = parseSum();
                    skipSpaces();
                }
                if (!eat(')')) {
                    throw new IllegalArgumentException(
                            "Es fehlt eine schließende Klammer bei " + name + "().");
                }
                return function(name, first, second);
            }
            return switch (name) {
                case "pi" -> vars -> Math.PI;
                case "e" -> vars -> Math.E;
                case "tau" -> vars -> Math.PI * 2;
                default -> vars -> vars.getOrDefault(name, 0.0);
            };
        }

        private Node function(String name, Node a, Node b) {
            return switch (name) {
                case "sin" -> vars -> Math.sin(a.eval(vars));
                case "cos" -> vars -> Math.cos(a.eval(vars));
                case "tan" -> vars -> Math.tan(a.eval(vars));
                case "asin" -> vars -> Math.asin(a.eval(vars));
                case "acos" -> vars -> Math.acos(a.eval(vars));
                case "atan" -> vars -> Math.atan(a.eval(vars));
                case "sqrt" -> vars -> Math.sqrt(Math.max(0, a.eval(vars)));
                case "abs" -> vars -> Math.abs(a.eval(vars));
                case "floor" -> vars -> Math.floor(a.eval(vars));
                case "ceil" -> vars -> Math.ceil(a.eval(vars));
                case "round" -> vars -> Math.round(a.eval(vars));
                case "sign" -> vars -> Math.signum(a.eval(vars));
                case "exp" -> vars -> Math.exp(a.eval(vars));
                case "log" -> vars -> Math.log(Math.max(1e-9, a.eval(vars)));
                case "random" -> vars -> Math.random() * a.eval(vars);
                case "atan2" -> requireTwo(name, b, vars -> Math.atan2(a.eval(vars), b.eval(vars)));
                case "min" -> requireTwo(name, b, vars -> Math.min(a.eval(vars), b.eval(vars)));
                case "max" -> requireTwo(name, b, vars -> Math.max(a.eval(vars), b.eval(vars)));
                case "pow" -> requireTwo(name, b, vars -> Math.pow(a.eval(vars), b.eval(vars)));
                default -> throw new IllegalArgumentException("Unbekannte Funktion: " + name);
            };
        }

        private Node requireTwo(String name, Node second, Node node) {
            if (second == null) {
                throw new IllegalArgumentException(name + "() braucht zwei Werte, z.B. "
                        + name + "(a, b).");
            }
            return node;
        }

        private void skipSpaces() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private boolean eat(char expected) {
            skipSpaces();
            if (pos < text.length() && text.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }
    }
}
