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
 * <p>Jump targets and exception ranges enter the digest as instruction numbers, and
 * {@code ldc}/{@code ldc_w} as well as {@code goto}/{@code goto_w} count as the same
 * instruction. The JVM may widen instructions when it merges constant pools during a
 * redefinition; the bytecode it reports afterwards ({@code retransformClasses}) still has
 * the same digest as the class file that was applied.</p>
 *
 * <p>The reader validates the structure it walks (bounds, constant-pool indices and tags,
 * instruction lengths, jump targets). It is not a bytecode verifier; see
 * {@code PreVerifier} for JVMs that do not verify redefined classes.</p>
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
        for (String line : codeLines(offset, length, methodName)) {
            update(md, line);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /**
     * Normalized listing of a {@code Code} attribute: one line for the stack/locals sizes,
     * one per instruction (constant-pool operands resolved, jump targets as instruction
     * numbers, {@code ldc_w}/{@code goto_w}/{@code jsr_w} as their short forms) and one per
     * exception handler. The method digest is computed from exactly these lines.
     */
    private List<String> codeLines(int offset, int length, String methodName) throws ClassFormatException {
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
        // Pass 1: instruction boundaries. Jump targets and exception ranges are listed as
        // instruction numbers, not byte offsets, so the digest does not change when the JVM
        // widens an instruction (ldc -> ldc_w after merging constant pools on redefinition).
        int[] number = new int[(int) codeLength + 1];
        java.util.Arrays.fill(number, -1);
        int count = 0;
        int pc = codeStart;
        while (pc < codeEnd) {
            number[pc - codeStart] = count++;
            pc += instructionLength(pc, codeStart, methodName);
        }
        if (pc != codeEnd) {
            throw new ClassFormatException("method " + methodName + ": last instruction exceeds the code array");
        }
        number[(int) codeLength] = count;
        // Pass 2: listing.
        List<String> out = new ArrayList<>(count + 2);
        out.add("max_stack " + maxStack + ", max_locals " + maxLocals);
        pc = codeStart;
        int index = 0;
        while (pc < codeEnd) {
            int op = this.b[pc] & 0xFF;
            int rel = pc - codeStart;
            int len = instructionLength(pc, codeStart, methodName);
            String text = switch (op) {
                case 0x12, 0x13 -> "ldc " + describe(op == 0x12 ? this.b[pc + 1] & 0xFF : u2(pc + 1), 0);
                case 0x14, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1, 0xba -> mnemonic(op) + " "
                        + describe(u2(pc + 1), 0);
                case 0xb9, 0xc5 -> mnemonic(op) + " " + describe(u2(pc + 1), 0) + " " + (this.b[pc + 3] & 0xFF);
                case 0xaa -> {
                    int base = pc + 1 + ((4 - ((rel + 1) % 4)) % 4);
                    int low = (int) u4(base + 4);
                    int high = (int) u4(base + 8);
                    StringBuilder sb = new StringBuilder("tableswitch " + low + ".." + high + " default #"
                            + target(number, rel, (int) u4(base), methodName));
                    for (int i = 0; i <= high - low; i++) {
                        sb.append(" #").append(target(number, rel, (int) u4(base + 12 + i * 4), methodName));
                    }
                    yield sb.toString();
                }
                case 0xab -> {
                    int base = pc + 1 + ((4 - ((rel + 1) % 4)) % 4);
                    int pairs = (int) u4(base + 4);
                    StringBuilder sb = new StringBuilder("lookupswitch default #" + target(number, rel, (int) u4(base), methodName));
                    for (int i = 0; i < pairs; i++) {
                        int at = base + 8 + i * 8;
                        sb.append(' ').append((int) u4(at)).append(":#").append(target(number, rel, (int) u4(at + 4), methodName));
                    }
                    yield sb.toString();
                }
                case 0xc8, 0xc9 -> (op == 0xc8 ? "goto" : "jsr") + " #" + target(number, rel, (int) u4(pc + 1), methodName);
                default -> {
                    if ((op >= 0x99 && op <= 0xa8) || op == 0xc6 || op == 0xc7) { // branches
                        yield mnemonic(op) + " #" + target(number, rel, (short) u2(pc + 1), methodName);
                    }
                    StringBuilder sb = new StringBuilder(mnemonic(op));
                    for (int i = 1; i < len; i++) {
                        sb.append(' ').append(this.b[pc + i] & 0xFF);
                    }
                    yield sb.toString();
                }
            };
            out.add("#" + index++ + " " + text);
            pc += len;
        }
        int q = codeEnd;
        int handlers = u2(q);
        q += 2;
        if (q + handlers * 8L > end) {
            throw new ClassFormatException("method " + methodName + ": exception table exceeds the Code attribute");
        }
        for (int i = 0; i < handlers; i++) {
            int catchType = u2(q + 6);
            out.add("try #" + boundary(number, u2(q), methodName) + "-#" + boundary(number, u2(q + 2), methodName) + " -> #"
                    + boundary(number, u2(q + 4), methodName) + " " + (catchType == 0 ? "any" : describe(catchType, 0)));
            q += 8;
        }
        return out;
    }

    /**
     * Normalized listing of one method's bytecode (see the class comment), for diagnostics.
     *
     * @param classFile  class file
     * @param methodName method name
     * @param descriptor method descriptor
     * @return the listing, or an empty list if the method does not exist or has no code
     * @throws ClassFormatException if the class file is invalid
     */
    public static List<String> codeListing(byte[] classFile, String methodName, String descriptor) throws ClassFormatException {
        try {
            ClassFileReader reader = new ClassFileReader(classFile);
            ClassInfo info = reader.parse();
            for (ClassInfo.MethodInfo m : info.methods()) {
                if (m.name().equals(methodName) && m.descriptor().equals(descriptor) && m.codeOffset() >= 0) {
                    return reader.codeLines(m.codeOffset(), (int) (u4Static(classFile, m.codeOffset() - 4)), methodName);
                }
            }
            return List.of();
        } catch (IndexOutOfBoundsException | NegativeArraySizeException e) {
            throw new ClassFormatException("truncated or corrupt class file (" + e.getMessage() + ")");
        }
    }

    private static long u4Static(byte[] b, int at) {
        return ((long) (b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16) | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
    }

    private static final String[] MNEMONICS = ("nop aconst_null iconst_m1 iconst_0 iconst_1 iconst_2 iconst_3 iconst_4 iconst_5"
            + " lconst_0 lconst_1 fconst_0 fconst_1 fconst_2 dconst_0 dconst_1 bipush sipush ldc ldc_w ldc2_w iload lload fload"
            + " dload aload iload_0 iload_1 iload_2 iload_3 lload_0 lload_1 lload_2 lload_3 fload_0 fload_1 fload_2 fload_3"
            + " dload_0 dload_1 dload_2 dload_3 aload_0 aload_1 aload_2 aload_3 iaload laload faload daload aaload baload"
            + " caload saload istore lstore fstore dstore astore istore_0 istore_1 istore_2 istore_3 lstore_0 lstore_1"
            + " lstore_2 lstore_3 fstore_0 fstore_1 fstore_2 fstore_3 dstore_0 dstore_1 dstore_2 dstore_3 astore_0 astore_1"
            + " astore_2 astore_3 iastore lastore fastore dastore aastore bastore castore sastore pop pop2 dup dup_x1 dup_x2"
            + " dup2 dup2_x1 dup2_x2 swap iadd ladd fadd dadd isub lsub fsub dsub imul lmul fmul dmul idiv ldiv fdiv ddiv"
            + " irem lrem frem drem ineg lneg fneg dneg ishl lshl ishr lshr iushr lushr iand land ior lor ixor lxor iinc i2l"
            + " i2f i2d l2i l2f l2d f2i f2l f2d d2i d2l d2f i2b i2c i2s lcmp fcmpl fcmpg dcmpl dcmpg ifeq ifne iflt ifge ifgt"
            + " ifle if_icmpeq if_icmpne if_icmplt if_icmpge if_icmpgt if_icmple if_acmpeq if_acmpne goto jsr ret tableswitch"
            + " lookupswitch ireturn lreturn freturn dreturn areturn return getstatic putstatic getfield putfield"
            + " invokevirtual invokespecial invokestatic invokeinterface invokedynamic new newarray anewarray arraylength"
            + " athrow checkcast instanceof monitorenter monitorexit wide multianewarray ifnull ifnonnull goto_w jsr_w").split(" ");

    private static String mnemonic(int op) {
        return op < MNEMONICS.length ? MNEMONICS[op] : "op" + op;
    }

    /** @return length of the instruction at {@code pc}, validated against the class file */
    private int instructionLength(int pc, int codeStart, String methodName) throws ClassFormatException {
        int op = this.b[pc] & 0xFF;
        int rel = pc - codeStart;
        int len;
        switch (op) {
            case 0xaa -> {
                int base = pc + 1 + ((4 - ((rel + 1) % 4)) % 4);
                int low = (int) u4(base + 4);
                int high = (int) u4(base + 8);
                if (high < low || (long) high - low > 65535) {
                    throw new ClassFormatException("method " + methodName + ": invalid tableswitch");
                }
                len = base + 12 + (high - low + 1) * 4 - pc;
            }
            case 0xab -> {
                int base = pc + 1 + ((4 - ((rel + 1) % 4)) % 4);
                int pairs = (int) u4(base + 4);
                if (pairs < 0 || pairs > 65535) {
                    throw new ClassFormatException("method " + methodName + ": invalid lookupswitch");
                }
                len = base + 8 + pairs * 8 - pc;
            }
            case 0xc4 -> len = (this.b[pc + 1] & 0xFF) == 0x84 ? 6 : 4; // wide
            case 0x12 -> len = 2;
            case 0x13, 0x14, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1 -> len = 3;
            case 0xb9, 0xba -> len = 5;
            case 0xc5 -> len = 4;
            default -> {
                len = simpleLength(op);
                if (len < 0) {
                    throw new ClassFormatException("method " + methodName + ": invalid opcode 0x" + Integer.toHexString(op)
                            + " at pc " + rel);
                }
            }
        }
        if (pc + len > this.b.length) {
            throw new ClassFormatException("method " + methodName + ": instruction at pc " + rel + " exceeds the class file");
        }
        return len;
    }

    /** @return instruction number of the jump target {@code from + offset} */
    private static int target(int[] number, int from, int offset, String methodName) throws ClassFormatException {
        long to = (long) from + offset;
        if (to < 0 || to >= number.length - 1 || number[(int) to] < 0) {
            throw new ClassFormatException("method " + methodName + ": jump at pc " + from + " to " + to
                    + " is not the start of an instruction");
        }
        return number[(int) to];
    }

    /** @return instruction number of an exception-table offset (the code length is allowed as an end) */
    private static int boundary(int[] number, int pc, String methodName) throws ClassFormatException {
        if (pc >= number.length || number[pc] < 0) {
            throw new ClassFormatException("method " + methodName + ": exception table offset " + pc
                    + " is not the start of an instruction");
        }
        return number[pc];
    }

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
