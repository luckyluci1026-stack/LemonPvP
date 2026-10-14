package dev.lemonpvp.runtime.livepatch.classfile;

import java.util.List;
import java.util.Optional;

/**
 * Structure of one class file as far as class redefinition is concerned. Names are in
 * internal form ({@code java/lang/Object}).
 *
 * @param majorVersion        class file major version
 * @param access              class access flags
 * @param name                class name
 * @param superName           super class, {@code null} only for {@code java/lang/Object}
 * @param interfaces          directly implemented interfaces, in declaration order
 * @param fields              fields, in declaration order
 * @param methods             methods, in declaration order
 * @param nestHost            {@code NestHost} attribute, or {@code null}
 * @param nestMembers         {@code NestMembers} attribute (empty if absent)
 * @param permittedSubclasses {@code PermittedSubclasses} attribute (empty if absent)
 * @param recordComponents    {@code Record} attribute as {@code name:descriptor}, or {@code null} if absent
 */
public record ClassInfo(
        int majorVersion,
        int access,
        String name,
        String superName,
        List<String> interfaces,
        List<FieldInfo> fields,
        List<MethodInfo> methods,
        String nestHost,
        List<String> nestMembers,
        List<String> permittedSubclasses,
        List<String> recordComponents
) {
    public ClassInfo {
        interfaces = List.copyOf(interfaces);
        fields = List.copyOf(fields);
        methods = List.copyOf(methods);
        nestMembers = List.copyOf(nestMembers);
        permittedSubclasses = List.copyOf(permittedSubclasses);
        recordComponents = recordComponents == null ? null : List.copyOf(recordComponents);
    }

    /** @return binary name with dots, e.g. {@code org.spigotmc.TicksPerSecondCommand} */
    public String javaName() {
        return this.name.replace('/', '.');
    }

    /** @return package in internal form ({@code ""} for the unnamed package) */
    public String packageName() {
        int i = this.name.lastIndexOf('/');
        return i < 0 ? "" : this.name.substring(0, i);
    }

    /**
     * @param methodName name
     * @param descriptor descriptor
     * @return the method, if declared
     */
    public Optional<MethodInfo> method(String methodName, String descriptor) {
        return this.methods.stream().filter(m -> m.name().equals(methodName) && m.descriptor().equals(descriptor)).findFirst();
    }

    /**
     * A field.
     *
     * @param name          name
     * @param descriptor    type descriptor
     * @param access        access flags
     * @param constantValue resolved {@code ConstantValue} attribute, or {@code null}
     */
    public record FieldInfo(String name, String descriptor, int access, String constantValue) {
    }

    /**
     * A method.
     *
     * @param name       name ({@code <init>} for constructors, {@code <clinit>} for the static initializer)
     * @param descriptor descriptor
     * @param access     access flags
     * @param codeDigest SHA-256 of the normalized {@code Code} attribute (constant-pool
     *                   references resolved, debug attributes ignored), or {@code null} for
     *                   abstract and native methods
     * @param codeOffset offset of the {@code Code} attribute's {@code max_stack} field in
     *                   the class file, or {@code -1}
     */
    public record MethodInfo(String name, String descriptor, int access, String codeDigest, int codeOffset) {

        /** @return whether this is a constructor */
        public boolean isConstructor() {
            return "<init>".equals(this.name);
        }

        /** @return whether this is the static initializer */
        public boolean isStaticInitializer() {
            return "<clinit>".equals(this.name);
        }

        /** @return whether the compiler generated it (e.g. a lambda body or bridge method) */
        public boolean isSynthetic() {
            return (this.access & 0x1000) != 0;
        }

        /** @return readable signature, e.g. {@code execute(CommandSender, String, String[]) : boolean} */
        public String pretty() {
            return Descriptors.prettyMethod(this.name, this.descriptor);
        }
    }
}
