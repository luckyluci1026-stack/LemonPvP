package dev.paperswap.runtime.livepatch;

import dev.paperswap.runtime.livepatch.classfile.ClassInfo;
import dev.paperswap.runtime.livepatch.classfile.ClassInfo.FieldInfo;
import dev.paperswap.runtime.livepatch.classfile.ClassInfo.MethodInfo;
import dev.paperswap.runtime.livepatch.classfile.Descriptors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compares the class running in the server with the class from a patch and lists every
 * difference by {@link ChangeKind}. The comparison follows the rules HotSpot applies when
 * a class is redefined (JVMTI {@code RedefineClasses}): methods are matched by name and
 * descriptor, fields by position, name, type and modifiers.
 */
public final class ClassDiff {

    private static final int ACC_STATIC = 0x0008;

    /** Access flags HotSpot compares for classes (ACC_SUPER is ignored). */
    private static final int CLASS_FLAGS = 0x0001 | 0x0010 | 0x0200 | 0x0400 | 0x1000 | 0x2000 | 0x4000;

    private ClassDiff() {
    }

    /**
     * @param base the class as it runs in the server
     * @param next the class from the patch
     * @return all differences; empty if both are equivalent at the bytecode level
     */
    public static List<ClassChange> compare(ClassInfo base, ClassInfo next) {
        List<ClassChange> out = new ArrayList<>();
        if ((base.access() & CLASS_FLAGS) != (next.access() & CLASS_FLAGS)) {
            out.add(new ClassChange(ChangeKind.CLASS_MODIFIERS,
                    "0x" + Integer.toHexString(base.access()) + " -> 0x" + Integer.toHexString(next.access())));
        }
        if (!Objects.equals(base.superName(), next.superName())) {
            out.add(new ClassChange(ChangeKind.SUPERCLASS_CHANGED, base.superName() + " -> " + next.superName()));
        }
        if (!base.interfaces().equals(next.interfaces())) {
            out.add(new ClassChange(ChangeKind.INTERFACES_CHANGED, base.interfaces() + " -> " + next.interfaces()));
        }
        restricted(out, "NestHost", base.nestHost(), next.nestHost());
        restricted(out, "NestMembers", base.nestMembers(), next.nestMembers());
        restricted(out, "PermittedSubclasses", base.permittedSubclasses(), next.permittedSubclasses());
        restricted(out, "Record", base.recordComponents(), next.recordComponents());
        compareFields(out, base.fields(), next.fields());
        compareMethods(out, base.methods(), next.methods());
        return out;
    }

    private static void restricted(List<ClassChange> out, String attribute, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            out.add(new ClassChange(ChangeKind.RESTRICTED_ATTRIBUTE, attribute + ": " + before + " -> " + after));
        }
    }

    private static String fieldText(FieldInfo f) {
        return f.name() + " : " + Descriptors.prettyField(f.descriptor());
    }

    private static void compareFields(List<ClassChange> out, List<FieldInfo> base, List<FieldInfo> next) {
        Map<String, FieldInfo> baseByName = new LinkedHashMap<>();
        base.forEach(f -> baseByName.put(f.name(), f));
        Map<String, FieldInfo> nextByName = new LinkedHashMap<>();
        next.forEach(f -> nextByName.put(f.name(), f));
        boolean schemaChange = false;
        for (FieldInfo f : base) {
            FieldInfo n = nextByName.get(f.name());
            if (n == null) {
                out.add(new ClassChange(ChangeKind.FIELD_REMOVED, fieldText(f)));
                schemaChange = true;
            } else if (!n.descriptor().equals(f.descriptor()) || n.access() != f.access()) {
                out.add(new ClassChange(ChangeKind.FIELD_CHANGED, fieldText(f) + " -> " + fieldText(n)
                        + (n.access() != f.access() ? " (modifiers 0x" + Integer.toHexString(f.access()) + " -> 0x"
                        + Integer.toHexString(n.access()) + ")" : "")));
                schemaChange = true;
            } else if ((f.access() & ACC_STATIC) != 0 && !Objects.equals(f.constantValue(), n.constantValue())) {
                // Only static fields: the JVM ignores ConstantValue on instance fields (JVMS 4.7.2) and
                // does not report it when it reconstitutes a class file.
                out.add(new ClassChange(ChangeKind.FIELD_CONSTANT_VALUE, fieldText(f)));
            }
        }
        for (FieldInfo n : next) {
            if (!baseByName.containsKey(n.name())) {
                out.add(new ClassChange(ChangeKind.FIELD_ADDED, fieldText(n)));
                schemaChange = true;
            }
        }
        if (!schemaChange) {
            List<String> before = base.stream().map(FieldInfo::name).toList();
            List<String> after = next.stream().map(FieldInfo::name).toList();
            if (!before.equals(after)) {
                out.add(new ClassChange(ChangeKind.FIELDS_REORDERED, before + " -> " + after));
            }
        }
    }

    private static void compareMethods(List<ClassChange> out, List<MethodInfo> base, List<MethodInfo> next) {
        Map<String, MethodInfo> nextByKey = new LinkedHashMap<>();
        next.forEach(m -> nextByKey.put(m.name() + m.descriptor(), m));
        Set<String> baseKeys = new HashSet<>();
        for (MethodInfo m : base) {
            String key = m.name() + m.descriptor();
            baseKeys.add(key);
            MethodInfo n = nextByKey.get(key);
            if (n == null) {
                out.add(new ClassChange(m.isConstructor() ? ChangeKind.CONSTRUCTOR_REMOVED : ChangeKind.METHOD_REMOVED, label(m)));
                continue;
            }
            if (n.access() != m.access()) {
                out.add(new ClassChange(ChangeKind.METHOD_MODIFIERS, label(m) + " (0x" + Integer.toHexString(m.access()) + " -> 0x"
                        + Integer.toHexString(n.access()) + ")"));
            }
            if (!Objects.equals(m.codeDigest(), n.codeDigest())) {
                ChangeKind kind = m.isConstructor() ? ChangeKind.CONSTRUCTOR_BODY
                        : m.isStaticInitializer() ? ChangeKind.STATIC_INITIALIZER : ChangeKind.METHOD_BODY;
                out.add(new ClassChange(kind, label(m)));
            }
        }
        for (MethodInfo n : next) {
            if (!baseKeys.contains(n.name() + n.descriptor())) {
                out.add(new ClassChange(n.isConstructor() ? ChangeKind.CONSTRUCTOR_ADDED : ChangeKind.METHOD_ADDED, label(n)));
            }
        }
    }

    private static String label(MethodInfo m) {
        String text = m.pretty();
        if (m.isSynthetic() && m.name().startsWith("lambda$")) {
            return text + " [lambda body]";
        }
        if (m.isSynthetic()) {
            return text + " [compiler-generated]";
        }
        return text;
    }
}
