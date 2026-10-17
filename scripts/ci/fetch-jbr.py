#!/usr/bin/env python3
"""Downloads the newest JetBrains Runtime (JBR) SDK for Linux x64 of a Java major version.

Usage: fetch-jbr.py <major> <dest-dir>

JBR is the JVM that supports enhanced class redefinition (-XX:+AllowEnhancedClassRedefinition,
the successor of DCEVM). The download link is taken from the release notes of
github.com/JetBrains/JetBrainsRuntime and the archive is verified against the published
.checksum file (SHA-512) before it is unpacked.

Writes "java=<dest>/.../bin/java" to $GITHUB_OUTPUT on success. If no build is found or the
download fails, prints a warning and writes nothing, so the dependent CI step is skipped and
reported as NOT executed (never as passed).
"""
import hashlib
import json
import os
import pathlib
import re
import sys
import tarfile
import urllib.request

API = "https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases?per_page=60"


def get(url: str, accept: str = "*/*") -> bytes:
    headers = {"Accept": accept, "User-Agent": "paperswap-ci"}
    token = os.environ.get("GITHUB_TOKEN")
    if token and url.startswith("https://api.github.com/"):
        headers["Authorization"] = f"Bearer {token}"
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=120) as r:
        return r.read()


def warn(message: str):
    print(f"::warning::{message}")
    sys.exit(0)


def main():
    major, dest = sys.argv[1], pathlib.Path(sys.argv[2])
    pattern = re.compile(r"https://cache-redirector\.jetbrains\.com/intellij-jbr/(jbrsdk-" + re.escape(major)
                         + r"(?:\.[0-9]+)*-linux-x64-b[0-9.]+\.tar\.gz)")
    try:
        releases = json.loads(get(API, "application/vnd.github+json"))
    except Exception as e:  # noqa: BLE001
        warn(f"JetBrains Runtime {major}: cannot list releases ({e!r}); enhanced-redefinition test NOT executed")
    url = name = None
    for release in releases:
        m = pattern.search(release.get("body") or "")
        if m:
            url, name = m.group(0), m.group(1)
            print(f"release {release.get('tag_name')}: {name}")
            break
    if url is None:
        warn(f"JetBrains Runtime {major}: no linux-x64 jbrsdk link in the latest releases; enhanced-redefinition test NOT executed")
    try:
        checksum = get(url + ".checksum").decode("utf-8", "replace")
        expected = re.search(r"\b[0-9a-fA-F]{128}\b", checksum)
        if expected is None:
            warn(f"{name}: checksum file has no SHA-512 digest; not using the download")
        archive = get(url)
    except Exception as e:  # noqa: BLE001
        warn(f"{name}: download failed ({e!r}); enhanced-redefinition test NOT executed")
    actual = hashlib.sha512(archive).hexdigest()
    if actual.lower() != expected.group(0).lower():
        print(f"::error::{name}: SHA-512 mismatch (expected {expected.group(0)}, got {actual})")
        sys.exit(1)
    print(f"{name}: {len(archive)} bytes, SHA-512 verified")
    dest.mkdir(parents=True, exist_ok=True)
    tar_path = dest / name
    tar_path.write_bytes(archive)
    with tarfile.open(tar_path) as tar:
        tar.extractall(dest, filter="data")
    java = next((p for p in dest.rglob("bin/java") if p.is_file()), None)
    if java is None:
        print(f"::error::{name}: no bin/java in the archive")
        sys.exit(1)
    print(f"java: {java}")
    out = os.environ.get("GITHUB_OUTPUT")
    if out:
        with open(out, "a", encoding="utf-8") as f:
            f.write(f"java={java}\n")


if __name__ == "__main__":
    main()
