# gjuton

See `README.md` for what the project does and how it is used.

## Build commands

```
mvnd clean verify -Pfast # inner-loop: skips checkstyle + spotbugs (~5s warm)
mvn clean verify         # full build incl. style/static analysis (~14s) — pre-commit gate
mvn test -Pmutation -pl gjuton-jackson2 -am # pitest; run from the root, report in gjuton-jackson2/target/pit-reports
```

Use `mvnd verify -Pfast` during work to keep the iteration loop tight.
Run plain `mvn clean verify` once before declaring a task done so style
and static-analysis gates are checked.

## Reference docs

- [Architecture](docs/architecture.md) — the three-phase pipeline, schema model
  design, generation strategy, and package layering with its allowed-dependency
  rules.
- [Ticket conventions](docs/tickets.md) — the shape a ticket takes and the rules
  for writing one.
- [Code conventions](docs/code_convention.md) — code style (Google-modified,
  `var` usage, line-breaking), design conventions (deep methods), documentation
  conventions (Ousterhout-style javadoc), and test conventions.

## Testing

Integration tests are parameterized and driven by schema files in
`gjuton-tests/src/test/resources/schemas/`. Adding a schema file is all that's
needed to add a test case.

## Issues

Issues are tracked on GitHub. Never close an issue — that is the user's
decision, or happens when the PR merges.

## Commits

Conventional commits without a scope: `<type>: <description>`. The project
is focused enough that a scope adds noise without signal. Keep the subject
short, around 50 characters. Types in use: feat, fix, refactor, test, docs,
chore, build, revert. A ticket reference goes at the end of the subject in
parentheses: `fix: refs in example fails parse (#163)`.

## Changelog

Every change a user could notice gets an entry under `## [Unreleased]` in
`CHANGELOG.md`, in the same working tree as the change itself — new
features, bug fixes, behaviour changes, anything affecting the public API.
Internal refactors, tests, build and docs changes get no entry. Write it
from the user's side: what they can now do, or what no longer misbehaves,
not which class changed.

## Personal setup

If `AGENTS.personal.md` exists at the repo root, read it. It holds
machine-specific and per-developer instructions and is not tracked.

## Workflow

- Start with the simplest implementation that passes the tests. Add
  complexity (helper methods, guards, extra abstractions) only when a
  failing test or concrete scenario forces it — not preemptively. No
  error handling for scenarios that can't occur; if a 200-line draft
  would work as 50, throw it away and write the 50.
- **Make surgical changes.** Each changed line should be justifiable
  straight from the request. Don't improve adjacent code, refactor what
  isn't broken, or restyle to taste — match the existing style even
  where you'd differ. Remove imports/variables/functions your change
  orphaned; leave pre-existing dead code alone (mention it, don't
  delete it).
- When a fix could go in two places, fix the root cause, not the
  symptom. A defensive check that filters out bad data is a sign the
  producer should be fixed instead.
- Do not introduce new patterns or invent new abstractions (factories,
  builders, strategy/visitor shapes, helper layers, new base classes,
  new interfaces, generic wrappers, etc.) unless the ticket explicitly
  calls for it OR you have asked and gotten explicit confirmation.
  Solving the stated problem with the existing shapes in the codebase
  is the default. "It would be cleaner" or "it would scale better" is
  not sufficient justification — propose it, wait for a yes, then
  implement.
