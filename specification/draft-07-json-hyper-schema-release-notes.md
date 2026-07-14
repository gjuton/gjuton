JSON Schema - JSON Hyper-Schema Draft-07 Release Notes

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

# JSON Hyper-Schema Draft-07 Release Notes

JSON Hyper-Schema [draft-07](../../draft-07/json-schema-hypermedia.html) completes the reworking of Hyper-Schema that was begun in draft-06.

Hyper-Schema is now solely focused on adding hyperlinks to JSON documents, so keywords used for other purposes (`readOnly` and `media`) have been [moved to the Validation specification](json-schema-release-notes).

The [new draft](../../draft-07/json-schema-hypermedia.html) has been completely rewritten for clarity and accessibility, so it is best to simply approach it as a new proposal. We hope to add tutorial material at some point, but that is outside of the scope of release notes.

However, if you wish to migrate from an earlier draft, this page is a guide to the key _changes_. The additions, which are much more numerous, should be learned directly from the new specification until we can provide tutorials.

Note that draft-handrews-json-schema-hyperschema-00 has been replaced by draft-handrews-json-schema-hyperschema-01 in order to fix confusing bugs. The newer -01 draft is **still considered to be draft-07**. It now references the draft-07 meta-schema with the correct URI, among other typo fixes. There are no funcitonal changes between -00 and -01.

*   [Migrating from draft-06](#migrating-from-draft-06)
*   [Migrating from draft-05](#migrating-from-draft-05)
*   [Migrating from draft-04](#migrating-from-draft-04)
    *   [GET](#get)
    *   [PUT](#put)
    *   [DELETE](#delete)
    *   [POST](#post)
    *   [PATCH](#patch)

### Migrating from draft-06

No draft-06 features were changed, although two keywords were renamed for clarity and consistency:

*   `mediaType` -> `targetMediaType`
*   `submissionEncType` -> `submissionMediaType`

Additionally, `hrefSchema` was somewhat confusing, so a great deal more effort has gone into explaining how it works, and how it fits into the overall link resolution process.

### Migrating from draft-05

See the [draft-06 release notes](../../draft-06/json-hyper-schema-release-notes) for information related to draft-05.

### Migrating from draft-04

In the ideal draft-07 world, links and [operations](https://json-schema.org/draft-07/json-schema-hypermedia.html#rfc.section.3.1) are not the same concept. Using terminology borrowed from [OpenAPI's Operation Object](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#operationObject), HTTP methods are operations, and each link (as described by a single LDO) can support multiple operations.

Therefore, unlike draft-04, draft-07 hyper-schemas [do not have separate links for each operation](../../draft-07/json-schema-hypermedia.html#rfc.section.8.1). This makes the migration guidelines below approximate at best.

For a more detailed explanation of how draft-04's `method` and `targetSchema` were typically used to create single-operation links, and how that poses a challenge for migrating to multi-operation links, see the [draft-06 release notes](../../draft-06/json-hyper-schema-release-notes). Those release notes also explain what happened to the link relations defined in draft-04 and subsequently removed, and the changes in how the instance base URI is determined.

Beyond those changes, a minimal migration would be something along the following lines, although the [intentional lack of explicit response descriptions](../../draft-07/json-schema-hypermedia.html#rfc.appendix.A.2) (except when the response happens to be a representation of the target resource) means that some uses of draft-04 do not have direct analogues in draft-07.

Any keyword not mentioned in a list below is unchanged for that link operation.

#### GET

*   `"method": "GET"` -> `"targetHints": {"allow": ["GET"]}`
*   `mediaType` -> `targetMediaType`
*   `schema` -> `hrefSchema` with parameters added to `href`
*   `encType` -> drop if `application/x-www-form-urlencoded`, contact the mailing list otherwise

#### PUT

If you have a PUT link where `schema`/`encType` differ from `targetSchema`/`mediaType`, where `targetSchema`/`mediaType` describe a non-representation response, then those fields do not have a direct replacement.

*   `"method": "PUT"` -> `"targetHints": {"allow": ["PUT"]}`
*   `schema` -> `targetSchema`
*   `encType` -> `targetMediaType`

#### DELETE

DELETE does not take a request payload, so `schema` and `encType` should be unused. If `targetSchema` and `mediaType` were being used for a response other than the just-deleted resource's representation, then they do not have a direct replacement.

*   `"method": "DELETE"` -> `"targetHints": {"allow": ["DELETE"]}`
*   `mediaType` -> `targetMediaType` (if describing the representation)

#### POST

In most cases, the response of a POST is **not** a representation of the target resource, but rather some sort of result or status of whatever the POST attempted to do. Therefore `targetSchema` and `mediaType` should almost certainly be dropped. Other than that:

*   `"method": "POST"` -> `"targetHints": {"allow": ["POST"]}`
*   `schema` -> `submissionSchema`
*   `encType` -> `submissionMediaType`

#### PATCH

It was never entirely clear how to model a proper PATCH (that uses a patch media type rather than `application/json` in the request) in Hyper-Schema. One option was to treat it identically to PUT except with the patch media type in `encType`. Assuming that approach (and the same `taregetSchema`/`mediaType` caveats as for PUT):

*   `"method": "PATCH"` -> `"targetHints": {"allow": ["PATCH"]}`
*   `schema` -> `targetSchema`
*   `"encType": "..."` -> `"targetHints": {"accept-patch": "..."}`

## Need Help?

### Did you find these docs helpful?

### Help us make our docs great!

At JSON Schema, we value docs contributions as much as every other type of contribution!

[Edit this page on Github](https://github.com/json-schema-org/website/blob/main/pages/draft-07/json-hyper-schema-release-notes.md)

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