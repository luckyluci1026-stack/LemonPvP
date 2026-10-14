#!/usr/bin/env python3
"""CI helper for Lemon core live patches (.lemonpatch archives).

  lemonpatch.py show <patch> [--expect-classes a,b]
      Prints id, target and classes; fails if the class list differs from --expect-classes.

  lemonpatch.py compare-base <paperclip.jar> <baseline.jar> <class>
      Diagnostic: compares the server jar embedded in a paperclip jar with the baseline jar
      (build manifest and the digest of one class). Always exits 0.

  lemonpatch.py variant <patch> <out-dir> --id <new-id> [options]
      Writes <out-dir>/<new-id>.lemonpatch (+ .sha256) derived from <patch>, to test that the
      server rejects bad patches. Options:
        --target-build <id>          claim a different server build
        --garbage-class <name>       replace that class file with bytes that are not a class file
        --add-corrupt-class <names>  add the first of the comma-separated classes found in --base-jar,
                                     with max_stack of one method set to 0 (passes every Lemon
                                     check; the JVM verifier rejects it)
        --add-wrong-base <names>     add the first class found in --base-jar unchanged, but with a
                                     wrong base digest
        --base-jar <jar>             server jar the patch was built against
        --bad-sidecar                write a .sha256 sidecar with a wrong digest

The manifest format is the one written by dev.lemonpvp.runtime.livepatch.PatchManifest#toYaml.
"""
import argparse
import hashlib
import io
import pathlib
import re
import struct
import sys
import zipfile

MANIFEST = "lemon-patch.yml"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_patch(path):
    with zipfile.ZipFile(path) as z:
        files = {n: z.read(n) for n in z.namelist()}
    return files[MANIFEST].decode("utf-8"), {n: d for n, d in files.items() if n != MANIFEST}


def class_names(manifest: str):
    return re.findall(r'(?m)^  - name: "([^"]+)"$', manifest)


def archive_path(name: str) -> str:
    return "classes/" + name.replace(".", "/") + ".class"


def write_patch(out_dir: pathlib.Path, patch_id: str, manifest: str, classes: dict, bad_sidecar: bool):
    out_dir.mkdir(parents=True, exist_ok=True)
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr(zipfile.ZipInfo(MANIFEST, (1980, 1, 1, 0, 0, 0)), manifest.encode("utf-8"))
        for name in class_names(manifest):
            z.writestr(zipfile.ZipInfo(archive_path(name), (1980, 1, 1, 0, 0, 0)), classes[archive_path(name)])
    data = buf.getvalue()
    file = out_dir / f"{patch_id}.lemonpatch"
    file.write_bytes(data)
    digest = sha256(data)
    sidecar_digest = sha256(data + b"tampered") if bad_sidecar else digest
    (out_dir / f"{patch_id}.lemonpatch.sha256").write_text(f"{sidecar_digest}  {file.name}\n")
    print(f"{file}: {len(data)} bytes, sha256 {digest}" + (" (sidecar deliberately wrong)" if bad_sidecar else ""))
    for name in class_names(manifest):
        print(f"  class {name}")


def set_entry_sha(manifest: str, name: str, new_sha: str) -> str:
    pattern = re.compile(r'(  - name: "' + re.escape(name) + r'"\n    sha256: ")([0-9a-f]{64})(")')
    result, n = pattern.subn(lambda m: m.group(1) + new_sha + m.group(3), manifest)
    if n != 1:
        sys.exit(f"class {name} not found in the manifest")
    return result


def add_entry(manifest: str, name: str, sha: str, base_sha: str) -> str:
    if not manifest.endswith("\n"):
        manifest += "\n"
    return manifest + f'  - name: "{name}"\n    sha256: "{sha}"\n    base-sha256: "{base_sha}"\n'


def zero_max_stack(data: bytes) -> bytes:
    """Sets max_stack of the first method whose code uses the operand stack to 0."""
    b = bytearray(data)
    if b[:4] != b"\xca\xfe\xba\xbe":
        sys.exit("not a class file")
    pos = 10
    count = struct.unpack_from(">H", b, 8)[0]
    utf8 = {}
    i = 1
    while i < count:
        tag = b[pos]
        if tag == 1:
            length = struct.unpack_from(">H", b, pos + 1)[0]
            utf8[i] = bytes(b[pos + 3:pos + 3 + length]).decode("utf-8", "replace")
            pos += 3 + length
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            pos += 3
        elif tag == 15:
            pos += 4
        else:
            sys.exit(f"unknown constant pool tag {tag}")
        i += 1
    pos += 6  # access, this, super
    interfaces = struct.unpack_from(">H", b, pos)[0]
    pos += 2 + 2 * interfaces

    def skip_attributes(p):
        n = struct.unpack_from(">H", b, p)[0]
        p += 2
        for _ in range(n):
            p += 6 + struct.unpack_from(">I", b, p + 2)[0]
        return p

    fields = struct.unpack_from(">H", b, pos)[0]
    pos += 2
    for _ in range(fields):
        pos = skip_attributes(pos + 6)
    methods = struct.unpack_from(">H", b, pos)[0]
    pos += 2
    for _ in range(methods):
        name = utf8.get(struct.unpack_from(">H", b, pos + 2)[0], "?")
        attrs = struct.unpack_from(">H", b, pos + 6)[0]
        p = pos + 8
        for _ in range(attrs):
            attr_name = utf8.get(struct.unpack_from(">H", b, p)[0])
            length = struct.unpack_from(">I", b, p + 2)[0]
            if attr_name == "Code":
                max_stack = struct.unpack_from(">H", b, p + 6)[0]
                if max_stack > 0:
                    struct.pack_into(">H", b, p + 6, 0)
                    print(f"  corrupted method {name}: max_stack {max_stack} -> 0")
                    return bytes(b)
            p += 6 + length
        pos = p
    sys.exit("no method with a non-empty operand stack found")


