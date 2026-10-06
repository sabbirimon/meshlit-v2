# Meshlit implementation plan

Previous upstream records are preserved in `docs/history/PLAN-before-2026-10-06.md`; their old build/device claims are historical, not validation of this checkout.
Updated: 2026-10-06. Follow `AGENTS.md` and `AGENT_BUILD.md` before implementation.
`PROGRESS.md` is the evidence ledger; `BUILD.md` contains reproducible commands.
User requirements outrank historical restrictions on layer/pipeline sharding.

## Priority 1 — reliable Android foundation
- Finish both-flavor compilation, unit tests, lint and emulator UI inspection.
- Validate GGUF download/resume/import/hash, actual SDK load/unload and generation on physical phones.
- Exercise Settings search/Basic/Advanced, persisted dynamic colors, log export and lifecycle cancellation.
- Validate encrypted OpenClaw gateway profile and opt-in phone model endpoint.
- Prove accessibility actions in an ordinary installed APK, not only instrumentation.
- Expose clear model/runtime/permission/error state; no successful stub results.

## Priority 2 — real phone layer execution
- Enroll at least two physical Android workers with TLS pins and explicit grants.
- Compare deterministic output to a single-device baseline with the same model SHA and runtime revision.
- Record nonzero layer/tensor allocation on each worker and coordinator memory.
- Demonstrate a model larger than the coordinator's feasible local RAM budget.
- Measure throughput, first-token latency, network volume, thermal throttling and battery on heterogeneous phones.
- Stop/kill worker, coordinator and service; verify bounded failure and cancellation.
- Separate layer/pipeline placement from tensor parallelism and independent job distribution.

## Priority 3 — hive durability and autonomy
- Replicate a bounded task/compact-memory journal on every enrolled phone; NAS optional.
- Use selected holders for heavy model files/checkpoints with hashes, quotas and leases.
- Add fencing epochs, authenticated heartbeats, quorum-aware election and idempotent task claims.
- Recover by replay first. Portable KV recovery requires genuine native export/import and compatibility checks.
- Integrate OpenClaw paired operator + node WebSocket sessions and persisted device identities.
- Expose delegated Android controls through an authenticated gateway bridge with revocation and audit.
- Add model-appropriate structured function calling; do not advertise it until actual model tests pass.
- Validate emergency stop, target scope and protected/password surfaces. Accessibility grants do not confer root.

## Priority 4 — devices, transports and platforms
- Improve QR/manual/BLE enrollment UX, distinguish discovery from authorization and compute capability.
- Validate implemented TLS web/API enrollment and local groups; implement verified SSH enrollment adapters; routers/switches/firewalls may contribute connectivity only.
- Optional rootless/Linux VM/desktop and terminal remain off until requested or delegated.
- Keep portable core DTOs and adapters for Linux, Windows, macOS and HarmonyOS.
- Probe and benchmark optional CUDA, HIP, SYCL, Metal, Vulkan, OpenCL, MUSA and CANN backends.
- Reserve adapters for Huawei/HarmonyOS, Hygon and other Chinese vendors without fabricated support badges.

## Acceptance and changes
Each milestone must name changed files, build/test commands, results and remaining device checks.
Do not estimate a whole-project percentage from source presence. Track acceptance cases instead.
Update this plan when the user changes scope; preserve previous evidence in `PROGRESS.md`.

## Later — user guide and replayable tutorial
Requested by the user after core/model/backend work. Add a first-run walkthrough and
Help → Replay tutorial. Cover permissions and denial recovery, model download/import/
startup load, chat/stop, QR pairing, layer workers versus discovery, phone groups,
OpenClaw gateway/node/provider boundaries, code workspace and log export. Keep a skip
option and resume points. Use simulated data only in a clearly marked demo mode; never
fake model tokens, worker health, system permission grants or successful execution.

## Current task-manager acceptance
- Validate manual task creation/edit/subtasks/search/filter/sort/bulk completion.
- Verify real human/agent job start, queue, Stop, results and explicit retry.
- Later: recurrence, dependencies, assignments, distributed claims and replicas.

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
