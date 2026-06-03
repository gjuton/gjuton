# json-schema-gen

Runtime test data generator for Java. Takes a JSON Schema (Draft 7) and
produces valid JSON. Designed for use in automated tests where hand-crafted
fixtures are error-prone and incomplete.

## Build commands

```
mvn clean verify        # full build + tests
mvn clean compile       # compile only
mvn test                # run unit tests
```

## Architecture

Three-phase pipeline:

1. **Parser** — reads a JSON Schema document (Jackson tree) and produces
   an internal schema model.
2. **Model** — plain Java classes representing schema constructs
   (type, constraints, $ref, combining keywords, etc.).
3. **Generator** — walks the model and produces a JSON value string.

Public API returns a plain `String`. No third-party types are exposed.
Convenience overloads write to `OutputStream`, `Writer`, or `File`.

## Package conventions

```
se.plilja.jsonschemagen
├── api          public API — everything a consumer imports
└── internal     implementation detail, not part of the public contract
    ├── parser   JSON → schema model
    ├── model    schema model classes
    └── generator model → JSON value string
```

- Code in `api` may import from `internal`; `api` is the facade that
  delegates to the implementation.
- Code in `internal` must not import from `api` (prevents circular deps).
- Consumers must only import from `api`, never from `internal` directly.
- Jackson is used only inside `internal`; it must not appear in `api` types.
