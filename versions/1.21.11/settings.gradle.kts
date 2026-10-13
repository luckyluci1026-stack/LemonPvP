pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lemon"

for (name in listOf("lemon-api", "lemon-server")) {
    include(name)
    file(name).mkdirs()
}
