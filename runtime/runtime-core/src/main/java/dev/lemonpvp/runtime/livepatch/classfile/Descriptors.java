package dev.lemonpvp.runtime.livepatch.classfile;

import java.util.ArrayList;
import java.util.List;

/** Converts JVM descriptors into short, readable text for reports. */
public final class Descriptors {

    private Descriptors() {
    }

    /**
     * @param name       method name
     * @param descriptor method descriptor
     * @return e.g. {@code execute(CommandSender, String, String[]) : boolean}
     */
    public static String prettyMethod(String name, String descriptor) {
        int close = descriptor.indexOf(')');
        if (!descriptor.startsWith("(") || close < 0) {
            return name + descriptor;
        }
        List<String> params = new ArrayList<>();
        int[] pos = {1};
        while (pos[0] < close) {
            params.add(prettyType(descriptor, pos));
        }
        int[] r = {close + 1};
        String ret = r[0] < descriptor.length() ? prettyType(descriptor, r) : "?";
        String shown = switch (name) {
            case "<init>" -> "constructor";
            case "<clinit>" -> "static initializer";
            default -> name;
        };
        if (name.equals("<clinit>")) {
            return shown;
        }
        return shown + "(" + String.join(", ", params) + ")" + (name.equals("<init>") ? "" : " : " + ret);
    }

    /**
     * @param descriptor field descriptor
     * @return e.g. {@code List} or {@code int[]}
     */
    public static String prettyField(String descriptor) {
        return prettyType(descriptor, new int[] {0});
    }

    private static String prettyType(String d, int[] pos) {
        int dims = 0;
        while (pos[0] < d.length() && d.charAt(pos[0]) == '[') {
            dims++;
            pos[0]++;
        }
        if (pos[0] >= d.length()) {
            return "?";
        }
        char c = d.charAt(pos[0]++);
        String base = switch (c) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";
            case 'L' -> {
                int end = d.indexOf(';', pos[0]);
                if (end < 0) {
                    pos[0] = d.length();
                    yield "?";
                }
                String cls = d.substring(pos[0], end);
                pos[0] = end + 1;
                int slash = Math.max(cls.lastIndexOf('/'), cls.lastIndexOf('$'));
                yield cls.substring(slash + 1);
            }
            default -> "?";
        };
        return base + "[]".repeat(dims);
    }
}
