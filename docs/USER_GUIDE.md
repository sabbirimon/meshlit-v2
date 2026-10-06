# Meshlit full app guide and configuration tutorial

Updated 2026-10-07. This guide is also packaged offline in Settings → Guide and tutorial. Reading progress is not execution evidence.

## Start here: what Meshlit does

Meshlit is a phone-first private AI workspace. Run a supported local model on one device; manage files, chats and tasks; explicitly connect providers and owner-approved devices when needed. Android is the current application target. A model catalog, device role claim or installed library does not establish usable inference or training.

Use the drawer to reach Models, Monitoring, Devices and clusters, Task manager, Code workspace, Cloud and credentials, Guide and tutorial, Settings and retained Tools. Settings has Basic/Advanced filters and a search field. Search matches every entered word across names, descriptions and keywords. Advanced exposes optional runtime, forwarding and automation controls.

Implementation status matters. Desktop layer workers have real execution evidence. Oversized-model execution across physical phones, replicated task/session failover and portable distributed KV recovery remain acceptance/implementation gates. This guide explains actual controls and labels future work explicitly. It contains no simulated devices, costs or performance charts.

## First-run walkthrough

1. Review optional setup permissions. Grant camera only for camera/QR work, microphone for voice, nearby/location permissions for supported discovery, notifications for ongoing work and Storage Access Framework grants for selected files/folders. Accessibility is a separate explicit opt-in. You can decline optional permissions and request them later.

2. Open Device, Power and Acceleration. Check the reported OS, ABI, memory, available storage and runtime availability. Start with a short context and smaller quantized model on constrained hardware. Unknown hardware metrics remain unknown.

3. Open Models and use the bundled SmolLM2 135M Instruct Q4_K_M starter. The pinned artifact is 105,454,432 bytes (about 101 MiB), not an empty download placeholder. Select Load, generate a short prompt, stop if necessary and unload. Confirm the real model/runtime before enabling remembered boot loading.

4. Start a new chat, choose its model and generation options, then send a short task. Review outputs. A small starter model is useful for installation checks, not a guarantee of strong coding, agent planning or vision quality.

5. Open Appearance to choose comfortable fonts/text size and colors. Then read the pairing and cloud chapters before enabling external communications or delegation. Tutorial checkboxes mark lessons read; they do not certify that a feature passed on your device.

## Local models, downloads and import

Models is the dedicated management section. Bundled means the real artifact is packaged; Installed means a validated local file exists. Loaded means the active runtime accepted the model. Do not treat download progress alone as installation or generation proof.

Use the RunAnywhere download path for compatible SDK artifacts, verified HTTPS downloads for selected direct artifacts, or multi-file import through the Android document picker. HTTPS downloads use validation and atomic installation; expected hashes and pinned Hub revisions give stronger provenance than a mutable filename.

For Hugging Face, select the repository/file/revision and use a token only when your access requires it. A public catalog can be browsed without entitlement to gated weights or hosted inference. Login, paid account status, inference API credentials and model-license access are separate concerns. Never paste tokens into chat or untrusted URLs.

Device admission considers usable RAM, storage, OS/ABI and policy. Do not add together nominal phone RAM and assume all of it is available to weights. The model also needs graph/workspace and KV memory. If loading fails, inspect real logs, file size/hash, supported architecture/quantization, memory budget and backend availability before retrying.

Select a startup model only after a successful load/generate/unload check. Remembered startup loading should report actual status and failure. Importing safetensors, PT/PTH or ONNX as a file does not make every format executable in the GGUF/llama backend; conversion and backend compatibility are separate.

## Context, quantization, KV and acceleration

Weight quantization belongs to the model artifact. Selecting a smaller context does not quantize its weights. Native-local context and key-cache precision are independent controls. Native CPU supports the packaged options; reload after changing runtime settings. Q8/Q4 key cache is different from model Q4_K_M weight quantization.

Context consumes memory and changes how much history/input can fit. Keep response token limits within the loaded context and the device plan. A larger context can reduce the memory left for weights. Use actual native-reported context rather than assuming an SDK preference was applied.

