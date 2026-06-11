# Boolean as a schema

## What to build

In JSON Schema Draft 6+ (carried into Draft 7), a bare boolean may stand
in for a schema:

- `true` means "any value is valid"
- `false` means "no value is valid"

This shows up most often inside sub-schema positions:

```json
{ "additionalProperties": false }
{ "items": true }
{ "patternProperties": { "^x": false } }
{ "properties": { "foo": true } }
```

Today the parser rejects boolean values where it expects a schema
object with `MismatchedInputException: Cannot construct instance of
UntypedSchema from boolean value`.

The model needs to accept a boolean as a valid schema value and the
generator needs to interpret it: `true` ≡ untyped any-value schema
(see #0024); `false` is unsatisfiable and should be propagated as
such (it has no representable value, so a generator hitting `false`
should fail loudly only when it actually needs to produce something
from that position — e.g. `additionalProperties: false` already means
"don't generate extras" and doesn't require producing a `false`-typed
value).

## Acceptance criteria

- [ ] `{"properties": {"foo": true}}` parses and generates a value for
      `foo` of any JSON type
- [ ] `{"items": true}` parses; generated array elements may be of any
      JSON type
- [ ] `{"additionalProperties": false}` continues to forbid generating
      additional properties (already works today via the non-boolean
      path; this should keep working)
- [ ] `{"additionalProperties": true}` parses and is treated as the
      default (extras are permitted but the generator isn't required to
      emit any)
- [ ] `mvn verify` passes

## Blocked by

#0024 — Untyped schemas
