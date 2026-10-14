#!/usr/bin/env python3
"""Generate Lemon's paperweight build-file and source patches for one Paper ref.

Why this script exists
----------------------
A paperweight fork stores its changes to Paper's ``build.gradle.kts`` files and
to Paper's own sources as unified diffs.  Hand-writing those diffs is error
prone, so this script derives them reproducibly from the *exact* upstream files
of a pinned Paper commit by applying a small list of **anchored** text edits.

Every edit is anchored on a line that must exist exactly once.  If upstream
changes such a line the script fails loudly instead of producing a silently
broken patch - that is the signal that the patch needs a manual port (see
docs/UPSTREAM-SYNC.md).

Usage
-----
    python3 scripts/fork/generate_patches.py \
        --paper-checkout /path/to/Paper  --ref <commit> \
        --version-dir versions/26.2 [--baseline]

``--paper-checkout`` must be a git clone of https://github.com/PaperMC/Paper
that contains ``--ref``.  ``--baseline`` omits the hot-deployment runtime and
produces a plain re-branded Paper fork (used to validate the fork setup alone).
"""

from __future__ import annotations

import argparse
import difflib
import pathlib
import subprocess
import sys

# Relative path (seen from versions/<mc>/lemon-server) to the shared runtime sources.
RUNTIME_ROOT = "../../../runtime"
RUNTIME_SOURCE_DIRS = (
    "runtime-api/src/main/java",
    "runtime-core/src/main/java",
    "runtime-paper-api/src/main/java",
    "runtime-paper/src/main/java",
)


class PatchError(RuntimeError):
    pass


def git_show(checkout: pathlib.Path, ref: str, path: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(checkout), "show", f"{ref}:{path}"],
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


def replace_once(text: str, old: str, new: str, what: str) -> str:
    count = text.count(old)
    if count != 1:
        raise PatchError(f"anchor for '{what}' found {count} times (expected exactly 1):\n{old}")
    return text.replace(old, new, 1)


def make_patch(path: str, before: str, after: str) -> str:
    diff = difflib.unified_diff(
        before.splitlines(keepends=True),
        after.splitlines(keepends=True),
        fromfile=f"a/{path}",
        tofile=f"b/{path}",
        n=3,
    )
    out = "".join(diff)
    if not out:
        raise PatchError(f"no changes produced for {path}")
    return out


# --------------------------------------------------------------------------- #
# paper-server/build.gradle.kts
# --------------------------------------------------------------------------- #
FORK_REGISTRATION = """    gitFilePatches = false

    val fork = forks.register("lemon") {
        upstream.patchDir("paperServer") {
            upstreamPath = "paper-server"
            excludes = setOf("src/minecraft", "patches", "build.gradle.kts")
            patchesDir = rootDirectory.dir("lemon-server/paper-patches")
            outputDir = rootDirectory.dir("paper-server")
        }
    }

    activeFork = fork
"""


LIVEPATCH_APPLY = """// Lemon start - core live patch tasks, see docs/LIVE-UPDATES.md
apply(from = rootProject.file("../../scripts/livepatch/lemon-livepatch.gradle.kts"))
// Lemon end - core live patch tasks
"""


