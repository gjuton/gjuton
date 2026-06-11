# Untyped schemas

## What to build

A JSON Schema node with neither `type` nor `enum` is valid Draft 7 and
means "any value" — anything from `null` to a deeply nested object is
acceptable. Common in practice for description-only nodes or for fields
where the author wants to defer typing entirely:

```json
{ "description": "freeform payload" }
{}
```

Today the generator rejects these with
`IllegalArgumentException: Schema has no type and no enum` when it
encounters one during dispatch in the generator.

This issue makes the generator pick a value for untyped schemas. The
simplest viable behaviour is to emit a representative sample across the
possible JSON types (string, number, boolean, null, array, object) so
"boundary-value exhaustiveness" still applies — i.e. the generator
should explore the type space, not always emit `null`.

## Acceptance criteria

- [ ] A schema like `{}` produces a valid JSON value rather than throwing
- [ ] A schema like `{"description": "x"}` produces a valid JSON value
- [ ] Generated values span more than one JSON type across iterations
      (i.e. not always the same constant)
- [ ] `enum`-only schemas (no `type`) continue to work as today
- [ ] Cross-cutting keywords (`const`, `if`/`then`/`else`, `allOf`/`oneOf`/
      `anyOf`) on an untyped node are still honoured
- [ ] `mvn verify` passes

## Blocked by

#0009 — Enum exhaustiveness
