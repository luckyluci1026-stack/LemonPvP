// Uses the API of pvp-core (stats service, combat tag service, PvpKillEvent); at runtime
// the classes come from pvp-core's class loader because lemon-module.yml declares the dependency.
dependencies {
    compileOnly(project(":pvp-suite:core"))
    testImplementation(project(":pvp-suite:core"))
}
