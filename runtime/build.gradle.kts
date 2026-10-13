import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// Version of the hot-deployment runtime itself (independent of Minecraft versions).
val runtimeVersion = providers.gradleProperty("runtimeVersion").getOrElse("0.1.0-SNAPSHOT")
val paperApiVersion = providers.gradleProperty("paperApiVersion").orNull
// Java release the runtime targets. 21 is the lowest JDK among all supported
// Paper versions (1.21.11), so the same sources compile for every target.
val javaRelease = providers.gradleProperty("javaRelease").map(String::toInt).getOrElse(21)

subprojects {
    apply(plugin = "java-library")
    group = "dev.lemonpvp.lemon"
    version = runtimeVersion

    repositories {
        mavenCentral()
        if (paperApiVersion != null) {
            maven("https://repo.papermc.io/repository/maven-public/")
        }
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = javaRelease
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing", "-Xlint:-try"))
        // Production code must compile without warnings; test code may use looser idioms.
        if (name == "compileJava") {
            options.compilerArgs.add("-Werror")
        }
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
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

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
    }
}
