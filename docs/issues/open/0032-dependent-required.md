# `dependencies` / `dependentRequired` on objects

## What to build

JSON Schema lets an object declare cross-property dependencies: "if
property X is present, properties Y and Z must also be present." Draft
7 spells this `dependencies` (which doubles as schema-dependencies — a
sub-schema whose constraints kick in when the trigger key is present).
Draft 2019-09 split the two: `dependentRequired` for the keys-only
form, `dependentSchemas` for the sub-schema form.

Today the generator ignores both. When it chooses to emit the trigger
key, the dependent keys (or sub-schema constraints) are not added, and
a strict validator rejects the output.

The required behaviour:

- **Keys form** (`dependentRequired` / `dependencies` with array value):
  if the generator emits a trigger key, it must also emit every key in
  the dependent list. Transitive dependencies must be followed — emitting
  `a` may force `baz`, which in turn forces `foo`, which forces `bar`.
- **Schema form** (`dependentSchemas` / `dependencies` with object
  value): if the trigger key is present, the value must additionally
  satisfy the dependent sub-schema. This composes like an implicit
  `allOf` gated on key presence.

## Acceptance criteria

- [ ] `dependentRequired: { foo: ["bar"] }` — whenever `foo` is emitted,
      `bar` is also emitted
- [ ] Transitive dependent-required chains are resolved (emit the full
      closure of dependents)
- [ ] `dependencies` (Draft 7) with an array value behaves identically
      to `dependentRequired`
- [ ] `dependentSchemas` (and `dependencies` with a sub-schema value)
      causes the dependent sub-schema's constraints to be honoured
      whenever the trigger key is present
- [ ] Across repeated calls, the trigger key is sometimes absent (so
      the dependency is vacuously satisfied) and sometimes present
      (with the dependents)
- [ ] `mvn verify` passes