RunAnywhere SDK-managed controls that cannot be verified stay explicit/unknown. Streamed text chunks are not tokens. SDK usage currently stays null when its terminal counters lack reliable provenance. Standalone native results use actual reported prompt/generated counts and timings; reused prompt tokens use timings.cache_n, not final slot occupancy.

Acceleration settings report installed backend capability. A vendor name, GPU descriptor or future configuration reserve does not prove CUDA, Metal, ROCm, Vulkan, OpenCL, QNN, CANN or NPU execution. Current phone pipeline/native checkpoint acceptance is CPU-based. Unsupported adapters must remain unavailable.

## Chat, routing and online models

Chats retain conversation history and selected options. Use local/offline models, a deliberately selected online profile or a configured scenario route. A local failure does not silently trigger a paid cloud request. Consumer chat subscriptions are different from provider API accounts.

Online providers support existing protocol adapters such as OpenAI/compatible, Anthropic and Gemini. Save API credentials encrypted, configure the actual endpoint/model and discover supported models when the provider offers discovery. Select whether the profile allows agents; do not assume every model accepts temperature or all modalities.

Online profiles can reference an API vault environment and variable name. The environment must match the endpoint origin and remain unexpired. A reference overrides a legacy saved key. Agent requests need both profile and environment approval; changing the API endpoint cannot redirect that bound secret.

Model router recipes can select by scenario, run sequential model chains or compare answers. Review the actual steps, model availability, instructions and agent policy. Sequential chaining/job distribution is not splitting transformer layers. Cloud usage-based estimates require reported tokens and known rates; missing data is unknown.

Use attachment/media options appropriate to the selected backend. Text file input, vision, image generation, speech or video are separate adapters; attaching a file is not proof that the model can perceive or generate its modality. Full general chat export remains a follow-up, not a hidden completed control.

The compact chat layout centers the welcome message and anchors a rounded composer above the keyboard. Welcome text and the secondary caption hide while typing. The plus menu opens text files, photo/camera input, media generation and chat options. Reply Copy and Share use the clipboard and a human-initiated Android chooser. Generated media has Save and Share controls. Photo input remains a separate vision workflow; a text model does not acquire vision capability from the button.

## Files, AI assets and ZIP / unzip

Files keeps the existing granted-storage browser and adds AI file inspection plus ZIP tools. Browse/copy/move/share/delete only within app/user-authorized storage. Android document providers control permission, available space and remote quotas. Removing a grant can invalidate a stored URI.

The AI file toolkit recognizes workflows for GGUF weights; safetensors/bin/PT/PTH/ONNX; tokenizer JSON/vocab/SentencePiece; LoRA adapters; JSONL/CSV/Parquet datasets; Markdown/PDF documents; source/configuration files; images, audio and video. Binary files are handled as files; they are not executed or rendered as text. Text preview is limited to 16 KiB and explicitly truncated.

To create ZIP: select multiple files, set the input byte limit, choose a new output ZIP document and start. To extract: select a ZIP, choose a writable output folder, set the expanded byte limit and extract into the new uniquely named folder. Keep the screen open; leaving it cancels its current foreground operation.

ZIP uses 64 KiB streaming buffers and real byte/file counters. Limits include 10,000 entries, depth 16 and a configurable 1–128 GiB input/expanded budget. Extraction rejects absolute/drive/traversal/ambiguous paths and duplicates, creates ordinary files and avoids overwrite. CRC errors, cancellation and provider failures stop the operation; partial outputs are removed where possible and cleanup failures are shown.

Encrypted ZIP, RAR, 7z, multi-volume archives and restoration of links/Unix permissions are unsupported. A very large ZIP64/provider combination still needs physical-device acceptance. Unpack model bundles before importing compatible weights through Models; archive extraction does not authorize scripts or install a runtime.

## Phone clusters, mixed devices and pairing

Phones are the primary compute target. Other nodes may contribute compute, storage, tools, routing, monitoring, sensors, actuation or media/preprocessing according to actual installed capability. Raspberry Pi/MCU/ESP32/Arduino participation does not imply transformer execution. A router/switch/firewall descriptor is not an inference backend.

Use Network and pairing for discovery, manual/QR enrollment, fingerprint/token verification and device groups. Independently verify identity before approval. Choose minimal scopes/roles. Enrolled web/API/SSH devices and native layer workers are distinct memberships; one approval does not automatically grant the other.

