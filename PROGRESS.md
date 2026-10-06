# Meshlit progress and evidence

Updated 2026-10-06. Baseline `ff0cd771c12f3daa63cb8fe45e1476245ab7b01b`.
Checkout: `/Users/code/Documents/Codex/2026-10-06/re/outputs/meshlit`.
Branch: `codex/meshlit-ui-pipeline-openclaw`. No push or release authorized.
Historical upstream records in `docs/history/` are not current test evidence.

`FEATURE_MAP.md` and `docs/feature-map.json` inventory **50 feature areas,
28 durable command operations and 33 modules**. Feature counts do not establish
an overall completion percentage. Source implementation, automated contracts,
emulator observations, host runtime proof and physical-phone proof are separate.

## Implemented in source

| Workstream | Source behavior | Current boundary |
| --- | --- | --- |
| App UI/settings | Shared chat/history, Models, Monitor, Network, Tasks, Logs, Settings/Tools; Basic/Advanced search; saved appearance/dynamic colors | Every retained legacy tool is not fully redesigned; final device UX checks remain |
| Model management | Actual pinned 135M starter, SDK/verified HTTPS downloads, SHA/atomic install, resumable transfers, multi-SAF import, remembered startup/load/unload | Real bundled emulator load/generate/unload now passes; physical phones pending |
| Model tuning/device identification | Real Android OS/ABI/RAM/storage/thermal/hardware readings, resource admission, GGUF metadata, native CPU context/KV/thread controls | SDK-managed context/GPU controls stay explicit/unknown; vendor plugins unavailable |
| Offline/online chat | Saved conversation options; encrypted OpenAI/compatible/Claude/Gemini profiles, real discovery/requests and usage-based cost estimate | Paid calls need operator credentials; online replies currently buffered |
| HF models/pricing | Revision-pinned Hub artifacts, encrypted access token, live router offers with source/time, search/free filter and provider-pinned price selection | Public catalog does not grant free/gated model inference or account entitlements |
| Human/agent control | 28 typed operations, encrypted durable local jobs, idempotent admission, scopes, bounded execution/status/cancel/retry; actual tools inventory | Fake Healthy AgentRuntimeStub removed; external-agent interoperability pending |
| Tasks/workspace | Durable tasks/subtasks, priority/tags/dates/search/bulk changes; bounded offline CodeMirror files and SHA write preconditions | No distributed claims/recurrence, full compiler/debugger or task execution from metadata |
| Pairing/web/API | Native worker QR/manual verification; optional TLS enrollment/approval/scopes/revocation; approved control groups | Control membership does not authorize native compute; physical LAN checks pending |
| Native layer execution | Actual CPU coordinator/workers, fresh offers, memory-weighted layer placement and authenticated pinned TLS | Desktop two-worker proof exists; physical-phone/oversized-model proof pending |
| Power/costs | Actual battery/thermal/network sensors, session estimates/export and saved transfer/new-load policies | Whole-device estimate; unknown sensors remain unknown; physical power validation pending |
| USB/OTG/storage | Actual descriptors/classes/permissions, mounted storage and SAF grants | No universal serial/GPU/camera driver; hotplug tests pending |
| SSH | JSch 2.28.7, mandatory host-key pin, encrypted credentials, bounded exec/cancel and per-host agent opt-in | Live server tests, PTY/SFTP and automatic deployment pending |
| Firewall | Validated persisted port/address policy wired to new legacy/control/RPC connections | Private IPv4 inbound app listeners; no OS-wide/outbound firewall |
| Portable settings | Strict non-secret schema-v1 export/import, preview, local preflight and partial-apply reporting | No trust, secrets, permissions, delegation or distributed transaction transfer |
| OpenClaw/autonomy | Gateway client, scoped signed Android-node adapter, optional loopback model provider, opt-in allowlisted Accessibility actions | Live gateway and physical consent/stop/revoke tests pending |
| Media/IoT | Main media settings; bounded image import/phone-camera thumbnail; existing voice wrappers; Pi/MCU/sensor/radio categories/role claims | Compatible media models/backends required; CCTV/UVC/MCU/radio adapters planned |
| Terminal/VM/crawler/browser | Optional bounded terminal/runtime/QEMU/SSH/VNC paths; scoped Crawl4AI companion; approved browser actions | Real binaries/guest/browser/physical runtime tests still required |
| Future platforms/recovery | Capability interfaces and detailed vendor/OS, journal/lease/checkpoint and internet federation plans | Linux/Windows/macOS/HarmonyOS apps, portable KV and automatic cluster failover not implemented |

