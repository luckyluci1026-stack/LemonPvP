// Lemon core live patches - applied to versions/<mc>/lemon-server by the fork build patch.
//
//   ./gradlew :lemon-server:livePatchBaseline              after building the jar you deploy
//   ./gradlew :lemon-server:createLivePatch -PpatchId=fix-tps [-PpatchDescription="..."]
//
// createLivePatch compiles the server sources and packs every class whose bytecode differs
// from the baseline jar into build/livepatch/<id>.lemonpatch (+ .sha256). The tool runs from
// the server's own compiled classes (dev.lemonpvp.runtime.livepatch.tool.LivePatchTool).
// See docs/LIVE-UPDATES.md.

val livePatchDir = layout.buildDirectory.dir("livepatch")
val mainOutput = the<SourceSetContainer>().named("main")

tasks.register<Copy>("livePatchBaseline") {
    group = "lemon"
    description = "Saves the current server jar as the base for createLivePatch (run it for the jar you deploy)."
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(livePatchDir.map { it.dir("baseline") })
    rename { "baseline.jar" }
}

tasks.register<JavaExec>("createLivePatch") {
    group = "lemon"
    description = "Builds a core live patch from every class that differs from the baseline jar."
    dependsOn(tasks.named("classes"))
    val baseline = livePatchDir.map { it.file("baseline/baseline.jar") }
    val classesDirs = mainOutput.map { it.output.classesDirs }
    classpath = files(mainOutput.map { it.runtimeClasspath })
    mainClass.set("dev.lemonpvp.runtime.livepatch.tool.LivePatchTool")
    val patchId = providers.gradleProperty("patchId")
    val patchDescription = providers.gradleProperty("patchDescription").orElse("")
    val outDir = livePatchDir.map { it.asFile.absolutePath }
    argumentProviders.add(CommandLineArgumentProvider {
        val base = baseline.get().asFile
        require(base.isFile) { "no baseline jar at $base - run :lemon-server:livePatchBaseline for the jar you deployed" }
        val args = mutableListOf(
            "create",
            "--base", base.absolutePath,
            "--classes", classesDirs.get().files.first { it.path.replace('\\', '/').endsWith("/java/main") }.absolutePath,
            "--out", outDir.get(),
            "--description", patchDescription.get(),
        )
        if (patchId.isPresent) {
            args += listOf("--id", patchId.get())
        }
        args
    })
    // The tool exits with 2 when nothing changed; that is not a build failure.
    isIgnoreExitValue = true
    doLast {
        val code = executionResult.get().exitValue
        if (code == 1) {
            throw GradleException("createLivePatch failed (see the error above)")
        }
    }
}