Native worker traffic uses authenticated pinned TLS, with private native sockets. The corrected Android TLS identity changes the fingerprint; previously paired workers/control endpoints need independent verification and explicit reapproval. The persisted listener firewall must permit the actual port; default denial is not a handshake success.

Capability offers are freshness/compatibility/thermal/memory checked. Placement budgets include weights, context-dependent KV and workspace margins. Election/placement remain advisory until fenced consensus exists. Do not call the current planner automatic master failover.

Physical acceptance requires at least two compatible phones, a model larger than one phone can hold, observed layer allocations, correct tokens, memory/latency/thermal readings and cancellation/loss/reconnect results. Desktop workers and a single emulator cannot replace that evidence.

## Recovery and memory holders

Native CPU checkpoint workflow: load native-local with a real model and selected context/key-cache precision; generate; save the completed cache in Checkpoints and recovery; stop/reload the identical model/settings; select Restore. Confirm actual native save/restore token/byte counts.

Snapshots are AES-256-GCM encrypted on this app installation, synchronously indexed after atomic blob writing and bounded to 16 snapshots, 128 MiB each and 256 MiB total. Model and executable hashes, context and cache precision must match. Corruption and incompatibility fail closed. Plaintext staging is temporary and removed.

Restored KV is not conversation history or task completion. It can reuse compatible prompt prefixes. Copying the ciphertext to another phone cannot decrypt it there. Distributed recipient key wrapping, durable artifact holders and portable/RPC KV compatibility remain future protocol work.

Task/session replication, majority commits, fenced ownership, leases, coordinator failover and crash recovery are not implemented by local snapshots. The planned design replicates compact journals across consenting phones and keeps large model/checkpoint objects on selected holders. Three durable replicas are needed for a one-failure majority design; five for two. Actual network-partition and no-quorum behavior must be tested.

## Tasks, agents and human controls

Task manager supports planning, task/subtask organization, priorities/tags/dates, real job links and bulk state changes. Metadata changes do not execute a task. Typed jobs expose queued/running/succeeded/failed/cancelled/interrupted phases and stable request IDs; restart does not silently repeat uncertain external actions.

Human and agent control are separate. Configure saved delegation per scope, then per-provider/host/device/environment controls where applicable. Credential/policy edits remain human-only. Root remains human-only; VM activation and Android autonomy require their own saved opt-ins.

Agents should call typed commands and inspect observable results rather than depend on UI coordinates. A successful job result must identify what happened; unsupported features return unavailable/blocked states. Revocation is checked at supported boundaries; cancellation cannot undo completed remote actions.

OpenClaw can connect to an explicitly configured gateway and optionally use the loopback phone-model provider. Pairing, signed node capabilities, permissions and Accessibility app allowlists are separate. Live gateway and physical consent/stop/revoke acceptance remain necessary.

In Browser, load a compatible on-device model, open an HTTPS site, enter a task and expand Autonomy settings. Enable this origin, choose Follow same-origin links and/or Fill ordinary text fields, select 1–20 actions and Run autonomously. Sessions stop after 120 seconds, Stop, backgrounding, permission revocation, page changes or invalid model JSON. Agents additionally need Allow delegated agents for the site and saved global/enrolled-device BROWSER scopes. BROWSER_STATUS, BROWSER_AUTONOMOUS_RUN and BROWSER_STOP are typed commands. Autonomous navigation follows normal links without site click handlers; action buttons/form submissions, login/CAPTCHA/MFA, payments and destructive actions require human review. Ordinary text filling does not fire input/change events, so complex widgets may need manual steps. This operates Meshlit’s visible WebView; external Chrome/Samsung Internet can use the existing Android Accessibility tools: in OpenClaw and autonomy add an actually detected browser to app scope, save it, explicitly enable delegation and grant Accessibility in Android settings. Agents then use android_control open/snapshot/click/type/back. This generic UI path needs physical testing and is separate from the bounded WebView DOM loop. The starter is not a proven browser planner. Do not confuse source/DOM tests with a successful real-site model task.

## Cloud: resources, costs and provider configuration

