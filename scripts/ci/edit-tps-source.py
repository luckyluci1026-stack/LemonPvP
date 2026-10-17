#!/usr/bin/env python3
"""Changes Paper's /tps command source like a developer would, for the CI core-patch test.

Usage: edit-tps-source.py <TicksPerSecondCommand.java> text|structural

  text        changes only the message literal (method body change; standard JVMs can apply it)
  structural  moves the message into a new method (adds a method; needs enhanced redefinition)
"""
import pathlib
import sys

LITERAL = '"TPS from last 1m, 5m, 15m: "'
path, mode = pathlib.Path(sys.argv[1]), sys.argv[2]
source = path.read_text(encoding="utf-8")
if source.count(LITERAL) != 1:
    sys.exit(f"{path}: expected exactly one {LITERAL}, found {source.count(LITERAL)}")
if mode == "text":
    source = source.replace(LITERAL, '"TPS (live patch) from last 1m, 5m, 15m: "')
elif mode == "structural":
    source = source.replace(LITERAL, "paperswapStructuralLabel()")
    end = source.rstrip().rfind("}")
    source = (source[:end] + "\n    private static String paperswapStructuralLabel() {\n"
              + '        return "TPS (structural) from last 1m, 5m, 15m: ";\n    }\n' + source[end:])
else:
    sys.exit("mode must be text or structural")
path.write_text(source, encoding="utf-8")
print(f"{path}: {mode} change written")
