rootProject.name = "lemon-runtime"

// Platform-neutral parts: build and test anywhere with JDK 21+.
include("runtime-api")
include("runtime-core")
// Java agent for core live patching (-javaagent:lemon-livepatch-agent.jar).
include("livepatch-agent")

// Paper-specific parts need the Paper API, which is only published on
// repo.papermc.io. They are included when a Paper API version is given, e.g.
//   ./gradlew build -PpaperApiVersion=26.2.build.+
// Inside the actual fork build these sources are compiled directly into the
// server (see scripts/fork/generate_patches.py), not through this build.
if (providers.gradleProperty("paperApiVersion").isPresent) {
    include("runtime-paper-api")
    include("runtime-paper")
    include("example-module")
}