Cloud and credentials has Dashboard, Providers, Credentials and Agent settings. First create a named CLOUD/API environment with the documented variable names; configure a vendor/profile; enable intended human actions; explicitly refresh an identity/resource response. Missing accounts produce no fake connected resource cards.

AWS adapters cover STS identity, EC2 instance inventory and month-to-date UnblendedCost through yesterday UTC. Temporary ASIA credentials need a session token. Cost Explorer needs metered-read consent and the supported us-east-1 profile; first-day current-month data is unavailable. Azure covers subscriptions/resources and ActualCost query with an existing Entra token. DigitalOcean covers account/Droplets/GPU Droplets and balance. GCP covers projects/Compute and billing association; spend needs a future billing-export adapter.

OpenRouter offers actual key usage/limits and a public model catalog. Public catalog access can be unauthenticated; it does not grant model inference entitlement. Custom/other providers can define read-only function IDs mapped to fixed public HTTPS paths with bearer authentication. Generic arbitrary HTTP methods, private metadata targets and auto-followed redirects are unavailable.

The dashboard derives values only from vendor fields and shows source/fetch time, missing values, delay and partial/truncated results. It is not necessarily a final invoice. Human and agent action lists are independent; agents also need global CLOUD delegation, environment permission and per-device CLOUD approval.

Refresh cooldown defaults to 30 seconds; agent budget defaults to 20 calls per UTC day, committed before attempting the request. Failed admitted requests count. This limits requests, not guaranteed currency spending. No background metered refresh or automatic retry occurs. Broader vendor services, OAuth refresh, provisioning, budgets and deployment require their own adapters.

## Credentials, environments, API keys and login

Create a vault environment/profile, choose purpose, save named values and optional expiry, and set agent permission separately. Values are encrypted and never shown in public descriptions, chat, typed command arguments or log/configuration export. Rotate values by name; remove unused variables/profiles. Profiles referencing an environment must be reconfigured before deleting it.

API environments can be bound to the exact HTTPS service origin. Online models/media consume a selected variable internally; endpoint changes fail binding checks. SSH environments are bound to the exact host and use SSH_PASSWORD or SSH_PRIVATE_KEY alongside the separately verified host-key pin. Vault environment IDs are device-local and need explicit rebinding after configuration transfer.

For web login, save LOGIN_USERNAME and LOGIN_PASSWORD in a WEB_LOGIN profile bound to the exact HTTPS origin. In the visible browser, select Saved login credentials and confirm sharing. Filling requires a single visible top-level same-origin form and does not click Submit. Site scripts can read entered values. Cross-origin/iframe/ambiguous forms, MFA and CAPTCHA require a human.

Automatic credential export/sync, OS-wide autofill, OAuth refresh and unattended password entry remain unavailable. Android backup/transfer excludes preference/policy and native cache files because Keystore keys do not transfer. Re-enroll and enter credentials on a new installation; do not expect a copied encrypted preference file to work.

## Terraform / OpenTofu, Pulumi and Ansible

These tools fit Cloud automation. Terraform/OpenTofu manage state-backed resource provisioning with saved plans. Pulumi uses code-based desired state and Automation API preview/up/refresh. Ansible configures machines and deploys software through an approved inventory/control host. Their execution environments belong on an approved host with the required CLIs/plugins/language runtime, not implicitly inside Android.

The dedicated managed IaC adapter is planned, not implemented. Existing pinned SSH can execute operator-approved commands but its bounded command lifecycle is not a durable deployment manager. Use validate → saved plan/preview → review → approved apply → reconcile/status, with state locking/fencing and an immutable approved plan hash.

Future settings must separate human operation, agent validate/preview, agent apply and destructive operations; bind account/project/region/resource scopes; apply money budgets and approval policy; preserve encrypted state and sanitized logs. CLOUD read permission must not grant deployment or shell permission. Plans/state may contain secrets and cannot be dumped into Android job history.

Cancellation does not roll back completed changes. Ansible check mode depends on module support and is not a universal no-side-effects guarantee. Rollback needs a fresh reviewed recovery plan. Packer, cloud-init, Helm and Chinese-vendor adapters are capability-specific follow-ups after actual host/tool acceptance.

