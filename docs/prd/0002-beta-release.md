# PRD-0002: Beta Release — JSON Schema Test Data Generator

## Problem Statement

Writing test fixtures by hand is tedious and error-prone. Developers working
with JSON-based APIs craft JSON payloads manually, which means edge cases and
boundary conditions are often missed. Existing tools either require extensive
configuration, target a single JSON Schema draft, or produce purely random data
that doesn't exercise the interesting corners of a schema.

## Solution

A Java library that generates valid JSON from a JSON Schema. Drop in your
schema, get realistic test data that exercises boundary conditions. Designed to
replace hand-crafted fixtures in automated tests. Supports the most commonly
used JSON Schema features across drafts, rather than targeting full compliance
with any single specification version.

The beta is the first version published to Maven Central for public use.

## Target Schema Generators

Rather than targeting a specific JSON Schema draft, the beta defines its scope
by the schema generators whose output it must handle correctly:

1. **springdoc-openapi** — the dominant OpenAPI/JSON Schema generator in the
   Spring Boot ecosystem. The primary use case: Java developers generating test
   data for their own or a colleague's API.
2. **Pydantic / FastAPI** — the dominant schema generator in the Python
   ecosystem (Draft 2020-12). Covers the cross-language integration case: a
   Java service consuming a Python API's schema.
3. **Zod v4** — the dominant schema generator in the JavaScript/TypeScript
   ecosystem (Draft 2020-12). Covers the cross-language integration case: a
   Java service consuming a JS/TS API's schema.

If a JSON Schema keyword appears in real output from these generators, it is in
scope. Esoteric keywords that no real-world generator emits are not.

## User Stories

1. As a Java developer, I want to point the generator at my springdoc-openapi
   schema and get valid test data, so that I don't have to hand-craft JSON
   fixtures for my API tests.
2. As a Java developer integrating with a Python service, I want to generate
   test data from a Pydantic-produced schema, so that I can test my client code
   against realistic payloads.
3. As a Java developer integrating with a TypeScript service, I want to generate
   test data from a Zod-produced schema, so that I can test my client code
   against realistic payloads.
4. As a test author, I want to supply a custom value producer for a specific
   field (e.g. a user ID from my test database), so that generated data
   satisfies application-level constraints the schema can't express.
5. As a test author, I want to compose the generator with a tool like JavaFaker,
   so that generated strings contain realistic names, addresses, and other
   domain values instead of random characters.
6. As a test author, I want to choose between exhaustive and random-only
   generation modes, so that I can trade off thoroughness for speed depending on
   the test scenario.
7. As a test author, I want to query the generator for a completeness measure,
   so that I know when exhaustive generation has covered all boundary values and
   I can stop iterating.
8. As a test author, I want to supply my own ObjectMapper for output
   serialization, so that I can control formatting, escaping, and other output
   behavior to match my project's conventions.
9. As a test author, I want to seed the generator for reproducibility, so that a
   failing test produces the same data on every run.
10. As a test author, I want the generator to silently ignore schema keywords it
    doesn't support, so that I can use it with real-world schemas without
    hitting errors on unimplemented features.
11. As a test author, I want the generator to fail fast on contradictory schemas
    (e.g. minimum > maximum), so that I catch schema bugs immediately rather
    than getting confusing output.
12. As a new user, I want a README with usage examples, so that I can get
    started quickly without reading source code.
13. As a new user, I want complete javadoc on the public API, so that I can
    understand every configuration option from my IDE.
14. As a library consumer, I want the generator to work out of the box with
    sensible defaults (exhaustive mode, no custom providers, standard
    ObjectMapper), so that the simplest use case requires no configuration.
15. As a library consumer, I want the generator to have no runtime dependencies
    beyond Jackson, so that it doesn't bloat my dependency tree.

## Implementation Decisions

- **Unsupported keywords are ignored silently.** The generator produces data as
  if the keyword is not present. This avoids blocking adoption when a schema
  contains a keyword the generator hasn't implemented yet. Most unsupported
  keywords are additional constraints, so ignoring them produces "too
  permissive" data rather than invalid data.
- **Contradictory schemas fail fast.** When constraints are mutually exclusive
  (e.g. `minimum: 10, maximum: 5`), the generator throws
  `UnsatisfiableSchemaException`. This is a bug in the caller's schema and
  should surface immediately.
- **Generation mode is an enum**, not a boolean toggle. The beta ships with two
  values (exhaustive and random-only) but the enum is extensible for future
  modes without breaking the API.
- **Completeness measure is a method on the generator** (e.g. `isExhausted()`
  or similar), not a change to the return type of `generate()`. The current
  `String` return type is preserved.
- **Custom value producers** allow callers to supply dynamic values for specific
  fields. The exact mechanism (lambda, interface, etc.) is an implementation
  decision to be made during design. The capability must support integration
  with tools like JavaFaker and database lookups.
- **Caller-supplied ObjectMapper** for output serialization. Callers can provide
  their own `ObjectMapper` to control formatting, escaping, and other
  serialization behavior.
- **The public API must be finalized for beta.** The current
  `JsonSchemaGenerator` class is a starting point, not the final shape. The
  configuration options above must be incorporated into a cohesive API design.
- **Java 21** is the minimum supported version (current LTS).
- **No new runtime dependencies beyond Jackson.** The library stays lightweight.

## Testing Decisions

Good tests for this project verify external behavior: given a schema, the
generated JSON must be valid according to a JSON Schema validator.
Implementation details (internal phase state, generator dispatch) are not
asserted in integration tests.

- **Integration tests with real-world schemas.** Add schema files extracted from
  actual springdoc-openapi, Pydantic/FastAPI, and Zod v4 output to the existing
  parameterized test harness in `IntegrationTest`. Each schema is validated over
  250 seeded iterations using the networknt JSON Schema validator. This is the
  primary acceptance mechanism: if the test passes, the generator handles that
  schema correctly.
- **Unit tests on configuration behavior.** Configuration options (generation
  mode, custom value producers, completeness measure) are tested at the
  individual generator level, following the existing pattern of generator unit
  tests.
- **Public API tests.** The finalized `JsonSchemaGenerator` API should have its
  own test class exercising the configuration surface, including default
  behavior (unconfigured) and each configuration option in isolation.

## Out of Scope

- **Pluggable JSON serialization library** (#39) — may be implemented but is
  not a beta requirement.
- **Thread-safe generation** (#46) — tracked separately; the beta documents
  instances as not thread-safe.
- **Full compliance with any single JSON Schema draft** — the beta targets
  common real-world usage, not spec completeness.
- **Maven coordinates and publishing mechanics** — release concerns, not product
  requirements.
- **CLI interface** — the library is consumed as a Java dependency only.
- **Source code generation** — the library generates JSON data at runtime, not
  Java source files.

## Further Notes

This PRD supersedes PRD-0001 (Project Foundation) as the guiding document for
what the library should be. PRD-0001 remains as a historical record of the
project's original scope.

The generation strategy prioritises values that are likely to expose bugs in the
system under test. For each type, deterministic "trouble-prone" values are
emitted first (e.g. empty string for strings; min, max, zero for integers),
followed by random valid values. This boundary-value-first approach is the
library's core differentiator from pure random generators.
