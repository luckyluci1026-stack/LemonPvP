plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.lemonpvp"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.luckperms.net/")
    maven("https://repo.intellectualsites.com/repository/intellectualsites-releases/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly(files("../LemonCore/build/libs/LemonCore-1.0.0.jar"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.11.1")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.1") { isTransitive = false }
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "com.lemonpvp.lemontraining.libs.hikari")
        relocate("com.mysql", "com.lemonpvp.lemontraining.libs.mysql")
        relocate("com.google.protobuf", "com.lemonpvp.lemontraining.libs.protobuf")
    }
    build { dependsOn(shadowJar) }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
