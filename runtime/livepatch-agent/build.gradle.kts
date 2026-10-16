description = "Lemon live-patch agent: makes java.lang.instrument.Instrumentation available to the server"

// The agent must be loadable by every supported server JVM (21 and newer).
tasks.jar {
    archiveFileName = "lemon-livepatch-agent.jar"
    manifest {
        attributes(
            "Premain-Class" to "dev.lemonpvp.livepatch.agent.LemonLivePatchAgent",
            // Redefinition applies patches. Retransformation is only used to read back the
            // bytecode the JVM runs after a patch (verification); no transformer changes a
            // class. Native method prefixes are not needed and therefore not requested.
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Can-Set-Native-Method-Prefix" to "false",
        )
    }
}