def base_class(base_jar: str, candidates: str):
    """Returns (name, bytes) of the first of the comma-separated class names that is in the jar."""
    with zipfile.ZipFile(base_jar) as z:
        for name in candidates.split(","):
            try:
                return name, z.read(name.replace(".", "/") + ".class")
            except KeyError:
                continue
    sys.exit(f"none of {candidates} is in {base_jar}")


def build_attributes(jar: zipfile.ZipFile) -> str:
    try:
        text = jar.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    except KeyError:
        return "(no manifest)"
    keys = ("Brand-Id", "Implementation-Version", "Build-Time")
    values = {}
    for line in text.splitlines():
        for k in keys:
            if line.startswith(k + ": "):
                values[k] = line[len(k) + 2:].strip()
    return "/".join(values.get(k, "-") for k in keys)


def compare_base(paperclip: str, baseline: str, class_name: str):
    entry = class_name.replace(".", "/") + ".class"
    with zipfile.ZipFile(baseline) as b:
        base_class = b.read(entry)
        print(f"baseline  {baseline}: build {build_attributes(b)}, {class_name} {sha256(base_class)}")
    with zipfile.ZipFile(paperclip) as p:
        nested = [n for n in p.namelist() if n.startswith("META-INF/versions/") and n.endswith(".jar")]
        if not nested:
            print(f"paperclip {paperclip}: no META-INF/versions/*.jar")
            return
        for name in nested:
            data = p.read(name)
            with zipfile.ZipFile(io.BytesIO(data)) as server:
                try:
                    cls = server.read(entry)
                    digest = sha256(cls)
                except KeyError:
                    digest = "(class missing)"
                same = "MATCH" if digest == sha256(base_class) else "DIFFERENT"
                print(f"embedded  {name}: jar {sha256(data)}, build {build_attributes(server)}, {class_name} {digest} -> {same}")
    print(f"baseline jar sha256 {sha256(open(baseline, 'rb').read())}")


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="cmd", required=True)
    cmp_ = sub.add_parser("compare-base")
    cmp_.add_argument("paperclip")
    cmp_.add_argument("baseline")
    cmp_.add_argument("class_name")
    show = sub.add_parser("show")
    show.add_argument("patch")
    show.add_argument("--expect-classes")
    var = sub.add_parser("variant")
    var.add_argument("patch")
    var.add_argument("out_dir")
    var.add_argument("--id", required=True)
    var.add_argument("--target-build")
    var.add_argument("--garbage-class")
    var.add_argument("--add-corrupt-class")
    var.add_argument("--add-wrong-base")
    var.add_argument("--base-jar")
    var.add_argument("--bad-sidecar", action="store_true")
    args = parser.parse_args()

    if args.cmd == "compare-base":
        compare_base(args.paperclip, args.baseline, args.class_name)
        return
    manifest, classes = read_patch(args.patch)
    if args.cmd == "show":
        print(manifest)
        names = class_names(manifest)
        if args.expect_classes is not None and sorted(names) != sorted(args.expect_classes.split(",")):
            sys.exit(f"{args.patch} contains {names}, expected {args.expect_classes}")
        return

    manifest, n = re.subn(r'(?m)^id: ".*"$', f'id: "{args.id}"', manifest)
    if n != 1:
        sys.exit("manifest has no single id line")
    manifest = re.sub(r'(?m)^description: ".*"$', f'description: "CI variant {args.id}"', manifest)
    if args.target_build:
        manifest, n = re.subn(r'(?m)^  build-id: ".*"$', f'  build-id: "{args.target_build}"', manifest)
        if n != 1:
            sys.exit("manifest has no single build-id line")
    if args.garbage_class:
        data = b"this is not a class file"
        classes[archive_path(args.garbage_class)] = data
        manifest = set_entry_sha(manifest, args.garbage_class, sha256(data))
    if args.add_corrupt_class or args.add_wrong_base:
        if not args.base_jar:
            sys.exit("--base-jar is required")
    if args.add_corrupt_class:
        name, original = base_class(args.base_jar, args.add_corrupt_class)
        corrupt = zero_max_stack(original)
        classes[archive_path(name)] = corrupt
        manifest = add_entry(manifest, name, sha256(corrupt), sha256(original))
    if args.add_wrong_base:
        name, original = base_class(args.base_jar, args.add_wrong_base)
        classes[archive_path(name)] = original
        manifest = add_entry(manifest, name, sha256(original), sha256(original + b"other build"))
    write_patch(pathlib.Path(args.out_dir), args.id, manifest, classes, args.bad_sidecar)


if __name__ == "__main__":
    main()
