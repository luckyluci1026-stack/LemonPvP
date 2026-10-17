// Paper adapter. Inside the fork these sources are compiled directly into the server
// (see scripts/fork/generate_patches.py); this project only checks that they compile
// against the public Paper API of a given version.
description = "PaperSwap hot-deployment runtime - Paper adapter"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()

dependencies {
    implementation(project(":runtime-core"))
    implementation(project(":runtime-paper-api"))
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

tasks.compileJava {
    // Implementing org.bukkit.plugin.Plugin requires overriding getPluginLoader(), which Paper
    // deprecated for removal; Paper's own build disables these lint categories as well.
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
}
