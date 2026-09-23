import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    grim.`base-conventions`
    id("com.gradleup.shadow")
}

repositories {
    exclusive("https://repo.papermc.io/repository/maven-public/", { name = "papermc" }) {
        includeGroup("com.velocitypowered")
    }
    mavenCentral()
}

dependencies {
    // Velocity ships Adventure, Guice, Gson and a logger itself.
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    // Velocity has no library loader, so the JDBC driver has to travel in the
    // jar. MariaDB's driver talks to MySQL too.
    implementation(libs.mariadb.client)

    // The proxy's half of the ban channel is pinned by tests against
    // protocol/ban-channel-samples.tsv, the same file the backend's encoder
    // is pinned to. velocity-api is compileOnly above, so the test source set
    // needs its own copy (it also carries slf4j, which ProxyBanList takes).
    testImplementation(libs.velocity.api)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName = "bucksmpac-velocity-${rootProject.version}.jar"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // java.sql.Driver is found through META-INF/services; without merging, the
    // shaded driver would never register itself.
    mergeServiceFiles()
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
