# Meshlit build and continuation contract for the Claude Opus agent

## Read this before changing code

The user has explicitly authorized genuine layer/pipeline execution across phones.
Do not ask to retain the old prohibition. Update contradictory instructions in
this checkout after verifying them. The user also requested a redesigned main UI,
working model download/import/load, fully wired main settings, dynamic colors,
heterogeneous device roles, automatic coordinator/worker negotiation and durable
storage/checkpoint recovery. User instructions supersede historical restrictions.

This is an implementation task, not a mockup. Do not fabricate successful model
loads, activations, tokens, GPU/NPU support, VM launches, checkpoint recovery or
completion percentages. A compiler pass is not inference evidence.

Read `AGENTS.md`, `CLAUDE.md`, `PLAN.md`, `REQUESTS.md`, `TODO.md`, relevant
`BUGS.md`/`PROGRESS.md`, `docs/architecture/current-state.md`,
`docs/layer-pipeline-and-recovery.md`, `docs/runtime-and-sandbox.md` and
`docs/runanywhere-browser-and-llama.md`. Plans contain historical claims; inspect
implementation and tests. `REQUESTS.md` R-23 describes mixed-device hive goals.

## Coordinate checkouts

The Opus working tree supplied by the user is:
`/Users/code/AndroidStudioProjects/mllm`.

The Codex implementation is in a separate checkout:
`/Users/code/Documents/Codex/2026-10-06/re/outputs/meshlit`.
Both began at `ff0cd771c12f3daa63cb8fe45e1476245ab7b01b`. Codex has not modified
the Opus tree. Inspect the latest branch, status and diffs before porting. Preserve
unrelated work. Include untracked source files, resources, tests and docs; copying
only `git diff` misses new files. Do not copy build directories, credentials,
model weights, caches, cloned upstream repositories or generated native binaries.
Use the pinned source synchronization and native build scripts instead.

User scope includes implementation, routine fixes and local validation. No push,
merge or release is authorized by this file. Do not send messages to other people
or agents without explicit human authorization.

## Architecture and build

Android Kotlin, Compose, Koin, two flavors (`MeshlitV1`, `MeshlitV2`). Keep versions
from the Gradle catalog and build files. Current code uses Maven RunAnywhere
0.20.12, not whatever API is present in a newer SDK clone. Verify changed SDK calls
against the actual AAR/jar, using javap if necessary.

Use installed JDK 21 and Android SDK. On this Mac:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME=/Users/code/Library/Android/sdk
```

For an isolated build, point `GRADLE_USER_HOME` and `ANDROID_USER_HOME` at writable
workspace directories. If Kotlin daemon files are blocked, use
`-Pkotlin.compiler.execution.strategy=in-process`; do not repeatedly start
competing Gradle builds. Do not downgrade AGP/Kotlin to avoid an unrelated error.

```sh
python3 scripts/sync-optional-sources.py --help
python3 scripts/build-pipeline-native.py --jobs 2
python3 scripts/build-pipeline-native.py --android --abi arm64-v8a \
  --ndk /Users/code/Library/Android/sdk/ndk/28.2.13676358 --jobs 2
# Build x86_64 separately for an emulator; ARM64 binaries cannot run there.
python3 scripts/build-pipeline-native.py --android --abi x86_64 \
  --ndk /Users/code/Library/Android/sdk/ndk/28.2.13676358 --jobs 2

./gradlew :core-inference:testDebugUnitTest :core-mcp:testDebugUnitTest \
  :core-net:testDebugUnitTest :core-sandbox:testDebugUnitTest \
  :app:testMeshlitV1DebugUnitTest :app:testMeshlitV2DebugUnitTest \
  --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
./gradlew :app:assembleMeshlitV1Debug :app:assembleMeshlitV2Debug \
  --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
