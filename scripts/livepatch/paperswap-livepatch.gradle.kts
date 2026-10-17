// PaperSwap core live patches - applied to versions/<mc>/paperswap-server by the fork build patch.
//
//   ./gradlew :paperswap-server:livePatchBaseline              after building the jar you deploy
//   ./gradlew :paperswap-server:createLivePatch -PpatchId=fix-tps [-PpatchDescription="..."]
//             [-PserverJar=<server jar from the server's versions/<mc>/ directory>]
//             [-PdeployDir=<existing directory, e.g. a local test server's paperswap/patches/inbox>]
//   ./gradlew :paperswap-server:checkLivePatch -PpatchId=fix-tps [-PserverJar=<server jar>]
//
// createLivePatch compiles the server sources and packs every class whose bytecode differs
// from the baseline jar into build/livepatch/<id>.swappatch (+ .sha256). The tool runs from
// the server's own compiled classes (dev.paperswap.runtime.livepatch.tool.LivePatchTool).
// -PserverJar: the jar that really runs (paperclip writes it to versions/<mc>/ in the server
// directory). Required for 1.21.11, whose paperclip rewrites the class files; the baseline
// jar is then only used to detect what changed. See docs/LIVE-UPDATES.md.
// -PdeployDir copies the patch and its .sha256 into an existing directory after a successful
// build. It does not apply anything and does not approve the patch for the production profile.
// checkLivePatch tests build/livepatch/<id>.swappatch against the server jar (default: the
// baseline): checksum, build id, class digests and the kind of every change.

val livePatchDir = layout.buildDirectory.dir("livepatch")
val mainOutput = the<SourceSetContainer>().named("main")

tasks.register<Copy>("livePatchBaseline") {
    group = "paperswap"
    description = "Saves the current server jar as the base for createLivePatch (run it for the jar you deploy)."
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(livePatchDir.map { it.dir("baseline") })
    rename { "baseline.jar" }
}

tasks.register<JavaExec>("createLivePatch") {
    group = "paperswap"
    description = "Builds a core live patch from every class that differs from the baseline jar."
    dependsOn(tasks.named("classes"))
    val baseline = livePatchDir.map { it.file("baseline/baseline.jar") }
    val classesDirs = mainOutput.map { it.output.classesDirs }
    classpath = files(mainOutput.map { it.runtimeClasspath })
    mainClass.set("dev.paperswap.runtime.livepatch.tool.LivePatchTool")
    val patchId = providers.gradleProperty("patchId")
    // Resolved without capturing the project (the build uses the configuration cache).
    val projectDir = layout.projectDirectory.asFile
    val serverJar = providers.gradleProperty("serverJar").map { p -> java.io.File(p).let { if (it.isAbsolute) it else java.io.File(projectDir, p) } }
    val patchDescription = providers.gradleProperty("patchDescription").orElse("")
    val outDir = livePatchDir.map { it.asFile.absolutePath }
    argumentProviders.add(CommandLineArgumentProvider {
        val base = baseline.get().asFile
        require(base.isFile) { "no baseline jar at $base - run :paperswap-server:livePatchBaseline for the jar you deployed" }
        val running = serverJar.orNull
        require(running == null || running.isFile) { "-PserverJar=$running does not exist" }
        val args = mutableListOf(
            "create",
            "--base", (running ?: base).absolutePath,
            "--classes", classesDirs.get().files.first { it.path.replace('\\', '/').endsWith("/java/main") }.absolutePath,
            "--out", outDir.get(),
            "--description", patchDescription.get(),
        )
        if (patchId.isPresent) {
            args += listOf("--id", patchId.get())
        }
        if (running != null) {
            args += listOf("--reference", base.absolutePath)
        }
        args
    })
    // The tool exits with 2 when nothing changed; that is not a build failure.
    isIgnoreExitValue = true
    val deployDir = providers.gradleProperty("deployDir").map { p -> java.io.File(p).let { if (it.isAbsolute) it else java.io.File(projectDir, p) } }
    doLast {
        val code = executionResult.get().exitValue
        if (code == 1) {
            throw GradleException("createLivePatch failed (see the error above)")
        }
        val target = deployDir.orNull
        if (code == 0 && target != null) {
            if (!target.isDirectory) {
                throw GradleException("-PdeployDir=$target is not an existing directory (nothing was copied)")
            }
            val written = patchId.orNull?.let { java.io.File(outDir.get(), "$it.swappatch") }
                ?: java.io.File(outDir.get()).listFiles { f -> f.name.endsWith(".swappatch") }!!.maxByOrNull { it.lastModified() }!!
            for (f in listOf(written, java.io.File(written.path + ".sha256"))) {
                f.copyTo(java.io.File(target, f.name), overwrite = true)
            }
            println("copied ${written.name} and ${written.name}.sha256 to $target")
        }
    }
}

tasks.register<JavaExec>("checkLivePatch") {
    group = "paperswap"
    description = "Checks build/livepatch/<patchId>.swappatch against the server jar (-PserverJar, default: the baseline)."
    dependsOn(tasks.named("classes"))
    classpath = files(mainOutput.map { it.runtimeClasspath })
    mainClass.set("dev.paperswap.runtime.livepatch.tool.LivePatchTool")
    val baseline = livePatchDir.map { it.file("baseline/baseline.jar") }
    val projectDir = layout.projectDirectory.asFile
    val serverJar = providers.gradleProperty("serverJar").map { p -> java.io.File(p).let { if (it.isAbsolute) it else java.io.File(projectDir, p) } }
    val patchId = providers.gradleProperty("patchId")
    val outDir = livePatchDir.map { it.asFile.absolutePath }
    argumentProviders.add(CommandLineArgumentProvider {
        require(patchId.isPresent) { "-PpatchId=<id> is required" }
        val jar = serverJar.orNull ?: baseline.get().asFile
        require(jar.isFile) { "no server jar at $jar - pass -PserverJar or run :paperswap-server:livePatchBaseline" }
        listOf("check", "--patch", java.io.File(outDir.get(), patchId.get() + ".swappatch").absolutePath, "--server-jar", jar.absolutePath)
    })
    // 3 = applicable only with enhanced class redefinition: reported, not a build failure.
    isIgnoreExitValue = true
    doLast {
        if (executionResult.get().exitValue == 1) {
            throw GradleException("the patch is not applicable to this server jar (see above)")
        }
    }
}
