# Fractional Numeric Constraints

## What to build

The constraint fields on `NumericSchema` (`minimum`, `maximum`,
`exclusiveMinimum`, `exclusiveMaximum`, `multipleOf`) are currently
`Long`. JSON Schema allows fractional values for all numeric constraint
keywords — e.g. `{"type": "integer", "multipleOf": 1.5, "minimum": 4.5}`
is valid and means the generated integers must be multiples of 1.5 that
are ≥ 4.5 (so 6, 9, 12, …).

Widen the fields to `Double` (or `BigDecimal`) so these schemas parse
correctly, and update `NumericGenerator` to handle fractional constraints
while still producing whole-number output for `"integer"` schemas.

## Acceptance criteria

- [ ] Constraint fields (`minimum`, `maximum`, `exclusiveMinimum`,
      `exclusiveMaximum`, `multipleOf`) use `Double` instead of `Long`
- [ ] Generator handles fractional constraints correctly while producing
      integer output (e.g. `multipleOf: 1.5` with `minimum: 4.5` yields
      6, 9, 12, …)
- [ ] Existing integration and unit tests still pass
- [ ] New unit tests cover fractional constraint values
- [ ] `mvn verify` passes

## Blocked by

#0011 — Numeric constraints
