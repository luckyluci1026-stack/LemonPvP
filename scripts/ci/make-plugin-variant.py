#!/usr/bin/env python3
"""Copies the PaperSwapTestPlugin jar with a changed plugin.yml (CI plugin update tests).

Usage:
  make-plugin-variant.py <PaperSwapTestPlugin.jar> <out.jar> version <new-version>
  make-plugin-variant.py <PaperSwapTestPlugin.jar> <out.jar> api-version <api-version>
  make-plugin-variant.py <PaperSwapTestPlugin.jar> <out.jar> addon
      addon: a second plugin "PaperSwapTestAddon" (main class PaperSwapTestAddon, depend: [PaperSwapTestPlugin],
      no commands) without the PaperSwapTestPlugin classes.
"""
import pathlib
import re
import sys
import zipfile

src, dst, mode = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3]
arg = sys.argv[4] if len(sys.argv) > 4 else None
dst.parent.mkdir(parents=True, exist_ok=True)

ADDON_YML = """name: PaperSwapTestAddon
version: "1.0.0"
main: dev.paperswap.example.plugin.PaperSwapTestAddon
api-version: "1.21"
depend: [PaperSwapTestPlugin]
description: Depends on PaperSwapTestPlugin (CI dependency test)
"""


def edit(text: str) -> str:
    if mode == "version":
        text, n = re.subn(r'(?m)^version:.*$', f'version: "{arg}"', text)
    elif mode == "api-version":
        text, n = re.subn(r'(?m)^api-version:.*$', f'api-version: "{arg}"', text)
    elif mode == "addon":
        return ADDON_YML
    else:
        sys.exit(f"unknown mode {mode}")
    if n != 1:
        sys.exit(f"plugin.yml has no single {mode} line")
    return text


with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if mode == "addon" and "/PaperSwapTestPlugin" in item.filename:
            continue
        data = zin.read(item.filename)
        if item.filename == "plugin.yml":
            data = edit(data.decode("utf-8")).encode("utf-8")
        zout.writestr(item, data)
print(f"{dst}: {mode} {arg or ''}")
