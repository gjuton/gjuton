# Numeric Constraints

## What to build

Extend integer generation to support `exclusiveMinimum`,
`exclusiveMaximum`, and `multipleOf` constraints.

## Acceptance criteria

- [x] `exclusiveMinimum` and `exclusiveMaximum` are respected
- [x] `multipleOf` produces values that are multiples of the given number
- [x] Boundary values are covered across repeated calls where applicable
- [x] Integration tests validate output against constrained schemas
- [x] Unit tests cover each constraint in isolation
- [x] `mvn test` passes

## Blocked by

#0006 — Integer type generation with boundary values
