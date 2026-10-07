# Continuation: physical-device tests deferred

The owner requested continuing remaining areas 2–8 while deferring physical-device
testing. Deferral does not establish hardware acceptance. Existing publication
authorization persists; no Play submission, force push or external posts.

## Build order and evidence

| Area | This implementation | Remaining acceptance / work |
|---|---|---|
| 2. Genuine sharding | Repeatable `scripts/verify-pipeline-host.py` compares real CPU and two-RPC-worker generation and checks native layer/buffer logs | Oversized model, independent failure domains, Android/physical memory/thermal evidence deferred; remote coordinator activation not added |
| 3. Recovery | Fixed 3/5-member signed Paxos journal, encrypted Android persistence, HTTP client, manual Recovery panel, durable promises/acceptance/commit, ownership epochs, monotonic offsets, model identity and immutable completion | Automatic TaskBoard/AgentController/model replay, checkpoint blob/key replication, membership reconfiguration, background replica service and independent-device qualification remain open |
| 4. Gateway | Human-enrolled encrypted MCP/A2A upstream routes, bounded authenticated JSON-response discovery/calls, tool allowlists, A2A task-session ownership, stop/revoke checks | Rust Linux runtime qualification, OAuth, streaming/SSE, broader provider/tool/media entitlement tests and production deployments remain open |
| 5. Cyber lab | VM-only gates preserved; original bounded ELF header and immutable SQLite schema metadata adapters added to existing tools/package lifecycle | Third-party Red Team dynamic suites, advanced forensic recovery, full PCAPNG, guest image qualification and root/non-root phone acceptance remain open/deferred |
| 6. Integrations | Human-controlled Terraform saved-plan companion; exact executable/workspace/plan hash, private saved plans, explicit apply hash, interruption marked uncertain | Real cloud accounts, full provider administration, integrated cloud UI deployment, Ansible/Pulumi, live OpenClaw/browser/Soup acceptance remain open |
| 7. Future nodes | Original Python host companion: actual OS/CPU/storage probe, bounded content-addressed staging/chunk reads, authenticated loopback API and offline I/Q FFT | Companion deployment/enrollment UI, storage-holder selection, Linux inference packaging, live media/radio/MCU firmware, other OS apps and GPU/NPU generation remain open |
| 8. Release | Updated setup/feature/evidence docs, repeatable tests, APK builds and lint; explicit remaining-acceptance notes | Production signing, SDK telemetry opt-out, legacy UI polish, independent security review, store gates and hardware tests remain open |

No feature percentage or completed project status is inferred from code or test counts.
Validation results belong in PROGRESS after the checks finish.

## Manual replica enrollment

Settings → Checkpoints and recovery → Replicated recovery journal. Each installation
creates an independent encrypted EC identity. Copy its **public** key to the owner;
tokens are secrets and have a separate Copy action. Configure exactly 3 or 5 members
with unique IDs and their independently verified public keys. Membership is immutable
once durable state exists. Losing/deleting the app key or state is a replica loss;
do not recreate the same member identity with empty promises.

The listener binds loopback, is disabled in Play Review and stops when leaving the
screen. Peers need verified private SSH tunnels, or HTTPS with a matching trusted
certificate and independently approved `sha256/…` certificate pin. TLS pinning does
not disable ordinary certificate-chain validation. No public unauthenticated listener.
For example, configuration fields (replace every placeholder with real values):

```json
{
  "group": {"id":"owner-cluster", "members":[
    {"id":"phone_a","publicKey":"BASE64_EC_PUBLIC_KEY_A"},
    {"id":"phone_b","publicKey":"BASE64_EC_PUBLIC_KEY_B"},
    {"id":"phone_c","publicKey":"BASE64_EC_PUBLIC_KEY_C"}
  ]},
  "localId":"phone_a", "port":18894,
  "endpoints":[
    {"member":"phone_a","url":"http://127.0.0.1:18894/replica","token":"LOCAL_REPLICA_SECRET_AT_LEAST_32_CHARACTERS"},
    {"member":"phone_b","url":"http://127.0.0.1:28894/replica","token":"VERIFIED_B_REPLICA_SECRET_AT_LEAST_32_CHARACTERS"},
    {"member":"phone_c","url":"http://127.0.0.1:38894/replica","token":"VERIFIED_C_REPLICA_SECRET_AT_LEAST_32_CHARACTERS"}
  ]
}
```

Acquire with a stable request ID and task ID:

```json
{"requestId":"owned-task-acquire-1","taskId":"owned-task","owner":"phone_a","epoch":1,"action":"ACQUIRE"}
```

SNAPSHOT/COMPLETE require that owner/epoch, nondecreasing offset and verified lower-case
model SHA-256. `replayText` is at most 8192 UTF-8 bytes. A checkpoint SHA is a reference,
not evidence that the blob exists on replicas. ACQUIRE advances the epoch exactly once;
it does not start a model, stop the old runtime or grant shell/cloud permissions.
Completed tasks cannot be reopened. Ownership protects this journal; existing legacy
worker/tool transports are not magically fenced by recording an epoch.

