# Ticket conventions

A ticket has to make sense to someone who never saw the conversation it came
from. That constraint drives everything below.

## Shapes

Defects:

```markdown
## Bug

What's wrong. Include a repro schema and stack trace if the trigger isn't
obvious from the title.

## Acceptance criteria

- [ ] ...
```

Everything else:

```markdown
## Problem        <- something is wrong or costly
## Goal           <- nothing is broken, we want a capability

One to three sentences. Pick whichever fits; never both.

## <sections named after their content>

## Acceptance criteria

- [ ] ...
```

Add `## Scope` / `## Out of scope` or `## Related` only when they earn their
place.

## Rules

1. **Keep it short.** Readers skim; every sentence earns its place.
2. **The ticket stands alone.** Cut anything not needed to implement this
   ticket — context from the conversation it came from reads as confusing
   non-sequitur to everyone else.
3. **Implementation hints, not implementation decisions.** Whoever implements
   it is better placed to decide. Include specifics only where they change the
   outcome. Not a hard rule — some tickets genuinely need the detail.
4. **Acceptance criteria are specific to this ticket.** Don't restate baseline
   expectations like "the build passes" — that is "the code should compile".
5. **Name types and classes, not file paths or line numbers.** Line numbers go
   stale before the ticket is picked up.
6. **Titles**: bugs state the symptom, everything else states the change.
7. **Under 150 words of prose, 250 at the outside.** Code blocks, repro schemas
   and acceptance criteria don't count — they're scanned, not read. Going over
   means either the ticket is too big and should be split, or it's carrying
   analysis that isn't needed to start work. If neither applies and it still
   won't fit, flag it and ask rather than silently exceeding the limit.
8. **Deep analysis goes in a comment, not the body.** The body stays skimmable
   and the reasoning stays available to whoever wants it.