./gradlew :app:lintMeshlitV1Debug :app:lintMeshlitV2Debug
python3 -m unittest discover -s companions/crawler/tests -v
```

Native binaries are installed PIE executables packaged as
`libmeshlit_pipeline_server.so`/`libmeshlit_pipeline_worker.so` under jniLibs.
`useLegacyPackaging` and native extraction are required for nativeLibraryDir
execution. Preserve SDK native libraries; these executables do not replace them.
Review APK size, Android page alignment, ABI support and startup on an installed
APK. Never claim physical-phone execution from an NDK build alone.

## Current change map

- `ui/modern`: shared chat, model library, monitoring, main settings and logs.
- `chat/ChatController`: local conversation history, streaming and cancellation;
  `ChatInferenceService`: generation lifetime without unauthenticated servers.
- `models/ModelLibrary`: shared model state, queued transfers, multi-file SAF
  imports, deletion/load and persisted interrupted-transfer state.
- `core-inference/models/ModelDownload`: bounded resumable transfer, ETag/Range,
  HTTPS redirect handling, exact-host token forwarding, validation and install.
- `RunAnywhereInferenceEngine`: content identity, registry `local_path`, SDK
  generation options, actual unload and cancellation handling.
- `pipeline/PipelineHost` and `core-inference/pipeline`: installed native processes,
  pinned TLS transport, explicit worker pairing, capability negotiation and
  coordinator-selected inference. Native worker starts only on user request.
- `SettingsDestinations`: one functional navigation/search catalog for main settings.
- `ModernLogsScreen`: severity/source/text filters, redacted preview and filtered
  export through Android's document picker as TXT/JSONL.
- `SettingsRepository`/theme: persisted dynamic-color option, explicit light/dark
  resolution, accent controls and animated palette option.
- Other earlier additions: optional sandbox/VM/terminal/network diagnostics,
  owner-approved Android browser actions and Crawl4AI companion. Consult their
  docs instead of assuming they are production-complete.

## Model requirements: finish and prove

The alleged bundled starter asset is absent. Do not show it as installed. Offer
an explicit verified download. The starter is pinned in `ModelLibrary`:
SmolLM2-360M-Instruct Q8_0, revision
`593b5a2e04c8f3e4ee880263f93e0bd2901ad47f`, SHA-256
`48ab3034d0dd401fbc721eb1df3217902fee7dab9078992d66431f09b7750201`.

Exercise fresh download, resume, ignored ranges, 416, changed validator, malformed
range, redirects, expiring CDN URLs, unknown length, HTML, truncated body, wrong
hash, gated model failure, cancellation, storage exhaustion, process death and
multi-transfer behavior. Tokens must never follow arbitrary CDN redirects.
GGUF header checking is preliminary; native loading must validate the full model.
Never report a download successful until validated atomic installation completes.

Imported/downloaded filenames are not model identity. Keep content SHA identity
and a registry local_path pointing to the actual managed file. Avoid duplicate
weight copies. Test same basename/different content, same size/replaced content,
load/unload/load, unsupported architecture, corrupt tensor data and native OOM.
Explicitly label unsupported split-GGUF/archive/SafeTensors import paths.

## Main UI and settings contract

Both flavors use the modern main shell. Maintain readable Material surfaces,
48dp actions, keyboard-safe composer, useful empty/loading/error states, selection
and code copy, history persistence, streaming/stop and actual loaded model state.
Validate light/dark, Android 12 wallpaper colors, accent presets, older Android
fallback, rotation, large fonts, accessibility and compact/wide layouts.

Every settings row must reach a working screen/backend. No empty handlers or
unexplained clickable placeholders. Basic/Advanced selection is persisted. Search
matches label, description and keywords while respecting the current filter.
Theme settings persist and affect the whole app; no forced-dark nested V2 wrapper.
Model settings share the same repository as chat and notifications.

The old `CategoryScreen` had no-op setting rows; notifications screens were empty.
Main/category entry points now route to working modern destinations. Android
notification controls open system permission/channel settings. Legacy account,
transport-policy and fine-grained performance knobs are not silently wired to
unrelated settings. Audit those modules individually before exposing controls.
Preserve advanced tools via the Tools route. Expand the destination catalog when
adding a real backend, then test navigation/search and persistence.

Logs must export the selected immutable filtered snapshot, with credentials and
sensitive context redacted. Handle picker cancellation, missing provider, write
failure and rotation during export. Do not silently export all logs when a filter
is active. Unredacted model/tool content requires separate explicit user choice.

## Layer sharding and negotiation contract

Local RunAnywhere is the default; no VM or LAN RPC worker on startup. Keep native
GGML RPC on loopback behind the approved TLS tunnel. The pinned RPC interpreter
is experimental; TLS is not a sandbox. Wrong tokens/pins/revisions must fail.

This implementation is native layer offload with remote tensor execution. It is
not tensor parallelism, independent-agent job routing, MoE-only routing or the
legacy activation echo. `LlamaCppPipelineStage` must return unsupported until a
real JNI stage path exists; never pass echoed/zero activations as evidence.

Capability negotiation is deterministic and memory weighted, excludes stale/hot/
unapproved workers and checks matching runtime revision. Current app activation
uses the local coordinator holding the model and manually paired worker offers.
Do not claim full automatic network membership, remote master activation, memory
reservation, distributed leases or transparent failover until implemented.

Required evidence: per-device layer placement, model/KV/compute buffers and native
output; one-worker/baseline versus two-worker fixed-seed agreement; actual Android
ABI execution; two physical phones; a model larger than each phone's safe local
budget; peak RSS/PSS, latency/throughput, thermal load and disconnect/cancellation.
The desktop tiny-model two-worker comparison passed in this checkout. Larger-than-
phone memory and physical-phone proof remain pending.

## Hive nodes and durable recovery contract

**Primary requirement: phone cluster first.** Every enrolled consenting phone
replicates a compact task/session ledger; a memory-bank phone can coordinate it.
Selected phones or optional NAS/server nodes serve heavy artifacts. Do not require
a NAS/server or replicate complete weights to every phone. Implement quotas,
content-hash references, committed acknowledgements, fenced task ownership and
rejoin/recovery tests. Main network/pairing must distinguish app, native companion,
SSH, web and Bluetooth availability; consult the enrollment section of the design.


Phones, PCs, servers, NAS, routers, switches and firewalls can be enrolled with
independent roles: coordinator, compute worker, storage, tool executor, networking
or monitor. A product label is not a compute capability. Mobile SoC/GPU/NPU support
must be probed using compiled backends. Use ABI-specific binaries and measured
RAM/VRAM budgets. External GPU inference belongs to the host with its actual
backend; it does not automatically turn a phone's GPU into CUDA hardware.

Implement the storage/recovery milestone in `docs/layer-pipeline-and-recovery.md`:
versioned authenticated checkpoint store, encrypted bounded blobs, model/runtime
identity, sampling/session/token metadata, checksums, atomic publication,
retention, storage quorum, lease fencing and idempotent tool effects.
First prove committed checkpoint-and-replay into a new native context. Direct
portable KV snapshots need real native export/import and compatibility checks.
Survival of one/two crashes needs the corresponding failure-domain replication
and an actual kill/recovery test. Conversation JSON alone is not KV recovery.

## Finish criteria and reporting

Keep a validation table with exact commands, outcomes and devices. Name unrun
checks. Record failing legacy tests separately only after proving they are
pre-existing; never suppress meaningful regressions. Compile both flavors, run
contract tests and relevant lint, inspect the installed UI and perform real model
inference. Never give an invented percent complete. Deliver a focused diff and
an honest implementation/evidence/remaining-work report.

## Autonomous agents alongside humans

The app is intended for future autonomous AI agents/SI alongside human users.
UI is a control/monitor surface; backend typed interfaces are the authority.
`PipelineMcpTools` exposes status, model inventory, planning/start/stop and worker
activation with persisted user delegation. No GUI automation is required for
these operations. Expand this to model management and recovery with task IDs,
cancellation, deadlines, observable events, audit, scoped credentials and
idempotency. Persist scopes once; do not repeatedly prompt within an existing
scope. Root/elevation and new data destinations still require their own grants.
Untrusted retrieved/model content cannot expand those grants. Expose actual
availability and failures to agents instead of returning placeholder success.

## Core comparison with the Opus tree

Read-only review on 2026-10-06 found both checkouts at the same base SHA; the Opus
tree has additional untracked feature CLAUDE files and an older copied Codex tree.
`core-orchestration` contains no executable orchestration implementation at this
base; the newer app pipeline does not gain a production scheduler from its name.
`core-agent-memory` includes an in-memory store, not phone-replicated durable
memory. `TrainingResumeService` persists local training tokens; it is not inference
KV-cache recovery. Federation handoff DTOs describe signatures/consent but require
verified server wiring. `core-ssh` currently contains configuration types, not a
functional host-enrollment client. BLE discovery and `PeerRepository` transport
controls already exist: reuse them and verify real discovery rather than building
a duplicate scanner. Discovery does not establish compute trust. Reconcile the
Opus core-orchestration CLAUDE ban with this user's explicit layer authorization
when porting; do not copy that obsolete ban into the new implementation.

## Multiplatform and vendor extension work

Read `docs/platform-and-backend-contract.md`. Android is first; preserve portable
protocols and extract pure policies/contracts behind host adapters for Linux,
Windows, macOS and HarmonyOS NEXT. CPU is the tested default. CUDA/HIP/SYCL/Metal/
Vulkan/OpenCL/MUSA/CANN have explicit optional build selectors; vendor hardware
and proprietary redistribution must be validated independently. Include Chinese
technology adapters without claiming support merely from a device/vendor label.
HarmonyOS NEXT requires a native OHOS/ArkUI host; an Android APK is not that port.
Keep upgrades, rollback, schema migrations, quotas and agent capability reporting
in the continuation plan. Avoid adding mandatory server/container infrastructure
to the phone-first cluster.

## Tracking and OpenClaw additions
Read `PLAN.md`, `BUILD.md`, `PROGRESS.md` and `docs/openclaw-integration.md` first.
Keep their implemented/tested/planned states accurate after every validation run.
The user authorizes built-in OpenClaw integration, local phone-model pairing and
optional Android autonomy. Android system grants and actual capability checks still apply.
Latest adapters are in `app/openclaw` and `core-inference/openclaw`; paired-node
protocol/custom command interoperability and physical device controls require evidence.
Do not present the text-only provider as verified function-calling support.

### Typed backend priority
The user asks to finish agent-facing backend operations before further UI expansion.
See `docs/typed-agent-backend.md`. Use stable request IDs, observable job results,
validated model IDs/URI grants, saved scopes and explicit uncertain-outcome recovery.
Do not let an agent mutate its own delegation. Keep replicated recovery claims false
until the phone ledger/fencing/native checkpoint work is real and demonstrated.

## Existing Android project reuse review

See `docs/old-android-reuse-review.md`. The old checkout was read-only.
Files/SAF and audited Termux integration now have modern settings routes.
Existing help/manual/tour are retained as foundations for the later tutorial.
Do not copy stale compatibility or generic OpenClaw scaffolding claims.

## Structured maps and task/device commands

Read [FEATURE_MAP.md](FEATURE_MAP.md), `docs/feature-map.json`,
`docs/task-manager.md` and `docs/web-api-and-devices.md`.
Manual tasks and actual operation jobs are distinct; web clients need per-device
access plus saved delegation. Groups are local selections, not compute membership.
Run `python3 scripts/validate-feature-map.py` after changing public commands or paths.

## Real data and bundled model requirement (user instruction)
Never ship fake responses, fake devices, fake transfer progress or fake monitoring
values. Tests may isolate failure contracts, but production unavailable features
must report unavailable/blocked. The APK must contain the pinned real starter
model: run `python3 scripts/prepare-bundled-model.py` before Gradle builds.
Read `app/src/main/assets/models/README.md`. RunAnywhere SDK downloads must
resolve and validate a real local artifact; completion progress alone is not proof.
Keep live Android generation evidence separate from unit/compile success.

Read `docs/device-aware-model-runtime.md` and `docs/hugging-face-integration.md` for device policy, native context/KV options and real Hub/hosted API paths. Never claim requested SDK registry context as effective native context.

## Latest continuation priorities (2026-10-06)

Keep genuine phone layer sharding/model execution and committed recovery as the
main product priority. Nodes also contribute storage, tools, capture, sensors,
actuation, preprocessing, DSP, routing and monitoring according to real evidence.
ESP32/Arduino/IoT membership does not imply transformer execution.
Read `docs/online-power-peripherals-and-configuration.md`,
`docs/declarative-federation-roadmap.md` and `docs/media-iot-and-radio-nodes.md`.
These record offline/online selection, free/public/gated model distinctions,
actual pricing provenance, power readings, hardware/vendor backend limits, open
source SSH, configuration transfer, media sources and owner-paired radio carriers.
Keep unsupported adapters visibly unavailable; never invent devices, samples,
throughput, tokens, costs, account entitlements or recovery success.

## Scenario routing and media continuation (2026-10-06)

Read `docs/model-router-and-media.md`. Scenario recipes, sequential chains/comparisons,
chat text attachments and explicit online vision/image/speech/video adapters are
wired in source. Validate real execution; paid provider media outputs and generic
music/sound/on-device diffusion/video remain separate gates. Do not substitute
text event counts for native tokens. Keep phone layer execution/recovery primary.

## Custom models and training continuation (2026-10-06)

Read [docs/local-model-behavior-and-training.md](docs/local-model-behavior-and-training.md). Custom local prompt behavior is default-off;
compatible custom weights are imported through Models. No toggle changes learned
refusals or hosted-provider rules. Settings → Fine-tuning drives the optional
Soup 0.75.0 POSIX host companion via pinned SSH, with real job/status/log/cancel
contracts. Android synthetic gradients are removed; phone autograd reports
unavailable. Actual Soup training, adapter quality/evaluation and GGUF deployment
remain acceptance gates. Keep phone inference layer sharding/recovery primary.

For the owner-connected Samsung, follow `docs/physical-device-test-guide.md`;
Mac USB is detected but actual ADB transport is pending. Never treat emulator
results or skipped physical checks as phone evidence.
