#!/usr/bin/env python3
"""Prints the source excerpts listed in request.txt for one version directory.

Usage: inspect.py <request.txt> <versions/<mc> directory>
"""
import pathlib
import re
import sys

request, root = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
MAX_LINES = 400


def show(path: pathlib.Path, start: int, end: int):
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    for i in range(max(0, start), min(len(lines), end)):
        print(f"{i + 1:5}: {lines[i]}")


def method(path: pathlib.Path, name: str):
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    decl = re.compile(r"^\s+(public|protected|private|static|final|synchronized|abstract|\s)*[\w<>\[\], ?.]+\s+" + re.escape(name) + r"\(")
    found = False
    for i, line in enumerate(lines):
        if decl.search(line):
            found = True
            depth, j = 0, i
            seen_open = False
            while j < len(lines) and j - i < MAX_LINES:
                depth += lines[j].count("{") - lines[j].count("}")
                seen_open |= "{" in lines[j]
                j += 1
                if seen_open and depth <= 0:
                    break
            print(f"--- {path} :: {name} (line {i + 1})")
            show(path, i, j)
    if not found:
        print(f"--- {path} :: {name} not found")


for raw in request.read_text().splitlines():
    line = raw.strip()
    if not line or line.startswith("#") or line.startswith("version "):
        continue
    kind, _, rest = line.partition(" ")
    if kind == "file":
        p = root / rest
        print(f"=== file {rest}")
        if p.is_file():
            show(p, 0, MAX_LINES * 3)
        else:
            print("    (missing)")
    elif kind == "method":
        name, _, rel = rest.partition(" ")
        p = root / rel
        print(f"=== method {name} in {rel}")
        if p.is_file():
            method(p, name)
        else:
            print("    (missing)")
    elif kind == "grep":
        regex, _, rel = rest.rpartition(" ")
        target = root / rel
        print(f"=== grep {regex} in {rel}")
        files = [target] if target.is_file() else sorted(target.rglob("*.java")) if target.is_dir() else []
        pattern = re.compile(regex)
        shown = 0
        for f in files:
            lines = f.read_text(encoding="utf-8", errors="replace").splitlines()
            for i, l in enumerate(lines):
                if pattern.search(l) and shown < 60:
                    shown += 1
                    print(f"--- {f.relative_to(root)}:{i + 1}")
                    show(f, i - 12, i + 13)
    elif kind == "grepn":
        n, _, rest2 = rest.partition(" ")
        regex, _, rel = rest2.rpartition(" ")
        target = root / rel
        print(f"=== grepn {n} {regex} in {rel}")
        files = [target] if target.is_file() else sorted(target.rglob("*.java")) if target.is_dir() else []
        pattern = re.compile(regex)
        for f in files:
            lines = f.read_text(encoding="utf-8", errors="replace").splitlines()
            for i, l in enumerate(lines):
                if pattern.search(l):
                    print(f"--- {f.relative_to(root)}:{i + 1}")
                    show(f, i - int(n), i + int(n) + 1)
    elif kind == "list":
        regex, _, rel = rest.rpartition(" ")
        target = root / rel
        print(f"=== list {regex} in {rel}")
        files = [target] if target.is_file() else sorted(target.rglob("*.java")) if target.is_dir() else []
        pattern = re.compile(regex)
        for f in files:
            for i, l in enumerate(f.read_text(encoding="utf-8", errors="replace").splitlines()):
                if pattern.search(l):
                    print(f"{f.relative_to(root)}:{i + 1}: {l.strip()}")
    else:
        print(f"unknown request line: {line}")
