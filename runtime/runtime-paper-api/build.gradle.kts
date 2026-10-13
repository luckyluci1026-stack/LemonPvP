// Paper-specific part of the module API (events, commands, scheduler).
// Compiled against the Paper API of the target Minecraft version.
description = "Lemon hot-deployment runtime - Paper module API"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()

dependencies {
    api(project(":runtime-api"))
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}
