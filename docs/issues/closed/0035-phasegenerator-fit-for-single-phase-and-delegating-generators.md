# `PhaseGenerator` fit for single-phase and delegating generators

## What to build

`PhaseGenerator<E, R>` models a "boundary-values first, then random"
progression: declare an enum of phases, advance through them, return
`Skip` for phases that don't apply, and the framework loops until one
returns `Present`. That shape is a good fit for `NumericGenerator`,
`StringGenerator`, and the format generators. It's an awkward fit for
two other groups:

**Single-phase generators** — `NullGenerator`, `AllOfGenerator`,
`RefGenerator` each declare a one-element enum (`NULL`, `ONLY_PHASE`,
`REF`) just to satisfy the type parameter. They have no
boundary-then-random cycle. The framework's loop, `minimalPhase()`, and
`advanceToNext()` machinery is dead weight for them.

**Pure delegators** — `AllOfGenerator` and `RefGenerator` don't own a
phase progression at all: every call forwards to a delegate
(`merged.generate()` / `target.generate()`) and the delegate is what
cycles through boundary values. Putting an enum on the outer class
implies it owns a phase cycle, but it doesn't.

The intended cleanup:

- Introduce a small `Generator<R>` interface exposing `generate()`.
- `PhaseGenerator` implements `Generator`. Multi-phase generators stay
  as-is.
- `NullGenerator`, `AllOfGenerator`, `RefGenerator` implement
  `Generator` directly and drop their dummy enum.
- Pure-delegating format generators (`IdnEmailGenerator`,
  `IdnHostnameGenerator`, `IriGenerator`, `IriReferenceGenerator`)
  implement `Generator` directly and drop the pass-through
  `PhaseGenerator` machinery.
- `JsonGenerator.delegate` becomes `Generator<?>` rather than
  `PhaseGenerator<?, ?>`.

No change to public API or to generation output.

## Acceptance criteria

- [x] A `Generator<R>` interface exists; `PhaseGenerator` implements it
- [x] `NullGenerator`, `AllOfGenerator`, `RefGenerator` no longer extend
      `PhaseGenerator` and have no `GenerationPhase` enum
- [x] `JsonGenerator` holds and dispatches through `Generator<?>`
- [x] Existing tests pass unchanged
- [x] `mvn verify` passes
