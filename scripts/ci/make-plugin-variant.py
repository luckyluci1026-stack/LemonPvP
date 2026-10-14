#!/usr/bin/env python3
"""Copies the LemonTestPlugin jar with a changed plugin.yml (CI plugin update tests).

Usage:
  make-plugin-variant.py <LemonTestPlugin.jar> <out.jar> version <new-version>
  make-plugin-variant.py <LemonTestPlugin.jar> <out.jar> api-version <api-version>
  make-plugin-variant.py <LemonTestPlugin.jar> <out.jar> addon
      addon: a second plugin "LemonTestAddon" (main class LemonTestAddon, depend: [LemonTestPlugin],
      no commands) without the LemonTestPlugin classes.
"""
import pathlib
import re
import sys
import zipfile

src, dst, mode = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3]
arg = sys.argv[4] if len(sys.argv) > 4 else None
dst.parent.mkdir(parents=True, exist_ok=True)

ADDON_YML = """name: LemonTestAddon
version: "1.0.0"
main: dev.lemonpvp.example.plugin.LemonTestAddon
api-version: "1.21"
depend: [LemonTestPlugin]
description: Depends on LemonTestPlugin (CI dependency test)
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
        if mode == "addon" and "/LemonTestPlugin" in item.filename:
            continue
        data = zin.read(item.filename)
        if item.filename == "plugin.yml":
            data = edit(data.decode("utf-8")).encode("utf-8")
        zout.writestr(item, data)
print(f"{dst}: {mode} {arg or ''}")
