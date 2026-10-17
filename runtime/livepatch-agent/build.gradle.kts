description = "PaperSwap live-patch agent: makes java.lang.instrument.Instrumentation available to the server"

// The agent must be loadable by every supported server JVM (21 and newer).
tasks.jar {
    archiveFileName = "paperswap-agent.jar"
    manifest {
        attributes(
            // -javaagent:paperswap-agent.jar (recommended)
            "Premain-Class" to "dev.paperswap.livepatch.agent.PaperSwapAgent",
            // java -jar paperswap-agent.jar attach <pid> (dynamic attach, VirtualMachine.loadAgent)
            "Agent-Class" to "dev.paperswap.livepatch.agent.PaperSwapAgent",
            "Main-Class" to "dev.paperswap.livepatch.agent.AttachLauncher",
            // Redefinition and retransformation apply patches; retransformation also reads
            // back the bytecode the JVM runs after a patch (verification). The agent itself
            // registers no transformer. Native method prefixes are not needed.
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Can-Set-Native-Method-Prefix" to "false",
        )
    }
}
