# RunAnywhere sources and browser assistant

Meshlit already uses RunAnywhere Android SDK 0.20.12 for its on-device inference.
That path remains the default. The SDK, standalone llama.cpp fork and Chrome
extension are separate integrations; they are not interchangeable Android modules.

## Optional upstream sources

`sources.lock.json` records reviewed revisions and licenses. Download the two
optional sources into ignored `vendored/` directories with:

```sh
python3 scripts/sync-optional-sources.py
python3 scripts/build-optional-llama.py --jobs 2
```

The downloader fetches pinned Git revisions and refuses mismatched existing
checkouts rather than resetting local work. The build script verifies the pin
and builds a standalone host `llama-server` with CMake. CMake and a C++ compiler
are required. It does not replace the Maven SDK, ship an Android native library,
implement the direct JNI engine stub or automatically start a server. For a
local host server, consult its `--help`, keep its listener on loopback and select
a properly licensed model yourself. No model weights are bundled.

The RunAnywhere `on-device-browser-agent` directory is an upstream Chrome/Edge
extension companion. Its own AGENTS.md, README and dependency/build instructions
apply when modifying it. Its WASM/WebGPU/offscreen APIs are browser-extension
APIs and cannot simply be included in an Android WebView. Upstream licenses,
NOTICE and attribution are retained. No extension source was copied into app.

## Android browser assistant

Cloud → Open local browser assistant opens the original Meshlit implementation:

1. Manually open an HTTPS URL and enter a task.
2. Load an on-device model through the existing RunAnywhere UI.
3. Ask the model for a next action based on a bounded page/DOM snapshot.
4. Review and approve or reject the proposed action. Repeat as needed.

The action parser accepts only a known visible element index for click/type,
bounded scrolling, or done. Unknown actions, arbitrary JavaScript and extra
fields are rejected. The model cannot execute browser script or start an
unattended loop. The app executes fixed DOM routines only after approval and
checks for a stale page/element. Password controls are excluded and cannot be
used as typing targets. A done result is a model proposal, not proof of success.

Navigation is restricted to the exact host manually opened; another host needs
manual opening. File/content access, mixed HTTP content, automatic new windows
and third-party cookies are disabled. JavaScript is enabled for normal websites
and DOM interaction. The embedded browser has no JavaScript-to-native bridge.
Page text is untrusted input and can still influence a model's proposed action:
review every action, especially submissions and clicks that change an account.

The page snapshot and task are passed to local inference by this screen, not to
the crawler companion. Visiting a website still makes ordinary browser network
requests. Limited DOM snapshots may miss shadow DOM, frames, hidden controls or
complex widgets; this is a small human-stepped assistant, not feature parity
with the upstream extension. Physical-device model/WebView tests remain needed.
