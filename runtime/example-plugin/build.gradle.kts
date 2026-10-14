// Normal Bukkit plugin (no Lemon API) for the live plugin update tests.
// Build: ./gradlew :example-plugin:jar -PpaperApiVersion=<version>
description = "Test plugin for Lemon's live plugin updates"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

tasks.jar {
    archiveFileName = "LemonTestPlugin.jar"
}

tasks.compileJava {
    // The Bukkit API marks some still-supported methods as deprecated; that is not what this plugin tests.
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
}
