package dev.lemonpvp.runtime.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.api.Capability;
import dev.lemonpvp.runtime.api.ModuleDescriptor;
import dev.lemonpvp.runtime.api.PermissionDeclaration.PermissionDefault;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DescriptorParserTest {

    private static ModuleDescriptor parse(String yaml) throws DescriptorException {
        return DescriptorParser.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "test");
    }

    private static DescriptorException fail(String yaml) {
        return assertThrows(DescriptorException.class, () -> parse(yaml));
    }

    @Test
    void parsesACompleteDescriptor() throws Exception {
        ModuleDescriptor d = parse("""
                id: event-tools
                version: 1.4.0-beta.1
                name: Event Tools
                description: Tools for events
                authors: [Alice, Bob]
                main: org.example.events.EventTools
                api-version: 1
                minecraft-versions: ["1.21.11", "26.*"]
                capabilities: [events, commands, scheduler]
                permissions:
                  event-tools.start:
                    description: Start an event
                    default: op
                  event-tools.join:
                    default: true
                dependencies:
                  - id: core-lib
                    version: ">=2.0.0"
                  - id: stats
                    optional: true
                plugin-dependencies: [LuckPerms]
                load-order: 10
                resources: [config.yml, lang/en.yml]
                """);
        assertEquals("event-tools", d.id());
        assertEquals("Event Tools", d.name());
        assertTrue(d.has(Capability.EVENTS) && d.has(Capability.SCHEDULER));
        assertEquals(2, d.permissions().size());
        assertEquals(PermissionDefault.TRUE, d.permissions().get(1).defaultValue());
        assertEquals(">=2.0.0", d.dependencies().get(0).versionRange());
        assertTrue(d.dependencies().get(1).optional());
        assertEquals(10, d.loadOrder());
        assertEquals("lang/en.yml", d.resources().get(1));
    }

    @Test
    void exampleModuleDescriptorIsValid() throws Exception {
        java.nio.file.Path file = java.nio.file.Path.of("../example-module/src/main/resources/lemon-module.yml");
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
            ModuleDescriptor d = DescriptorParser.parse(in, file.toString());
            assertEquals("example-greeter", d.id());
            assertTrue(d.has(Capability.COMMANDS));
        }
    }

    @Test
    void reportsAllProblemsAtOnce() {
        DescriptorException e = fail("""
                id: Bad Id
                version: 1.0
                main: NoPackage
                api-version: 0
                minecraft-versions: []
                capabilities: [teleport]
                unknown: 1
                """);
        assertTrue(e.problems().size() >= 6, e.problems().toString());
    }

    @Test
    void rejectsDangerousValues() {
        String base = "id: safe\nversion: 1.0.0\napi-version: 1\nminecraft-versions: [\"*\"]\n";
        assertTrue(fail(base + "main: dev.lemonpvp.runtime.Evil\n").getMessage().contains("must not be in package"));
        assertTrue(fail(base + "main: java.lang.Evil\n").getMessage().contains("must not be in package"));
        assertTrue(fail(base + "main: a.B\nresources: [\"../x.yml\"]\n").getMessage().contains("not a safe relative path"));
        assertTrue(fail(base + "main: a.B\nresources: [\"/etc/passwd\"]\n").getMessage().contains("not a safe relative path"));
        assertTrue(fail(base + "main: a.B\npermissions:\n  other.node: {}\n").getMessage().contains("must start with 'safe.'"));
        assertTrue(fail(base + "main: a.B\ndependencies: [safe]\n").getMessage().contains("must not depend on itself"));
        assertTrue(fail("id: minecraft\nversion: 1.0.0\nmain: a.B\napi-version: 1\nminecraft-versions: [\"*\"]\n")
                .getMessage().contains("reserved"));
    }

    @Test
    void rejectsYamlTricks() {
        assertTrue(fail("id: a1\nid: a2\n").getMessage().contains("test"), "duplicate keys");
        assertTrue(fail("!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\"http://x\"]]]]")
                .getMessage().contains("test"), "no arbitrary object construction");
        assertTrue(fail("- a\n- b\n").getMessage().contains("mapping"));
    }
}
