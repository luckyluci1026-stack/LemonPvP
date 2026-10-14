package dev.lemonpvp.runtime.livepatch.classfile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Minimal class file reader (JVMS chapter 4) that extracts what matters for class
 * redefinition: names, hierarchy, fields, methods and a normalized digest of every method
 * body.
 *
 * <p>The body digest resolves every constant-pool reference in the bytecode to its value
 * (class, member, string, number, bootstrap method). A change in one method therefore
 * does not make every other method look changed just because the compiler renumbered the
 * constant pool. Debug attributes (line numbers, local variable names, stack maps) are not
 * part of the digest.</p>
 *
 * <p>The reader validates the structure it walks (bounds, constant-pool indices and tags,
 * instruction lengths). It is not a bytecode verifier: the JVM still verifies a class when
 * it is redefined.</p>
 */
public final class ClassFileReader {

    private static final int MAGIC = 0xCAFEBABE;
    private static final int MAX_DEPTH = 16;

    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_DYNAMIC = 17;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;
    private static final int CONSTANT_MODULE = 19;
    private static final int CONSTANT_PACKAGE = 20;

    private final byte[] b;
    private int pos;
    private int[] tags;
    private int[] refA;
    private int[] refB;
    private long[] numbers;
    private String[] utf8;
    private final List<int[]> bootstrapMethods = new ArrayList<>(); // [methodHandleIndex, arg...]

    private ClassFileReader(byte[] bytes) {
        this.b = bytes;
    }

    /**
     * Parses a class file.
     *
     * @param bytes class file bytes
     * @return the structure
     * @throws ClassFormatException if the bytes are malformed
     */
    public static ClassInfo read(byte[] bytes) throws ClassFormatException {
        try {
            return new ClassFileReader(bytes).parse();
        } catch (IndexOutOfBoundsException | NegativeArraySizeException e) {
            throw new ClassFormatException("truncated or corrupt class file (" + e.getMessage() + ")");
        }
    }

    /**
     * Constant-pool strings of a class file: enough to see which APIs a class refers to
     * without parsing members (used for plugin compatibility checks).
     *
     * @param majorVersion class file major version
     * @param utf8         every UTF-8 constant (class names, descriptors, strings)
     */
    public record Constants(int majorVersion, List<String> utf8) {
        public Constants {
            utf8 = List.copyOf(utf8);
        }
    }

    /**
     * Reads only the header and constant pool.
     *
     * @param bytes class file bytes
     * @return version and UTF-8 constants
     * @throws ClassFormatException if the bytes are malformed
     */
    public static Constants constants(byte[] bytes) throws ClassFormatException {
        try {
            ClassFileReader r = new ClassFileReader(bytes);
            if (bytes.length < 10 || r.u4() != MAGIC) {
                throw new ClassFormatException("not a class file (bad magic number)");
            }
            r.u2();
            int major = r.u2();
            r.readConstantPool();
            List<String> strings = new ArrayList<>();
            for (String u : r.utf8) {
                if (u != null) {
                    strings.add(u);
                }
            }
            return new Constants(major, strings);
        } catch (IndexOutOfBoundsException | NegativeArraySizeException e) {
            throw new ClassFormatException("truncated or corrupt class file (" + e.getMessage() + ")");
        }
    }

    private record RawMethod(int access, String name, String descriptor, int codeOffset, int codeLength) {
    }

