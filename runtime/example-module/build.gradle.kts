// Example runtime module. Build: ./gradlew :example-module:jar -PpaperApiVersion=<version>
// The resulting jar goes into <server>/lemon/modules/ together with a .sha256 file
// (task :example-module:sha256).
description = "Example Lemon runtime module"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()

dependencies {
    compileOnly(project(":runtime-api"))
    compileOnly(project(":runtime-paper-api"))
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

tasks.jar {
    archiveFileName = "example-greeter.jar"
}

// Writes example-greeter.jar.sha256 in sha256sum format next to the jar.
val sha256 by tasks.registering {
    val jarFile = tasks.jar.flatMap { it.archiveFile }
    inputs.file(jarFile)
    val out = layout.buildDirectory.file("libs/example-greeter.jar.sha256")
    outputs.file(out)
    doLast {
        val file = jarFile.get().asFile
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val hex = digest.joinToString("") { "%02x".format(it) }
        out.get().asFile.writeText("$hex  ${file.name}\n")
    }
}
tasks.assemble { dependsOn(sha256) }
