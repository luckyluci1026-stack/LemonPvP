description = "Lemon hot-deployment runtime - platform-neutral manager"

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
    systemProperty("lemon.test.apiClasses", project(":runtime-api").layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath)
    dependsOn(":runtime-api:classes")
}