## Latest completed validation

- `../meshlit-validation/media-configuration-validation.log`: **BUILD SUCCESSFUL in 2m 49s**.
  Both app flavors and V1 instrumentation APK assembled.
- Core inference: **250 tests**; core MCP: **103**; firewall: **17**; SSH: **6**;
  training: **27**; app: **115 per flavor**. All report zero failures/errors/skips. Machine-readable
  result: `../meshlit-validation/test-results-2026-10-06.json`.
- Earlier sandbox **8**, network **40** and crawler **9** checks passed. They are
  earlier checks, not a new full-module run after every current source addition.
- Map validation: **52 features, 28 operations, 33 modules**, referenced paths checked.
- Desktop native RPC: `../meshlit-validation/native-rpc/proof.json` records two
  real worker processes, nonzero allocations and 32 deterministic tokens matching
  the local baseline. `physical_phone_proof=false`, `oversized_model_proof=false`.
- Actual public HF router fetch: **136 models, 317 provider offers, 206 priced
  offers, zero explicit free offers** in this captured catalog. No fake free row.
  Evidence: `../meshlit-validation/hf-public-catalog-evidence.json`.
- Real bundled asset: **105,454,432 bytes**, SHA-256
  `2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d`.
  APK contents were hashed, and the updated x86_64 APK installed on the API35
  emulator. Binary model/APKs are ignored by Git.

## Resolved startup regression and emulator proof

The earlier bundled test timed out at **180 seconds**. Buffered bounded GGUF
metadata parsing now resolves the observed startup delay: tokenizer-sized arrays
no longer require many tiny storage operations. Three regression tests cover
later metadata, truncated scalars and oversized arrays. Both flavors and the
instrumentation APK rebuilt (**BUILD SUCCESSFUL in 5m 23s**).

The same real bundled model now passes load → generate → unload in **8.723 seconds**
on the Mac's API35 x86_64 emulator. SHA-256 matched the pinned manifest; native
logs identify the actual 0.13B model and context 2048. The generated text was
`Hello, hello, hello.`. Evidence: `../meshlit-validation/bundled-model-emulator-proof.json`,
`bundled-model-emulator-pass.log` and `bundled-model-native-evidence.log` in that
same validation directory. Earlier failures remain saved; this is emulator proof,
not a physical-phone, model-quality or oversized-model result.

Native logs reported **6 generated tokens**, while the existing wrapper reported
**3 streamed text events as tokens**. Token/rate accounting remains a known engine
bug; see `docs/remaining-engine-bugs.md`. Current UI inspection confirms optional
permission setup, navigation and main Settings render; screenshots are under
`../meshlit-validation`. This is not acceptance of every legacy screen or feature.

Full environment/media lint finished (**BUILD SUCCESSFUL in 17m 47s**) with
**51 errors, 330 warnings and 17 hints per flavor**, `abortOnError=false`.
`docs/lint-findings.md` and `../meshlit-validation/lint-summary-2026-10-06.json`
record its scope/results. The small buffered-parser follow-up came afterward;
no new full lint result is claimed for that follow-up. Lint is not clean.

## Remaining implementation and acceptance

1. Preserve the resolved startup behavior, correct stream-event/token accounting
   and prove physical-phone generation/stop/unload/reboot. Verify public/gated
   download, multi-import, interruption, storage,
   memory and power-policy failures on named phones.
2. Execute a genuinely oversized model across at least two physical phones:
   allocations, token correctness, latency, thermal/power, cancellation, loss and
   reconnect evidence. Desktop workers cannot substitute for this gate.
3. Build replicated compact task/session journals, fenced coordinator epochs,
   task leases, selected heavy-file holders and committed checkpoint replay.
   Portable KV export/import needs native compatibility checks. Automatic recovery
   after one/two node crashes must be demonstrated before it is advertised.
4. Validate current SSH, web/TLS/QR, OpenClaw, accessibility, permissions and
   optional native terminal/VM paths against real external hosts/devices.