    private ClassInfo parse() throws ClassFormatException {
        if (this.b.length < 10 || u4() != MAGIC) {
            throw new ClassFormatException("not a class file (bad magic number)");
        }
        u2(); // minor
        int major = u2();
        if (major < 45 || major > 255) {
            throw new ClassFormatException("unsupported class file version " + major);
        }
        readConstantPool();
        int access = u2();
        String name = className(u2());
        int superIndex = u2();
        String superName = superIndex == 0 ? null : className(superIndex);
        if (superName == null && !"java/lang/Object".equals(name) && (access & 0x8000) == 0) {
            throw new ClassFormatException("class " + name + " has no super class");
        }
        int interfaceCount = u2();
        List<String> interfaces = new ArrayList<>(interfaceCount);
        for (int i = 0; i < interfaceCount; i++) {
            interfaces.add(className(u2()));
        }

        List<ClassInfo.FieldInfo> fields = new ArrayList<>();
        int fieldCount = u2();
        for (int i = 0; i < fieldCount; i++) {
            int fAccess = u2();
            String fName = utf8At(u2());
            String fDesc = utf8At(u2());
            String constant = null;
            int attrCount = u2();
            for (int a = 0; a < attrCount; a++) {
                String attrName = utf8At(u2());
                int len = u4();
                int start = this.pos;
                if (attrName.equals("ConstantValue")) {
                    constant = describe(u2(), 0);
                }
                this.pos = checkedEnd(start, len);
            }
            fields.add(new ClassInfo.FieldInfo(fName, fDesc, fAccess, constant));
        }

        List<RawMethod> rawMethods = new ArrayList<>();
        int methodCount = u2();
        for (int i = 0; i < methodCount; i++) {
            int mAccess = u2();
            String mName = utf8At(u2());
            String mDesc = utf8At(u2());
            int codeOffset = -1;
            int codeLength = 0;
            int attrCount = u2();
            for (int a = 0; a < attrCount; a++) {
                String attrName = utf8At(u2());
                int len = u4();
                int start = this.pos;
                if (attrName.equals("Code")) {
                    codeOffset = start;
                    codeLength = len;
                }
                this.pos = checkedEnd(start, len);
            }
            rawMethods.add(new RawMethod(mAccess, mName, mDesc, codeOffset, codeLength));
        }

        String nestHost = null;
        List<String> nestMembers = List.of();
        List<String> permitted = List.of();
        List<String> record = null;
        int attrCount = u2();
        for (int a = 0; a < attrCount; a++) {
            String attrName = utf8At(u2());
            int len = u4();
            int start = this.pos;
            switch (attrName) {
                case "NestHost" -> nestHost = className(u2());
                case "NestMembers" -> nestMembers = classList();
                case "PermittedSubclasses" -> permitted = classList();
                case "Record" -> record = recordComponents();
                case "BootstrapMethods" -> readBootstrapMethods();
                default -> {
                    // other attributes may change freely
                }
            }
            this.pos = checkedEnd(start, len);
        }
        if (this.pos != this.b.length) {
            throw new ClassFormatException("unexpected " + (this.b.length - this.pos) + " trailing bytes");
        }

        List<ClassInfo.MethodInfo> methods = new ArrayList<>(rawMethods.size());
        for (RawMethod m : rawMethods) {
            String digest = m.codeOffset() < 0 ? null : codeDigest(m.codeOffset(), m.codeLength(), m.name());
            methods.add(new ClassInfo.MethodInfo(m.name(), m.descriptor(), m.access(), digest, m.codeOffset()));
        }
        return new ClassInfo(major, access, name, superName, interfaces, fields, methods, nestHost, nestMembers, permitted, record);
    }

    // ------------------------------------------------------------------ constant pool

    private void readConstantPool() throws ClassFormatException {
        int count = u2();
        if (count == 0) {
            throw new ClassFormatException("empty constant pool");
        }
        this.tags = new int[count];
        this.refA = new int[count];
        this.refB = new int[count];
        this.numbers = new long[count];
        this.utf8 = new String[count];
        for (int i = 1; i < count; i++) {
            int tag = u1();
            this.tags[i] = tag;
            switch (tag) {
                case CONSTANT_UTF8 -> {
                    int len = u2();
                    this.utf8[i] = decodeUtf8(this.pos - 2, len);
                    this.pos = checkedEnd(this.pos, len);
                }
                case CONSTANT_INTEGER, CONSTANT_FLOAT -> this.numbers[i] = u4();
                case CONSTANT_LONG, CONSTANT_DOUBLE -> {
                    this.numbers[i] = ((long) u4() << 32) | (u4() & 0xFFFFFFFFL);
                    i++; // takes two slots
                    if (i >= count) {
                        throw new ClassFormatException("8-byte constant at the end of the constant pool");
                    }
                }
                case CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHOD_TYPE, CONSTANT_MODULE, CONSTANT_PACKAGE -> this.refA[i] = u2();
                case CONSTANT_FIELDREF, CONSTANT_METHODREF, CONSTANT_INTERFACE_METHODREF, CONSTANT_NAME_AND_TYPE,
                     CONSTANT_DYNAMIC, CONSTANT_INVOKE_DYNAMIC -> {
                    this.refA[i] = u2();
                    this.refB[i] = u2();
                }
                case CONSTANT_METHOD_HANDLE -> {
                    this.refA[i] = u1();
                    this.refB[i] = u2();
                }
                default -> throw new ClassFormatException("unknown constant pool tag " + tag + " at index " + i);
            }
        }
    }

