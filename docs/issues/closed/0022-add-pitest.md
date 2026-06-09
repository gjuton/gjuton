# Add mutation testing with PIT

## What to build

Wire [PIT](https://pitest.org/) into the Maven build so we can measure
mutation coverage and catch tests that execute code without actually
asserting its behavior.

PIT mutates bytecode (flips operators, removes calls, returns nulls,
etc.) and re-runs the tests. Surviving mutants point at weak tests or
dead code. Higher mutation score = tests genuinely exercise the code.

- Add `pitest-maven` plugin (with `pitest-junit5-plugin`) to
  `json-schema-gen-core/pom.xml`.
- Target classes: `se.plilja.jsonschemagen.*`.
- Run behind a profile (e.g. `-Pmutation`) — pitest is slow and
  shouldn't be on every `verify`.
- Invocation: `mvn test -Pmutation` produces HTML + XML reports under
  `target/pit-reports/`.

## Make IntegrationTest deterministic first (prerequisite)

PIT assumes deterministic tests. Unit tests already use hard-coded
seeds (`new Random(42)`), but `IntegrationTest` was relying on ambient
randomness via `JsonSchemaGenerator.of(content).generate()`, which made
mutation runs flaky.

Seed `IntegrationTest` from a single global seed read in `parameters()`:

- `-Dtest.seed=<long>` → use that seed
- unset or `-Dtest.seed=random` → fixed default `42L`
- The resolved seed is logged once via SLF4J at the start of
  `parameters()` so any failure prints a seed that reproduces it.

Scope is `IntegrationTest` only. Other unit tests keep their hard-coded
`new Random(42)`. The library's public API still produces random values
by default for consumers.

## Acceptance criteria

- [x] `IntegrationTest` runs deterministically with no flags set
- [x] `mvn test -Dtest.seed=<long>` reproduces the same value sequence
      across runs
- [x] The chosen seed is logged once per `IntegrationTest` run; on
      failure the logged seed is sufficient to reproduce the failure
      locally
- [x] `IntegrationTest` no longer uses unseeded `new Random()` /
      ambient randomness
- [x] `mvn test -Pmutation` runs PIT and produces an HTML + XML report
- [x] Mutation run is stable: top-line mutation score matches across
      consecutive runs (exact killed counts may drift by 1 due to PIT
      timeout fluctuation — see [#0023](./0023-mutation-survivor-cleanup.md))
- [x] PIT is NOT part of the default `mvn verify` pipeline
- [x] A baseline mutation score is recorded — see
      [#0023](./0023-mutation-survivor-cleanup.md) (**85%, 253/299
      killed** after this issue's cleanup passes)
- [x] `mvn clean verify` still passes

## Out of scope

- Cleaning up the surviving mutants surfaced by the baseline run —
  tracked in [#0023](./0023-mutation-survivor-cleanup.md). Two early
  cleanup passes landed in this issue (`SchemaParser.collectRefs`
  array recursion; `NumericGenerator` range/random-bound coverage)
  because they were small and surfaced design observations worth
  capturing alongside the wiring work.
- Wiring PIT into CI gating — start with manual/nightly runs; gate
  once a baseline is trusted.

## Blocked by

None.
