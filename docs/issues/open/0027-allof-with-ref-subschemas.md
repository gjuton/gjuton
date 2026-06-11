# `allOf` with `$ref` sub-schemas

## What to build

`allOf` is commonly used together with `$ref` to "extend" a reusable
definition with extra constraints:

```json
{
  "allOf": [
    { "$ref": "#/definitions/Base" },
    { "properties": { "extra": { "type": "string" } } }
  ]
}
```

Today this is rejected up front in `SchemaMerger.rejectUnsupportedComposition`
with `IllegalArgumentException: allOf composition with $ref sub-schemas
is not yet supported`. The merger declines to operate on a branch whose
`$ref` hasn't been resolved into a concrete schema.

This issue lifts that restriction: resolve `$ref` branches inside an
`allOf` before merging, so the merge proceeds against the concrete
referenced schema. Watch for self-referential cycles — a `$ref` whose
chain leads back to an enclosing `allOf` needs to be guarded so the
resolver doesn't recurse forever.

## Acceptance criteria

- [ ] `allOf` with a `$ref` branch and an inline-constraint branch
      produces a value that satisfies both
- [ ] `allOf` whose branches are all `$ref` produces a value satisfying
      every referenced schema
- [ ] Self-referential `$ref` inside an `allOf` is detected and rejected
      with a clear error (or handled via the recursion strategy used
      elsewhere in the generator) rather than overflowing the stack
- [ ] Existing `allOf` behaviour without `$ref` is unchanged
- [ ] `mvn verify` passes

## Blocked by

#0014 — $ref resolution
#0015 — Combining schemas (anyOf/oneOf/allOf)

## Related

#0021 — Compose combining keywords on the same schema node (overlapping
        merger limits)
