# Gateway and Linux Security Lab implementation — 2026-10-07

This continuation implements an initial working subset of the approved full plan.
Do not describe the entire cyber suite or upstream Rust gateway as embedded.

Settings routes: Agent Gateway, Commands and instructions, Security Lab, Lab packages.

## Built-in protocol endpoints

`GatewayHost` wires `EmbeddedGateway` to real durable AgentBackend jobs, models and
assessment/package repositories. Listener is authenticated loopback only, manually
started while the Gateway screen is open. Leaving screen stops it and requests
cancellation of owned active jobs. No foreground service or remote public listener.
Play Review refuses inbound start. No first-use acceptance bypass.

MCP: stateless JSON-response Streamable HTTP subset, initialize/ping/tools/list/
tools/call and initialized notification. GET SSE is explicitly 405; unsupported
methods fail. Versions 2025-03-26, 2025-06-18, 2025-11-25. Owner token is shared by
configured clients; per-client credentials, OAuth/JWT and server notifications remain
future work. Tools: typed command submit/job status/cancel, delegated assessment
list/run and allowlisted pip package actions. Normal saved command delegation still
applies. Lab tools additionally require a running SSH-ready isolated VM.

A2A: advertised 0.3 JSON-RPC model-task subset, Agent Card, message/send,
tasks/get and tasks/cancel. No task streaming, push, arbitrary modalities or external
agent federation. Restart does not replay tasks. Gateway-owned IDs stay session-local;
encrypted underlying job journal survives but old tasks are not exposed after restart.

LLM: /v1/models and /v1/chat/completions buffered text for the owner-selected model.
Installed local or enabled cloud profile IDs are routed through the real backend.
Actual streaming/tools/Responses compatibility remains a further milestone.

All endpoints require Bearer token, reject browser Origin headers, enforce request
bounds and bounded concurrent handlers. Rotating token stops old listener.
Content policy uses bounded literal input/output rules. Empty filtered/custom rules
provide no moderation classifier. Minimal skips optional rules, never auth/permissions.
Hosted provider policy and model learned behavior are unchanged.

## Manual commands and instructions

Runtime commands use existing RuntimeHost: APP, explicit per-command human root,
PRoot/Bubblewrap and VM configuration. Pinned SSH host selection uses SshConnections.
Input is never executed until Run. Stop propagates cancellation; completed side effects
and remote descendants are not guaranteed rolled back. Output is screen-local.
Natural-language instructions submit a real model-generation human job; they do not
automatically become shell commands or an autonomous tool-calling loop.

## Security Lab and packages

Lab only runs with VM_SSH and SSH_READY, for rooted/non-rooted phones. PRoot, APP,
root and root-chroot alone do not establish isolation. Guest must already have SSH,
Python, timeout/base64 and installed companion scripts. No Kali/Parrot image or
native QEMU installer is bundled. VM startup/resource policy remains independently
configured, and phone native-runtime/guest acceptance remains open.

Assessment records are encrypted, digest/path/tool/expiry scoped with independent
agent permission. Implemented adapters: original APK inventory, classic-PCAP summary,
optional installed tshark. Prior requested tools remain catalogued as planned:
Androguard, Quark, Objection/Frida, Drozer, Metasploit, Havoc reports, Magisk,
Atomic Red Team, Autopsy/Sleuth Kit, qualified dynamic sandbox and PCAPdroid.

Package operations run only in the ready VM using an explicitly provisioned guest
marker. Manual pip/apt/apk/dnf/pacman list/install/uninstall; managers must be installed
and compatible. pip has a dedicated non-root venv. Distro root operations require
explicit human guest-root approval; no automatic sudo. Repo names or SHA-checked
file/web artifacts. Phone SAF transfer copies to a unique guest /tmp artifact and
computes a digest. Dependency trust is inherited from guest repositories/indexes.

Agent package operations additionally require saved VM activation plus a one-hour
owner allowlist of pip names. Root distro operations/arbitrary file or web installs
are not delegated. Uninstall leaves possible configs/dependencies/evidence/staged files.
Guest networking defaults restricted, with a human-only outbound opt-in configured while stopped and applied at next VM start. Current QEMU snapshots discard guest changes on stop. Persistence and one-click
Alpine/Kali/Parrot/Ubuntu/RHEL provisioning remain open.

## Upstream and remaining full-plan work

Official agentgateway v1.6.0 Linux assets pinned in companions/agentgateway; fetching
verifies digests and does not execute. Host runtime tests pending (no Docker daemon,
Mac x86_64 without matching reviewed Darwin binary). Android Rust embedding requires
Bionic cross-compilation and native packaging, not a Linux binary copy.
Remote MCP/OAuth/session/SSE federation, remote A2A delegation, streaming LLM tool loop,
Responses, enforceable cross-client cost budgets, moderation-service adapters and
physical-device/provider proof remain acceptance milestones.
