import java.security.MessageDigest
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// LemonPvP suite: four PaperSwap modules that work together (README.md).
//   ./gradlew collect -PpaperApiVersion=<Paper API version> [-PjavaRelease=21|25]
// Runs the unit tests of the game logic, then puts every module into build/modules/:
// pvp-<name>.jar + pvp-<name>.jar.sha256, ready for paperswap/modules/ on a PaperSwap server.
// The module id is "pvp-" plus the directory name.
val paperApiVersion: String? = providers.gradleProperty("paperApiVersion").orNull
// 21 is the lowest JDK of the Paper versions PaperSwap supports.
val javaRelease = providers.gradleProperty("javaRelease").map(String::toInt).getOrElse(21)
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
    apply(plugin = "java-library")
    group = "dev.lemonpvp"
    version = "1.0.0"
    val moduleId = "pvp-${project.name}"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        // Substituted by the projects of the included PaperSwap build (settings.gradle.kts).
        "compileOnly"("dev.paperswap:runtime-api")
        "compileOnly"("dev.paperswap:runtime-paper-api")
        if (paperApiVersion != null) {
            "compileOnly"("io.papermc.paper:paper-api:$paperApiVersion")
        }
        // Unit tests cover the plain-Java game logic (no server needed).
        "testImplementation"(platform("org.junit:junit-bom:$junitVersion"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        if (paperApiVersion == null) {
            doFirst { throw GradleException("Pass the Paper API version, e.g. -PpaperApiVersion=1.21.11-R0.1-SNAPSHOT or 26.2.build.+") }
        }
        options.encoding = Charsets.UTF_8.name()
        options.release = javaRelease
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing", "-Xlint:-try"))
        // Production code must compile without warnings; test code may use looser idioms.
        if (name == "compileJava") {
            options.compilerArgs.add("-Werror")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("failed", "skipped")
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
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
