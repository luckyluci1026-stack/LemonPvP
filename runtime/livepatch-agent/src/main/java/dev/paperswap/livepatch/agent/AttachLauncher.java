package dev.paperswap.livepatch.agent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Command line tool in the agent jar: loads the agent into a running server JVM.
 *
 * <pre>
 * java -jar paperswap-agent.jar list           JVMs of this OS user that accept an attach
 * java -jar paperswap-agent.jar attach &lt;pid&gt;   load this jar as agent into JVM &lt;pid&gt;
 * </pre>
 *
 * <p>Uses the JDK attach API ({@code com.sun.tools.attach}, module {@code jdk.attach}),
 * which connects through a local file socket of the target JVM: it works only for the same
 * OS user, opens no network port and is unavailable if the server runs with
 * {@code -XX:+DisableAttachMechanism}. The launcher itself needs a full JDK (a JRE has no
 * {@code jdk.attach} module); the server may run on a JRE.</p>
 */
public final class AttachLauncher {

    private AttachLauncher() {
    }

    /**
     * @param args {@code list} or {@code attach <pid>}
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        String command = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        try {
            if (command.equals("list") && args.length == 1) {
                Attach.list();
                return 0;
            }
            if (command.equals("attach") && args.length == 2) {
                if (!args[1].matches("[0-9]{1,10}")) {
                    System.err.println("not a process id: " + args[1]);
                    return 2;
                }
                return Attach.attach(args[1], ownJar());
            }
        } catch (LinkageError e) {
            // Thrown on first use of com.sun.tools.attach when the module is missing.
            System.err.println("this Java installation has no attach API (module jdk.attach, " + e
                    + "); run this command with a JDK, e.g. /usr/lib/jvm/<jdk>/bin/java -jar paperswap-agent.jar ...");
            return 1;
        } catch (IOException | URISyntaxException | RuntimeException e) {
            System.err.println("failed: " + e);
            return 1;
        }
        System.err.println("PaperSwap live-patch agent " + PaperSwapAgent.VERSION);
        System.err.println("usage: java -jar paperswap-agent.jar list");
        System.err.println("       java -jar paperswap-agent.jar attach <pid>");
        System.err.println("Recommended instead of attach: start the server with -javaagent:paperswap-agent.jar");
        return 2;
    }

    private static Path ownJar() throws URISyntaxException, IOException {
        Path jar = Path.of(AttachLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        if (!Files.isRegularFile(jar) || !jar.getFileName().toString().endsWith(".jar")) {
            throw new IOException("the launcher must run from the agent jar (java -jar paperswap-agent.jar), not from " + jar);
        }
        return jar;
    }

    /** Everything that touches {@code com.sun.tools.attach}; loaded only when needed. */
    private static final class Attach {

        private Attach() {
        }

        static void list() {
            List<com.sun.tools.attach.spi.AttachProvider> providers = com.sun.tools.attach.spi.AttachProvider.providers();
            if (providers.isEmpty()) {
                System.out.println("no attach provider in this JDK");
                return;
            }
            for (com.sun.tools.attach.VirtualMachineDescriptor d : com.sun.tools.attach.VirtualMachine.list()) {
                System.out.println(d.id() + "  " + (d.displayName().isBlank() ? "(no main class reported)" : d.displayName()));
            }
        }

        static int attach(String pid, Path jar) throws IOException {
            com.sun.tools.attach.VirtualMachine vm;
            try {
                vm = com.sun.tools.attach.VirtualMachine.attach(pid);
            } catch (com.sun.tools.attach.AttachNotSupportedException e) {
                System.err.println("cannot attach to " + pid + ": " + e.getMessage() + " (the server must run as the same OS"
                        + " user, on a HotSpot JVM, without -XX:+DisableAttachMechanism)");
                return 1;
            }
            try {
                String version = vm.getSystemProperties().getProperty("java.specification.version", "?");
                System.out.println("attached to " + pid + " (Java " + version + "); loading " + jar);
                vm.loadAgent(jar.toString());
                System.out.println("agent loaded; the server log shows '[PaperSwap] live-patch agent " + PaperSwapAgent.VERSION
                        + " loaded (" + PaperSwapAgent.AGENTMAIN + ")'. Check it with /fork instrumentation status");
                return 0;
            } catch (com.sun.tools.attach.AgentLoadException e) {
                System.err.println("the JVM refused to load the agent: " + e.getMessage() + " (if dynamic agent loading is"
                        + " disabled, start the server with -XX:+EnableDynamicAgentLoading or use -javaagent)");
                return 1;
            } catch (com.sun.tools.attach.AgentInitializationException e) {
                System.err.println("agentmain failed in the target JVM (return value " + e.returnValue() + "): " + e.getMessage());
                return 1;
            } finally {
                vm.detach();
            }
        }
    }
}
