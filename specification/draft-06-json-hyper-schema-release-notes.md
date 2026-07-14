JSON Schema - JSON Hyper-Schema Draft-06 Release Notes

The JSON Schema Office Hours Now Runs Weekly! [Join Us!](https://github.com/orgs/json-schema-org/discussions/34)✕

[![Dynamic image](/img/logos/logo-blue.svg)](/)

[Specification](/specification)[Docs](/docs)[Tools](/tools?query=&sortBy=name&sortOrder=ascending&groupBy=toolingTypes&licenses=&languages=&drafts=&toolingTypes=&environments=)[Blog](/blog)[Community](/community)

Search

![Dark Mode](/icons/theme-switch.svg)

![System theme](/icons/theme-switch.svg)System

![Light theme](/icons/sun.svg)Light

![Dark theme](/icons/moon.svg)Dark

[Star on GitHub](https://github.com/json-schema-org/json-schema-spec)

Introduction

Get Started

Guides

Reference

Specification

Introduction

Get Started

Guides

Reference

Specification

# JSON Hyper-Schema Draft-06 Release Notes

Release notes for migrating from draft-luff-json-hyper-schema-00 (draft-04) to draft-wright-json-schema-hyperschema-01 (draft-06).

**NOTE**

: draft-07 has been released

The [migration notes for draft-07](../draft-07/json-hyper-schema-release-notes) give a much more straightforward overview of migrating from draft-04 to draft-07 by skipping the complicated intermediate states of draft-05 and draft-06. This page has been retained for historical interest, but it is not recommened for those who just want to get going with the latest draft.

**For implementors:** We recommend just implementing draft-07, and not draft-06 or earlier.

*   [Q: What are the incompatible changes between draft-04 and draft-06?](#q:-what-are-the-incompatible-changes-between-draft-04-and-draft-06)
    *   [Changes from draft-04 to draft-05](#changes-from-draft-04-to-draft-05)
    *   [Changes from draft-05 to draft-06](#changes-from-draft-05-to-draft-06)
    *   [Proper use of "targetSchema"](#proper-use-of)
*   [Q: Why were several major changes made to Hyper-Schema just before draft-06’s publication?](#q:-why-were-several-major-changes-made-to-hyper-schema-just-before-draft-06's-publication)
*   [Q: Why doesn’t the spec mention or behave like HTML anymore?](#q:-why-doesn't-the-spec-mention-or-behave-like-html-anymore)
    *   [Splitting "schema"](#splitting)
    *   [Removing "method"](#removing)
*   [Q: So how do I indicate which HTTP methods are supported on a link?](#q:-so-how-do-i-indicate-which-http-methods-are-supported-on-a-link)
*   [Q: No, really. How do I explicitly indicate which HTTP methods are supported on a link?](#q:-no-really.-how-do-i-indicate-which-http-methods-are-supported-on-a-link)
*   [Q: If "targetSchema" is not the response, how do I describe responses?](#q:-if-is-not-the-response-how-do-i-describe-responses)

### Q: What are the incompatible changes between draft-04 and draft-06?

Between drafts 04 and 06 we undertook a major re-examining of Hyper-Schema, which has never been as widely adopted as JSON Schema Validation.

While we knew that there were still major gaps in draft-06, we felt that it was a good set of changes for collecting feedback. With draft-07 published, that draft or later should be used, and draft-06 becomes an historical curiosity.

#### Changes from draft-04 to draft-05

| keyword | change | consequence |
| --- | --- | --- |
| `"base"` | replaces looking up the nearest "self" link to determine the base URI for `"href"` | if you were relying on "self" links to change the base, set `"base"` explicitly |
| `"rel"` | "full" relation removed | use ["item"](https://github.com/json-schema-org/json-schema-spec/issues/295) |
| `"rel"` | "instances" and "create" relations removed | use ["collection"](https://github.com/json-schema-org/json-schema-spec/issues/295) |
| `"rel"` | "root" relation removed | use a fragment in your `"href"` URI Template |
| `"fragmentResolution"` | _removed_ | media type determines how fragments are interpreted |
| `"pathStart"` | _removed_ | _\[no replacement\]_ |
| `"method"` | [changed back to HTML form semantics](../draft-03/draft-zyp-json-schema-03.pdf) of "get" and "post" rather than all HTTP methods | _\[changed again in draft-06 due to feedback that this was confusing\]_ |

#### Changes from draft-05 to draft-06

| keyword | change | consequence |
| --- | --- | --- |
| `"method"` | _removed_ | for HTTP method proposals, see issues [#73](https://github.com/json-schema-org/json-schema-spec/issues/73) and [#296](https://github.com/json-schema-org/json-schema-spec/issues/296) (use either `"method"` or `"allow"` as an extension keyword if needed); indication of how to use `"schema"` and `"encType"` no longer necessary |
| `"schema"` | _removed_ | use `"hrefSchema"`, `"submissionSchema"`, or `"targetSchema"` |
| `"encType"` | _removed_ | use `"submissionEncType"` for request bodies; no longer needed for URI query strings |
| `"hrefSchema"` | _added_ | replaces `"method": "get", "schema": {...}`, with additional functionality |
| `"submissionSchema"` | _added_ | replaces `"method": "post", "schema": {...}` |
| `"submissionEncType"` | _added_ | replaces `"method": "post", "encType": "..."` |
| `"href"` | preprocessing removed | _to be replaced and expanded in future drafts_ |

#### Proper use of `"targetSchema"`

While `"targetSchema"` did not change its meaning in either recent draft, it has been widely misinterpreted. So it may feel like a change to use it as specified.

Due to draft-04 emphasizing individual HTTP methods as `"method"` values, many users interpreted `"targetSchema"` as a hint of the response to the method in `"method"`. This was never correct; all drafts define this keyword as describing the representation of the target resource, which appears as a response to HTTP GET, but may or may not appear in other responses.

Draft-06 clarifies this usage and provides guidance on its use with different HTTP methods. This includes using `"targetSchema"` as a request description for PUT and PATCH. With draft-04, many users used `"schema"` to describe PUT and PATCH requests which is not needed.

However, the [`"targetHints"` proposal](https://github.com/json-schema-org/json-schema-spec/issues/296) has been accepted into draft-07. Among other things, it enables hinting at "Accept-Patch", which is needed to properly use `"targetSchema"` with HTTP PATCH. There will be examples and detailed guidance in draft-07.

### Q: Why were several major changes made to Hyper-Schema just before draft-06's publication?

A: During final review, it became apparent that there was no consensus on how to use the spec as written. The late changes were necessary to publish a spec with unambiguous meaning, so that we could get feedback on its contents rather than differing interpretations. Originally we attempted to simply clarify what was there, but then we realized there was no agreement on what was there in the first place.

### Q: Why doesn't the spec mention or behave like HTML anymore?

A: We came to a consensus that the existing analogies caused more harm than good, for two reasons:

1.  The change between draft-03 and draft-04 to let `"method"` indicate any HTTP method instead of HTML's `<form method="...">` "get" and "post" values broke the original analogy to HTML, and trying to change it back was not well-received
2.  Only being able to use `"schema"` and `"encType"` for either the URI query string (but no other part of the URI) or the request body, but not having any way to work with both at once, was overly restrictive for API design

#### Splitting `"schema"`

Instead of having `"schema"` perform two different things depending on `"method"`, we split it into two keywords, one for each use. This allows using both simultaneously when needed, which is a use case not present in HTML forms.

`"hrefSchema"` replaces the `"method": "get"` use, but leverages URI Template variables so that client data-driven dynamic URIs are no longer limited to the query string. `"encType"` is no longer needed with this approach.

`"submissionSchema"` directly replaces the `"method": "post"` use, with `"submissionEncType"` replacing `"encType"`.

#### Removing `"method"`

Draft-05 tried to restore the draft-03 behavior of `"method"`, but feedback told us that users found the change very confusing. With `"schema"` split, the draft-05 behavior of `"method"` was no longer needed.

We could have switched by to draft-04's `"method"` behavior, but in addition to producing more confusion from all of the back and forth, the draft-04 approach to `"method"` was not consistent with the rest of the LDO design anyway. Most notably, it caused problems with the usage of `"targetSchema"` as described above.

### Q: So how do I indicate which HTTP methods are supported on a link?

A: Ideally, this is implicitly conveyed by your link relation type, which is the primary indicator of semantics across machine-oriented hypermedia in general. [RFC 5988](https://tools.ietf.org/html/rfc5988) provides guidance on creating custom (a.k.a. "extension") link relations.

Several URI schemes and namespaces, such as the [UUID namespace in the `urn:` scheme](https://tools.ietf.org/html/rfc4122), or the [`tag:` scheme](https://tools.ietf.org/html/rfc4151), are particularly suitable for creating unique identifiers.

And of course, there are ways to detect this at runtime such as HTTP's `"Allow"` response header, or attempting a method and handling a `405 Method Not Allowed` error accordingly.

### Q: No, really. How do I _explicitly_ indicate which HTTP methods are supported on a link?

A: The [`"targetHints"` proposal](https://github.com/json-schema-org/json-schema-spec/issues/296) is part of draft-07, so using it as an extension to draft-06 is an option, but we recommend simply using draft-07 at this point.

### Q: If `"targetSchema"` is not the response, how do I describe responses?

A: You should have hyper-schemas for your various success and error responses, but connecting them to links is more of a documentation question than a usage question: each response will indicate its own schema, so you don't need to know it in advance at runtime.

There has never been a Hyper-Schema keyword to explicitly associate responses with operations such as HTTP methods. The use cases for this seem to be about generating API documentation, so this is most likely a candidate for a [JSON Schema API Documentation vocabulary](https://github.com/json-schema-org/json-schema-vocabularies/issues/1).

## Need Help?

### Did you find these docs helpful?

### Help us make our docs great!

At JSON Schema, we value docs contributions as much as every other type of contribution!

[Edit this page on Github](https://github.com/json-schema-org/website/blob/main/pages/draft-06/json-hyper-schema-release-notes.md)

[Learn how to contribute](https://github.com/json-schema-org/website/blob/main/CONTRIBUTING.md)

### Still Need Help?

Learning JSON Schema is often confusing, but don't worry, we are here to help!.

[Ask the community on GitHub](https://github.com/orgs/json-schema-org/discussions/new?category=q-a)

[Ask the community on Slack](https://json-schema.org/slack)

![logo-white](/img/logos/logo-white.svg)

[Open Collective](https://opencollective.com/json-schema)

[Code of Conduct](/overview/code-of-conduct)

[![Slack logo](/img/logos/slack_logo_small-white.svg)Slack](https://json-schema.org/slack)

 [![X logo](/img/logos/x-twitter.svg) X](https://x.com/jsonschema)

[![LinkedIn logo](/img/logos/icons8-linkedin-2.svg)LinkedIn](https://linkedin.com/company/jsonschema/)

[![YouTube logo](/img/logos/icons8-youtube.svg)Youtube](https://www.youtube.com/@JSONSchemaOrgOfficial)

[![GitHub logo](/img/logos/github_logo-white.svg)GitHub](https://github.com/json-schema-org)

Copyright © 2026 JSON Schema. All rights reserved.