## SSH, terminal, Linux VM and IDE

SSH uses the open-source JSch implementation, independently verified SHA256 host-key fingerprints, encrypted password/private-key credentials and per-host agent opt-in. Commands are bounded to 60 seconds and 64 KiB stdout/stderr each. Cancellation disconnects the session, not already-completed remote work. PTY/SFTP and durable deployment are follow-ups.

Optional local terminal/Termux/runtime paths need actually installed binaries and Android permissions. The full VM stays off by default; a user or an opted-in agent can request compatible activation. Root, rootless and sandbox methods have different capabilities; no UI switch creates missing kernel/device support.

VM/VNC needs a real compatible guest/runtime and owner-approved display/network setup. A configured path or status label is not a booted guest or connected desktop. Keep administrative ports private/authenticated.

The code workspace is a bounded offline source editor with files, syntax highlighting/search and SHA write checks. Save before switching files. Build/debug/test toolchains require a real installed local/Linux/SSH host integration. It is not a full VS Code desktop runtime or compiler by default.

## Camera, vision, audio, radio and peripherals

Media options expose currently wired image/phone-camera-thumbnail and compatible voice/model paths. Camera permission does not imply live CCTV ingestion; microphone permission does not prove STT/TTS model compatibility. Hosted image/video/speech APIs require the appropriate provider adapter, account/model and explicit data sharing.

External devices and OTG reports actual USB descriptors/classes, permissions and mounted/user-granted storage. A detected GPU/serial/camera descriptor does not install its driver or make it usable for inference. Hotplug and vendor hardware testing remain necessary.

CCTV/RTSP/UVC/live audio, MCU telemetry/actuation and radio/DSP adapters are future capability work. Two-way HF/VHF/microwave/cellular communication requires suitable owner-paired hardware, drivers and permitted operation; receive-only evidence is not transmit support. A phone alone cannot emit arbitrary radio bands.

Future Linux, Windows, macOS and HarmonyOS clients and NVIDIA/AMD/Intel/Huawei/Chinese vendor backends need independent runtime builds/tests. Mixed-node role claims must be tied to real capability and stop/revoke behavior, not invented throughput.

## Power, networking, firewall and pricing

Power and costs shows available battery/current/thermal/network readings plus policy and estimates. Whole-device power can be an estimate; missing sensors stay unknown. Transfer/new-load constraints, low battery/power-save and thermal state can limit work. They do not enable hardware bypass charging.

Firewall settings control supported Meshlit listeners/connections, with persisted validated port/address rules. It is not an Android OS-wide packet firewall or arbitrary outbound interception. Verify the actual transport/port and separate enrollment from worker authorization.

Price snapshots record provider/source/fetch time. Refresh official live catalog data when supported and review the provider-specific pricing page for other models. Free/catalog/login/gated/API entitlement distinctions remain visible. Usage-based estimates exclude taxes, cache/tool discounts and unreported token types unless the source explicitly covers them.

Before using the cloud, set credential expiry, scopes, request limits and intended human/agent actions. Before using a cluster, confirm actual network reachability, independent pins and memory budget. Unknown readings should not be replaced with optimistic defaults.

## Appearance, accessibility and modern surfaces

Appearance persists light/dark/system/time mode, wallpaper dynamic colors (Android 12+), accent/base palette, animation choice and font size. UI font options are packaged Figtree, system sans, serif and Maple Mono. Code/metric styles can retain their explicit mono family.

Tinted glass is an optional translucent surface treatment over color-tinted backgrounds; it is not a claim of iOS Liquid Glass or live background blur. Solid surfaces remain the default. Low memory, power-save, severe thermal and high-contrast modes use solid surfaces and constrain animation.

Use the live text sample, keep important controls readable and test large text on your device. System font scaling is preserved in addition to the app font scale. High contrast prioritizes solid surfaces. Custom font-file import and arbitrary blur/background effects are future work.

The redesign does not mean every retained legacy screen is modernized. Guide/tutorial and new settings sections share the modern navigation. Real phone layout, touch/keyboard, contrast and scrolling still need inspection; emulator screenshots are distinct evidence.

