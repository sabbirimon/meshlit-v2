# Remaining model-engine correctness gates

Updated 2026-10-06. Real bundled generation now passes on the API35 x86_64
emulator; this document records unresolved correctness work, not fake failures.

## Stream accounting corrected; SDK native usage still unavailable

The app no longer counts text deltas as tokens or truncates generation after a
number of callbacks. Native max_tokens/stop options bound generation. Missing
terminal completion fails, and unavailable usage is null in result/UI/wire types.

Pinned RunAnywhere 0.20.12 final metadata still reports 3 completion tokens for
`Hello, hello, hello.`, previously measured as 6 tokens by its native backend.
Its public stream result does not establish whether counts are measured or
estimated. Those SDK usage figures are suppressed; do not use them for billing
or native throughput. A future SDK change must expose measured counters with
provenance and agree with actual backend evidence before this gate is removed.

The standalone native adapter reads tokens_predicted, tokens_evaluated,
tokens_cached and decode throughput from the native completion response.
Context/path checks and real native checkpoint reuse now have emulator evidence.
Read `native-checkpoints.md` and the latest PROGRESS entry.

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
