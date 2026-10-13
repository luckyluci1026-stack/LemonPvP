#!/usr/bin/env python3
"""Copies a module jar with a different 'version' in lemon-module.yml (CI hot-swap test).

Usage: make-module-variant.py <input.jar> <output.jar> <new-version>
Also writes <output.jar>.sha256 in sha256sum format.
"""
import hashlib
import pathlib
import re
import sys
import zipfile

src, dst, version = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3]
dst.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "lemon-module.yml":
            text, n = re.subn(r"(?m)^version:.*$", f"version: {version}", data.decode("utf-8"))
            if n != 1:
                sys.exit("lemon-module.yml has no single version line")
            data = text.encode("utf-8")
        zout.writestr(item, data)
digest = hashlib.sha256(dst.read_bytes()).hexdigest()
pathlib.Path(str(dst) + ".sha256").write_text(f"{digest}  {dst.name}\n")
print(f"{dst} version={version} sha256={digest}")
