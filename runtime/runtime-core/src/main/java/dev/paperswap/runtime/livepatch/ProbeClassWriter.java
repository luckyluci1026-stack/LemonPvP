package dev.paperswap.runtime.livepatch;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes tiny class files (version 52, straight-line code only, so no stack map frames
 * are needed) for the capability probes. Not a general purpose class writer.
 */
final class ProbeClassWriter {

    static final int ACC_PUBLIC = 0x0001;
    static final int ACC_PRIVATE = 0x0002;
    static final int ACC_STATIC = 0x0008;
    private static final int ACC_SUPER = 0x0020;

    private final String name;
    private final String superName;
    private final List<String> interfaces = new ArrayList<>();
    private final List<String[]> fields = new ArrayList<>(); // name, descriptor, access
    private final Map<String, int[]> methods = new LinkedHashMap<>(); // name -> {access, returnValue}
    private boolean invalidStack;

    /**
     * @param name      internal class name
     * @param superName internal super class name
     */
    ProbeClassWriter(String name, String superName) {
        this.name = name;
        this.superName = superName;
    }

    ProbeClassWriter implement(String iface) {
        this.interfaces.add(iface);
        return this;
    }

    /** Writes every int method with max_stack 0, which the bytecode verifier must reject. */
    ProbeClassWriter invalidStack() {
        this.invalidStack = true;
        return this;
    }

    ProbeClassWriter staticIntField(String fieldName) {
        this.fields.add(new String[] {fieldName, "I", Integer.toString(ACC_PUBLIC | ACC_STATIC)});
        return this;
    }

    /** Adds {@code static int name() { return value; }} with the given access flags (STATIC is added). */
    ProbeClassWriter staticIntMethod(String methodName, int access, int value) {
        if (value < 0 || value > 5) {
            throw new IllegalArgumentException("value must be 0..5");
        }
        this.methods.put(methodName, new int[] {access | ACC_STATIC, value});
        return this;
    }

    byte[] toBytes() {
        ConstantPool cp = new ConstantPool();
        int thisClass = cp.classRef(this.name);
        int superClass = cp.classRef(this.superName);
        int[] ifaces = this.interfaces.stream().mapToInt(cp::classRef).toArray();
        int superInit = cp.methodRef(this.superName, "<init>", "()V");
        int codeAttr = cp.utf8("Code");
        int initName = cp.utf8("<init>");
        int voidDesc = cp.utf8("()V");
        int intDesc = cp.utf8("()I");
        int[][] fieldRefs = new int[this.fields.size()][];
        for (int i = 0; i < this.fields.size(); i++) {
            fieldRefs[i] = new int[] {cp.utf8(this.fields.get(i)[0]), cp.utf8(this.fields.get(i)[1])};
        }
        Map<String, Integer> methodNames = new LinkedHashMap<>();
        for (String m : this.methods.keySet()) {
            methodNames.put(m, cp.utf8(m));
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0xCAFEBABE);
            out.writeShort(0);
            out.writeShort(52);
            cp.write(out);
            out.writeShort(ACC_PUBLIC | ACC_SUPER);
            out.writeShort(thisClass);
            out.writeShort(superClass);
            out.writeShort(ifaces.length);
            for (int i : ifaces) {
                out.writeShort(i);
            }
            out.writeShort(this.fields.size());
            for (int i = 0; i < this.fields.size(); i++) {
                out.writeShort(Integer.parseInt(this.fields.get(i)[2]));
                out.writeShort(fieldRefs[i][0]);
                out.writeShort(fieldRefs[i][1]);
                out.writeShort(0);
            }
            out.writeShort(1 + this.methods.size());
            // public <init>() { super(); }
            out.writeShort(ACC_PUBLIC);
            out.writeShort(initName);
            out.writeShort(voidDesc);
            out.writeShort(1);
            out.writeShort(codeAttr);
            out.writeInt(12 + 5);
            out.writeShort(1); // max_stack
            out.writeShort(1); // max_locals
            out.writeInt(5);
            out.writeByte(0x2a); // aload_0
            out.writeByte(0xb7); // invokespecial
            out.writeShort(superInit);
            out.writeByte(0xb1); // return
            out.writeShort(0); // exception table
            out.writeShort(0); // attributes
            for (Map.Entry<String, int[]> m : this.methods.entrySet()) {
                out.writeShort(m.getValue()[0]);
                out.writeShort(methodNames.get(m.getKey()));
                out.writeShort(intDesc);
                out.writeShort(1);
                out.writeShort(codeAttr);
                out.writeInt(12 + 2);
                out.writeShort(this.invalidStack ? 0 : 1); // max_stack
                out.writeShort(0);
                out.writeInt(2);
                out.writeByte(0x03 + m.getValue()[1]); // iconst_<n>
                out.writeByte(0xac); // ireturn
                out.writeShort(0);
                out.writeShort(0);
            }
            out.writeShort(0); // class attributes
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Constant pool with de-duplication. */
    private static final class ConstantPool {
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private final DataOutputStream out = new DataOutputStream(this.data);
        private final Map<String, Integer> index = new LinkedHashMap<>();
        private int next = 1;

        int utf8(String s) {
            return this.index.computeIfAbsent("u:" + s, k -> add(o -> {
                o.writeByte(1);
                o.writeUTF(s);
            }));
        }

        int classRef(String internalName) {
            int n = utf8(internalName);
            return this.index.computeIfAbsent("c:" + internalName, k -> add(o -> {
                o.writeByte(7);
                o.writeShort(n);
            }));
        }

        int methodRef(String owner, String methodName, String descriptor) {
            int c = classRef(owner);
            int n = utf8(methodName);
            int d = utf8(descriptor);
            int nt = this.index.computeIfAbsent("nt:" + methodName + descriptor, k -> add(o -> {
                o.writeByte(12);
                o.writeShort(n);
                o.writeShort(d);
            }));
            return this.index.computeIfAbsent("m:" + owner + "." + methodName + descriptor, k -> add(o -> {
                o.writeByte(10);
                o.writeShort(c);
                o.writeShort(nt);
            }));
        }

        private int add(Entry e) {
            try {
                e.write(this.out);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return this.next++;
        }

        void write(DataOutputStream target) throws IOException {
            this.out.flush();
            target.writeShort(this.next);
            target.write(this.data.toByteArray());
        }

        @FunctionalInterface
        private interface Entry {
            void write(DataOutputStream out) throws IOException;
        }
    }
}