5. Implement verified Linux/Pi companions, MCU telemetry/actuation firmware,
   CCTV/UVC/live audio and two-way radio adapters. Record drivers, actual hardware,
   scopes, rate/RTT, loss, revocation and stop results. RX-only is not TX support.
6. Implement actual accelerator adapters and platform apps one target at a time.
   SoC names and future settings reserves do not prove CUDA/NPU/vendor execution.
7. Implement cross-cluster task handoff and desired-state deployment after the
   real resource, durable lease and network-partition contracts exist.
8. Complete UI inspection and legacy tool modernization, richer settings indexing,
   chat export, tutorial replay and IDE toolchains; fix lint and build release gates.

No production fake responses/devices/transfers/metrics/costs are acceptable.
Unsupported paths must report unavailable rather than fabricate success.

## Other Android checkout and continuation

`/Users/code/AndroidStudioProjects/mllm` source/Git were not edited. Only the
requested `MESHLIT_BUG_AUDIT.md`, `MESHLIT_FIX_GUIDE.md` and
`MESHLIT_UI_REDESIGN_GUIDE.md` were written there; copies are in `../meshlit-handoff`.
`docs/old-android-reuse-review.md` records reuse decisions.

Read `AGENT_BUILD.md`, `docs/layer-pipeline-and-recovery.md`,
`docs/online-power-peripherals-and-configuration.md`,
`docs/declarative-federation-roadmap.md` and `docs/media-iot-and-radio-nodes.md`.
Record date/commit, changed contracts, exact checks and evidence paths after work.
Never convert a UI control, DTO, unit pass or host proof into physical-device proof.

## Router/media follow-up in progress

Scenario rules, single/chain/compare modes, chat recipe selection and scoped MCP
route calls are wired. Text file attachments and Media Studio add actual image
upload/vision, image/speech generation and persistent async video references.
Current inference suite: **247 tests, zero failures/errors/skips**, including six
routing and five media-protocol failure checks. Latest app/build checks are
running. Real two-distinct-local-model router instrumentation is queued; paid
media outputs are unverified without user credentials. These tests do not imply
physical phone sharding or media-model quality.

## Latest router, media, behavior and training checks

Both flavors and V1 instrumentation assembled with the latest settings/router/media
source (`behavior-training-validation.log`: BUILD SUCCESSFUL in 2m 22s). Follow-up
training availability/gradient checks rebuilt both APKs successfully in 29s/10s;
`training-final-tests.log` passed 27 core-training checks. Current totals are 250
inference, 27 training and 115 per app flavor, zero failures/errors/skips. Earlier
MCP/firewall/SSH totals remain 103/17/6. The feature map has 52 referenced features.

The updated API35 emulator passed **three real model tests in 13.823s**: configured
TinyStories → bundled SmolLM2 chain, actual bundled load/generate/unload, and actual
imported model execution. Evidence: `../meshlit-validation/router-and-bundled-emulator.log`,
`router-real-model-outputs.log` and `router-and-bundled-emulator-proof.json`.
This is one-device sequential multi-model routing, not phone layer sharding.

Custom local model instructions are saved encrypted and default off. The optional
Soup 0.75.0 SSH host companion/UI provide actual-process job supervision/status/logs
and acknowledged cancellation. Ten Python companion contract tests passed.
The legacy synthetic-gradient/benchmark path is disabled; fake loss/peer deltas
and silent missing-desktop-gradient success are removed. Android training remains
unavailable. The local Soup diagnostic reports the package is absent; **no real
Soup training or trained-quality result exists**. Read
`docs/local-model-behavior-and-training.md` and `companions/training/README.md`.
Online paid vision/image/speech/video outputs and physical SAF behavior still
need operator/device acceptance. Their offline protocol checks are not live proof.

## Samsung physical test preparation

Owner authorized testing a connected Samsung. USB discovery identifies Samsung
and an ADB interface, but ADB 37.0.1 lists only the emulator. Its Mac log reports
`Unable to create an interface plug-in (e00002be)`. Owner restart/authorization or
wireless pairing is pending. No physical-phone tests are claimed yet. Test scope,
preservation rules and exact model evidence gates are in
`docs/physical-device-test-guide.md`. The updated APKs are in `../meshlit-install`.
