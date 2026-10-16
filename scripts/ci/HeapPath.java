import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shows why an object is still alive: reads a HotSpot heap dump (HPROF) and prints the
 * shortest chain of strong references from a GC root to every instance of a class whose
 * boolean field has a given value (for Lemon: closed module class loaders).
 *
 * <pre>java scripts/ci/HeapPath.java dump.hprof dev.lemonpvp.runtime.loader.ModuleClassLoader closed</pre>
 *
 * References from {@code java.lang.ref.Reference.referent} (weak, soft, phantom, final
 * references) are ignored, as the GC does. Every instance also references its class and a
 * class references its loader, so "kept alive by an instance of a module class" shows up.
 * No dependencies; meant for CI diagnostics (needs a few hundred MiB of heap for a server dump).
 */
public final class HeapPath {

    private static final int OBJECT = 2;
    private static final String NO_LABEL = "";

    private int idSize;
    private final Map<Long, String> utf8 = new HashMap<>();
    private final Map<Long, Long> classNameIds = new HashMap<>();
    private final Map<Long, ClassInfo> classes = new HashMap<>();
    private long[] ids = new long[1 << 20];
    private int idCount;
    private final Map<Long, String> roots = new HashMap<>();
    private final Map<Integer, Long> threadBySerial = new HashMap<>();
    private final Map<Long, Integer> rootThread = new HashMap<>();

    // edges (pass 2), as parallel int arrays: from, to, label
    private int[] eFrom = new int[1 << 22];
    private int[] eTo = new int[1 << 22];
    private int[] eLabel = new int[1 << 22];
    private int edgeCount;
    private final List<String> labels = new ArrayList<>();
    private final Map<String, Integer> labelIndex = new HashMap<>();

    /** Class id per node index (0 for classes and arrays), filled in pass 2. */
    private long[] classOf;
    /** Raw field data of strings, threads and class loaders (for names and flags). */
    private final Map<Long, byte[]> keptInstances = new HashMap<>();
    private final Map<Long, Boolean> keptClass = new HashMap<>();

    record Field(long nameId, int type) {
    }

