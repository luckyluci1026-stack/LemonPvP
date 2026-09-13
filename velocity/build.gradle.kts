plugins {
    grim.`base-conventions`
}

repositories {
    exclusive("https://repo.papermc.io/repository/maven-public/", { name = "papermc" }) {
        includeGroup("com.velocitypowered")
    }
    mavenCentral()
}

dependencies {
    // Velocity ships Adventure, Guice, Gson and a logger itself, so this is
    // the only dependency and the jar stays a plain, unshaded plugin.
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}
