# Intel Mac offline desktop preview

Updated 2026-10-10. Desktop-only addition to build40. Android's bundled SmolLM2
starter, prompt and inference path remain unchanged. The earlier host-only Mac
preview is superseded by the installer payload described here.

The desktop bundle includes the publisher's Qwen2.5 1.5B Instruct Q4_K_M GGUF,
1,117,320,736 bytes, Apache-2.0. The immutable revision, source URL and full
SHA-256 are in `desktopApp/distribution/bundled-model.json`. `LocalEngine` checks
the full hash before loading the built-in file. Imported files get structural
admission, not a claim that their publisher or compatibility has been verified.

The unmodified pinned standalone llama.cpp CPU server is built with static
libraries, system Accelerate, no Metal, CURL, OpenSSL, OpenMP or native AVX flags.
It uses only system dynamic dependencies. It starts only after a user Load, on
127.0.0.1 with a fresh 256-bit bearer in a private 0600 temporary file. One worker,
4,096 context tokens and up to four CPU threads are configured. The owner-only
process stops on Unload, window disposal, JVM shutdown, source switch or Stop
during generation. Stale load callbacks cannot attach a superseded model.

Load takes at most two minutes. Chat uses the existing bounded SSE transport and
reports actual server completion tokens divided by total request time. This is
end-to-end throughput, not a decode-only benchmark. A large chat/context can fail;
there is no automatic summarization, cloud fallback or fake response. This model
is useful for compact text/chat/code tasks but is not a frontier-quality model,
vision model or autonomous tool executor.

Settings -> Local offline -> Select GGUF selects a local file up to 32 GiB without
copying it; model/architecture/RAM compatibility is established at load time by the
engine. Download GGUFs through the publisher's browser page first. Direct in-app
search/downloads, a durable model registry, idle unloading, agents, RAG and GPU
adapters are the separate milestones in [DESKTOP_STUDIO_PLAN.md](DESKTOP_STUDIO_PLAN.md).
Switching local/remote clears session history. Appearance remains persisted.

## Build and package

1. Verify the desktop model against its pinned manifest and obtain a checked
   portable Eclipse Temurin 21 Intel JDK and matching full upstream sources.
2. Build Kotlin desktop classes with `:desktopApp:test :desktopApp:createDistributable`.
   Only the application input/resources are reused; the packager replaces the
   Homebrew-dependent runtime and launcher with Temurin's jlink/jpackage output.
3. Run `python3 scripts/build-desktop-llama.py --output <scratch>/llama-cpu --jobs 2`.
4. Run `python3 scripts/package-macos-preview.py --app-image <MeshlitPreview.app>
   --jdk-home <portable-jdk>/Contents/Home --output <new-delivery-dir>
   --scratch <new-scratch-dir> --runtime-provenance <runtime-provenance.json>
   --runtime-source <matching-source.tar.gz> --model <pinned-model.gguf>
   --server <llama-cpu>/bin/llama-server` (one command).

The packager preserves licences, validates all native files are Intel and rejects
absolute dynamic dependencies outside system library paths. It creates an ad-hoc
integrity signature only. No Developer ID or notarization is asserted. It creates
a read-only compressed DMG with an Applications shortcut and a script-free PKG
containing the exact same app, plus SHA256SUMS, runtime/source provenance and
matching runtime source download. Only installers/assets go to GitHub Releases;
weights, JDKs and binaries must remain out of Git.

Run `MeshlitPreview --local-check <evidence.json>` from each recovered installer
payload with no external JAVA_HOME/PATH. This explicitly generates a real reply,
checks reported tokens, rejects missing local authentication and verifies Unload
closed the listener. Run `--render-check <workspace.png>` for the actual Compose
renderer. These checks do not prove a physical click-through installation on a
clean Mac or qualify other OS/GPU targets. Only macOS 15.8.1 Intel is currently
tested; the configured OS floor is 11.0. Production signing/notarization and a
second clean Mac remain release qualification work.
