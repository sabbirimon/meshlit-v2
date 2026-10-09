# Meshlit implementation and qualification plan
<!-- meshlit-document-tracking:start -->
Tracking reconciled 2026-10-10: [Plan](PLAN.md) · [Progress](PROGRESS.md) · [Document status](docs/DOCUMENTATION_STATUS.md).
Scope: cross-platform tracking; feature and device gates remain separate. Phase labels in older sections retain their original scope.
<!-- meshlit-document-tracking:end -->

Updated 2026-10-10. [Current progress](PROGRESS.md), [sequential gates](BUILD_MILESTONES.md), [requests](REQUESTS.md), [open work](TODO.md) and [documentation index](docs/DOCUMENTATION_STATUS.md) share the current tracking model. Older plans are retained under docs/history.

## Goals and channels

Build a capable private AI workspace for desktop/server and Android, with real local inference, permitted tools, owner-approved heterogeneous devices and genuine model-layer execution. Production candidates and Experimental features stay separate. The Android Core Candidate is a restricted candidate, not a production certification. Intel Studio is Experimental; a dashboard or library dependency never establishes a working backend.

No single project-wide phase number or completion percentage is used. The workstreams below have separate implementation and qualification gates. Historical phase numbers identify their original design snapshots only.

| Workstream | Current state | Next gate |
| --- | --- | --- |
| Local core engine | Intel bundled Qwen/CPU baseline and verified AVX2; lazy load, context/thread/batch/KV controls, memory admission and idle unload; both recovered installer launchers pass | Clean-machine installation, long-run lifecycle/leak/crash/quality cases; wider model and hardware qualification |
| Shared sharding | Portable GGUF and CPU worker admission shared by Android/desktop; capacity-bounded integer/manual layer proportions and weak-worker exclusion pass contracts | Desktop live-worker RPC controller, paired independent devices, measured allocation/latency/memory, oversized-model and peer-loss tests |
| Desktop management | Seven categories, distinct Basic/Advanced menus, bounded global/settings/chat/model/node search, real local monitoring, human shell, SSH/status listener and HyperL CPU | HF download/cancel/storage failures, actual process Stop, interactive SSH; finish pending category backends |
| Android reliability | Both current Full V1/V2 APK assemblies/lints pass; earlier Samsung build-37 local/UI checks are historical | Install and test current build on Samsung, then Xiaomi/Windows categories; preserve data and human permissions |
| Governed agents and fleet | Android typed controls/grants/audit and optional gateways are references; desktop shared agent dispatch remains pending | One common authority/controller layer, durable redacted audit, scopes/budgets/revocation/Stop, then MCP/A2A/hooks and controlled automation |
| Platform and release | Intel DMG/PKG recovered-payload qualification; source/review pushed; all eight public asset digests verified | Experimental release published with matching digests; Developer ID/notarization and independent OS/device acceptance precede production |

## Ordered implementation queue

1. Intel release publication is verified; finish model/library operational acceptance. Retain one working build and remove only known generated duplicates.
2. Add genuine desktop worker enrollment/execution to the shared planner. Distinguish remote model API/job routing from pooled-memory layer execution. Use pinned identities, exact revision/model hash, fresh capability offers and bounded failure/Stop.
3. Build the common desktop agent permission and audit foundation before unattended jobs. Human root/credential/policy edits stay human-controlled; model responses and received messages do not grant authority.
4. Port agent/MCP/A2A/hooks, optional memory/personality/voice and document workspaces through real controllers. Ship unsupported controls as pending, not successful stubs.
5. Implement qualified OS firewall/capture, container/VM/Kubernetes, cloud vault/billing, measured power/costs and bounded repair/rollback in their own categories. Each needs real native/runtime/account evidence.
6. Qualify independent Windows/Linux/Apple Silicon and HarmonyOS NEXT/iOS paths. NEXT has no DevEco/device proof; GPU/NPU capability requires an actual backend, driver and device, not a label.

## Core distributed acceptance

Require at least two independent approved workers, positive model-layer contribution, exact model/revision agreement, actual native allocation and output, and a single-device comparison. Measure first-token latency, authoritative native tokens, transfer bytes, resident/KV memory, thermal and battery where supported. Internet/P2P controls are separate from native model transport. Do not sum nominal RAM or device counts into a throughput estimate.

Replicated journal acknowledgements, fencing/quorum, idempotent claims and portable compatible KV recovery must precede automatic coordinator failover. Existing local encrypted checkpoints and retries do not establish fleet self-healing. Training needs a real licensed dataset, actual host job and evaluated adapter; a file import is not successful training.

## Update rule

For every change record request/feature IDs, source, commands and failures, model/device/backend, actual observations, remaining gates and artifacts/digests. Update the appropriate workstream, not every historical phase. Run scripts/update-doc-tracking.py --check and scripts/validate-feature-map.py before committing documentation. All 41 desktop/server requested areas remain individually tracked in docs/DESKTOP_FEATURE_TRACKER.md and docs/desktop-features.json.
