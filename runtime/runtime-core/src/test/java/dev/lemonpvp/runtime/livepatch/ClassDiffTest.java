package dev.lemonpvp.runtime.livepatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFormatException;
import dev.lemonpvp.runtime.livepatch.classfile.ClassInfo;
import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Change classification on real javac output. */
class ClassDiffTest {

    @TempDir
    Path dir;

    private int counter;

    private ClassInfo compile(String source) throws Exception {
        Path jar = ModuleJarBuilder.create().noDescriptor().source("cd.Subject", source).writeTo(this.dir, "c" + (this.counter++) + ".jar", false);
        try (JarFile f = new JarFile(jar.toFile()); InputStream in = f.getInputStream(f.getEntry("cd/Subject.class"))) {
            return ClassFileReader.read(in.readAllBytes());
        }
    }

    private List<ChangeKind> kinds(String before, String after) throws Exception {
        return ClassDiff.compare(compile(before), compile(after)).stream().map(ClassChange::kind).toList();
    }

    private static final String BASE = """
            package cd;
            public class Subject {
                private int count;
                static final String NAME;
                static { NAME = "x"; }
                public Subject() { this.count = 1; }
                public String a() { return "a1"; }
                public String b() { return "b1"; }
            }
            """;

    @Test
    void onlyTheEditedMethodIsReportedDespiteConstantPoolRenumbering() throws Exception {
        assertEquals(List.of(ChangeKind.METHOD_BODY), kinds(BASE, BASE.replace("\"a1\"", "\"a2 with a new constant\"")));
    }

    @Test
    void lineNumberChangesAreNotBytecodeChanges() throws Exception {
        assertEquals(List.of(), kinds(BASE, BASE.replace("public class Subject {", "public class Subject {\n\n\n")));
    }

    @Test
    void constructorAndStaticInitializerBodiesAreRecognised() throws Exception {
        assertEquals(List.of(ChangeKind.CONSTRUCTOR_BODY), kinds(BASE, BASE.replace("this.count = 1;", "this.count = 2;")));
        assertEquals(List.of(ChangeKind.STATIC_INITIALIZER), kinds(BASE, BASE.replace("NAME = \"x\";", "NAME = \"y\";")));
    }

    @Test
    void structuralChangesAreRecognised() throws Exception {
        assertEquals(List.of(ChangeKind.METHOD_ADDED), kinds(BASE, BASE.replace("public String b()", "public int c() { return 3; }\n    public String b()")));
        assertEquals(List.of(ChangeKind.METHOD_REMOVED), kinds(BASE, BASE.replace("public String b() { return \"b1\"; }", "")));
        assertEquals(List.of(ChangeKind.FIELD_ADDED), kinds(BASE, BASE.replace("private int count;", "private int count;\n    private long extra;")));
        assertEquals(List.of(ChangeKind.FIELD_CHANGED), kinds(BASE, BASE.replace("private int count;", "private long count;")
                .replace("this.count = 1;", "this.count = 1L;")).subList(0, 1));
        assertEquals(List.of(ChangeKind.METHOD_MODIFIERS), kinds(BASE, BASE.replace("public String b()", "protected String b()")));
        assertEquals(List.of(ChangeKind.INTERFACES_CHANGED), kinds(BASE, BASE.replace("public class Subject {", "public class Subject implements java.io.Serializable {")));
        // A new super class also changes the super() call in the constructor.
        assertEquals(List.of(ChangeKind.SUPERCLASS_CHANGED, ChangeKind.CONSTRUCTOR_BODY),
                kinds(BASE, BASE.replace("public class Subject {", "public class Subject extends Thread {")));
        assertEquals(List.of(ChangeKind.CONSTRUCTOR_ADDED), kinds(BASE, BASE.replace("public Subject() {", "public Subject(int x) { this(); }\n    public Subject() {")));
    }

    @Test
    void newLambdaIsReportedAsAddedSyntheticMethod() throws Exception {
        List<ClassChange> changes = ClassDiff.compare(compile(BASE),
                compile(BASE.replace("return \"a1\";", "java.util.function.Supplier<String> s = () -> \"a1\"; return s.get();")));
        assertTrue(changes.stream().anyMatch(c -> c.kind() == ChangeKind.METHOD_ADDED && c.detail().contains("[lambda body]")), changes.toString());
        assertTrue(changes.stream().anyMatch(c -> c.kind() == ChangeKind.METHOD_BODY), changes.toString());
    }

    @Test
    void newAnonymousClassChangesTheNestAttributes() throws Exception {
        List<ChangeKind> k = kinds(BASE, BASE.replace("return \"b1\";", "return new Object() { public String toString() { return \"b1\"; } }.toString();"));
        assertTrue(k.contains(ChangeKind.RESTRICTED_ATTRIBUTE), k.toString());
    }

    @Test
    void codeListingResolvesConstantsAndNumbersJumpTargets() throws Exception {
        Path jar = ModuleJarBuilder.create().noDescriptor().source("cd.Subject", """
                package cd;
                public class Subject {
                    public static String pick(int i) { return i > 3 ? "big" : "small"; }
                }
                """).writeTo(this.dir, "listing.jar", false);
        byte[] bytes;
        try (JarFile f = new JarFile(jar.toFile()); InputStream in = f.getInputStream(f.getEntry("cd/Subject.class"))) {
            bytes = in.readAllBytes();
        }
        List<String> listing = ClassFileReader.codeListing(bytes, "pick", "(I)Ljava/lang/String;");
        assertTrue(listing.get(0).startsWith("max_stack "), listing.toString());
        assertTrue(listing.contains("#0 iload_0"), listing.toString());
        assertTrue(listing.stream().anyMatch(l -> l.matches("#\\d+ if_icmple #\\d+")), listing.toString());
        assertTrue(listing.stream().anyMatch(l -> l.endsWith("ldc s:big")), listing.toString());
        assertEquals(List.of(), ClassFileReader.codeListing(bytes, "missing", "()V"));
    }

    @Test
    void readerRejectsGarbage() {
        for (byte[] bad : List.of(new byte[0], new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 65, 0, 0},
                "not a class file at all".getBytes())) {
            try {
                ClassFileReader.read(bad);
                throw new AssertionError("accepted garbage");
            } catch (ClassFormatException expected) {
                // ok
            }
        }
    }
}
