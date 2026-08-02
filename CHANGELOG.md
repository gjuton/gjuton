# Changelog

All notable changes to Gjuton are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

While Gjuton is pre-1.0 the public API is unstable: breaking changes may land in
any release. Entries are added under `[Unreleased]` as changes are made, and that
section is promoted to a version at release time (see `docs/releasing.md`).

## [Unreleased]

### Added

- `withOverrideByRef` and `withOverrideByFormat`, which override a value by what
  a position *is* rather than where it sits: positions referencing a given
  definition, or strings carrying a given `format`. Custom formats count,
  so a schema naming a domain type gjuton has no generator for can still be
  filled with realistic values.
- Support for `format: "duration"` (ISO 8601 durations, e.g. `P3D`, `PT1H30M`).
- Support for Draft 4 style boolean `exclusiveMinimum`/`exclusiveMaximum`, the
  form used by OpenAPI 3.0 and Swagger 2.0. The bound is taken from the
  accompanying `minimum`/`maximum`; previously such a schema failed to parse.
- Trace logging to explain an unexpected value or a generation failure. Off by
  default; enable the `io.github.gjuton` logger at `TRACE`. Adds a dependency on
  `slf4j-api`; no logging implementation is bound.

### Changed

- `UnsatisfiableSchemaException` messages now say more about what failed and
  where: merge conflicts name the conflicting schema locations (JSON Pointers)
  and the values or types on both sides, a string `format` failure names only
  the constraints the schema actually declares, and a failure at the document
  root is located as `(at $)` — a message with no location now means the
  location could not be determined, rather than that the root was at fault.
- Jackson dependency updated from 2.22.0 to 2.22.1.

### Fixed

- A `$ref` appearing inside data rather than in a schema position — in an
  `example`, an `enum` payload, or under a property that happens to be named
  after a keyword — is no longer resolved as a schema, which could fail the
  parse of the whole document.
- `$ref` targets that sit outside any schema position, such as an OpenAPI
  `components/schemas` entry, are now type-inferred like any other schema.
  Previously a target that omitted `type` produced output violating its own
  constraints.
- Array index out of bounds in the untyped generator in minimal mode.
- `allOf` branches that each declare a `contains` clause no longer report a
  conflict when no single element can satisfy both; such clauses are now kept
  apart and each given an element of its own.

## [0.0.1] — 2026-07-23

- First release.
