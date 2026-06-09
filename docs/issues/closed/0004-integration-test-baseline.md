# Integration Test Baseline

## What to build

Set up the integration test infrastructure that all high-level tests will
use. Tests generate a JSON string from a schema and validate it against
that same schema using an external validator library.

## Acceptance criteria

- [x] `networknt/json-schema-validator` added as a test-scoped dependency
- [x] At least one smoke test validates generated JSON against its schema
- [x] `mvn test` passes

## Blocked by

#0002 — Basic API entry point
