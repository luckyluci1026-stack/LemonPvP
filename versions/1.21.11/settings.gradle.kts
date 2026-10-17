pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "paperswap"

for (name in listOf("paperswap-api", "paperswap-server")) {
    include(name)
    file(name).mkdirs()
}
