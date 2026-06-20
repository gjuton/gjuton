# Generator Configuration

## What to build

Expose configuration options that let callers control how the generator
behaves. The public API should accept a configuration object alongside
the schema.

Candidate options (not exhaustive — implement the ones that prove
useful as the generator matures):

- **Field pinning** — caller specifies a JSON path and a fixed value;
  that value appears in the output unchanged, rest is generated from
  the schema.
- **Generate additional properties** — when `additionalProperties` is
  absent or `true`, generate random extra properties on objects to
  exercise code that doesn't expect unexpected fields. Off by default.
- **Recursion depth limits** — configure soft and hard depth ceilings
  for `$ref` resolution (currently hardcoded to 5 and 10 in
  `RefGenerator`).
- **Output ObjectMapper** — allow callers to supply their own
  `ObjectMapper` for serializing the generated value, controlling
  formatting, escaping, and other output behaviour.

## Acceptance criteria

- [ ] Public API accepts a configuration object (builder pattern or
      similar) alongside the schema
- [ ] At least one configuration option is implemented end-to-end
- [ ] Unconfigured usage behaves identically to today (zero breaking
      changes)
- [ ] Integration tests cover configured and unconfigured paths
- [ ] `mvn test` passes

## Blocked by

#0012 — Object type generation
