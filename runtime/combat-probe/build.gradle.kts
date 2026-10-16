// Test plugin for the CI live-patch test of the spear cooldown fix on 1.21.11. Measures real
// attacks of a server-side test player (no client). Not for production servers.
// Build: ./gradlew :combat-probe:jar -PpaperApiVersion=<version>
description = "CombatProbe - measures spear jabs, sword attacks and attribute swaps on a running server (tests only)"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

tasks.jar {
    archiveFileName = "CombatProbe.jar"
}

tasks.compileJava {
    // PlayerAttackEntityCooldownResetEvent is deprecated for removal in 26.x; the probe only runs on 1.21.11.
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
}