    private String decodeUtf8(int lengthOffset, int len) throws ClassFormatException {
        if (lengthOffset + 2 + len > this.b.length) {
            throw new ClassFormatException("truncated UTF-8 constant");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(this.b, lengthOffset, len + 2))) {
            return in.readUTF();
        } catch (IOException e) {
            throw new ClassFormatException("malformed UTF-8 constant: " + e.getMessage());
        }
    }

    private void checkIndex(int index, int expectedTag) throws ClassFormatException {
        if (index <= 0 || index >= this.tags.length || this.tags[index] != expectedTag) {
            throw new ClassFormatException("constant pool index " + index + " does not refer to a tag-" + expectedTag + " entry");
        }
    }

    private String utf8At(int index) throws ClassFormatException {
        checkIndex(index, CONSTANT_UTF8);
        return this.utf8[index];
    }

    private String className(int index) throws ClassFormatException {
        checkIndex(index, CONSTANT_CLASS);
        return utf8At(this.refA[index]);
    }

    private List<String> classList() throws ClassFormatException {
        int n = u2();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(className(u2()));
        }
        return out;
    }

    private List<String> recordComponents() throws ClassFormatException {
        int n = u2();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String cName = utf8At(u2());
            String cDesc = utf8At(u2());
            int attrs = u2();
            for (int a = 0; a < attrs; a++) {
                u2();
                int len = u4();
                this.pos = checkedEnd(this.pos, len);
            }
            out.add(cName + ":" + cDesc);
        }
        return out;
    }

    private void readBootstrapMethods() throws ClassFormatException {
        int n = u2();
        for (int i = 0; i < n; i++) {
            int handle = u2();
            int argc = u2();
            int[] entry = new int[argc + 1];
            entry[0] = handle;
            for (int a = 0; a < argc; a++) {
                entry[a + 1] = u2();
            }
            this.bootstrapMethods.add(entry);
        }
    }

    /** Canonical text of a constant, independent of its index. */
    private String describe(int index, int depth) throws ClassFormatException {
        if (depth > MAX_DEPTH) {
            throw new ClassFormatException("constant pool references nested too deeply");
        }
        if (index <= 0 || index >= this.tags.length || this.tags[index] == 0) {
            throw new ClassFormatException("invalid constant pool index " + index);
        }
        return switch (this.tags[index]) {
            case CONSTANT_UTF8 -> "u:" + this.utf8[index];
            case CONSTANT_INTEGER -> "i:" + (int) this.numbers[index];
            case CONSTANT_FLOAT -> "f:" + Integer.toHexString((int) this.numbers[index]);
            case CONSTANT_LONG -> "j:" + this.numbers[index];
            case CONSTANT_DOUBLE -> "d:" + Long.toHexString(this.numbers[index]);
            case CONSTANT_CLASS -> "c:" + utf8At(this.refA[index]);
            case CONSTANT_STRING -> "s:" + utf8At(this.refA[index]);
            case CONSTANT_METHOD_TYPE -> "mt:" + utf8At(this.refA[index]);
            case CONSTANT_MODULE -> "mod:" + utf8At(this.refA[index]);
            case CONSTANT_PACKAGE -> "pkg:" + utf8At(this.refA[index]);
            case CONSTANT_FIELDREF -> "F:" + className(this.refA[index]) + "." + nameAndType(this.refB[index]);
            case CONSTANT_METHODREF -> "M:" + className(this.refA[index]) + "." + nameAndType(this.refB[index]);
            case CONSTANT_INTERFACE_METHODREF -> "IM:" + className(this.refA[index]) + "." + nameAndType(this.refB[index]);
            case CONSTANT_NAME_AND_TYPE -> "nt:" + nameAndType(index);
            case CONSTANT_METHOD_HANDLE -> "mh" + this.refA[index] + ":" + describe(this.refB[index], depth + 1);
            case CONSTANT_DYNAMIC -> "dyn:" + bootstrap(this.refA[index], depth) + ":" + nameAndType(this.refB[index]);
            case CONSTANT_INVOKE_DYNAMIC -> "indy:" + bootstrap(this.refA[index], depth) + ":" + nameAndType(this.refB[index]);
            default -> throw new ClassFormatException("unexpected constant tag " + this.tags[index]);
        };
    }

    private String nameAndType(int index) throws ClassFormatException {
        checkIndex(index, CONSTANT_NAME_AND_TYPE);
        return utf8At(this.refA[index]) + ":" + utf8At(this.refB[index]);
    }

    private String bootstrap(int bsmIndex, int depth) throws ClassFormatException {
        if (bsmIndex < 0 || bsmIndex >= this.bootstrapMethods.size()) {
            throw new ClassFormatException("invalid bootstrap method index " + bsmIndex);
        }
        int[] entry = this.bootstrapMethods.get(bsmIndex);
        StringBuilder sb = new StringBuilder("[").append(describe(entry[0], depth + 1));
        for (int i = 1; i < entry.length; i++) {
            sb.append(',').append(describe(entry[i], depth + 1));
        }
        return sb.append(']').toString();
    }

    // ------------------------------------------------------------------ code

    private String codeDigest(int offset, int length, String methodName) throws ClassFormatException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
        int end = checkedEnd(offset, length);
        int p = offset;
        int maxStack = u2(p);
        int maxLocals = u2(p + 2);
        long codeLength = u4(p + 4) & 0xFFFFFFFFL;
        int codeStart = p + 8;
        if (codeLength <= 0 || codeLength >= 65536 || codeStart + codeLength > end) {
            throw new ClassFormatException("method " + methodName + ": invalid code length " + codeLength);
        }
        int codeEnd = codeStart + (int) codeLength;
        update(md, "ms" + maxStack + "ml" + maxLocals + ";");
        int pc = codeStart;
        while (pc < codeEnd) {
            int op = this.b[pc] & 0xFF;
            int rel = pc - codeStart;
            int len;
            md.update((byte) op);
            switch (op) {
                case 0x12 -> { // ldc
                    len = 2;
                    update(md, describe(this.b[pc + 1] & 0xFF, 0));
                }
                case 0x13, 0x14, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1 -> {
                    len = 3;
                    update(md, describe(u2(pc + 1), 0));
                }
                case 0xb9 -> { // invokeinterface
                    len = 5;
                    update(md, describe(u2(pc + 1), 0));
                    md.update(this.b, pc + 3, 1);
                }
                case 0xba -> { // invokedynamic
                    len = 5;
                    update(md, describe(u2(pc + 1), 0));
                }
                case 0xc5 -> { // multianewarray
                    len = 4;
                    update(md, describe(u2(pc + 1), 0));
                    md.update(this.b, pc + 3, 1);
                }
                case 0xaa -> { // tableswitch
                    int base = pc + 1 + ((4 - ((rel + 1) % 4)) % 4);
                    int low = (int) u4(base + 4);
                    int high = (int) u4(base + 8);
                    if (high < low || (long) high - low > 65535) {
                        throw new ClassFormatException("method " + methodName + ": invalid tableswitch");
                    }
                    len = base + 12 + (high - low + 1) * 4 - pc;
                    md.update(this.b, pc + 1, len - 1);
                }
                case 0xab -> { // lookupswitch
                    int base = pc + 1 + ((4 - ((rel + 1) % 4)) % 4);
                    int pairs = (int) u4(base + 4);
                    if (pairs < 0 || pairs > 65535) {
                        throw new ClassFormatException("method " + methodName + ": invalid lookupswitch");
                    }
                    len = base + 8 + pairs * 8 - pc;
                    md.update(this.b, pc + 1, len - 1);
                }
                case 0xc4 -> { // wide
                    int op2 = this.b[pc + 1] & 0xFF;
                    len = op2 == 0x84 ? 6 : 4;
                    md.update(this.b, pc + 1, len - 1);
                }
                default -> {
                    len = simpleLength(op);
                    if (len < 0) {
                        throw new ClassFormatException("method " + methodName + ": invalid opcode 0x" + Integer.toHexString(op)
                                + " at pc " + rel);
                    }
                    if (len > 1) {
                        md.update(this.b, pc + 1, len - 1);
                    }
                }
            }
            pc += len;
        }
        if (pc != codeEnd) {
            throw new ClassFormatException("method " + methodName + ": last instruction exceeds the code array");
        }
        int q = codeEnd;
        int handlers = u2(q);
        q += 2;
        for (int i = 0; i < handlers; i++) {
            md.update(this.b, q, 6);
            int catchType = u2(q + 6);
            update(md, catchType == 0 ? "any" : describe(catchType, 0));
            q += 8;
        }
        if (q > end) {
            throw new ClassFormatException("method " + methodName + ": exception table exceeds the Code attribute");
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /** Length of instructions without constant-pool operands or variable size; -1 if invalid. */
    private static int simpleLength(int op) {
        if (op <= 0x0f) {
            return 1;
        }
        if (op == 0x10) {
            return 2; // bipush
        }
        if (op == 0x11) {
            return 3; // sipush
        }
        if (op >= 0x15 && op <= 0x19) {
            return 2; // xload index
        }
        if (op >= 0x1a && op <= 0x35) {
            return 1;
        }
        if (op >= 0x36 && op <= 0x3a) {
            return 2; // xstore index
        }
        if (op >= 0x3b && op <= 0x83) {
            return 1;
        }
        if (op == 0x84) {
            return 3; // iinc
        }
        if (op >= 0x85 && op <= 0x98) {
            return 1;
        }
        if (op >= 0x99 && op <= 0xa8) {
            return 3; // branches, goto, jsr
        }
        if (op == 0xa9) {
            return 2; // ret
        }
        if (op >= 0xac && op <= 0xb1) {
            return 1; // returns
        }
        if (op == 0xbc) {
            return 2; // newarray
        }
        if (op == 0xbe || op == 0xbf || op == 0xc2 || op == 0xc3) {
            return 1;
        }
        if (op == 0xc6 || op == 0xc7) {
            return 3; // ifnull, ifnonnull
        }
        if (op == 0xc8 || op == 0xc9) {
            return 5; // goto_w, jsr_w
        }
        return -1;
    }

    private static void update(MessageDigest md, String s) {
        md.update(s.getBytes(StandardCharsets.UTF_8));
        md.update((byte) 0);
    }

    // ------------------------------------------------------------------ primitives

    private int checkedEnd(int start, int length) throws ClassFormatException {
        long end = (long) start + (length & 0xFFFFFFFFL);
        if (length < 0 || end > this.b.length) {
            throw new ClassFormatException("attribute or constant exceeds the class file");
        }
        return (int) end;
    }

    private int u1() {
        return this.b[this.pos++] & 0xFF;
    }

    private int u2() {
        int v = u2(this.pos);
        this.pos += 2;
        return v;
    }

    private int u4() {
        int v = (int) u4(this.pos);
        this.pos += 4;
        return v;
    }

    private int u2(int at) {
        return ((this.b[at] & 0xFF) << 8) | (this.b[at + 1] & 0xFF);
    }

    private long u4(int at) {
        return ((long) (this.b[at] & 0xFF) << 24) | ((this.b[at + 1] & 0xFF) << 16) | ((this.b[at + 2] & 0xFF) << 8)
                | (this.b[at + 3] & 0xFF);
    }
}
