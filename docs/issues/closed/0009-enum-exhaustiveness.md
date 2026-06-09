# Enum Exhaustiveness

## What to build

Implement generation for schemas with an `enum` keyword. Across repeated
calls every value in the enum must be produced.

## Acceptance criteria

- [x] A schema with `enum` generates one of the listed values
- [x] Across N repeated calls all enum values are produced
- [x] Works for enums containing mixed types (string, integer, boolean,
      null)
- [x] Integration test validates output against the schema
- [x] Unit tests cover exhaustiveness in isolation
- [x] `mvn test` passes

## Blocked by

#0005 — String type generation
