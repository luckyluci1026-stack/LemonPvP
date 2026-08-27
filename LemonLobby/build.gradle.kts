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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly(files("../LemonCore/build/libs/LemonCore-1.0.0.jar"))
    compileOnly(files("../LemonCosmetics/build/libs/LemonCosmetics-1.0.0.jar"))
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "com.lemonpvp.lemonlobby.libs.hikari")
        relocate("com.mysql", "com.lemonpvp.lemonlobby.libs.mysql")
        relocate("com.google.protobuf", "com.lemonpvp.lemonlobby.libs.protobuf")
    }
    build { dependsOn(shadowJar) }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
