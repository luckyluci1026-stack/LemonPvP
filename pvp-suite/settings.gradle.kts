// LemonPvP suite: four PaperSwap modules (stats, combat tag, killstreaks, scoreboard).
//
// The PaperSwap module API (runtime-api, runtime-paper-api) is taken from a PaperSwap checkout,
// included as a composite build. Default location: ../../PaperSwap, i.e. next to this repository.
// Override with -PpaperswapDir=<path to the PaperSwap repository>.
rootProject.name = "lemonpvp-suite"

val paperswapDirSetting = providers.gradleProperty("paperswapDir").getOrElse("../../PaperSwap")
val paperswapDir = File(paperswapDirSetting).let { if (it.isAbsolute) it else File(settingsDir, paperswapDirSetting) }.canonicalFile
val runtimeBuild = File(paperswapDir, "runtime")
if (!File(runtimeBuild, "settings.gradle.kts").isFile) {
    throw GradleException(
        "PaperSwap checkout not found at $paperswapDir. Clone " +
            "https://github.com/paperswapstudios-official/PaperSwap there or pass -PpaperswapDir=<path>."
    )
}
includeBuild(runtimeBuild)

include("core", "combattag", "killstreaks", "scoreboard")
