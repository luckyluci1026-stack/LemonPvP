#!/usr/bin/env python3
"""Prints a summary of JUnit XML reports below the given directory (used in CI logs)."""
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")
totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
per_project = {}
for f in root.rglob("TEST-*.xml"):
    if "test-results" not in f.parts:
        continue
    suite = ET.parse(f).getroot()
    project = f.parts[f.parts.index("build") - 1] if "build" in f.parts else "?"
    stats = per_project.setdefault(project, dict.fromkeys(totals, 0))
    for k in totals:
        v = int(suite.get(k, 0))
        totals[k] += v
        stats[k] += v
for p, s in sorted(per_project.items()):
    print(f"{p}: {s['tests']} tests, {s['failures']} failures, {s['errors']} errors, {s['skipped']} skipped")
print(f"TOTAL: {totals['tests']} tests, {totals['failures']} failures, {totals['errors']} errors, {totals['skipped']} skipped")
