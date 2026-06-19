# Respect constraints inherited through combining keywords

## What to build

When a schema's constraints are nested under `allOf` / `oneOf` / `anyOf`
(or under `contains` on an array, or under `if`/`then`/`else`), the
generator currently drops or fails to propagate several of them. The
generated JSON parses fine but a strict validator rejects it.

Observed symptoms — each is a separate validator complaint, but they
all stem from the generator not honouring a constraint that lives on a
sub-schema inside a combining keyword:

- ~~**`required`** — required properties on a sub-schema inside `allOf`
  are not generated on the merged object~~ **Already works** —
  `SchemaMerger` unions required fields across branches.
- **`minProperties`** — empty objects are emitted even when an
  enclosing schema demands at least one property
- **`additionalProperties: false`** — extra properties are generated
  for a sub-schema branch that forbids them
- **`const`** — `contains` schemas with `{"const": X}` are not
  satisfied; the generated array doesn't contain `X`
- ~~**`enum`** — `enum` on a combining branch isn't honoured~~ **Already
  works** — `SchemaMerger` intersects enum values and `buildDelegate`
  checks enum before type dispatch.

### Root cause

The root cause is not that combining-keyword generators fail to
propagate constraints — `SchemaMerger` already merges everything it
knows about. The real problem is that four JSON Schema keywords are
missing from the data model entirely, so they are silently ignored
everywhere:

| Keyword                | Missing from        |
|------------------------|---------------------|
| `const`                | `Schema` (base)     |
| `additionalProperties` | `ObjectSchema`      |
| `minProperties`        | `ObjectSchema`      |
| `contains`             | `ArraySchema`       |

Adding each keyword to the model, parser (Jackson annotations),
generator, and merger fixes it both standalone and under combining
keywords.

## Implementation steps

1. **`const`** — add to `Schema` as a cross-cutting keyword (like
   `enum`), check in `JsonGenerator.buildDelegate` before `enum`,
   merge in `SchemaMerger` (two const values must match or throw)
2. **`additionalProperties: false`** — add `Boolean` field to
   `ObjectSchema` (only the `false` case; schema-valued
   `additionalProperties` is out of scope), merge in `SchemaMerger`
   (`false` wins), respect in `ObjectGenerator` (skip optional fields)
3. **`minProperties`** — add `Integer` field to `ObjectSchema`, merge
   in `SchemaMerger` (take max), respect in `ObjectGenerator` (ensure
   enough fields)
4. **`contains`** — add `Schema` field to `ArraySchema`, merge in
   `SchemaMerger`, respect in `ArrayGenerator` (ensure at least one
   element matches)

## Acceptance criteria

- [x] `allOf` whose branches contribute additional `required` properties
      produces objects containing every required property across all
      branches
- [x] A `const` value declared on a schema (standalone or under a
      combining keyword) is emitted verbatim
- [ ] A sub-schema with `additionalProperties: false` is honoured even
      when reached through a combining keyword (no extras emitted)
- [ ] An object schema whose sub-schema sets `minProperties: 1` produces
      non-empty objects when that sub-schema is selected
- [ ] `contains: { "const": X }` on an array produces arrays containing
      at least one `X`
- [x] `enum` declared on a combining branch is honoured for the value
      produced when that branch is selected
- [ ] `mvn verify` passes

## Blocked by

#0012 — Object type generation
#0013 — Array type generation
#0015 — Combining schemas (anyOf/oneOf/allOf)
