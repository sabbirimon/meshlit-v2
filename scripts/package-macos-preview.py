#!/usr/bin/env python3
"""Package an already compiled Intel desktop client using a portable JDK.

No downloads, installation, elevated privileges, signing, or publication occur.
The output and scratch directories must be new to avoid replacing user files.
"""
import argparse
import hashlib
import json
import platform
import plistlib
import re
import shutil
import subprocess
import zipfile
from pathlib import Path

MODULES = "java.base,java.desktop,java.net.http,java.prefs,jdk.crypto.ec,jdk.unsupported"


def run(*args):
    subprocess.run([str(a) for a in args], check=True)


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-image", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--scratch", type=Path, required=True)
    parser.add_argument("--runtime-provenance", type=Path, required=True)
    parser.add_argument("--runtime-source", type=Path, required=True)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--server", type=Path, required=True)
    args = parser.parse_args()
    if platform.system() != "Darwin" or platform.machine() != "x86_64":
        parser.error("This preview packager requires an Intel Mac.")
    repo = Path(__file__).resolve().parents[1]
    app_input = args.app_image.resolve() / "Contents/app"
    jars = list(app_input.glob("desktopApp-*.jar"))
    if len(jars) != 1:
        parser.error("Expected exactly one compiled desktopApp jar.")
    provenance = json.loads(args.runtime_provenance.read_text(encoding="utf-8"))
    if digest(args.runtime_source) != provenance["sourceSha256"]:
        parser.error("Runtime source archive checksum mismatch.")
    model_manifest = json.loads((repo / "desktopApp/distribution/bundled-model.json").read_text())
    if args.model.stat().st_size != model_manifest["sizeBytes"] or digest(args.model) != model_manifest["sha256"]:
        parser.error("Desktop starter model checksum/size mismatch.")
    args.output.mkdir(parents=True, exist_ok=False)
    args.scratch.mkdir(parents=True, exist_ok=False)
    runtime = args.scratch / "runtime"
    run(args.jdk_home / "bin/jlink", "--add-modules", MODULES,
        "--strip-debug", "--no-man-pages", "--no-header-files", "--output", runtime)
    image_dir = args.scratch / "image"
    run(args.jdk_home / "bin/jpackage", "--type", "app-image", "--dest", image_dir,
        "--input", app_input, "--runtime-image", runtime, "--name", "MeshlitPreview",
        "--main-jar", jars[0].name, "--main-class", "com.meshlit.desktop.MainKt",
        "--app-version", "2.0.40", "--vendor", "Sabbir Hassan Imon",
        "--description", "Experimental Meshlit authenticated host client",
        "--mac-package-identifier", "com.meshlit.desktop",
        "--icon", args.app_image / "Contents/Resources/MeshlitPreview.icns",
        "--java-options", "-Dcompose.application.resources.dir=$APPDIR/resources",
        "--java-options", "-Dcompose.application.configure.swing.globals=true",
        "--java-options", "-Dskiko.library.path=$APPDIR")
    app = image_dir / "MeshlitPreview.app"
    plist = app / "Contents/Info.plist"
    metadata = plistlib.loads(plist.read_bytes())
    # Conservative distribution floor; only macOS 15.8.1 is physically qualified.
    metadata["LSMinimumSystemVersion"] = "11.0"
    plist.write_bytes(plistlib.dumps(metadata))
    resources = app / "Contents/Resources"
    notices = resources / "licenses"
    notices.mkdir()
    for source in (repo / "desktopApp/distribution/notices").iterdir():
        shutil.copy2(source, notices / source.name)
    shutil.copy2(repo / "vendored/runanywhere-llama/LICENSE", notices / "LLAMA-LICENSE.txt")
    license_cpp = args.server.parent.parent / "license.cpp"
    native_notices = "\n\n".join(re.findall(r'R"=L=\((.*?)\)=L="', license_cpp.read_text(), re.S))
    if not native_notices:
        raise RuntimeError("Missing generated native dependency licence texts")
    (notices / "LLAMA-DEPENDENCY-LICENSES.txt").write_text(native_notices, encoding="utf-8")
    local_resources = app / "Contents/app/resources"
    (local_resources / "local").mkdir(parents=True, exist_ok=True)
    (local_resources / "models").mkdir(parents=True, exist_ok=True)
    shutil.copy2(args.server, local_resources / "local/llama-server")
    shutil.copy2(args.model, local_resources / "models" / model_manifest["filename"])
    shutil.copy2(repo / "desktopApp/distribution/bundled-model.json", local_resources / "models/bundled-model.json")
    (local_resources / "local/engine.json").write_text(json.dumps({
        "sha256": digest(args.server), "source": "https://github.com/RunanywhereAI/llama.cpp",
        "revision": "4df29be4f4c3673f428170fda944a5b19f743bb8", "backend": "CPU",
        "curl": False, "openssl": False, "metal": False}, indent=2) + "\n", encoding="utf-8")
    for filename in ("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md"):
        shutil.copy2(repo / filename, notices / filename)
    for jar in (app / "Contents/app").glob("*.jar"):
        with zipfile.ZipFile(jar) as archive:
            for index, name in enumerate(archive.namelist()):
                lower = name.lower()
                if lower.startswith("meta-inf/") and any(
                    word in lower for word in ("license", "notice", "copyright")
                ) and not name.endswith("/"):
                    (notices / f"{jar.stem}-{index}.txt").write_bytes(archive.read(name))
    shutil.copy2(repo / "docs/MACOS_PREVIEW_INSTALL.txt", resources / "INSTALL.txt")
    shutil.copy2(args.runtime_provenance, notices / "RUNTIME_SOURCE.json")
    shutil.copy2(repo / "docs/MACOS_PREVIEW_NOTICES.txt", notices / "DESKTOP_NOTICES.txt")
    # Reject absolute developer dependencies before producing downloadable files.
    native_dependencies = []
    for path in app.rglob("*"):
        if path.is_file() and not path.is_symlink():
            kind = subprocess.run(["/usr/bin/file", "-b", str(path)],
                                  capture_output=True, text=True, check=True).stdout
            if "Mach-O" in kind:
                if "x86_64" not in kind:
                    raise RuntimeError(f"Non-Intel native file: {path.relative_to(app)}")
                deps = subprocess.run(["/usr/bin/otool", "-L", str(path)],
                                      capture_output=True, text=True, check=True).stdout
                for line in deps.splitlines()[1:]:
                    dependency = line.strip().split(" (", 1)[0]
                    if dependency.startswith("/") and not dependency.startswith(
                        ("/System/Library/", "/usr/lib/")
                    ):
                        raise RuntimeError(f"External native dependency: {dependency}")
                native_dependencies.append(str(path.relative_to(app)))
    # Modified bundles are explicitly ad-hoc signed for local integrity only.
    # This is not an Apple Developer ID identity or notarization.
    run("/usr/bin/codesign", "--force", "--deep", "--sign", "-", app)
    run("/usr/bin/codesign", "--verify", "--deep", "--strict", app)
    stem = "MeshlitPreview-2.0.40-macos-intel"
    pkg = args.output / (stem + ".pkg")
    run("/usr/bin/pkgbuild", "--component", app, "--install-location", "/Applications",
        "--identifier", "com.meshlit.desktop.preview.pkg", "--version", "2.0.40",
        "--ownership", "recommended", pkg)
    staging = args.scratch / "dmg-root"
    staging.mkdir()
    shutil.copytree(app, staging / app.name, symlinks=True)
    (staging / "Applications").symlink_to("/Applications", target_is_directory=True)
    shutil.copy2(repo / "docs/MACOS_PREVIEW_INSTALL.txt", staging / "INSTALL.txt")
    dmg = args.output / (stem + ".dmg")
    run("/usr/bin/hdiutil", "create", "-srcfolder", staging, "-volname",
        "Meshlit Intel Preview", "-format", "UDZO", dmg)
    run("/usr/bin/hdiutil", "verify", dmg)
    shutil.copy2(repo / "docs/MACOS_PREVIEW_INSTALL.txt", args.output / "INSTALL.txt")
    shutil.copy2(args.runtime_provenance, args.output / "RUNTIME_SOURCE.json")
    shutil.copy2(args.runtime_source, args.output / args.runtime_source.name)
    report = {"architecture": "x86_64", "appVersion": "2.0.40",
              "nativeFilesChecked": native_dependencies,
              "externalNativeDependencies": [], "developerIdSigned": False,
              "notarized": False, "appSignature": "ad-hoc", "installerScripts": False,
              "runtime": provenance, "bundledModel": model_manifest,
              "applicationSourceCommit": provenance["applicationSourceCommit"]}
    (args.output / "PACKAGING.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    assets = sorted(p for p in args.output.iterdir() if p.is_file())
    (args.output / "SHA256SUMS").write_text(
        "".join(f"{digest(p)}  {p.name}\n" for p in assets), encoding="utf-8")
    print(f"Packaged {dmg.name} and {pkg.name}; still requires payload runtime verification.")


if __name__ == "__main__":
    main()
