# Upgrading

What to do when moving between gjuton versions. What
changed in each release is in the [changelog](CHANGELOG.md).

## 0.1.0

The `io.github.gjuton:gjuton` artifact no longer exists. Depend on the one
matching the Jackson version your project already uses — `gjuton-jackson2`
behaves as before, `gjuton-jackson3` the same on Jackson 3:

```xml
<dependency>
    <groupId>io.github.gjuton</groupId>
    <artifactId>gjuton-jackson2</artifactId>
    <version>0.1.0</version>
</dependency>
```

No source changes are needed — the `io.github.gjuton.api` classes are unchanged.

## 0.0.2

`withRecursionLimits*` is renamed, and the old names are gone:

| before                          | after                         |
| ------------------------------- | ----------------------------- |
| `withRecursionLimitsShallow()`  | `withNestingLimitsShallow()`  |
| `withRecursionLimitsDeep()`     | `withNestingLimitsDeep()`     |
| `withRecursionLimits(int, int)` | `withNestingLimits(int, int)` |

The arguments mean something different: they bound nesting depth in the
generated value rather than counting `$ref` expansions. A hand-picked pair
needs choosing again against the new meaning; the presets need no attention.

## 0.0.1

First release — nothing to upgrade from.
