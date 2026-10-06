# Sequential implementation and acceptance ledger

Updated 2026-10-06. User requested these milestones in this order. Implementation
and acceptance evidence are separate; dependencies do not establish completion.
No overall percentage is inferred.

| Order | Milestone | Current implementation | Remaining acceptance |
| --- | --- | --- | --- |
| 1 | Phone layer execution | Packaged CPU workers/coordinator, authenticated pinned TLS, real context/KV-aware admission and split weights; corrected Android TLS identity and CPU arguments | Two or more physical workers, oversized model, measured layer allocations/outputs/memory, peer loss and thermal/background behavior |
| 2 | Cluster recovery | Synchronous encrypted job/task commits; actual bounded encrypted native CPU KV checkpoints; human and scoped typed agent management | Replicated task/session journal, persistent peer acknowledgements, quorum, fenced coordinator failover, portable/RPC KV compatibility and phone fault tests |
| 3 | Model correctness | Removed chunk counting; unreliable SDK usage stays null, standalone native counters; native context/path checks; actual cache precision/save/restore test harness | Correctly measured restored-prefix reuse and latest APK execution; vendor acceleration remains unsupported pending backend tests |
| 4 | Integration tests | Explicit model/router and native Android instrumentation; opt-in JSch/OpenSSH host harness | Operator-provided/working SSH host, OpenClaw gateway, physical Android permissions/autonomy, real guest VM/VNC, authenticated providers and compatible media runtimes |
| 5 | Fine-tuning | Actual Soup host command/log/job/cancel contracts and Android unavailable states | Compatible host/Python/Soup installation, real licensed dataset, train/evaluate adapter, merge/export and real imported generation |
| 6 | Future adapters | Existing capability interfaces and truthful unavailable states | Working CCTV/audio/radio/MCU hardware connectors, vendor backend builds, other OS clients and fenced cross-cluster deployment |
| 7 | Polish/release | Shared modern screens, new recovery and Cloud/vault screens, corrected lint errors and fatal error gate | Interactive tutorial, full chat export, remaining legacy redesign/full IDE, physical UX and release checks; reference UI passes both builds/128 app tests per flavor/zero lint errors |

## Continue without fabricating missing hardware

- Samsung is visible in macOS USB inventory but not in `adb devices`. The current
  ADB USB interface fails with e00002be. Resolve this in a normal Android Studio/Mac
  terminal or supply an approved wireless endpoint. Do not wipe phone data or
  silently authorize debugging. See `docs/physical-device-test-guide.md`.
- The disposable local OpenSSH host started but its preauthentication sandbox was
  denied by the enclosing macOS environment. `python3 scripts/prove-ssh-host.py`
  from a normal terminal performs a pinned preflight and real JSch test, generates
  disposable keys in a temporary directory and cleans up its server. No system
  SSH configuration is modified. No successful SSH evidence is recorded yet.
- Training/provider/hardware tests must use actual supplied runtimes/credentials,
  not a dummy successful adapter. Preserve unavailable/blocked results.

## Next cluster recovery build

Implement the replicated journal before automatic failover: explicit consented
membership; monotonic term/sequence/hash chain; persistent acknowledgements;
majority checkpoint commit; bounded replicated conversation/task replay state;
content-addressed heavy artifacts on selected holders; per-checkpoint encryption
keys wrapped for approved recovery members and revocation; fenced leases; stale-owner
and duplicate-execution rejection. Use three durable replicas for one failure and
five for two, with explicit degraded/no-quorum behavior. Test restart/partition/
rejoin before advertising automatic continuation. Do not turn planner election
or a locally saved cache into a consensus claim.

Read `PROGRESS.md` for the latest completed checks and `docs/native-checkpoints.md`
for actual API, storage, identity migration and compatibility behavior.

## Added cloud/vault workstream

User requested dedicated AWS/Azure/DigitalOcean/GCP/OpenRouter/custom Cloud,
resource/cost dashboard, credential/environment/API management, and separate
human/agent settings. Implemented read adapters, encrypted vault references,
request budgets and dashboard provenance are documented in
`docs/cloud-and-credential-management.md`. Full vendor service coverage, deployment
and OAuth login/refresh are future adapters; real account tests need local
operator credentials. Do not use this workstream to claim phone sharding proof.

## Latest validated continuation

Read PROGRESS.md: final debug APKs, unit checks, full fatal lint and seven emulator
integration checks pass. Native local checkpoint prompt reuse is measured, SDK
usage remains unknown, public catalog/vault/WebView contracts have real evidence.
Physical oversized phone execution, replicated journal/fencing/failover and real
browser-model tasks remain outstanding. New cloud/files/guide/browser work does
not close those gates. No physical device is visible to ADB in this environment.
