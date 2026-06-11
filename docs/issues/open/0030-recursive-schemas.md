# Recursive / self-referential schemas

## What to build

A schema may refer to itself, directly or transitively, via `$ref`. A
tree node whose `children` are an array of the same node type is the
canonical example:

```json
{
  "$id": "#node",
  "type": "object",
  "properties": {
    "value": { "type": "string" },
    "children": { "type": "array", "items": { "$ref": "#node" } }
  }
}
```

Today the generator expands the reference unconditionally, recurses
into itself, and overflows the JVM stack — `StackOverflowError` —
without ever producing a value.

This issue introduces a recursion strategy so cyclic schemas terminate.
Two reasonable approaches:

- **Depth-bounded expansion** — track recursion depth per `$ref` chain
  and, past a configurable depth, generate the "minimal" value for the
  recursive position (e.g. empty array, `null`, or a value that
  satisfies the schema's other constraints without recursing further).
- **Probabilistic descent** — at each recursion step, roll against a
  decaying probability of continuing; this gives varied tree shapes
  across iterations while guaranteeing termination.

Either is fine; pick whichever fits the existing generator style best.
The recursion guard introduced here should also unblock self-referential
`$ref` chains in `allOf` (see #0027).

## Acceptance criteria

- [ ] A self-referential schema produces a finite JSON value without
      throwing `StackOverflowError`
- [ ] Generated values vary in depth across iterations (i.e. the
      recursion strategy is not "always terminate immediately")
- [ ] Recursive `$ref` inside an `allOf` is also handled (no overflow)
- [ ] Non-recursive schemas are unaffected by the new guard
- [ ] `mvn verify` passes

## Blocked by

#0014 — $ref resolution
