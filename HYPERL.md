# HyperL experimental branch

This branch holds the separate HyperL project foundation under Apache-2.0.
It is not merged into the Meshlit app. Start with [owner requests and build plan](docs/hyperl/REQUESTS_AND_PLAN.md),
[language v1](docs/hyperl/LANGUAGE_V1.md) and [research](docs/hyperl/RESEARCH_2026_10_08.md).

Implementation: `core-gpu` HyperL files and tests; `examples/hyperl`.
Only CPU reference execution and elementwise source generation exist. Real
accelerator compilation/runtime execution, crypto providers, tensors/vision,
RISC-V deployment and LTS release maintenance remain future milestones.
Use open-source/free research dependencies first. No vendor source is copied.

Meshlit targets dynamic AI deployments across hosts and clusters; phones are one
client. Direct hardware/driver/kernel and low-latency native networking are future
qualified backends. See the research and owner plan for evidence and boundaries.

## Run the foundation checks

Use Java 21, the repository's Android SDK and pinned Gradle dependencies:

```sh
./gradlew :core-gpu:testDebugUnitTest --no-daemon --max-workers=1
```

`HyperLCpuBackend` is a Kotlin library API; it has no standalone installer yet.
Deserialize `examples/hyperl/elementwise.json` as `HyperLProgram`, pass finite f32
`x`/`w` arrays to `HyperLRuntime.execute(CPU_REFERENCE, program, inputs)`, or call
`HyperLSourceEmitter.emit(program, LLVM_CPU)` to obtain C99 text. Input bindings
in generated code sort names (`w`, then `x` in this example). Validate shapes and
aliasing before any native execution. The source emitter does not compile or load
untrusted code. The unit host check uses installed Clang; missing Clang is an
explicit skipped host check, not proof of another backend.