def server_build_edits(text: str, baseline: bool) -> str:
    text = replace_once(text, "    gitFilePatches = false\n", FORK_REGISTRATION, "fork registration")

    runtime_dirs = ""
    if not baseline:
        runtime_dirs = "        // Lemon start - hot deployment runtime (shared sources, see docs/HOT-DEPLOYMENT.md)\n"
        for d in RUNTIME_SOURCE_DIRS:
            runtime_dirs += f'        java {{ srcDir("{RUNTIME_ROOT}/{d}") }}\n'
        runtime_dirs += "        // Lemon end - hot deployment runtime\n"

    source_sets = (
        "sourceSets {\n"
        "    main {\n"
        '        java { srcDir("../paper-server/src/main/java") }\n'
        '        resources { srcDir("../paper-server/src/main/resources") }\n'
        f"{runtime_dirs}"
        "    }\n"
        "    test {\n"
        '        java { srcDir("../paper-server/src/test/java") }\n'
        '        resources { srcDir("../paper-server/src/test/resources") }\n'
        "    }\n"
        "}\n"
        "\n"
        'val log4jPlugins = sourceSets.create("log4jPlugins") {\n'
        '    java { srcDir("../paper-server/src/log4jPlugins/java") }\n'
        "}\n"
    )
    text = replace_once(text, 'val log4jPlugins = sourceSets.create("log4jPlugins")\n', source_sets, "source sets")
    text = replace_once(
        text,
        '    implementation(project(":paper-api"))\n',
        '    implementation(project(":lemon-api"))\n',
        "api project dependency",
    )
    # Honest branding: the fork must not identify itself as Paper.
    text = replace_once(text, '"Brand-Id" to "papermc:paper",', '"Brand-Id" to "lemonpvp:lemon",', "brand id")
    text = replace_once(text, '"Brand-Name" to "Paper",', '"Brand-Name" to "Lemon",', "brand name")
    if not baseline:
        # Gradle tasks for core live patches (livePatchBaseline, createLivePatch).
        text = text.rstrip("\n") + "\n\n" + LIVEPATCH_APPLY
    text = replace_once(
        text,
        'val generatedDir: java.nio.file.Path = layout.projectDirectory.dir("src/generated/java").asFile.toPath()',
        'val generatedDir: java.nio.file.Path = rootProject.layout.projectDirectory.dir("paper-server/src/generated/java").asFile.toPath()',
        "server generated dir",
    )
    return text


# --------------------------------------------------------------------------- #
# paper-api/build.gradle.kts
# --------------------------------------------------------------------------- #
def remove_checkstyle(text: str) -> str:
    """Paper >= 26.2 lints its API with a checkstyle setup that lives in the separate
    ':paper-checkstyle' project, which does not exist inside a fork. The fork does not
    change Paper's API sources, so the upstream lint is dropped from the fork API build."""
    if "checkstyle" not in text.lower():
        return text
    text = replace_once(text, "import io.papermc.paperweight.checkstyle.JavadocTag\n", "", "checkstyle import")
    text = replace_once(text, '    id("io.papermc.paperweight.paper-checkstyle")\n', "", "checkstyle plugin")
    # The custom javadoc tag set is shared with the javadoc task; keep the tag, but as a
    # plain javadoc option ("name:locations:header") so no checkstyle class is needed.
    tags_decl = 'val projectCustomJavadocTags = setOf(\n    JavadocTag("apiNote", "a", "API Note:"),\n)\n'
    if tags_decl not in text:
        raise PatchError("unexpected javadoc tag declaration; port remove_checkstyle() manually")
    text = replace_once(
        text,
        "options.tags(projectCustomJavadocTags.map { it.toOptionString() })",
        'options.tags("apiNote:a:API Note:")',
        "javadoc custom tags",
    )
    start = text.find("val projectCustomJavadocTags = setOf(")
    end = text.find("}\n", text.find("paperCheckstyle {"))
    if start < 0 or end < 0:
        raise PatchError("checkstyle configuration block not found")
    text = text[:start] + text[end + 2:].lstrip("\n")
    text = replace_once(text, '\n    // checkstyle\n    checkstyle(project(":paper-checkstyle"))\n', "\n", "checkstyle dependency")
    if "checkstyle" in text.lower():
        raise PatchError("unhandled checkstyle reference left in paper-api/build.gradle.kts")
    return text


