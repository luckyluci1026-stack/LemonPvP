description = "Lemon live-patch agent: makes java.lang.instrument.Instrumentation available to the server"

// The agent must be loadable by every supported server JVM (21 and newer).
tasks.jar {
    archiveFileName = "lemon-livepatch-agent.jar"
    manifest {
        attributes(
            "Premain-Class" to "dev.lemonpvp.livepatch.agent.LemonLivePatchAgent",
            // Required for Instrumentation.redefineClasses. Retransformation and native
            // method prefixes are not needed and therefore not requested.
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "false",
            "Can-Set-Native-Method-Prefix" to "false",
        )
    }
}
