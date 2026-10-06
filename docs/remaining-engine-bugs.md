# Remaining model-engine correctness gates

Updated 2026-10-06. Real bundled generation now passes on the API35 x86_64
emulator; this document records unresolved correctness work, not fake failures.

## Stream events are incorrectly counted as tokenizer tokens

`RunAnywhereInferenceEngine.infer` increments `tokensEmitted` for nonempty text
events and uses that count for generatedTokens, tokensPerSecond and an extra
max-token stop condition. It also sets promptTokens to zero without a measured
prompt count. SDK text event boundaries are not reliable tokenizer boundaries.

Observed reproduction: the real SmolLM2 bundled test produced
`Hello, hello, hello.`. The native backend reported **6 tokens**; the wrapper
reported **3**. Evidence: `../../meshlit-validation/bundled-model-native-evidence.log`.
This is a metrics bug, not proof that generation is fabricated. Rates and prompt
counts from this wrapper must not be used for billing, placement benchmarks or
an accurate token meter until corrected.

Fix guide: inspect the **pinned 0.20.12** stream/result schema and completion
metadata, use native/SDK tokenizer usage when actually available, and represent
unavailable usage explicitly in shared result/UI contracts. Keep text callback
counts separate. Let the native max_tokens option bound generation rather than
treating text chunks as token units. Preserve cancellation, stop-sequence handling
and backend failures. Check all stop sequences, not just the first. Do not derive
token counts from characters or words or silently substitute zeros.

Acceptance: real native output and public result agree when usage is supplied;
multiple tokens in one chunk and split text events do not change reported usage;
unavailable usage remains visibly unknown; max-token/stop/cancellation cases work.

## Physical-device and resource gates

- Real Android generation currently has emulator evidence; physical-phone
  boot/load/generate/unload, thermal, battery and low-memory tests remain.
- Native two-worker proof is desktop-only. Genuine oversized-model execution
  across physical phones remains a separate required gate.
- Compatible VLM/native speech model availability and real capture/feed inference
  must be tested before reporting media analysis as functional on a given device.
- SDK-managed context and unsupported acceleration controls must remain explicit;
  a metadata training limit is not an applied runtime context or usable NPU.
- Replicated journals, fenced claims, portable KV and automatic restart are
  future implementations; current local job persistence is insufficient.

See PROGRESS.md and the layer/recovery plan for the broader acceptance sequence.
