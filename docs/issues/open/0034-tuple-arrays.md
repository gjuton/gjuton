# Tuple-typed arrays (`items` as array, `prefixItems`)

## What to build

A JSON Schema array can be a *tuple*: each positional slot has its own
sub-schema. Two spellings exist:

- **Draft 7** — `items` is an array of sub-schemas; `additionalItems`
  controls whether (and how) elements past the tuple are allowed.
- **Draft 2019-09+** — `prefixItems` carries the positional schemas;
  `items` (now singular) describes any element after the prefix.

Today the generator treats both as if there were no positional
information: each slot comes out as `null`, which a strict validator
rejects whenever the slot expects a concrete type.

The required behaviour:

- For each position `i`, generate a value that satisfies the `i`th
  sub-schema in `items` (Draft 7) or `prefixItems` (Draft 2019-09+).
- For positions past the end of the tuple, fall back to:
  - Draft 7: `additionalItems` — `true` (any element), `false` (no
    additional elements allowed), or a sub-schema (each extra element
    must satisfy it).
  - Draft 2019-09+: `items` — the sub-schema for elements past the
    prefix; `false` forbids them.
- Existing array constraints (`minItems`, `maxItems`, `uniqueItems`)
  continue to apply on top of the positional shape.

## Acceptance criteria

- [ ] `items: [ {type:"string"}, {type:"integer"} ]` (Draft 7) produces
      a two-element array `[string, integer]` with no nulls
- [ ] `prefixItems: [ ... ]` (Draft 2019-09+) is honoured the same way
- [ ] `minItems` larger than the tuple length triggers element
      generation past the prefix using `additionalItems` / `items`
- [ ] `additionalItems: false` (Draft 7) or `items: false` (Draft
      2019-09+) yields an array no longer than the tuple
- [ ] Existing single-schema array behaviour
      (`items: { type: "string" }`) is unaffected
- [ ] `mvn verify` passes
