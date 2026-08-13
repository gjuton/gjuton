# Changelog

All notable changes to Gjuton are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

While Gjuton is pre-1.0 the public API is unstable: breaking changes may land in
any release. Entries are added under `[Unreleased]` as changes are made, and that
section is promoted to a version at release time (see `docs/releasing.md`).

## [Unreleased]

### Added

- Support for `propertyNames`, which constrains what a property may be called.
  Generated names now satisfy it, and a schema requiring a property whose name
  it forbids says so instead of producing JSON its own schema rejects.
- Support for `minContains` and `maxContains`. An array schema asking for
  several matching elements now gets them, one capping the matches stays within
  the cap, and bounds nothing could satisfy are reported instead of producing
  JSON the schema rejects.

### Changed

- Generated strings and arrays now stay within a default size ceiling instead of
  taking a schema's bound literally, so a schema using a very large `maxLength`
  or `maxItems` to mean "unbounded" no longer produces multi-megabyte values or
  runs out of memory. `Constraints.stringLength`/`arrayLength` still override it,
  and a `minLength`/`minItems` above the default is still generated in full, which
  gjuton reports in the log.
- Gjuton is now split into `gjuton-core` plus a module per Jackson version:
  `gjuton-jackson2` and `gjuton-jackson3`. The `io.github.gjuton:gjuton` artifact
  is gone; depend instead on the one matching the jackson version you are using,
  `gjuton-jackson2` behaving as before and `gjuton-jackson3` the same way but on
  Jackson 3. On its own `gjuton-core` cannot read or write JSON and says so.

### Fixed

- A `$ref` naming a definition whose key contains a character a URI must escape,
  such as `#/patternProperties/%5Ba-z%5D%2B`, now resolves to that definition
  instead of failing the parse. A `+` in a fragment still means a literal `+`.
- A schema whose `allOf` holds one `if`/`then` per value of a discriminator
  property now generates, instead of being reported as unsatisfiable. Each
  conditional is satisfied on its own side rather than every `then` having to
  hold at once, so the variants may exclude one another, and a conditional
  reached through several paths in the schema is enforced once rather than once
  per path.

## [0.0.2] — 2026-08-06

### Added

- `withOverrideByFormat`, which overrides a value by what a position *is*
  rather than where it sits: every string carrying a given `format`. Custom
  formats count, so a schema naming a domain type gjuton has no generator for
  can still be filled with realistic values.
- Support for `format: "duration"` (ISO 8601 durations, e.g. `P3D`, `PT1H30M`).
- Support for Draft 4 style boolean `exclusiveMinimum`/`exclusiveMaximum`, the
  form used by OpenAPI 3.0 and Swagger 2.0. The bound is taken from the
  accompanying `minimum`/`maximum`; previously such a schema failed to parse.
- Trace logging to explain an unexpected value or a generation failure. Off by
  default; enable the `io.github.gjuton` logger at `TRACE`. Adds a dependency on
  `slf4j-api`; no logging implementation is bound.

### Changed

- `withRecursionLimits*` are renamed `withNestingLimits*` (the old names are
  removed) and now bound nesting depth in the generated value rather than
  counting `$ref` expansions, so how a schema is factored into definitions no
  longer changes what it generates. The presets are retuned, the same schema
  and seed produce different values than before, and a schema that cannot be
  generated within the limits — including one whose recursion cannot bottom
  out — says so instead of reporting a `$ref` depth count.
- `UnsatisfiableSchemaException` messages now say more about what failed and
  where: merge conflicts name the conflicting schema locations (JSON Pointers)
  and the values or types on both sides, a string `format` failure names only
  the constraints the schema actually declares, and a failure at the document
  root is located as `(at $)` — a message with no location now means the
  location could not be determined, rather than that the root was at fault.
- Jackson dependency updated from 2.22.0 to 2.22.1.
- An external schema named by several `$ref`s is retrieved once per parse
  rather than once per ref, so a shared definitions file over HTTP costs one
  request no matter how many definitions are referenced from it.

### Fixed

- Constraints behind a `$ref` now survive a merge: an `allOf` or a
  `oneOf`/`anyOf` branch built from a reference keeps what the definition it
  names declares, instead of that being dropped. Two schemas that both fix a
  value to `boolean` merge rather than reporting a type conflict, and branches
  whose array-length bounds cannot both hold are recognised as incompatible up
  front instead of failing part-way through generation.
- A `$ref` appearing inside data rather than in a schema position — in an
  `example`, an `enum` payload, or under a property that happens to be named
  after a keyword — is no longer resolved as a schema, which could fail the
  parse of the whole document. This now holds inside an external schema too,
  where such a value could previously be rewritten before being emitted.
- `$ref` targets that sit outside any schema position, such as an OpenAPI
  `components/schemas` entry, are now type-inferred like any other schema.
  Previously a target that omitted `type` produced output violating its own
  constraints.
- Array index out of bounds in the untyped generator in minimal mode.
- `allOf` branches that each declare a `contains` clause no longer report a
  conflict when no single element can satisfy both; such clauses are now kept
  apart and each given an element of its own.
- A relative `$ref` now resolves against the nearest enclosing `$id` (or Draft
  4's `id`) as the spec requires, rather than against the file it was loaded
  from — inside external schemas too, so schemas split across subdirectories
  resolve correctly. A schema whose `$id` names a remote location fetches its
  siblings from there instead of picking up a same-named file on disk, and a
  `$ref` naming a schema's own `$id` is found within it rather than fetched.
  Schemas declaring no `$id` are unaffected.

## [0.0.1] — 2026-07-23

- First release.
