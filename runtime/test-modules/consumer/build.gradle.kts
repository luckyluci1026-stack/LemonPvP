// Uses the service interface of test-library; at runtime the class comes from the
// library's class loader because paperswap-module.yml declares the dependency.
dependencies {
    compileOnly(project(":test-modules:library"))
}
