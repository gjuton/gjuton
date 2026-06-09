# Integer Type Generation with Boundary Values

## What to build

Implement generation for `{"type": "integer"}` schemas, including
`minimum` and `maximum` constraints. Across repeated calls the generator
must cover boundary values: min, max, zero, min+1, and max-1.

## Acceptance criteria

- [x] `{"type": "integer"}` generates a valid JSON integer value
- [x] `minimum` and `maximum` constraints are respected
- [x] Across N repeated calls, the values min, max, zero, min+1, and max-1
      are all produced (where constraints are set)
- [x] Integration test validates output against the schema
- [x] Unit tests cover boundary value exhaustiveness in isolation
- [x] `mvn test` passes

## Blocked by

#0005 — String type generation
