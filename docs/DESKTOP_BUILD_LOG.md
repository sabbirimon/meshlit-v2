# Desktop/server build update ledger

Updated 2026-10-10. Source implementation, tests, native/package execution and
hardware/production acceptance are separate columns. Append corrections and
actual results; do not report a checklist or successful submission as execution.

[Requested feature tracker](DESKTOP_FEATURE_TRACKER.md) · [Old/new Android inventory](ANDROID_DESKTOP_PARITY.md) ·
[Earlier phased studio plan](DESKTOP_STUDIO_PLAN.md) · [Project evidence](../PROGRESS.md).

## Revision history

| Date / source | Change | Observed evidence | Remaining gates |
| --- | --- | --- | --- |
| 2026-10-09 / `a30081f` and earlier build 40 | Thin Compose host client/CLI, shared visual contracts | Historical host/TCP/offscreen records in PROGRESS; no desktop local engine in that old UI | This is the old screenshot's limited settings surface; no feature parity implied |
| 2026-10-10 / `1c1572f` | Pinned desktop-only Qwen2.5 1.5B GGUF, CPU llama-server lifecycle and Mac packaging source | Full original model size/SHA verified and CPU binary built; source tests recorded earlier | Actual packaged generation/load/unload and successful DMG/PKG payload tests pending |
| 2026-10-10 / `1922916` | Provider presets, scoped endpoint keys, Open WebUI /api contract | Provider contract tests; exact /api and /v1 routes | Actual operator-owned provider instances/billing not verified |
| 2026-10-10 / working tree after `1922916` | Grouped desktop menus, model registry/HF discovery, nodes, monitor/task manager, crypto, JNI HyperL adapter, bidirectional scoped SSH | Portable Temurin build: desktopApp 16 tests, desktop-ssh 4 tests, createDistributable successful before subsequent terminal/policy additions | New app package/native monitor/HyperL/LLM acceptance and all other OS/device checks pending |
| 2026-10-10 / current working tree | Human terminal, Ghostty SSH handoff, local response policies, exhaustive trackers | Terminal real exit/output/cancel/limit and Ghostty pin/argv cases added; latest full rerun pending | Ghostty interactive remote login, embedded PTY and agent bridge pending |

## Failures and corrections retained

- First packaging attempt used `pkgbuild --component`; its bundle analysis rejected
  the supplied app. Packaging source now uses an app-only `--root` payload.
  No successful new PKG/DMG should be claimed until rebuilding and inspecting both.
- Initial offline check assumed `/models` required auth. This llama.cpp build
  exposes model metadata on loopback. Qualification now tests the actual protected
  generation route without its key, then authorised generation and unload.
- Response-policy test exposed a real source bug: shared `ChatTurn` intentionally
  accepts only user/assistant roles. Local system instructions had incorrectly
  been passed through that type. The desktop serializer now receives a separate
  bounded system prompt; shared Android contracts are unchanged. Serializer contracts and real pinned local generation now pass.
- Ghostty 1.3.1 is installed and its version was read. That is runtime detection,
  not a successful SSH session or an embedded-terminal test.
- This documentation comparison uses old menu `98faaf0` (43 destinations), current
  Android (49) and first structured catalog `966015b`. The pre-catalog `ff0cd771`
  baseline has no feature-map JSON; it must not be claimed as a generated catalog.

## Current host evidence

The current desktop classes and application image build successfully with portable
Temurin. Targeted JVM executions: desktopApp 30, desktop-engine 12 (shared GGUF and
placement source), desktop-ssh 4, shared-workspace 4, core-inference 274: **324**,
zero failures/errors/skips on Intel Mac. These are not 324 independent hardware
checks. Core inference lint has zero errors; Android full flavor lints/builds are
running after the shared planner and API-24/voice permission corrections.

- Native Intel HyperL JNI: all twelve bounded CPU recipes plus precise reduction.
- Real OSHI samples 2.1 seconds apart: CPU/RAM and process rows; no stop/energy proof.
- Real HF HTTPS search and pinned file metadata: no new weight download/gated proof.
- Real starter generation, missing generation auth rejection and process unload.
- Three real native cache-key cases f16/q8_0/q4_0 with context 1,024 readback.
  q4_0 produced a poor arithmetic answer; functional text/token success is not
  quality success. It is excluded from normal settings; f16 remains default.