def api_build_edits(text: str) -> str:
    text = remove_checkstyle(text)
    text = replace_once(
        text,
        'val generatedDir: java.nio.file.Path = layout.projectDirectory.dir("src/generated/java").asFile.toPath()',
        'val generatedDir: java.nio.file.Path = rootProject.layout.projectDirectory.dir("paper-api/src/generated/java").asFile.toPath()',
        "api generated dir",
    )
    text = replace_once(
        text,
        "            srcDir(generatedDir)\n        }\n    }\n",
        "            srcDir(generatedDir)\n"
        '            srcDir(file("../paper-api/src/main/java"))\n'
        "        }\n"
        "        resources {\n"
        '            srcDir(file("../paper-api/src/main/resources"))\n'
        "        }\n"
        "    }\n"
        "    test {\n"
        "        java {\n"
        '            srcDir(file("../paper-api/src/test/java"))\n'
        "        }\n"
        "        resources {\n"
        '            srcDir(file("../paper-api/src/test/resources"))\n'
        "        }\n"
        "    }\n",
        "api source sets",
    )
    text = replace_once(
        text,
        'options.overview = "src/main/javadoc/overview.html"',
        'options.overview = "../paper-api/src/main/javadoc/overview.html"',
        "javadoc overview",
    )
    text = replace_once(
        text,
        'inputs.dir("src/main/javadoc")',
        'inputs.dir("../paper-api/src/main/javadoc")',
        "javadoc inputs",
    )
    text = replace_once(text, 'from("src/main/javadoc") {', 'from("../paper-api/src/main/javadoc") {', "javadoc copy")
    return text


# --------------------------------------------------------------------------- #
# CraftServer.java (Paper-owned source, patched via lemon-server/paper-patches)
# --------------------------------------------------------------------------- #
HOOKS = "dev.lemonpvp.runtime.paper.LemonRuntimeHooks"


def craftserver_edits(text: str) -> str:
    # 1) Start the (optional) runtime after all POSTWORLD plugins are enabled,
    #    before commands are synced to players.
    old = (
        "            if (!io.papermc.paper.configuration.GlobalConfiguration.get().misc.loadPermissionsYmlBeforePlugins) "
        "this.loadCustomPermissions(); // Paper\n"
        "            this.syncCommands();\n"
    )
    new = (
        "            if (!io.papermc.paper.configuration.GlobalConfiguration.get().misc.loadPermissionsYmlBeforePlugins) "
        "this.loadCustomPermissions(); // Paper\n"
        f"            {HOOKS}.onPluginsEnabled(this); // Lemon - hot deployment runtime\n"
        "            this.syncCommands();\n"
    )
    text = replace_once(text, old, new, "postworld hook")
    # 2) Stop the runtime before plugins are disabled on shutdown.
    text = replace_once(
        text,
        "    public void disablePlugins() {\n        this.pluginManager.disablePlugins();\n",
        "    public void disablePlugins() {\n"
        f"        {HOOKS}.onPluginsDisabling(); // Lemon - hot deployment runtime\n"
        "        this.pluginManager.disablePlugins();\n",
        "disable hook",
    )
    # 3) Stop the runtime before /reload clears commands, listeners and plugins.
    text = replace_once(
        text,
        "        this.commandMap.clearCommands(); // Paper - Move command reloading up\n",
        f"        {HOOKS}.onReloadStarting(); // Lemon - hot deployment runtime\n"
        "        this.commandMap.clearCommands(); // Paper - Move command reloading up\n",
        "reload hook",
    )
    return text


def write(path: pathlib.Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")
    print(f"wrote {path}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--paper-checkout", required=True, type=pathlib.Path)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--version-dir", required=True, type=pathlib.Path)
    parser.add_argument("--baseline", action="store_true", help="plain fork without the runtime")
    args = parser.parse_args()

    out = args.version_dir
    try:
        server = git_show(args.paper_checkout, args.ref, "paper-server/build.gradle.kts")
        write(out / "lemon-server/build.gradle.kts.patch",
              make_patch("paper-server/build.gradle.kts", server, server_build_edits(server, args.baseline)))

        api = git_show(args.paper_checkout, args.ref, "paper-api/build.gradle.kts")
        write(out / "lemon-api/build.gradle.kts.patch",
              make_patch("paper-api/build.gradle.kts", api, api_build_edits(api)))

        craft_path = "src/main/java/org/bukkit/craftbukkit/CraftServer.java"
        patch_file = out / "lemon-server/paper-patches/files" / (craft_path + ".patch")
        if args.baseline:
            if patch_file.exists():
                patch_file.unlink()
                print(f"removed {patch_file}")
        else:
            craft = git_show(args.paper_checkout, args.ref, "paper-server/" + craft_path)
            write(patch_file, make_patch(craft_path, craft, craftserver_edits(craft)))
    except (PatchError, subprocess.CalledProcessError) as e:
        print(f"error: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
