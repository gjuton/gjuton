# Clean up remaining PIT mutation survivors

## What to build

After [#0022](./0022-add-pitest.md) wired up PIT and the first two
cleanup passes (`SchemaParser.collectRefs` recursion + `NumericGenerator`
range/random-bound coverage), the mutation score sits at **85% (253/299
killed)** with **46 mutants still alive** (30 SURVIVED + 16 NO_COVERAGE).

This issue is the catch-all for working through the rest. Each cluster
below is independently grabbable — pick one, kill it, repeat.

## Findings (baseline = 85% / 253 killed of 299)

Counts are per source file. Reproduce locally with `mvn test -Pmutation`
and open `json-schema-gen-core/target/pit-reports/index.html`.

### Cluster A — `minimalPhase()` universally uncovered (10 NO_COVERAGE)

Every generator's `minimalPhase()` override has a surviving
`NullReturnValsMutator`:

- `AllOfGenerator:28`, `AnyOfGenerator:30`, `ArrayGenerator:34`,
  `BooleanGenerator:17`, `EnumGenerator:23`, `NullGenerator:17`,
  `NumericGenerator:31`, `OneOfGenerator:30`, plus survivors inside
  `RefGenerator:34` and `StringGenerator` paths.

The pattern is coherent: nothing exercises the minimal-mode generation
path. A single test fixture that forces `GeneratorContext` into minimal
mode and asserts each generator returns the expected value should clear
most of these at once.

### Cluster B — `JsonSchemaGenerator` public I/O surface (7 NO_COVERAGE)

`JsonSchemaGenerator.java` lines 69, 79, 110, 113, 118, 139, 147:

- `of(File)` and `of(InputStream)` — convenience constructors untested.
- `withPin` — both `null` argument guards plus the return value.
- `generate(OutputStream)` and `generate(Writer)` — untested overloads.

Straightforward API-level tests.

### Cluster C — `RefGenerator` branch logic (8 SURVIVED)

`RefGenerator.java` lines 34, 39, 45 (×3), 46, 54, 55. Boundary mutants,
negated conditionals, and an `exitMinimal` call removal all survive.
Suggests the depth/recursion guard around `$ref` resolution isn't
asserted. Worth investigating whether tests actually exercise nested or
recursive ref resolution under minimal-mode.

### Cluster D — `StringGenerator` regex and bounds plumbing (9 SURVIVED)

`StringGenerator.java` lines 43, 44, 55, 72, 75, 78, 84, 86 (×2).
A mix of:

- `setInProperties(...)` call removal at L43 (regex generator config
  setup).
- Null-return mutants on `buildRgxGen` and `generateFromPatternWithLength`
  (L44, L75, L78).
- Multiple `ConditionalsBoundaryMutator` survivors on string length
  handling (L72, L84, L86).

Likely real coverage gaps in pattern-length edge cases.

### Cluster E Group 3 — `NumericGenerator.isInRange` is equivalent-under-callers (5 alive)

`NumericGenerator.java` lines 129 (×2 boundary), 132 (math + negate),
133 (return-true). These survive **not because the tests are weak, but
because `isInRange` is only invoked from `ZERO`, `NEAR_MIN`, and
`NEAR_MAX` phases — none of which call it at a boundary value or with
inputs that would expose a flipped multipleOf check**. `MIN`/`MAX`
phases bypass `isInRange` entirely. `RANDOM` produces the same multiples
that `NEAR_*` does, so flipping the verdict only changes which phase
emits a given value, not which values are observable.

Two options to genuinely kill these:

1. **Relax `isInRange` from `private` to package-private** and add
   direct unit tests asserting boundary inclusivity and multipleOf
   correctness. Smallest change.
2. **Refactor `MIN`/`MAX` phases to gate through `isInRange`** before
   emitting. Production behavior change with broader risk; worth doing
   only if there's an independent reason.

### Cluster G — `FunctionalUtil` min/max boundaries (2 SURVIVED)

`FunctionalUtil.java:30,34` — `ConditionalsBoundaryMutator` on `min` and
`max` helpers. Off-by-one not asserted.

### Cluster H — `SchemaMerger.mergeTwoSchemas` (2 SURVIVED)

`SchemaMerger.java:87` — two negated-conditional mutants on the same
line.

### Clusters I / J / K / L — one-offs (4 alive)

- `ObjectGenerator.java:39` — negated conditional.
- `ObjectSchema.java:34` — `lambda$getOptionalFields$0` boolean-true
  return.
- `OneOfGenerator.java:35` — `advanceToNext` negated conditional.
- `GenerationResult.java:11` — `skip()` null return.

## Notes from the first cleanup pass

A small reproducibility caveat surfaced during the work in #0022: the
total killed count fluctuates by 1 between consecutive runs even with
no code changes (one mutant occasionally times out vs. completes). The
**mutation score is stable**, but exact killed/timed-out counts may
drift by 1. Worth knowing when comparing baselines.

## Acceptance criteria

- [ ] Each cluster above either has its surviving mutants killed, or is
      explicitly marked as accepted (with a one-line rationale in this
      issue or in a code comment).
- [ ] Mutation score is **≥ 90%** after this work (current: 85%).
- [ ] `mvn clean verify` still passes.
- [ ] `mvn test -Pmutation` still produces a stable HTML + XML report
      whose top-line score matches what's recorded here.

## Out of scope

- Touching the PIT plugin configuration itself (covered by #0022).
- Adding mutation-score gating to CI (separate follow-up once we trust
  the baseline).

## Blocked by

#0022 — PIT must be wired up first.