Limits: 128 entries, 2 MiB serialized state, fixed membership, 5-second peer calls,
60-second append and eight contention rounds. No unsafe compaction/reset or automatic
membership change. Plan a new namespace only with an explicit migration, not by clearing
promises. The journal can contain sensitive replay text; Android encrypts storage and
peer transport must be private. Secrets/state are excluded from settings export/audit.

Each slot uses prepare/accept with a unique proposer ballot. A proposer adopts the
highest accepted value from its prepare quorum before proposing a new entry. EC-signed
acceptances bind membership, ballot and the complete hash-chained value. Success also
requires a durable learner quorum; an interrupted outcome requires retry with the same
request ID. Reading returns the longest majority-certified prefix visible in the read
quorum; a chosen value with lost commit delivery may need explicit append retry to
finish learning. It is not a background, linearizable live task-status service.
This is a **crash-fault** protocol for honest approved replicas, not Byzantine consensus.
Storage promises must survive process restart; corrupt state fails closed.

Reference: [Paxos Made Simple](https://lamport.azurewebsites.net/pubs/paxos-simple.pdf).
Tests exercise protocol faults; they do not constitute independent consensus/security
review. Automatic inference recovery must wait for that review and model/blob readiness.

## Remote agent gateway routes

Gateway → Remote routes JSON → Save (stops listener) → Start → Refresh routes.
Routes have `id`, `endpoint`, `token`, `protocol` (`MCP` or `A2A`), `humanEnabled` (default true), `agentEnabled` (default false)
and MCP `allowedTools`. IDs are short namespace labels, cannot contain `__` and cannot end in `_`;
this makes the federation delimiter unambiguous even for underscore-prefixed tools.
Tokens are encrypted; saved agent delegation is separate from human invocation. Exact HTTPS URLs
or loopback private tunnels only; redirects, URL credentials/query/fragment and
unbounded replies are rejected. CA trust remains enforced. No automatic OAuth login.

MCP initializes a real session, negotiates a supported version, sends initialized,
lists at most four pages, and federates only named approved tools as
`remote_ROUTE__TOOL`. Remote descriptions/schemas/results are untrusted data.
A2A verifies an exact-origin 0.3 JSON-RPC card and adds send/get/cancel tools. Read/cancel
only accepts IDs created in that route session. Stop/rekey/edit clears discovery and
ownership. Cancelling the local HTTP request does not prove a remote task stopped;
inspect its operator-owned host before retry. Saved local tool scopes remain independent.

Protocol boundaries: [MCP transport](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports),
[A2A 0.3](https://a2a-protocol.org/v0.3.0/specification/).
The pinned upstream [Rust companion](../companions/agentgateway/README.md) remains distinct;
JSON connector tests are not proof of its runtime or every LLM/provider feature.

## Host deployment and node companions

See `companions/deployment/README.md` and `companions/node/README.md`. They are optional
operator-installed source companions, not APK-embedded interpreters or completed cloud,
radio, multi-platform app, or Linux compute deployments. Use existing pinned SSH/manual
commands for deliberate host installation and invocation. Do not execute downloads
silently or expose their loopback endpoints directly to the internet.

## Additional platform and control work

Meshlit is intended for dynamic AI deployments across hosts and clusters; phones are
one supported surface. See [PLATFORM_ADAPTER_PLAN.md](PLATFORM_ADAPTER_PLAN.md) for
chip/SDK, memory, cross-platform, direct/kernel and HFT-inspired networking plans.
HyperL is isolated on `codex/hyperl-experimental` and is not merged here.

The Operations dashboard saves a stop latch, per-function human/agent restrictions,
inventory ceiling (1–10,000), separate bounded agent capacity policy and native worker
limit (2–8). Existing identities survive lowering capacity. These are management
limits, not demonstrated simultaneous throughput. Tracked requests are cancelled;
local teardown has explicit completed/failed/timeout reports. Human resume is blocked
while local cleanup is running. Remote accepted actions and independent services
remain uncertain and retain their own stop controls.

Gateway unified route JSON supports LLM/MCP/A2A, separate agent permission, priority
or recent observed latency selection, cooldown and bounded concurrency. Selection
is once per invocation; failure never silently repeats a possibly accepted action.
`route:ID` aliases appear in the model list for delegated LLM routes. Local agent tools
can list/execute delegated routes; configuration and human resume stay human-only.
Extension protocols fail unavailable until implemented. Discovery is not execution
qualification. OAuth/SSE/streaming, spend enforcement and Linux Rust runtime remain
open. Cloud/media/agent prompt/recovery calls have tracked admission/cancellation.

Recovery can explicitly publish bounded `JOB_METADATA` from an actual durable job,
excluding prompts/results/paths/credentials. This content does not fabricate a model
SHA or inference offset. The `INFERENCE` snapshot/complete path still requires model
identity and monotonic offsets. Neither content automatically resumes a runtime.

Accelerator nodes can be human-enrolled by exact HTTPS/private-loopback `/node`
endpoint and encrypted token. Manual probes show actual installed SDK/OS observations
and unavailable states. No GPU/NPU inference, native transport or privileged module
is advertised as qualified by a version probe.

Protocol routing uses the enrolled route metadata, never an A2A-like tool-name prefix.
Tests cover valid MCP tool names that resemble A2A and namespace alias rejection.