Settings uses one toolbar; detail pages hide the main tab bar. Appearance has compact controls instead of a large banner. Fresh defaults use Sky accent and system light/dark with neutral surfaces. Existing saved choices remain. Reference blue selects light/Paper/Sky in one saved edit while preserving font and accessibility preferences. Models opens with import, downloads, search and models; device/startup policy and download access details expand on demand.

## Configuration transfer, logs and troubleshooting

Configuration profiles export/import bounded non-secret desired settings with preview/preflight and partial-apply reporting. Online templates import disabled and without agent permission; device-local vault references are removed. Credentials, trust/pairing, root/autonomy/delegation and distributed transactions are not transferred.

Logs can be searched, filtered by severity/source and exported as TXT/JSONL through supported flows. Review the timestamp/source/error, actual model path/hash and job phase. A task marked done in metadata is not proof that a remote action succeeded. Do not place secrets in user-authored scripts, prompts or labels.

If a model does not load: verify a complete artifact and hash, supported format/architecture, current backend/ABI, storage/RAM, context and policy; stop conflicting sessions; inspect failure logs. If download fails: check access/revision/HTTPS/space, pause/resume where supported and inspect install validation. Do not delete working data blindly.

If pairing fails: verify reachability, explicit firewall allowance, identity revision/pin/token and independent approvals. If cloud fails: check token expiry, IAM/RBAC/quota/region and service origin. If an archive fails: inspect byte/count/path/CRC/provider limits and partial-output cleanup. If recovery fails: verify exact model/runtime/context/cache identity and installation key.

Current physical-phone ADB testing is blocked while Samsung is absent from adb devices even though USB inventory sees it. Use normal Android Studio/Mac ADB authorization or an approved wireless endpoint; never silently authorize, wipe or elevate. Local OpenSSH preauthentication is also blocked by this enclosing environment. These are unresolved acceptance gates, not successful tests.

## Settings directory and safe recipes

Basic destinations: Appearance; Models; Camera/vision/audio; Configuration profiles; Custom local model behavior; Checkpoints/recovery; Fine-tuning; Model router; Cloud/vault; Online providers; Power/costs; External devices/OTG; Acceleration; Agents; SSH; Network rules; Device; Tasks; Code workspace; Permissions; Files; Notifications; Monitoring; Logs; Network/pairing; OpenClaw; Guide/tutorial; About. Advanced also exposes Termux, forwarding peers, Linux runtime, crawler, Android automation and hooks.

Offline starter recipe: keep external providers/VM/autonomy off; use bundled starter, short context, measured generation, solid surfaces and logs. Higher-capability single device: verify memory, import a compatible larger GGUF and raise context only within admission. Phone cluster: independently approve compatible native workers and verify actual layered execution before depending on oversized weights.

Cloud read recipe: create an expiring scoped environment, configure a profile, enable only required human reads and refresh. Agent cloud recipe: add global/per-device CLOUD approval plus per-profile action and per-environment permission, with request budgets; credential edits stay human-only. SSH recipe: independently verify host key, bind a vault environment to the host, then separately approve agent commands.

Recovery recipe: save and test an exact native-local cache restore; keep conversation/task history separately. Distributed recovery remains pending. Training recipe: use a licensed dataset and a compatible Soup host through pinned SSH; train, evaluate and only then merge/export/import. Android autograd training remains unavailable. Media recipe: choose an actually compatible local/hosted modality adapter and verify real input/output before automation.

## Evidence and future roadmap

Read PROGRESS.md, BUILD_MILESTONES.md and the feature map for exact source/test evidence and remaining gates. Priorities remain genuine phone layer sharding, durable replicated journals/fenced failover, trustworthy model/context/cache usage and real integration tests, followed by training/hardware/platform acceptance and release polish.

Cloud/vault, modern fonts/surfaces, guided offline help and safe archive source are part of the current continuation. Managed IaC, full vendor services, OAuth/refresh, general browser credential automation, native platform apps, full IDE/compiler and every hardware connector are not automatically completed by those screens.

Never report completion percentages from feature counts or convert a unit fixture, UI control, emulator run or desktop proof into physical cluster acceptance. Unsupported features must clearly return unavailable or blocked states. Use real resources/models/metrics and keep credentials entered locally.
