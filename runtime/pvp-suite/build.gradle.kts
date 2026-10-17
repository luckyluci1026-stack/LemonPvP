import java.security.MessageDigest

// LemonPvP suite: four Lemon modules that work together (docs/PVP-SUITE.md).
//   ./gradlew :pvp-suite:collect -PpaperApiVersion=<version>
// Runs the unit tests of the game logic, then puts every module into
// runtime/pvp-suite/build/modules/: pvp-<name>.jar + pvp-<name>.jar.sha256, ready for lemon/modules/.
// The module id is "pvp-" plus the directory name.
description = "LemonPvP suite modules"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()
val junitVersion = "5.13.4"

/** Writes <jar>.sha256 in sha256sum format next to the jar of [jarTask]. */
fun Project.sha256Of(jarTask: TaskProvider<out AbstractArchiveTask>): TaskProvider<Task> =
    tasks.register("sha256") {
        val jarFile = jarTask.flatMap { it.archiveFile }
        val out = jarFile.map { File(it.asFile.path + ".sha256") }
        inputs.file(jarFile)
        outputs.file(out)
        doLast {
            val file = jarFile.get().asFile
            val hex = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            out.get().writeText("$hex  ${file.name}\n")
        }
    }

val collect = tasks.register<Sync>("collect") {
    into(layout.buildDirectory.dir("modules"))
}

subprojects {
    val moduleId = "pvp-${project.name}"
    dependencies {
        "compileOnly"(project(":runtime-api"))
        "compileOnly"(project(":runtime-paper-api"))
        "compileOnly"("io.papermc.paper:paper-api:$paperApiVersion")
        // Unit tests cover the plain-Java game logic (no server needed).
        "testImplementation"(platform("org.junit:junit-bom:$junitVersion"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
    val jar = tasks.named<Jar>("jar") {
        archiveFileName = "$moduleId.jar"
    }
    val sha256 = sha256Of(jar)
    tasks.named("assemble") {
        dependsOn(sha256)
    }
    collect.configure {
        dependsOn(tasks.named("test"))
        from(jar, sha256)
    }
}
