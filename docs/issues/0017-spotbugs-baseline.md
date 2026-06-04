# SpotBugs Baseline

## What to build

Add SpotBugs static analysis to the build so that common bug patterns are
caught automatically during `mvn verify`.

## Rules

1. **SpotBugs runs on verify** — `spotbugs:check` is bound to the `verify`
   phase and fails the build on any detected bug.
2. **Exclude file in place** — `spotbugs-exclude.xml` at the project root is
   wired in so that false positives can be suppressed without touching the
   plugin config.

## Acceptance criteria

- [x] `spotbugs-maven-plugin` added to root `pom.xml`
- [x] `spotbugs-exclude.xml` created and referenced by the plugin
- [x] `mvn verify` passes with no SpotBugs errors