    static final class ClassInfo {
        long id;
        long superId;
        long loaderId;
        List<Field> fields = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: HeapPath <dump.hprof> <class name> <boolean field that must be true>");
            System.exit(2);
        }
        new HeapPath().run(Path.of(args[0]), args[1], args[2]);
    }

    private void run(Path dump, String className, String flagField) throws IOException {
        pass(dump, 1);
        Arrays.sort(this.ids, 0, this.idCount);
        this.classOf = new long[this.idCount];
        pass(dump, 2);
        Set<Long> targetClasses = new HashSet<>();
        for (ClassInfo c : this.classes.values()) {
            if (className.equals(name(c.id))) {
                targetClasses.add(c.id);
            }
        }
        List<Long> targets = new ArrayList<>();
        for (int i = 0; i < this.idCount; i++) {
            if (targetClasses.contains(this.classOf[i]) && Boolean.TRUE.equals(booleanField(this.ids[i], flagField))) {
                targets.add(this.ids[i]);
            }
        }
        System.out.println("heap dump: " + this.idCount + " objects, " + this.edgeCount + " references; "
                + targets.size() + " instance(s) of " + className + " with " + flagField + "=true");
        if (targets.isEmpty()) {
            return;
        }
        int[][] reverse = reverseIndex();
        Map<Long, String> strings = resolveStrings(dump);
        for (long t : targets) {
            System.out.println();
            System.out.println("== " + describe(t, strings));
            printPath(index(t), reverse, strings);
        }
    }

    // ------------------------------------------------------------------ parsing

    private void pass(Path dump, int pass) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(dump), 1 << 20))) {
            StringBuilder header = new StringBuilder();
            for (int b; (b = in.read()) > 0; ) {
                header.append((char) b);
            }
            if (!header.toString().startsWith("JAVA PROFILE")) {
                throw new IOException("not an HPROF file: " + header);
            }
            this.idSize = in.readInt();
            in.readLong();
            while (true) {
                int tag;
                try {
                    tag = in.readUnsignedByte();
                } catch (EOFException e) {
                    break;
                }
                in.readInt();
                long length = in.readInt() & 0xFFFFFFFFL;
                switch (tag) {
                    case 0x01 -> {
                        long id = readId(in);
                        byte[] b = new byte[(int) (length - this.idSize)];
                        in.readFully(b);
                        if (pass == 1) {
                            this.utf8.put(id, new String(b, StandardCharsets.UTF_8));
                        }
                    }
                    case 0x02 -> {
                        in.readInt();
                        long classId = readId(in);
                        in.readInt();
                        long nameId = readId(in);
                        if (pass == 1) {
                            this.classNameIds.put(classId, nameId);
                        }
                    }
                    case 0x0C, 0x1C -> heapDump(in, length, pass);
                    default -> skip(in, length);
                }
            }
        }
    }

    private void heapDump(DataInputStream in, long length, int pass) throws IOException {
        long end = length;
        long read = 0;
        while (read < end) {
            int sub = in.readUnsignedByte();
            read += 1;
            switch (sub) {
                case 0xFF -> read += root(in, "unknown root", 0, pass);
                case 0x01 -> {
                    read += root(in, "JNI global", 0, pass);
                    readId(in);
                    read += this.idSize;
                }
                case 0x02 -> read += root(in, "JNI local", 8, pass);
                case 0x03 -> read += root(in, "Java frame", 8, pass);
                case 0x04 -> read += root(in, "native stack", 4, pass);
                case 0x05 -> read += root(in, "system class", 0, pass);
                case 0x06 -> read += root(in, "thread block", 4, pass);
                case 0x07 -> read += root(in, "monitor in use", 0, pass);
                case 0x08 -> {
                    long id = readId(in);
                    int serial = in.readInt();
                    in.readInt();
                    read += this.idSize + 8;
                    if (pass == 1) {
                        this.threadBySerial.put(serial, id);
                        this.roots.putIfAbsent(id, "thread object");
                    }
                }
                case 0x20 -> read += classDump(in, pass);
                case 0x21 -> read += instanceDump(in, pass);
                case 0x22 -> read += objectArrayDump(in, pass);
                case 0x23 -> read += primitiveArrayDump(in, pass);
                default -> throw new IOException("unknown heap dump sub-record 0x" + Integer.toHexString(sub));
            }
        }
    }

    /** Reads a root sub-record: id, then {@code extra} bytes (thread serial first if present). */
    private long root(DataInputStream in, String kind, int extra, int pass) throws IOException {
        long id = readId(in);
        int threadSerial = -1;
        if (extra >= 4) {
            threadSerial = in.readInt();
            skip(in, extra - 4);
        }
        if (pass == 1) {
            this.roots.putIfAbsent(id, kind);
            if (threadSerial >= 0) {
                this.rootThread.putIfAbsent(id, threadSerial);
            }
        }
        return this.idSize + extra;
    }

    private long classDump(DataInputStream in, int pass) throws IOException {
        long start = 0;
        ClassInfo c = new ClassInfo();
        c.id = readId(in);
        in.readInt();
        c.superId = readId(in);
        c.loaderId = readId(in);
        long signers = readId(in);
        long domain = readId(in);
        readId(in);
        readId(in);
        in.readInt();
        start += 7L * this.idSize + 8;
        int cp = in.readUnsignedShort();
        start += 2;
        for (int i = 0; i < cp; i++) {
            in.readUnsignedShort();
            int type = in.readUnsignedByte();
            start += 3 + skipValue(in, type);
        }
        int statics = in.readUnsignedShort();
        start += 2;
        if (pass == 2) {
            int from = index(c.id);
            edge(from, c.superId, "<superclass>");
            edge(from, c.loaderId, "<class loader>");
            edge(from, signers, "<signers>");
            edge(from, domain, "<protection domain>");
        }
        for (int i = 0; i < statics; i++) {
            long nameId = readId(in);
            int type = in.readUnsignedByte();
            start += this.idSize + 1;
            if (type == OBJECT) {
                long value = readId(in);
                start += this.idSize;
                if (pass == 2) {
                    edge(index(c.id), value, "static " + this.utf8.getOrDefault(nameId, "?"));
                }
            } else {
                start += skipValue(in, type);
            }
        }
        int fields = in.readUnsignedShort();
        start += 2;
        for (int i = 0; i < fields; i++) {
            long nameId = readId(in);
            int type = in.readUnsignedByte();
            start += this.idSize + 1;
            c.fields.add(new Field(nameId, type));
        }
        if (pass == 1) {
            this.classes.put(c.id, c);
            addId(c.id);
        }
        return start;
    }

    private long instanceDump(DataInputStream in, int pass) throws IOException {
        long id = readId(in);
        in.readInt();
        long classId = readId(in);
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readFully(data);
        if (pass == 1) {
            addId(id);
        } else if (pass == 2) {
            int from = index(id);
            this.classOf[from] = classId;
            edge(from, classId, "<class>");
            int offset = 0;
            for (ClassInfo c = this.classes.get(classId); c != null; c = this.classes.get(c.superId)) {
                boolean reference = "java.lang.ref.Reference".equals(name(c.id));
                for (Field f : c.fields) {
                    if (f.type() == OBJECT) {
                        long value = id(data, offset);
                        String field = this.utf8.getOrDefault(f.nameId(), "?");
                        if (!(reference && field.equals("referent"))) {
                            edge(from, value, field);
                        }
                    }
                    offset += typeSize(f.type());
                }
            }
            if (isKept(classId)) {
                this.keptInstances.put(id, data);
            }
        }
        return 2L * this.idSize + 8 + size;
    }

    private boolean isKept(long classId) {
        return this.keptClass.computeIfAbsent(classId, k -> {
            for (ClassInfo c = this.classes.get(k); c != null; c = this.classes.get(c.superId)) {
                String n = name(c.id);
                if ("java.lang.String".equals(n) || "java.lang.Thread".equals(n) || "java.lang.ClassLoader".equals(n)) {
                    return true;
                }
            }
            return false;
        });
    }

    private Long classOf(long instance) {
        int i = index(instance);
        return i < 0 || this.classOf[i] == 0 ? null : this.classOf[i];
    }

    private long objectArrayDump(DataInputStream in, int pass) throws IOException {
        long id = readId(in);
        in.readInt();
        int length = in.readInt();
        long arrayClass = readId(in);
        if (pass == 1) {
            addId(id);
            skip(in, (long) length * this.idSize);
        } else {
            int from = index(id);
            edge(from, arrayClass, "<class>");
            for (int i = 0; i < length; i++) {
                long value = readId(in);
                if (value != 0) {
                    edge(from, value, "[" + Math.min(i, 9) + (i > 9 ? "+]" : "]"));
                }
            }
        }
        return 2L * this.idSize + 8 + (long) length * this.idSize;
    }

    private long primitiveArrayDump(DataInputStream in, int pass) throws IOException {
        readId(in);
        in.readInt();
        int length = in.readInt();
        int type = in.readUnsignedByte();
        long bytes = (long) length * typeSize(type);
        skip(in, bytes);
        return this.idSize + 9 + bytes;
    }

    // ------------------------------------------------------------------ graph

    private void addId(long id) {
        if (this.idCount == this.ids.length) {
            this.ids = Arrays.copyOf(this.ids, this.ids.length * 2);
        }
        this.ids[this.idCount++] = id;
    }

    private int index(long id) {
        int i = Arrays.binarySearch(this.ids, 0, this.idCount, id);
        return i < 0 ? -1 : i;
    }

    private void edge(int from, long toId, String label) {
        if (from < 0 || toId == 0) {
            return;
        }
        int to = index(toId);
        if (to < 0) {
            return; // primitive arrays and unknown ids are not graph nodes
        }
        if (this.edgeCount == this.eFrom.length) {
            int n = this.eFrom.length * 2;
            this.eFrom = Arrays.copyOf(this.eFrom, n);
            this.eTo = Arrays.copyOf(this.eTo, n);
            this.eLabel = Arrays.copyOf(this.eLabel, n);
        }
        Integer l = this.labelIndex.get(label);
        if (l == null) {
            l = this.labels.size();
            this.labels.add(label);
            this.labelIndex.put(label, l);
        }
        this.eFrom[this.edgeCount] = from;
        this.eTo[this.edgeCount] = to;
        this.eLabel[this.edgeCount] = l;
        this.edgeCount++;
    }

    /** @return {start offsets per node, referrer per slot, edge index per slot} */
    private int[][] reverseIndex() {
        int[] start = new int[this.idCount + 1];
        for (int e = 0; e < this.edgeCount; e++) {
            start[this.eTo[e] + 1]++;
        }
        for (int i = 0; i < this.idCount; i++) {
            start[i + 1] += start[i];
        }
        int[] fill = Arrays.copyOf(start, start.length);
        int[] edgeAt = new int[this.edgeCount];
        for (int e = 0; e < this.edgeCount; e++) {
            edgeAt[fill[this.eTo[e]]++] = e;
        }
        return new int[][] {start, edgeAt};
    }

    private void printPath(int target, int[][] reverse, Map<Long, String> strings) {
        int[] start = reverse[0];
        int[] edgeAt = reverse[1];
        int[] via = new int[this.idCount];
        Arrays.fill(via, -2);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        via[target] = -1;
        queue.add(target);
        int root = -1;
        while (!queue.isEmpty() && root < 0) {
            int n = queue.poll();
            if (this.roots.containsKey(this.ids[n])) {
                root = n;
                break;
            }
            for (int s = start[n]; s < start[n + 1]; s++) {
                int e = edgeAt[s];
                int referrer = this.eFrom[e];
                if (via[referrer] == -2) {
                    via[referrer] = e;
                    queue.add(referrer);
                }
            }
        }
        if (root < 0) {
            System.out.println("   no strong path from a GC root (only weakly reachable: it goes away with the next GC)");
            return;
        }
        long rootId = this.ids[root];
        String kind = this.roots.get(rootId);
        Integer serial = this.rootThread.get(rootId);
        if (serial != null && this.threadBySerial.containsKey(serial)) {
            kind += " of thread " + describe(this.threadBySerial.get(serial), strings);
        }
        System.out.println("   GC root (" + kind + ")");
        for (int n = root; via[n] != -1; ) {
            int e = via[n];
            System.out.println("   " + describe(this.ids[n], strings) + " --" + this.labels.get(this.eLabel[e]) + "-->");
            n = this.eTo[e];
        }
        System.out.println("   " + describe(this.ids[target], strings));
    }

    // ------------------------------------------------------------------ values

    private String describe(long id, Map<Long, String> strings) {
        ClassInfo asClass = this.classes.get(id);
        if (asClass != null) {
            return "class " + name(id);
        }
        Long classId = classOf(id);
        String cn = classId == null ? "array" : name(classId);
        String extra = "";
        if (this.keptInstances.containsKey(id) && classId != null) {
            for (String f : List.of("name", "moduleId")) {
                Long ref = objectField(id, f);
                if (ref != null && strings.containsKey(ref)) {
                    extra += " " + f + "=\"" + strings.get(ref) + "\"";
                }
            }
        }
        return cn + "@" + Long.toHexString(id) + extra;
    }

    private Boolean booleanField(long instance, String field) {
        byte[] data = this.keptInstances.get(instance);
        Long classId = classOf(instance);
        if (data == null || classId == null) {
            return null;
        }
        int offset = 0;
        for (ClassInfo c = this.classes.get(classId); c != null; c = this.classes.get(c.superId)) {
            for (Field f : c.fields) {
                if (f.type() == 4 && field.equals(this.utf8.get(f.nameId()))) {
                    return data[offset] != 0;
                }
                offset += typeSize(f.type());
            }
        }
        return null;
    }

    private Long objectField(long instance, String field) {
        byte[] data = this.keptInstances.get(instance);
        Long classId = classOf(instance);
        if (data == null || classId == null) {
            return null;
        }
        int offset = 0;
        for (ClassInfo c = this.classes.get(classId); c != null; c = this.classes.get(c.superId)) {
            for (Field f : c.fields) {
                if (f.type() == OBJECT && field.equals(this.utf8.get(f.nameId()))) {
                    return id(data, offset);
                }
                offset += typeSize(f.type());
            }
        }
        return null;
    }

    /** Reads the text of the String fields "name"/"moduleId" of loaders and threads (one more pass). */
    private Map<Long, String> resolveStrings(Path dump) throws IOException {
        Map<Long, Long> valueOf = new HashMap<>();
        Map<Long, Integer> coderOf = new HashMap<>();
        for (long owner : this.keptInstances.keySet()) {
            if ("java.lang.String".equals(name(classOf(owner)))) {
                continue;
            }
            for (String f : List.of("name", "moduleId")) {
                Long s = objectField(owner, f);
                Long value = s == null ? null : objectField(s, "value");
                if (value != null) {
                    valueOf.put(s, value);
                    coderOf.put(s, byteField(s, "coder"));
                }
            }
        }
        Set<Long> wanted = new HashSet<>(valueOf.values());
        Map<Long, byte[]> arrays = new HashMap<>();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(dump), 1 << 20))) {
            while (in.read() > 0) {
                // header string
            }
            in.readInt();
            in.readLong();
            while (true) {
                int tag;
                try {
                    tag = in.readUnsignedByte();
                } catch (EOFException e) {
                    break;
                }
                in.readInt();
                long length = in.readInt() & 0xFFFFFFFFL;
                if (tag != 0x0C && tag != 0x1C) {
                    skip(in, length);
                    continue;
                }
                long read = 0;
                while (read < length) {
                    int sub = in.readUnsignedByte();
                    read++;
                    if (sub == 0x23) {
                        long id = readId(in);
                        in.readInt();
                        int len = in.readInt();
                        int type = in.readUnsignedByte();
                        long bytes = (long) len * typeSize(type);
                        if (wanted.contains(id) && type == 8) {
                            byte[] b = new byte[len];
                            in.readFully(b);
                            arrays.put(id, b);
                        } else {
                            skip(in, bytes);
                        }
                        read += this.idSize + 9 + bytes;
                    } else {
                        read += skipSub(in, sub);
                    }
                }
            }
        }
        Map<Long, String> out = new HashMap<>();
        for (Map.Entry<Long, Integer> e : coderOf.entrySet()) {
            byte[] b = arrays.get(valueOf.get(e.getKey()));
            if (b != null) {
                out.put(e.getKey(), new String(b, e.getValue() == 1 ? StandardCharsets.UTF_16LE : StandardCharsets.ISO_8859_1));
            }
        }
        return out;
    }

    private int byteField(long instance, String field) {
        byte[] data = this.keptInstances.get(instance);
        Long classId = classOf(instance);
        if (data == null || classId == null) {
            return 0;
        }
        int offset = 0;
        for (ClassInfo c = this.classes.get(classId); c != null; c = this.classes.get(c.superId)) {
            for (Field f : c.fields) {
                if (f.type() == 8 && field.equals(this.utf8.get(f.nameId()))) {
                    return data[offset];
                }
                offset += typeSize(f.type());
            }
        }
        return 0;
    }

    /** Skips a heap dump sub-record other than a primitive array (third pass). */
    private long skipSub(DataInputStream in, int sub) throws IOException {
        return switch (sub) {
            case 0xFF, 0x05, 0x07 -> skipN(in, this.idSize);
            case 0x01 -> skipN(in, 2L * this.idSize);
            case 0x02, 0x03 -> skipN(in, this.idSize + 8L);
            case 0x04, 0x06 -> skipN(in, this.idSize + 4L);
            case 0x08 -> skipN(in, this.idSize + 8L);
            case 0x20 -> classDump(in, 3);
            case 0x21 -> {
                skipN(in, this.idSize + 4L);
                readId(in);
                int size = in.readInt();
                skip(in, size);
                yield 2L * this.idSize + 8 + size;
            }
            case 0x22 -> {
                skipN(in, this.idSize + 4L);
                int length = in.readInt();
                readId(in);
                skip(in, (long) length * this.idSize);
                yield 2L * this.idSize + 8 + (long) length * this.idSize;
            }
            default -> throw new IOException("unknown heap dump sub-record 0x" + Integer.toHexString(sub));
        };
    }

    private long skipN(DataInputStream in, long n) throws IOException {
        skip(in, n);
        return n;
    }

    private String name(Long classId) {
        if (classId == null) {
            return null;
        }
        Long nameId = this.classNameIds.get(classId);
        String n = nameId == null ? null : this.utf8.get(nameId);
        return n == null ? null : n.replace('/', '.');
    }

    private long readId(DataInputStream in) throws IOException {
        return this.idSize == 8 ? in.readLong() : in.readInt() & 0xFFFFFFFFL;
    }

    private long id(byte[] data, int offset) {
        long v = 0;
        for (int i = 0; i < this.idSize; i++) {
            v = (v << 8) | (data[offset + i] & 0xFF);
        }
        return v;
    }

    private int typeSize(int type) {
        return switch (type) {
            case OBJECT -> this.idSize;
            case 4, 8 -> 1;
            case 5, 9 -> 2;
            case 6, 10 -> 4;
            case 7, 11 -> 8;
            default -> throw new IllegalArgumentException("unknown basic type " + type);
        };
    }

    private long skipValue(DataInputStream in, int type) throws IOException {
        int n = typeSize(type);
        skip(in, n);
        return n;
    }

    private static void skip(InputStream in, long n) throws IOException {
        long left = n;
        while (left > 0) {
            long s = in.skip(left);
            if (s <= 0) {
                if (in.read() < 0) {
                    throw new EOFException();
                }
                s = 1;
            }
            left -= s;
        }
    }
}