- Current offscreen Compose management render inspected: seven compact categories,
  scoped functions and pending ports distinguished.
- Both native Intel variants built from the same unmodified pinned source.
  Auto dispatch selected AVX2/FMA/F16C on this CPU/OS. A real default-context reply
  reported 82 tokens at 16.047 end-to-end tokens/sec, load 8.788 seconds, missing
  generation authentication rejected and Unload listener closed. This ran while
  Android lint was active; it is not a controlled comparison against the earlier
  baseline. Matched trials, installer payload checks and publication remain pending.

The default system Python crawler run failed for missing FastAPI; the existing
isolated crawler test environment then passed all **9** API/policy cases. No crawler
code changed. Kotlin daemon's sandbox timestamp-path error used the supported
compiler fallback and the desktop build succeeded; subsequent builds select the
in-process Kotlin strategy rather than writing outside the workspace.

## Current acceptance queue

- [x] Current desktop + shared JVM contracts and scoped SSH tests pass.
- [x] Current management components render with compact grouped navigation.
- [x] Actual host pinned offline answer reports native tokens and unload stops listener.
- [x] Actual host HyperL JNI runs all bounded native CPU recipes and precise sums; recovered-package check pending.
- [x] Monitor samples actual host CPU/RAM/processes; no fabricated gauges or costs.
- [ ] HF actual metadata/verified transfer and cancellation acceptance recorded.
- [ ] New DMG and PKG created, recovered and payload-tested; native hashes and provenance match.
- [ ] Intel Mac install/open accepted; unsigned/ad-hoc/notarisation status explicit.
- [ ] Source/review and release publication URLs/checksums verified on GitHub.
- [ ] Windows, Linux, independent phones, server daemon and other platform proof separately recorded.

## Template for each next build

```text
Date/time and build/channel:
Source commit + dirty-tree status:
Feature IDs changed:
Commands and results (include failures):
Runtime/device/OS/ABI:
Observed outputs, timings and token provenance:
Permission/Stop/revocation checks:
Missing or unqualified capabilities:
Artifact names, SHA-256, signing status and payload checks:
GitHub review/release URL and uploaded digest verification:
```

A production candidate needs its own immutable admitted-feature set and actual
qualification. These desktop changes are Experimental and do not promote the
old Android candidate or another platform to production readiness.

## Validation retry and generated-file cleanup

The first combined Android assembly/lint run stopped progressing in app analysis;
the dedicated Gradle daemons were stopped and checks retried with one worker,
parallel tasks disabled, a 4 GiB build heap and the in-process Kotlin compiler.
A generated 3,321,165,255-byte heap dump and the known failed installer scratch
(application input/image/runtime only, about 1.3 GiB) were removed. Primary
models, source/JDK/native build inputs and actual evidence were retained. Heap
dumps are now ignored and must never be committed or published.

The first Studio package attempt was rejected by the native dependency gate:
the supplied HyperL dylib's own install name was an absolute development path.
The gate was preserved. A reproducible Intel/JNI builder now compiles the same
unmodified CPU/JNI sources with an `@rpath` install name; the new library and
installer payload still require their actual runtime checks.

## Android/shared regression gate — current Studio source

The serial validation completes successfully in **13m34s**: both Full V1/V2 debug
APK assemblies and both flavor lints, core-inference lint, and MCP/sandbox/network/
inference unit tasks. Both app flavors report **0 fatal/errors, 374 warnings**;
core-inference reports **0 fatal/errors, 7 warnings**. Warnings are not erased.
Together with desktop/workspace/engine/SSH suites, current valid JVM result files
contain **521** executions, zero failures/errors/skips on this Mac; shared-source
re-executions/cached unchanged suites are not distinct hardware evidence. Crawler
API/policy checks separately pass 9. No new phone installation or APK release is
claimed. Build daemons were stopped before the matched CPU timing trial.

The relocatable unmodified HyperL library then passes all twelve real native CPU
recipes and precise reduction. See `evidence/hyperl-relocatable-host-check.json`.
