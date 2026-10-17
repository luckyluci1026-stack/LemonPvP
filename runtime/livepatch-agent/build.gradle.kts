description = "Lemon live-patch agent: makes java.lang.instrument.Instrumentation available to the server"

// The agent must be loadable by every supported server JVM (21 and newer).
tasks.jar {
    archiveFileName = "lemon-livepatch-agent.jar"
    manifest {
        attributes(
            // -javaagent:lemon-livepatch-agent.jar (recommended)
            "Premain-Class" to "dev.lemonpvp.livepatch.agent.LemonLivePatchAgent",
            // java -jar lemon-livepatch-agent.jar attach <pid> (dynamic attach, VirtualMachine.loadAgent)
            "Agent-Class" to "dev.lemonpvp.livepatch.agent.LemonLivePatchAgent",
            "Main-Class" to "dev.lemonpvp.livepatch.agent.AttachLauncher",
            // Redefinition and retransformation apply patches; retransformation also reads
            // back the bytecode the JVM runs after a patch (verification). The agent itself
            // registers no transformer. Native method prefixes are not needed.
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Can-Set-Native-Method-Prefix" to "false",
        )
    }
}
