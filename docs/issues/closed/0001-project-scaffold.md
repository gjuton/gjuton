# Project Scaffold

## What to build

Set up the Maven project structure: a parent POM with a single child module
(`json-schema-gen-core`), package skeleton, CLAUDE.md, .gitignore, and
empty `docs/adr` and `docs/prd` folders.

## Acceptance criteria

- [x] Parent POM exists with packaging `pom` and one declared module
- [x] `json-schema-gen-core` module builds with `mvn clean verify`
- [x] Root Java package `se.plilja.jsonschemagen` exists with `api` and
      `internal` sub-packages
- [x] CLAUDE.md at repo root documents purpose, build commands,
      architecture overview, and package conventions
- [x] .gitignore covers Maven `target/` and common IDE files
- [x] `docs/adr/` and `docs/prd/` directories exist

## Blocked by

None — can start immediately.
