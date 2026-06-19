# Untyped schemas

## What was built

A JSON Schema node with neither `type` nor `enum` is valid Draft 7 and
means "any value" — anything from `null` to a deeply nested object is
acceptable. Common in practice for description-only nodes or for fields
where the author wants to defer typing entirely:

```json
{ "description": "freeform payload" }
{}
```

The generator now handles these via `UntypedGenerator`, which cycles
through a pool of representative values spanning all JSON types (null,
booleans, integers, strings, arrays, objects — including non-empty
variants), then picks randomly from the same pool.

## Acceptance criteria

- [x] A schema like `{}` produces a valid JSON value rather than throwing
- [x] A schema like `{"description": "x"}` produces a valid JSON value
- [x] Generated values span more than one JSON type across iterations
      (i.e. not always the same constant)
- [x] `enum`-only schemas (no `type`) continue to work as today
- [x] Cross-cutting keywords (`const`, `if`/`then`/`else`, `allOf`/`oneOf`/
      `anyOf`) on an untyped node are still honoured
- [x] `mvn verify` passes

## Blocked by

#0009 — Enum exhaustiveness
