 JSON Schema: A Media Type for Describing JSON Documents                                                         

<table class="header"><tbody><tr><td class="left">Internet Engineering Task Force</td><td class="right">A. Wright, Ed.</td></tr><tr><td class="left">Internet-Draft</td><td class="right"></td></tr><tr><td class="left">Intended status: Informational</td><td class="right">H. Andrews, Ed.</td></tr><tr><td class="left">Expires: September 20, 2018</td><td class="right">Cloudflare, Inc.</td></tr><tr><td class="left"></td><td class="right">March 19, 2018</td></tr></tbody></table>

JSON Schema: A Media Type for Describing JSON Documents  
draft-handrews-json-schema-01

# [Abstract](#rfc.abstract)

JSON Schema defines the media type "application/schema+json", a JSON-based format for describing the structure of JSON data. JSON Schema asserts what a JSON document must look like, ways to extract information from it, and how to interact with it. The "application/schema-instance+json" media type provides additional feature-rich integration with "application/schema+json" beyond what can be offered for "application/json" documents.

# Note to Readers

The issues list for this draft can be found at <[https://github.com/json-schema-org/json-schema-spec/issues](https://github.com/json-schema-org/json-schema-spec/issues)\>.

For additional information, see <[http://json-schema.org/](https://json-schema.org/)\>.

To provide feedback, use this issue tracker, the communication methods listed on the homepage, or email the document editors.

# [Status of This Memo](#rfc.status)

This Internet-Draft is submitted in full conformance with the provisions of BCP 78 and BCP 79.

Internet-Drafts are working documents of the Internet Engineering Task Force (IETF). Note that other groups may also distribute working documents as Internet-Drafts. The list of current Internet-Drafts is at http://datatracker.ietf.org/drafts/current/.

Internet-Drafts are draft documents valid for a maximum of six months and may be updated, replaced, or obsoleted by other documents at any time. It is inappropriate to use Internet-Drafts as reference material or to cite them other than as "work in progress."

This Internet-Draft will expire on September 20, 2018.

# [Copyright Notice](#rfc.copyrightnotice)

Copyright (c) 2018 IETF Trust and the persons identified as the document authors. All rights reserved.

This document is subject to BCP 78 and the IETF Trust's Legal Provisions Relating to IETF Documents (http://trustee.ietf.org/license-info) in effect on the date of publication of this document. Please review these documents carefully, as they describe your rights and restrictions with respect to this document. Code Components extracted from this document must include Simplified BSD License text as described in Section 4.e of the Trust Legal Provisions and are provided without warranty as described in the Simplified BSD License.

* * *

# [Table of Contents](#rfc.toc)

*   1\. [Introduction](#rfc.section.1)
*   2\. [Conventions and Terminology](#rfc.section.2)
*   3\. [Overview](#rfc.section.3)

*   3.1. [Validation](#rfc.section.3.1)
*   3.2. [Annotation](#rfc.section.3.2)
*   3.3. [Hypermedia and Linking](#rfc.section.3.3)

*   4\. [Definitions](#rfc.section.4)

*   4.1. [JSON Document](#rfc.section.4.1)
*   4.2. [Instance](#rfc.section.4.2)

*   4.2.1. [Instance Data Model](#rfc.section.4.2.1)
*   4.2.2. [Instance Media Types](#rfc.section.4.2.2)
*   4.2.3. [Instance Equality](#rfc.section.4.2.3)

*   4.3. [JSON Schema Documents](#rfc.section.4.3)

*   4.3.1. [JSON Schema Values and Keywords](#rfc.section.4.3.1)
*   4.3.2. [JSON Schema Vocabularies](#rfc.section.4.3.2)
*   4.3.3. [Root Schema and Subschemas](#rfc.section.4.3.3)

*   5\. [Fragment Identifiers](#rfc.section.5)
*   6\. [General Considerations](#rfc.section.6)

*   6.1. [Range of JSON Values](#rfc.section.6.1)
*   6.2. [Programming Language Independence](#rfc.section.6.2)
*   6.3. [Mathematical Integers](#rfc.section.6.3)
*   6.4. [Extending JSON Schema](#rfc.section.6.4)

*   7\. [The "$schema" Keyword](#rfc.section.7)
*   8\. [Base URI and Dereferencing](#rfc.section.8)

*   8.1. [Initial Base URI](#rfc.section.8.1)
*   8.2. [The "$id" Keyword](#rfc.section.8.2)

*   8.2.1. [Identifying the root schema](#rfc.section.8.2.1)
*   8.2.2. [Changing the base URI within a schema file](#rfc.section.8.2.2)
*   8.2.3. [Location-independent identifiers](#rfc.section.8.2.3)
*   8.2.4. [Schema identification examples](#rfc.section.8.2.4)

*   8.3. [Schema References With "$ref"](#rfc.section.8.3)

*   8.3.1. [Loading a referenced schema](#rfc.section.8.3.1)
*   8.3.2. [Dereferencing](#rfc.section.8.3.2)

*   9\. [Comments With "$comment"](#rfc.section.9)
*   10\. [Usage for Hypermedia](#rfc.section.10)

*   10.1. [Linking to a Schema](#rfc.section.10.1)
*   10.2. [Identifying a Schema via a Media Type Parameter](#rfc.section.10.2)
*   10.3. [Usage Over HTTP](#rfc.section.10.3)

*   11\. [Security Considerations](#rfc.section.11)
*   12\. [IANA Considerations](#rfc.section.12)

*   12.1. [application/schema+json](#rfc.section.12.1)
*   12.2. [application/schema-instance+json](#rfc.section.12.2)

*   13\. [References](#rfc.references)

*   13.1. [Normative References](#rfc.references.1)
*   13.2. [Informative References](#rfc.references.2)

*   Appendix A. [Acknowledgments](#rfc.appendix.A)
*   Appendix B. [ChangeLog](#rfc.appendix.B)
*   [Authors' Addresses](#rfc.authors)

# [1.](#rfc.section.1) Introduction

JSON Schema is a JSON media type for defining the structure of JSON data. JSON Schema is intended to define validation, documentation, hyperlink navigation, and interaction control of JSON data.

This specification defines JSON Schema core terminology and mechanisms, including pointing to another JSON Schema by reference, dereferencing a JSON Schema reference, and specifying the vocabulary being used.

Other specifications define the vocabularies that perform assertions about validation, linking, annotation, navigation, and interaction.

# [2.](#rfc.section.2) Conventions and Terminology

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC 2119](#RFC2119) \[RFC2119\].

The terms "JSON", "JSON text", "JSON value", "member", "element", "object", "array", "number", "string", "boolean", "true", "false", and "null" in this document are to be interpreted as defined in [RFC 7159](#RFC7159) \[RFC7159\].

# [3.](#rfc.section.3) Overview

This document proposes a new media type "application/schema+json" to identify a JSON Schema for describing JSON data. It also proposes a further optional media type, "application/schema-instance+json", to provide additional integration features. JSON Schemas are themselves JSON documents. This, and related specifications, define keywords allowing authors to describe JSON data in several ways.

# [3.1.](#rfc.section.3.1) Validation

JSON Schema describes the structure of a JSON document (for instance, required properties and length limitations). Applications can use this information to validate instances (check that constraints are met), or inform interfaces to collect user input such that the constraints are satisfied.

Validation behaviour and keywords are specified in [a separate document](#json-schema-validation) \[json-schema-validation\].

# [3.2.](#rfc.section.3.2) Annotation

JSON Schema can annotate an instance with information, whenever the instance validates against the schema object containing the annotation, and all of its parent schema objects.

Detailed annotation behavior, along with a small set of basic annotation keywords are defined in [the validation specification](#json-schema-validation) \[json-schema-validation\].

# [3.3.](#rfc.section.3.3) Hypermedia and Linking

JSON Hyper-Schema describes the hypertext structure of a JSON document. This includes link relations from the instance to other resources, interpretation of instances as multimedia data, and submission data required to use an API.

Hyper-schema behaviour and keywords are specified in [a separate document](#json-hyper-schema) \[json-hyper-schema\].

# [4.](#rfc.section.4) Definitions

# [4.1.](#rfc.section.4.1) JSON Document

A JSON document is an information resource (series of octets) described by the application/json media type.

In JSON Schema, the terms "JSON document", "JSON text", and "JSON value" are interchangeable because of the data model it defines.

JSON Schema is only defined over JSON documents. However, any document or memory structure that can be parsed into or processed according to the JSON Schema data model can be interpreted against a JSON Schema, including media types like [CBOR](#RFC7049) \[RFC7049\].

# [4.2.](#rfc.section.4.2) Instance

A JSON document to which a schema is applied is known as an "instance".

# [4.2.1.](#rfc.section.4.2.1) Instance Data Model

JSON Schema interprets documents according to a data model. A JSON value interpreted according to this data model is called an "instance".

An instance has one of six primitive types, and a range of possible values depending on the type:

null:

A JSON "null" production

boolean:

A "true" or "false" value, from the JSON "true" or "false" productions

object:

An unordered set of properties mapping a string to an instance, from the JSON "object" production

array:

An ordered list of instances, from the JSON "array" production

number:

An arbitrary-precision, base-10 decimal number value, from the JSON "number" production

string:

A string of Unicode code points, from the JSON "string" production

Whitespace and formatting concerns, including different lexical representations of numbers that are equal within the data model, are thus outside the scope of JSON Schema. JSON Schema [vocabularies](#vocabulary) \[vocabulary\] that wish to work with such differences in lexical representations SHOULD define keywords to precisely interpret formatted strings within the data model rather than relying on having the original JSON representation Unicode characters available.

Since an object cannot have two properties with the same key, behavior for a JSON document that tries to define two properties (the "member" production) with the same key (the "string" production) in a single object is undefined.

Note that JSON Schema vocabularies are free to define their own extended type system. This should not be confused with the core data model types defined here. As an example, "integer" is a reasonable type for a vocabulary to define as a value for a keyword, but the data model makes no distinction between integers and other numbers.

# [4.2.2.](#rfc.section.4.2.2) Instance Media Types

JSON Schema is designed to fully work with "application/json" documents, as well as media types using the "+json" structured syntax suffix.

Some functionality that is useful for working with schemas is defined by each media type, namely media type parameters and URI fragment identifier syntax and semantics. These features are useful in content negotiation and in calculating URIs for specific locations within an instance, respectively.

This specification defines the "application/schema-instance+json" media type in order to allow instance authors to take full advantage of parameters and fragment identifiers for these purposes.

# [4.2.3.](#rfc.section.4.2.3) Instance Equality

Two JSON instances are said to be equal if and only if they are of the same type and have the same value according to the data model. Specifically, this means:

*   both are null; or
*   both are true; or
*   both are false; or
*   both are strings, and are the same codepoint-for-codepoint; or
*   both are numbers, and have the same mathematical value; or
*   both are arrays, and have an equal value item-for-item; or
*   both are objects, and each property in one has exactly one property with a key equal to the other's, and that other property has an equal value.

Implied in this definition is that arrays must be the same length, objects must have the same number of members, properties in objects are unordered, there is no way to define multiple properties with the same key, and mere formatting differences (indentation, placement of commas, trailing zeros) are insignificant.

# [4.3.](#rfc.section.4.3) JSON Schema Documents

A JSON Schema document, or simply a schema, is a JSON document used to describe an instance. A schema is itself interpreted as an instance, but SHOULD always be given the media type "application/schema+json" rather than "application/schema-instance+json". The "application/schema+json" media type is defined to offer a superset of the media type parameter and fragment identifier syntax and semantics provided by "application/schema-instance+json".

# [4.3.1.](#rfc.section.4.3.1) JSON Schema Values and Keywords

A JSON Schema MUST be an object or a boolean.

Object properties that are applied to the instance are called keywords, or schema keywords. Broadly speaking, keywords fall into one or both of two categories:

assertions:

produce a boolean result when applied to an instance

annotations:

attach information to an instance for application use

Keywords may fall into either or both categories. Extension keywords, meaning those defined outside of this document and its companions, are free to define other behaviors as well.

The boolean schema values "true" and "false" are trivial assertions that always return themselves regardless of the instance value. As an example, in terms of the validation vocabulary, boolean schemas are equivalent to the following behaviors:

true:

Always passes validation, as if the empty schema {}

false:

Always fails validation, as if the schema { "not":{} }

A JSON Schema MAY contain properties which are not schema keywords. Unknown keywords SHOULD be ignored.

An empty schema is a JSON Schema with no properties, or only unknown properties.

# [4.3.2.](#rfc.section.4.3.2) [JSON Schema Vocabularies](#vocabulary)

A JSON Schema vocabulary is a set of keywords defined for a particular purpose. The vocabulary specifies the meaning of its keywords as assertions, annotations, and/or any vocabulary-defined keyword category. The two companion standards to this document each define a vocabulary: One for instance validation, and one for hypermedia annotations. Vocabularies are the primary mechanism for extensibility within the JSON Schema media type.

Vocabularies may be defined by any entity. Vocabulary authors SHOULD take care to avoid keyword name collisions if the vocabulary is intended for broad use, and potentially combined with other vocabularies. JSON Schema does not provide any formal namespacing system, but also does not constrain keyword names, allowing for any number of namespacing approaches.

Vocabularies may build on each other, such as by defining the behavior of their keywords with respect to the behavior of keywords from another vocabulary, or by using a keyword from another vocabulary with a restricted or expanded set of acceptable values. Not all such vocabulary re-use will result in a new vocabulary that is compatible with the vocabulary on which it is built. Vocabulary authors SHOULD clearly document what level of compatibility, if any, is expected.

A schema that itself describes a schema is called a meta-schema. Meta-schemas are used to validate JSON Schemas and specify which vocabulary it is using. \[CREF1\]Currently, only a single meta-schema may be specified per schema, meaning that in order to use multiple vocabularies, a meta-schema must be written that encompasses all of them. The hyper-schema meta-schema is an example of this, as it encompasses the validation vocabulary as well as the hypermedia vocabulary.

# [4.3.3.](#rfc.section.4.3.3) Root Schema and Subschemas

The root schema is the schema that comprises the entire JSON document in question.

Some keywords take schemas themselves, allowing JSON Schemas to be nested:

{
    "title": "root",
    "items": {
        "title": "array item"
    }
}

                        

In this example document, the schema titled "array item" is a subschema, and the schema titled "root" is the root schema.

As with the root schema, a subschema is either an object or a boolean.

# [5.](#rfc.section.5) [Fragment Identifiers](#fragments)

In accordance with section 3.1 of [\[RFC6839\]](#RFC6839), the syntax and semantics of fragment identifiers specified for any +json media type SHOULD be as specified for "application/json". (At publication of this document, there is no fragment identification syntax defined for "application/json".)

Additionally, the "application/schema+json" media type supports two fragment identifier structures: plain names and JSON Pointers. The "application/schema-instance+json" media type supports one fragment identifier structure: JSON Pointers.

The use of JSON Pointers as URI fragment identifiers is described in [RFC 6901](#RFC6901) \[RFC6901\]. For "application/schema+json", which supports two fragment identifier syntaxes, fragment identifiers matching the JSON Pointer syntax, including the empty string, MUST be interpreted as JSON Pointer fragment identifiers.

Per the W3C's [best practices for fragment identifiers](#W3C.WD-fragid-best-practices-20121025) \[W3C.WD-fragid-best-practices-20121025\], plain name fragment identifiers in "application/schema+json" are reserved for referencing locally named schemas. All fragment identifiers that do not match the JSON Pointer syntax MUST be interpreted as plain name fragment identifiers.

Defining and referencing a plain name fragment identifier within an "application/schema+json" document are specified in the ["$id" keyword](#id-keyword) \[id-keyword\] section.

# [6.](#rfc.section.6) General Considerations

# [6.1.](#rfc.section.6.1) Range of JSON Values

An instance may be any valid JSON value as defined by [JSON](#RFC7159) \[RFC7159\]. JSON Schema imposes no restrictions on type: JSON Schema can describe any JSON value, including, for example, null.

# [6.2.](#rfc.section.6.2) [Programming Language Independence](#language)

JSON Schema is programming language agnostic, and supports the full range of values described in the data model. Be aware, however, that some languages and JSON parsers may not be able to represent in memory the full range of values describable by JSON.

# [6.3.](#rfc.section.6.3) [Mathematical Integers](#integers)

Some programming languages and parsers use different internal representations for floating point numbers than they do for integers.

For consistency, integer JSON numbers SHOULD NOT be encoded with a fractional part.

# [6.4.](#rfc.section.6.4) Extending JSON Schema

Implementations MAY define additional keywords to JSON Schema. Save for explicit agreement, schema authors SHALL NOT expect these additional keywords to be supported by peer implementations. Implementations SHOULD ignore keywords they do not support.

Authors of extensions to JSON Schema are encouraged to write their own meta-schemas, which extend the existing meta-schemas using "allOf". This extended meta-schema SHOULD be referenced using the "$schema" keyword, to allow tools to follow the correct behaviour.

Note that the recursive nature of meta-schemas requires re-defining recursive keywords in the extended meta-schema, as can be seen in the JSON Hyper-Schema meta-schema.

# [7.](#rfc.section.7) The "$schema" Keyword

The "$schema" keyword is both used as a JSON Schema version identifier and the location of a resource which is itself a JSON Schema, which describes any schema written for this particular version.

The value of this keyword MUST be a [URI](#RFC3986) \[RFC3986\] (containing a scheme) and this URI MUST be normalized. The current schema MUST be valid against the meta-schema identified by this URI.

If this URI identifies a retrievable resource, that resource SHOULD be of media type "application/schema+json".

The "$schema" keyword SHOULD be used in a root schema. It MUST NOT appear in subschemas.

\[CREF2\]Using multiple "$schema" keywords in the same document would imply that the vocabulary and therefore behavior can change within a document. This would necessitate resolving a number of implementation concerns that have not yet been clearly defined. So, while the pattern of using "$schema" only in root schemas is likely to remain the best practice for schema authoring, implementation behavior is subject to be revised or liberalized in future drafts.

Values for this property are defined in other documents and by other parties. JSON Schema implementations SHOULD implement support for current and previous published drafts of JSON Schema vocabularies as deemed reasonable.

# [8.](#rfc.section.8) Base URI and Dereferencing

To differentiate between schemas in a vast ecosystem, schemas are identified by [URI](#RFC3986) \[RFC3986\], and can embed references to other schemas by specifying their URI.

# [8.1.](#rfc.section.8.1) Initial Base URI

[RFC3986 Section 5.1](#RFC3986) \[RFC3986\] defines how to determine the default base URI of a document.

Informatively, the initial base URI of a schema is the URI at which it was found, or a suitable substitute URI if none is known.

# [8.2.](#rfc.section.8.2) [The "$id" Keyword](#id-keyword)

The "$id" keyword defines a URI for the schema, and the base URI that other URI references within the schema are resolved against. A subschema's "$id" is resolved against the base URI of its parent schema. If no parent sets an explicit base with "$id", the base URI is that of the entire document, as determined per [RFC 3986 section 5](#RFC3986) \[RFC3986\].

If present, the value for this keyword MUST be a string, and MUST represent a valid [URI-reference](#RFC3986) \[RFC3986\]. This value SHOULD be normalized, and SHOULD NOT be an empty fragment <#> or an empty string <>.

# [8.2.1.](#rfc.section.8.2.1) Identifying the root schema

The root schema of a JSON Schema document SHOULD contain an "$id" keyword with an [absolute-URI](#RFC3986) \[RFC3986\] (containing a scheme, but no fragment), or this absolute URI but with an empty fragment.

# [8.2.2.](#rfc.section.8.2.2) Changing the base URI within a schema file

When an "$id" sets the base URI, the object containing that "$id" and all of its subschemas can be identified by using a JSON Pointer fragment starting from that location. This is true even of subschemas that further change the base URI. Therefore, a single subschema may be accessible by multiple URIs, each consisting of base URI declared in the subschema or a parent, along with a JSON Pointer fragment identifying the path from the schema object that declares the base to the subschema being identified. Examples of this are shown in section [8.2.4](#idExamples).

# [8.2.3.](#rfc.section.8.2.3) Location-independent identifiers

Using JSON Pointer fragments requires knowledge of the structure of the schema. When writing schema documents with the intention to provide re-usable schemas, it may be preferable to use a plain name fragment that is not tied to any particular structural location. This allows a subschema to be relocated without requiring JSON Pointer references to be updated.

To specify such a subschema identifier, the "$id" keyword is set to a URI reference with a plain name fragment (not a JSON Pointer fragment). This value MUST begin with the number sign that specifies a fragment ("#"), then a letter (\[A-Za-z\]), followed by any number of letters, digits (\[0-9\]), hyphens ("-"), underscores ("\_"), colons (":"), or periods (".").

The effect of using a fragment in "$id" that isn't blank or doesn't follow the plain name syntax is undefined. \[CREF3\]How should an "$id" URI reference containing a fragment with other components be interpreted? There are two cases: when the other components match the current base URI and when they change the base URI.

# [8.2.4.](#rfc.section.8.2.4) [Schema identification examples](#idExamples)

Consider the following schema, which shows "$id" being used to identify the root schema, change the base URI for subschemas, and assign plain name fragments to subschemas:

{
    "$id": "http://example.com/root.json",
    "definitions": {
        "A": { "$id": "#foo" },
        "B": {
            "$id": "other.json",
            "definitions": {
                "X": { "$id": "#bar" },
                "Y": { "$id": "t/inner.json" }
            }
        },
        "C": {
            "$id": "urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f"
        }
    }
}

                        

The schemas at the following URI-encoded [JSON Pointers](#RFC6901) \[RFC6901\] (relative to the root schema) have the following base URIs, and are identifiable by any listed URI in accordance with Section [5](#fragments) above:

\# (document root)

http://example.com/root.json

http://example.com/root.json#

#/definitions/A

http://example.com/root.json#foo

http://example.com/root.json#/definitions/A

#/definitions/B

http://example.com/other.json

http://example.com/other.json#

http://example.com/root.json#/definitions/B

#/definitions/B/definitions/X

http://example.com/other.json#bar

http://example.com/other.json#/definitions/X

http://example.com/root.json#/definitions/B/definitions/X

#/definitions/B/definitions/Y

http://example.com/t/inner.json

http://example.com/t/inner.json#

http://example.com/other.json#/definitions/Y

http://example.com/root.json#/definitions/B/definitions/Y

#/definitions/C

urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f

urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f#

http://example.com/root.json#/definitions/C

# [8.3.](#rfc.section.8.3) Schema References With "$ref"

The "$ref" keyword is used to reference a schema, and provides the ability to validate recursive structures through self-reference.

An object schema with a "$ref" property MUST be interpreted as a "$ref" reference. The value of the "$ref" property MUST be a URI Reference. Resolved against the current URI base, it identifies the URI of a schema to use. All other properties in a "$ref" object MUST be ignored.

The URI is not a network locator, only an identifier. A schema need not be downloadable from the address if it is a network-addressable URL, and implementations SHOULD NOT assume they should perform a network operation when they encounter a network-addressable URI.

A schema MUST NOT be run into an infinite loop against a schema. For example, if two schemas "#alice" and "#bob" both have an "allOf" property that refers to the other, a naive validator might get stuck in an infinite recursive loop trying to validate the instance. Schemas SHOULD NOT make use of infinite recursive nesting like this; the behavior is undefined.

# [8.3.1.](#rfc.section.8.3.1) Loading a referenced schema

The use of URIs to identify remote schemas does not necessarily mean anything is downloaded, but instead JSON Schema implementations SHOULD understand ahead of time which schemas they will be using, and the URIs that identify them.

When schemas are downloaded, for example by a generic user-agent that doesn't know until runtime which schemas to download, see [Usage for Hypermedia](#hypermedia) \[hypermedia\].

Implementations SHOULD be able to associate arbitrary URIs with an arbitrary schema and/or automatically associate a schema's "$id"-given URI, depending on the trust that the validator has in the schema. Such URIs and schemas can be supplied to an implementation prior to processing instances, or may be noted within a schema document as it is processed, producing associations as shown in section [8.2.4](#idExamples).

A schema MAY (and likely will) have multiple URIs, but there is no way for a URI to identify more than one schema. When multiple schemas try to identify as the same URI, validators SHOULD raise an error condition.

# [8.3.2.](#rfc.section.8.3.2) Dereferencing

Schemas can be identified by any URI that has been given to them, including a JSON Pointer or their URI given directly by "$id". In all cases, dereferencing a "$ref" reference involves first resolving its value as a URI reference against the current base URI per [RFC 3986](#RFC3986) \[RFC3986\].

If the resulting URI identifies a schema within the current document, or within another schema document that has been made available to the implementation, then that schema SHOULD be used automatically.

For example, consider this schema:

{
    "$id": "http://example.net/root.json",
    "items": {
        "type": "array",
        "items": { "$ref": "#item" }
    },
    "definitions": {
        "single": {
            "$id": "#item",
            "type": "object",
            "additionalProperties": { "$ref": "other.json" }
        }
    }
}

                        

When an implementation encounters the <#/definitions/single> schema, it resolves the "$id" URI reference against the current base URI to form <http://example.net/root.json#item>.

When an implementation then looks inside the <#/items> schema, it encounters the <#item> reference, and resolves this to <http://example.net/root.json#item>, which it has seen defined in this same document and can therefore use automatically.

When an implementation encounters the reference to "other.json", it resolves this to <http://example.net/other.json>, which is not defined in this document. If a schema with that identifier has otherwise been supplied to the implementation, it can also be used automatically. \[CREF4\]What should implementations do when the referenced schema is not known? Are there circumstances in which automatic network dereferencing is allowed? A same origin policy? A user-configurable option? In the case of an evolving API described by Hyper-Schema, it is expected that new schemas will be added to the system dynamically, so placing an absolute requirement of pre-loading schema documents is not feasible.

# [9.](#rfc.section.9) Comments With "$comment"

This keyword is reserved for comments from schema authors to readers or maintainers of the schema. The value of this keyword MUST be a string. Implementations MUST NOT present this string to end users. Tools for editing schemas SHOULD support displaying and editing this keyword. The value of this keyword MAY be used in debug or error output which is intended for developers making use of schemas. Schema vocabularies SHOULD allow "$comment" within any object containing vocabulary keywords. Implementations MAY assume "$comment" is allowed unless the vocabulary specifically forbids it. Vocabularies MUST NOT specify any effect of "$comment" beyond what is described in this specification. Tools that translate other media types or programming languages to and from application/schema+json MAY choose to convert that media type or programming language's native comments to or from "$comment" values. The behavior of such translation when both native comments and "$comment" properties are present is implementation-dependent. Implementations SHOULD treat "$comment" identically to an unknown extension keyword. They MAY strip "$comment" values at any point during processing. In particular, this allows for shortening schemas when the size of deployed schemas is a concern. Implementations MUST NOT take any other action based on the presence, absence, or contents of "$comment" properties.

# [10.](#rfc.section.10) [Usage for Hypermedia](#hypermedia)

JSON has been adopted widely by HTTP servers for automated APIs and robots. This section describes how to enhance processing of JSON documents in a more RESTful manner when used with protocols that support media types and [Web linking](#RFC8288) \[RFC8288\].

# [10.1.](#rfc.section.10.1) Linking to a Schema

It is RECOMMENDED that instances described by a schema provide a link to a downloadable JSON Schema using the link relation "describedby", as defined by [Linked Data Protocol 1.0, section 8.1](#W3C.REC-ldp-20150226) \[W3C.REC-ldp-20150226\].

In HTTP, such links can be attached to any response using the [Link header](#RFC8288) \[RFC8288\]. An example of such a header would be:

Link: <http://example.com/my-hyper-schema#>; rel="describedby"

                    

# [10.2.](#rfc.section.10.2) [Identifying a Schema via a Media Type Parameter](#parameter)

Media types MAY allow for a "schema" media type parameter, which gives HTTP servers the ability to perform Content-Type Negotiation based on schema. The media-type parameter MUST be a whitespace-separated list of URIs (i.e. relative references are invalid).

When using the media type application/schema-instance+json, the "schema" parameter MUST be supplied.

The schema URI is opaque and SHOULD NOT automatically be dereferenced. If the implementation does not understand the semantics of the provided schema, the implementation can instead follow the "describedby" links, if any, which may provide information on how to handle the schema. Since "schema" doesn't necessarily point to a network location, the "describedby" relation is used for linking to a downloadable schema. However, for simplicity, schema authors should make these URIs point to the same resource when possible.

In HTTP, the media-type parameter would be sent inside the Content-Type header:

Content-Type: application/json;
          schema="http://example.com/my-hyper-schema#"

                    

Multiple schemas are whitespace separated:

Content-Type: application/json;
          schema="http://example.com/alice http://example.com/bob"

                    

\[CREF5\]This paragraph assumes that we can register a "schema" link relation. Should we instead specify something like "tag:json-schema.org,2017:schema" for now? HTTP can also send the "schema" in a Link, though this may impact media-type semantics and Content-Type negotiation if this replaces the media-type parameter entirely:

Link: </alice>;rel="schema", </bob>;rel="schema"

                    

# [10.3.](#rfc.section.10.3) Usage Over HTTP

When used for hypermedia systems over a network, [HTTP](#RFC7231) \[RFC7231\] is frequently the protocol of choice for distributing schemas. Misbehaving clients can pose problems for server maintainers if they pull a schema over the network more frequently than necessary, when it's instead possible to cache a schema for a long period of time.

HTTP servers SHOULD set long-lived caching headers on JSON Schemas. HTTP clients SHOULD observe caching headers and not re-request documents within their freshness period. Distributed systems SHOULD make use of a shared cache and/or caching proxy.

User-Agent: product-name/5.4.1 so-cool-json-schema/1.0.2 curl/7.43.0

                        

Clients SHOULD set or prepend a User-Agent header specific to the JSON Schema implementation or software product. Since symbols are listed in decreasing order of significance, the JSON Schema library name/version should precede the more generic HTTP library name (if any). For example:

Clients SHOULD be able to make requests with a "From" header so that server operators can contact the owner of a potentially misbehaving script.

# [11.](#rfc.section.11) [Security Considerations](#security)

Both schemas and instances are JSON values. As such, all security considerations defined in [RFC 7159](#RFC7159) \[RFC7159\] apply.

Instances and schemas are both frequently written by untrusted third parties, to be deployed on public Internet servers. Validators should take care that the parsing and validating against schemas doesn't consume excessive system resources. Validators MUST NOT fall into an infinite loop.

Servers MUST ensure that malicious parties can't change the functionality of existing schemas by uploading a schema with an pre-existing or very similar "$id".

Individual JSON Schema vocabularies are liable to also have their own security considerations. Consult the respective specifications for more information.

Schema authors should take care with "$comment" contents, as a malicious implementation can display them to end-users in violation of a spec, or fail to strip them if such behavior is expected.

A malicious schema author could place executable code or other dangerous material within a "$comment". Implementations MUST NOT parse or otherwise take action based on "$comment" contents.

# [12.](#rfc.section.12) IANA Considerations

# [12.1.](#rfc.section.12.1) application/schema+json

The proposed MIME media type for JSON Schema is defined as follows:

*   Type name: application
*   Subtype name: schema+json
*   Required parameters: N/A
*   Encoding considerations: Encoding considerations are identical to those specified for the "application/json" media type. See [JSON](#RFC7159) \[RFC7159\].
*   Security considerations: See Section [11](#security) above.
*   Interoperability considerations: See Sections [6.2](#language) and [6.3](#integers) above.
*   Fragment identifier considerations: See Section [5](#fragments)

# [12.2.](#rfc.section.12.2) application/schema-instance+json

The proposed MIME media type for JSON Schema Instances that require a JSON Schema-specific media type is defined as follows:

*   Type name: application
*   Subtype name: schema-instance+json
*   Required parameters:
    
    schema:
    
    A non-empty list of space-separated URIs, each identifying a JSON Schema resource. The instance SHOULD successfully validate against at least one of these schemas. Non-validating schemas MAY be included for purposes such as allowing clients to make use of older versions of a schema as long as the runtime instance validates against that older version.
    
*   Encoding considerations: Encoding considerations are identical to those specified for the "application/json" media type. See [JSON](#RFC7159) \[RFC7159\].
*   Security considerations: See Section [11](#security) above.
*   Interoperability considerations: See Sections [6.2](#language) and [6.3](#integers) above.
*   Fragment identifier considerations: See Section [5](#fragments)

# [13.](#rfc.references) References

# [13.1.](#rfc.references.1) Normative References

<table><tbody><tr><td class="reference"><b id="RFC2119">[RFC2119]</b></td><td class="top"><a>Bradner, S.</a>, "<a href="http://tools.ietf.org/html/rfc2119">Key words for use in RFCs to Indicate Requirement Levels</a>", BCP 14, RFC 2119, DOI 10.17487/RFC2119, March 1997.</td></tr><tr><td class="reference"><b id="RFC3986">[RFC3986]</b></td><td class="top"><a>Berners-Lee, T.</a>, <a>Fielding, R.</a> and <a>L. Masinter</a>, "<a href="http://tools.ietf.org/html/rfc3986">Uniform Resource Identifier (URI): Generic Syntax</a>", STD 66, RFC 3986, DOI 10.17487/RFC3986, January 2005.</td></tr><tr><td class="reference"><b id="RFC6839">[RFC6839]</b></td><td class="top"><a>Hansen, T.</a> and <a>A. Melnikov</a>, "<a href="http://tools.ietf.org/html/rfc6839">Additional Media Type Structured Syntax Suffixes</a>", RFC 6839, DOI 10.17487/RFC6839, January 2013.</td></tr><tr><td class="reference"><b id="RFC6901">[RFC6901]</b></td><td class="top"><a>Bryan, P.</a>, <a>Zyp, K.</a> and <a>M. Nottingham</a>, "<a href="http://tools.ietf.org/html/rfc6901">JavaScript Object Notation (JSON) Pointer</a>", RFC 6901, DOI 10.17487/RFC6901, April 2013.</td></tr><tr><td class="reference"><b id="RFC7159">[RFC7159]</b></td><td class="top"><a>Bray, T.</a>, "<a href="http://tools.ietf.org/html/rfc7159">The JavaScript Object Notation (JSON) Data Interchange Format</a>", RFC 7159, DOI 10.17487/RFC7159, March 2014.</td></tr><tr><td class="reference"><b id="W3C.REC-ldp-20150226">[W3C.REC-ldp-20150226]</b></td><td class="top"><a>Speicher, S.</a>, <a>Arwe, J.</a> and <a>A. Malhotra</a>, "<a href="http://www.w3.org/TR/2015/REC-ldp-20150226">Linked Data Platform 1.0</a>", World Wide Web Consortium Recommendation REC-ldp-20150226, February 2015.</td></tr></tbody></table>

# [13.2.](#rfc.references.2) Informative References

<table><tbody><tr><td class="reference"><b id="RFC7049">[RFC7049]</b></td><td class="top"><a>Bormann, C.</a> and <a>P. Hoffman</a>, "<a href="http://tools.ietf.org/html/rfc7049">Concise Binary Object Representation (CBOR)</a>", RFC 7049, DOI 10.17487/RFC7049, October 2013.</td></tr><tr><td class="reference"><b id="RFC7231">[RFC7231]</b></td><td class="top"><a>Fielding, R.</a> and <a>J. Reschke</a>, "<a href="http://tools.ietf.org/html/rfc7231">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content</a>", RFC 7231, DOI 10.17487/RFC7231, June 2014.</td></tr><tr><td class="reference"><b id="RFC8288">[RFC8288]</b></td><td class="top"><a>Nottingham, M.</a>, "<a href="http://tools.ietf.org/html/rfc8288">Web Linking</a>", RFC 8288, DOI 10.17487/RFC8288, October 2017.</td></tr><tr><td class="reference"><b id="W3C.WD-fragid-best-practices-20121025">[W3C.WD-fragid-best-practices-20121025]</b></td><td class="top"><a>Tennison, J.</a>, "<a href="http://www.w3.org/TR/2012/WD-fragid-best-practices-20121025">Best Practices for Fragment Identifiers and Media Type Definitions</a>", World Wide Web Consortium LastCall WD-fragid-best-practices-20121025, October 2012.</td></tr><tr><td class="reference"><b id="json-schema-validation">[json-schema-validation]</b></td><td class="top"><a>Wright, A.</a>, <a title="Cloudflare, Inc.">Andrews, H.</a> and <a>G. Luff</a>, "<a href="http://tools.ietf.org/html/draft-handrews-json-schema-validation-01">JSON Schema Validation: A Vocabulary for Structural Validation of JSON</a>", Internet-Draft draft-handrews-json-schema-validation-01, November 2017.</td></tr><tr><td class="reference"><b id="json-hyper-schema">[json-hyper-schema]</b></td><td class="top"><a title="Cloudflare, Inc.">Andrews, H.</a> and <a>A. Wright</a>, "<a href="http://tools.ietf.org/html/draft-handrews-json-schema-hyperschema-01">JSON Hyper-Schema: A Vocabulary for Hypermedia Annotation of JSON</a>", Internet-Draft draft-handrews-json-schema-hyperschema-01, November 2017.</td></tr></tbody></table>

# [Appendix A.](#rfc.appendix.A) Acknowledgments

Thanks to Gary Court, Francis Galiegue, Kris Zyp, and Geraint Luff for their work on the initial drafts of JSON Schema.

Thanks to Jason Desrosiers, Daniel Perrett, Erik Wilde, Ben Hutton, Evgeny Poberezkin, Brad Bowman, Gowry Sankar, Donald Pipowitch, and Dave Finlay for their submissions and patches to the document.

# [Appendix B.](#rfc.appendix.B) ChangeLog

\[CREF6\]This section to be removed before leaving Internet-Draft status.

draft-handrews-json-schema-01

*   This draft is purely a clarification with no functional changes
*   Emphasized annotations as a primary usage of JSON Schema
*   Clarified $id by use cases
*   Exhaustive schema identification examples
*   Replaced "external referencing" with how and when an implementation might know of a schema from another doucment
*   Replaced "internal referencing" with how an implementation should recognized schema identifiers during parsing
*   Dereferencing the former "internal" or "external" references is always the same process
*   Minor formatting improvements

draft-handrews-json-schema-00

*   Make the concept of a schema keyword vocabulary more clear
*   Note that the concept of "integer" is from a vocabulary, not the data model
*   Classify keywords as assertions or annotations and describe their general behavior
*   Explain the boolean schemas in terms of generalized assertions
*   Reserve "$comment" for non-user-visible notes about the schema
*   Wording improvements around "$id" and fragments
*   Note the challenges of extending meta-schemas with recursive references
*   Add "application/schema-instance+json" media type
*   Recommend a "schema" link relation / parameter instead of "profile"

draft-wright-json-schema-01

*   Updated intro
*   Allowed for any schema to be a boolean
*   "$schema" SHOULD NOT appear in subschemas, although that may change
*   Changed "id" to "$id"; all core keywords prefixed with "$"
*   Clarify and formalize fragments for application/schema+json
*   Note applicability to formats such as CBOR that can be represented in the JSON data model

draft-wright-json-schema-00

*   Updated references to JSON
*   Updated references to HTTP
*   Updated references to JSON Pointer
*   Behavior for "id" is now specified in terms of RFC3986
*   Aligned vocabulary usage for URIs with RFC3986
*   Removed reference to draft-pbryan-zyp-json-ref-03
*   Limited use of "$ref" to wherever a schema is expected
*   Added definition of the "JSON Schema data model"
*   Added additional security considerations
*   Defined use of subschema identifiers for "id"
*   Rewrote section on usage with HTTP
*   Rewrote section on usage with rel="describedBy" and rel="profile"
*   Fixed numerous invalid examples

draft-zyp-json-schema-04

*   Salvaged from draft v3.
*   Split validation keywords into separate document.
*   Split hypermedia keywords into separate document.
*   Initial post-split draft.
*   Mandate the use of JSON Reference, JSON Pointer.
*   Define the role of "id". Define URI resolution scope.
*   Add interoperability considerations.

draft-zyp-json-schema-00

*   Initial draft.

# [Authors' Addresses](#rfc.authors)

Austin Wright (editor) Wright EMail: [\[email protected\]](/cdn-cgi/l/email-protection#a7c6c6c6e7c5ddc1df89c9c2d3)

Henry Andrews (editor) Andrews Cloudflare, Inc. San Francisco, CA USA EMail: [\[email protected\]](/cdn-cgi/l/email-protection#fe969b908c87be9d92918b9a98929f8c9bd09d9193)