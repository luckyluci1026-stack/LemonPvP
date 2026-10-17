description = "PaperSwap hot-deployment runtime - platform-neutral manager"

// SnakeYAML 2.2 is bundled by Paper (1.21.11 through 26.3) and therefore
// provided at runtime by the server. Tests need it explicitly.
val snakeYamlVersion = "2.2"
val junitVersion = "5.13.4"

dependencies {
    api(project(":runtime-api"))
    compileOnly("org.yaml:snakeyaml:$snakeYamlVersion")

    testImplementation("org.yaml:snakeyaml:$snakeYamlVersion")
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    // The integration-style tests compile small module jars at test time.
    systemProperty("paperswap.test.apiClasses", project(":runtime-api").layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath)
    dependsOn(":runtime-api:classes")
    // Core live patch tests redefine real classes, so the test JVM runs with the agent, like a server would.
    val agentJar = project(":livepatch-agent").layout.buildDirectory.file("libs/paperswap-agent.jar")
    dependsOn(":livepatch-agent:jar")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:" + agentJar.get().asFile.absolutePath,
            // The dynamic-attach test starts a second JVM without the agent and attaches this jar to it.
            "-Dpaperswap.test.agentJar=" + agentJar.get().asFile.absolutePath)
    })
    // Optional: run the tests on another JVM (e.g. JetBrains Runtime) with extra flags, e.g.
    //   -PtestJava=/opt/jbr/bin/java -PtestJvmArgs=-XX:+AllowEnhancedClassRedefinition
    providers.gradleProperty("testJava").orNull?.let { executable = it }
    providers.gradleProperty("testJvmArgs").orNull?.let { jvmArgs(it.split(' ').filter(String::isNotBlank)) }
}
