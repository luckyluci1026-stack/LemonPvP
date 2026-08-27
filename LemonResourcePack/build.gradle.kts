plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.lemonpvp"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "com.lemonpvp.lemonresourcepack.libs.hikari")
        relocate("com.mysql", "com.lemonpvp.lemonresourcepack.libs.mysql")
        relocate("com.google.protobuf", "com.lemonpvp.lemonresourcepack.libs.protobuf")
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
