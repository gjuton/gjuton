# `if` / `then` / `else` conditional branches

## What to build

When a schema declares `if` / `then` / `else`, the generated value must
satisfy whichever conditional branch the generator chose to honour. The
simplest contract: generate a value that satisfies the `if` schema and
the `then` schema together — or generate a value that fails the `if`
schema and satisfies the `else` schema. A validator will reject any
output that picks one branch but doesn't produce the keys/constraints
that branch requires.

Today the generator ignores `if` / `then` / `else` entirely. Required
properties declared inside `then` (e.g. a `data` payload when
`status: "ok"`) or inside `else` (e.g. an `error` payload otherwise) are
not present in the output, and a strict validator rejects every
generated value.

The most natural shape:

1. Decide which branch to satisfy (deterministic on a discriminator, or
   random if the generator is free to pick).
2. Compose the parent schema with `if`+`then` (or, for the `else` path,
   the parent with `else` plus a constraint that violates `if`).
3. Generate from that composed schema using the existing merge logic.

`then` and `else` are both optional in JSON Schema. If only `then` is
present, the `else` path is unconstrained beyond the parent schema.

## Acceptance criteria

- [ ] When `if` matches the generated value, all `then` constraints are
      satisfied (required properties present, sub-schemas honoured)
- [ ] When `if` does not match, all `else` constraints are satisfied
- [ ] Missing `then` (or missing `else`) is treated as "no extra
      constraint on that branch"
- [ ] Across repeated calls, both branches are exercised when both are
      satisfiable
- [ ] `mvn verify` passes

## Blocked by

#0015 — Combining schemas (anyOf/oneOf/allOf)
