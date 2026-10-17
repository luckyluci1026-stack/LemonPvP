// Uses the API of pvp-core (stats service, combat tag service, PvpKillEvent); at runtime
// the classes come from pvp-core's class loader because paperswap-module.yml declares the dependency.
dependencies {
    compileOnly(project(":core"))
    testImplementation(project(":core"))
}
