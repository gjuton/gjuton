# Respect constraints inherited through combining keywords

## What to build

When a schema's constraints are nested under `allOf` / `oneOf` / `anyOf`
(or under `contains` on an array, or under `if`/`then`/`else`), the
generator currently drops or fails to propagate several of them. The
generated JSON parses fine but a strict validator rejects it.

Observed symptoms — each is a separate validator complaint, but they
all stem from the generator not honouring a constraint that lives on a
sub-schema inside a combining keyword:

- **`required`** — required properties on a sub-schema inside `allOf`
  are not generated on the merged object
- **`minProperties`** — empty objects are emitted even when an
  enclosing schema demands at least one property
- **`additionalProperties: false`** — extra properties are generated
  for a sub-schema branch that forbids them
- **`const`** — `contains` schemas with `{"const": X}` are not
  satisfied; the generated array doesn't contain `X`
- **`enum`** — `enum` on a combining branch isn't honoured

The likely root cause is shared: constraints from sub-schemas aren't
being folded into the value-producing path when the parent uses a
combining keyword. A single fix to how the generator composes
sub-schema constraints into the parent's generation should address all
of the above.

## Acceptance criteria

- [ ] `allOf` whose branches contribute additional `required` properties
      produces objects containing every required property across all
      branches
- [ ] An object schema whose sub-schema sets `minProperties: 1` produces
      non-empty objects when that sub-schema is selected
- [ ] A sub-schema with `additionalProperties: false` is honoured even
      when reached through a combining keyword (no extras emitted)
- [ ] `contains: { "const": X }` on an array produces arrays containing
      at least one `X`
- [ ] `enum` declared on a combining branch is honoured for the value
      produced when that branch is selected
- [ ] `mvn verify` passes

## Blocked by

#0012 — Object type generation
#0013 — Array type generation
#0015 — Combining schemas (anyOf/oneOf/allOf)
