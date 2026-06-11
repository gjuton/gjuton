# `minProperties` / `maxProperties` on objects

## What to build

`minProperties` and `maxProperties` constrain the count of own
properties on an object. Today the generator ignores both, so:

- objects with `minProperties: N` may come out with fewer than `N`
  keys when the schema has no `required` (or fewer required than `N`)
  and the generator chooses to omit optional keys;
- objects with `maxProperties: N` may come out with more than `N` keys
  when several optional properties happen to be selected.

The required behaviour:

- When emitting properties, the generator counts both required and
  optional ones and ensures the final key count lies in
  `[minProperties, maxProperties]` (defaulting to `[0, ∞)` if either is
  unset).
- `required` and `minProperties` interact: if `required` already names
  ≥ `minProperties` properties, the lower bound is satisfied for free;
  otherwise the generator must keep at least `minProperties - |required|`
  optional properties in the output.
- `additionalProperties` / `patternProperties` interact with
  `maxProperties`: the budget for extras is bounded by what `max` allows.
- The "boundary-value exhaustiveness" approach used elsewhere applies:
  produce objects at the minimum, in the middle, and at the maximum
  property count across repeated calls.

## Acceptance criteria

- [ ] `{ type: "object", minProperties: N }` produces objects with at
      least `N` own properties (across repeated calls)
- [ ] `{ type: "object", maxProperties: N }` produces objects with at
      most `N` own properties (across repeated calls)
- [ ] Both bounds together — `minProperties` ≤ count ≤ `maxProperties`
      is always honoured
- [ ] Boundary values (the min, the max) are exercised across iterations
- [ ] `required` properties remain present even when including them
      pushes the count above any chosen optional-only baseline
- [ ] `mvn verify` passes

## Related

#0029 — Respect constraints under combining keywords (covers
        `minProperties` only when reached through `allOf` / `oneOf` /
        `anyOf`; this issue is the plain top-level case)
