import java.security.MessageDigest

// Test modules for the module runtime (docs/TEST-MODULES.md).
//   ./gradlew :test-modules:collect -PpaperApiVersion=<version>
// Result in runtime/test-modules/build/modules/:
//   <id>.jar + <id>.jar.sha256                   one per module, ready for lemon/modules/
//   variants/2.0.0/<id>.jar + <id>.jar.sha256    the same module with version 2.0.0 (hot swap,
//                                                version-range checks)
// The module id is "test-" plus the directory name.
description = "Lemon test modules"

val paperApiVersion: String = providers.gradleProperty("paperApiVersion").get()
val variantVersion = "2.0.0"

/** Writes <jar>.sha256 in sha256sum format next to the jar of [jarTask]. */
fun Project.sha256Of(name: String, jarTask: TaskProvider<out AbstractArchiveTask>): TaskProvider<Task> =
    tasks.register(name) {
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
    val moduleId = "test-${project.name}"
    dependencies {
        "compileOnly"(project(":runtime-api"))
        "compileOnly"(project(":runtime-paper-api"))
        "compileOnly"("io.papermc.paper:paper-api:$paperApiVersion")
    }
    val jar = tasks.named<Jar>("jar") {
        archiveFileName = "$moduleId.jar"
    }
    val main = extensions.getByType<SourceSetContainer>()["main"]
    val variantJar = tasks.register<Jar>("variantJar") {
        archiveFileName = "$moduleId.jar"
        destinationDirectory = layout.buildDirectory.dir("variant/$variantVersion")
        from(main.output) {
            exclude("lemon-module.yml")
        }
        from(main.output) {
            include("lemon-module.yml")
            filter { line: String -> if (line.startsWith("version:")) "version: $variantVersion" else line }
        }
    }
    val sha256 = sha256Of("sha256", jar)
    val variantSha256 = sha256Of("variantSha256", variantJar)
    tasks.named("assemble") {
        dependsOn(sha256, variantSha256)
    }
    collect.configure {
        from(jar, sha256)
        into("variants/$variantVersion") {
            from(variantJar, variantSha256)
        }
    }
}
