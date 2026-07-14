   JSON Schema: A Media Type for Describing JSON Documents                

| Internet-Draft | JSON Schema | June 2022 |
| --- | --- | --- |
| Wright, et al. | Expires 18 December 2022 | \[Page\] |

Workgroup:

Internet Engineering Task Force

Internet-Draft:

draft-bhutton-json-schema-01

Published:

16 June 2022

Intended Status:

Informational

Expires:

18 December 2022

Authors:

A. Wright, Ed.

H. Andrews, Ed.

B. Hutton, Ed.

Postman

G. Dennis

# JSON Schema: A Media Type for Describing JSON Documents

## [Abstract](#abstract)

JSON Schema defines the media type "application/schema+json", a JSON-based format for describing the structure of JSON data. JSON Schema asserts what a JSON document must look like, ways to extract information from it, and how to interact with it. The "application/schema-instance+json" media type provides additional feature-rich integration with "application/schema+json" beyond what can be offered for "application/json" documents.[¶](#section-abstract-1)

## [Note to Readers](#name-note-to-readers)

The issues list for this draft can be found at [https://github.com/json-schema-org/json-schema-spec/issues](https://github.com/json-schema-org/json-schema-spec/issues).[¶](#section-note.1-1)

For additional information, see [https://json-schema.org/](https://json-schema.org/).[¶](#section-note.1-2)

To provide feedback, use this issue tracker, the communication methods listed on the homepage, or email the document editors.[¶](#section-note.1-3)

## [Status of This Memo](#name-status-of-this-memo)

This Internet-Draft is submitted in full conformance with the provisions of BCP 78 and BCP 79.[¶](#section-boilerplate.1-1)

Internet-Drafts are working documents of the Internet Engineering Task Force (IETF). Note that other groups may also distribute working documents as Internet-Drafts. The list of current Internet-Drafts is at [https://datatracker.ietf.org/drafts/current/](https://datatracker.ietf.org/drafts/current/).[¶](#section-boilerplate.1-2)

Internet-Drafts are draft documents valid for a maximum of six months and may be updated, replaced, or obsoleted by other documents at any time. It is inappropriate to use Internet-Drafts as reference material or to cite them other than as "work in progress."[¶](#section-boilerplate.1-3)

This Internet-Draft will expire on 18 December 2022.[¶](#section-boilerplate.1-4)

## [Copyright Notice](#name-copyright-notice)

Copyright (c) 2022 IETF Trust and the persons identified as the document authors. All rights reserved.[¶](#section-boilerplate.2-1)

This document is subject to BCP 78 and the IETF Trust's Legal Provisions Relating to IETF Documents ([https://trustee.ietf.org/license-info](https://trustee.ietf.org/license-info)) in effect on the date of publication of this document. Please review these documents carefully, as they describe your rights and restrictions with respect to this document. Code Components extracted from this document must include Revised BSD License text as described in Section 4.e of the Trust Legal Provisions and are provided without warranty as described in the Revised BSD License.[¶](#section-boilerplate.2-2)

[▲](#)

## [Table of Contents](#name-table-of-contents)

*   [1](#section-1).  [Introduction](#name-introduction)
    
*   [2](#section-2).  [Conventions and Terminology](#name-conventions-and-terminology)
    
*   [3](#section-3).  [Overview](#name-overview)
    
*   [4](#section-4).  [Definitions](#name-definitions)
    
    *   [4.1](#section-4.1).  [JSON Document](#name-json-document)
        
    *   [4.2](#section-4.2).  [Instance](#name-instance)
        
        *   [4.2.1](#section-4.2.1).  [Instance Data Model](#name-instance-data-model)
            
        *   [4.2.2](#section-4.2.2).  [Instance Equality](#name-instance-equality)
            
        *   [4.2.3](#section-4.2.3).  [Non-JSON Instances](#name-non-json-instances)
            
    *   [4.3](#section-4.3).  [JSON Schema Documents](#name-json-schema-documents)
        
        *   [4.3.1](#section-4.3.1).  [JSON Schema Objects and Keywords](#name-json-schema-objects-and-key)
            
        *   [4.3.2](#section-4.3.2).  [Boolean JSON Schemas](#name-boolean-json-schemas)
            
        *   [4.3.3](#section-4.3.3).  [Schema Vocabularies](#name-schema-vocabularies)
            
        *   [4.3.4](#section-4.3.4).  [Meta-Schemas](#name-meta-schemas)
            
        *   [4.3.5](#section-4.3.5).  [Root Schema and Subschemas and Resources](#name-root-schema-and-subschemas-)
            
*   [5](#section-5).  [Fragment Identifiers](#name-fragment-identifiers)
    
*   [6](#section-6).  [General Considerations](#name-general-considerations)
    
    *   [6.1](#section-6.1).  [Range of JSON Values](#name-range-of-json-values)
        
    *   [6.2](#section-6.2).  [Programming Language Independence](#name-programming-language-indepe)
        
    *   [6.3](#section-6.3).  [Mathematical Integers](#name-mathematical-integers)
        
    *   [6.4](#section-6.4).  [Regular Expressions](#name-regular-expressions)
        
    *   [6.5](#section-6.5).  [Extending JSON Schema](#name-extending-json-schema)
        
*   [7](#section-7).  [Keyword Behaviors](#name-keyword-behaviors)
    
    *   [7.1](#section-7.1).  [Lexical Scope and Dynamic Scope](#name-lexical-scope-and-dynamic-s)
        
    *   [7.2](#section-7.2).  [Keyword Interactions](#name-keyword-interactions)
        
    *   [7.3](#section-7.3).  [Default Behaviors](#name-default-behaviors)
        
    *   [7.4](#section-7.4).  [Identifiers](#name-identifiers)
        
    *   [7.5](#section-7.5).  [Applicators](#name-applicators)
        
        *   [7.5.1](#section-7.5.1).  [Referenced and Referencing Schemas](#name-referenced-and-referencing-)
            
    *   [7.6](#section-7.6).  [Assertions](#name-assertions)
        
        *   [7.6.1](#section-7.6.1).  [Assertions and Instance Primitive Types](#name-assertions-and-instance-pri)
            
    *   [7.7](#section-7.7).  [Annotations](#name-annotations)
        
        *   [7.7.1](#section-7.7.1).  [Collecting Annotations](#name-collecting-annotations)
            
    *   [7.8](#section-7.8).  [Reserved Locations](#name-reserved-locations)
        
    *   [7.9](#section-7.9).  [Loading Instance Data](#name-loading-instance-data)
        
*   [8](#section-8).  [The JSON Schema Core Vocabulary](#name-the-json-schema-core-vocabu)
    
    *   [8.1](#section-8.1).  [Meta-Schemas and Vocabularies](#name-meta-schemas-and-vocabulari)
        
        *   [8.1.1](#section-8.1.1).  [The "$schema" Keyword](#name-the-schema-keyword)
            
        *   [8.1.2](#section-8.1.2).  [The "$vocabulary" Keyword](#name-the-vocabulary-keyword)
            
        *   [8.1.3](#section-8.1.3).  [Updates to Meta-Schema and Vocabulary URIs](#name-updates-to-meta-schema-and-)
            
    *   [8.2](#section-8.2).  [Base URI, Anchors, and Dereferencing](#name-base-uri-anchors-and-derefe)
        
        *   [8.2.1](#section-8.2.1).  [The "$id" Keyword](#name-the-id-keyword)
            
        *   [8.2.2](#section-8.2.2).  [Defining location-independent identifiers](#name-defining-location-independe)
            
        *   [8.2.3](#section-8.2.3).  [Schema References](#name-schema-references)
            
        *   [8.2.4](#section-8.2.4).  [Schema Re-Use With "$defs"](#name-schema-re-use-with-defs)
            
    *   [8.3](#section-8.3).  [Comments With "$comment"](#name-comments-with-comment)
        
*   [9](#section-9).  [Loading and Processing Schemas](#name-loading-and-processing-sche)
    
    *   [9.1](#section-9.1).  [Loading a Schema](#name-loading-a-schema)
        
        *   [9.1.1](#section-9.1.1).  [Initial Base URI](#name-initial-base-uri)
            
        *   [9.1.2](#section-9.1.2).  [Loading a referenced schema](#name-loading-a-referenced-schema)
            
        *   [9.1.3](#section-9.1.3).  [Detecting a Meta-Schema](#name-detecting-a-meta-schema)
            
    *   [9.2](#section-9.2).  [Dereferencing](#name-dereferencing)
        
        *   [9.2.1](#section-9.2.1).  [JSON Pointer fragments and embedded schema resources](#name-json-pointer-fragments-and-)
            
    *   [9.3](#section-9.3).  [Compound Documents](#name-compound-documents)
        
        *   [9.3.1](#section-9.3.1).  [Bundling](#name-bundling)
            
        *   [9.3.2](#section-9.3.2).  [Differing and Default Dialects](#name-differing-and-default-diale)
            
        *   [9.3.3](#section-9.3.3).  [Validating](#name-validating)
            
    *   [9.4](#section-9.4).  [Caveats](#name-caveats)
        
        *   [9.4.1](#section-9.4.1).  [Guarding Against Infinite Recursion](#name-guarding-against-infinite-r)
            
        *   [9.4.2](#section-9.4.2).  [References to Possible Non-Schemas](#name-references-to-possible-non-)
            
    *   [9.5](#section-9.5).  [Associating Instances and Schemas](#name-associating-instances-and-s)
        
        *   [9.5.1](#section-9.5.1).  [Usage for Hypermedia](#name-usage-for-hypermedia)
            
*   [10](#section-10). [A Vocabulary for Applying Subschemas](#name-a-vocabulary-for-applying-s)
    
    *   [10.1](#section-10.1).  [Keyword Independence](#name-keyword-independence)
        
    *   [10.2](#section-10.2).  [Keywords for Applying Subschemas in Place](#name-keywords-for-applying-subsc)
        
        *   [10.2.1](#section-10.2.1).  [Keywords for Applying Subschemas With Logic](#name-keywords-for-applying-subsch)
            
        *   [10.2.2](#section-10.2.2).  [Keywords for Applying Subschemas Conditionally](#name-keywords-for-applying-subsche)
            
    *   [10.3](#section-10.3).  [Keywords for Applying Subschemas to Child Instances](#name-keywords-for-applying-subschem)
        
        *   [10.3.1](#section-10.3.1).  [Keywords for Applying Subschemas to Arrays](#name-keywords-for-applying-subschema)
            
        *   [10.3.2](#section-10.3.2).  [Keywords for Applying Subschemas to Objects](#name-keywords-for-applying-subschemas)
            
*   [11](#section-11). [A Vocabulary for Unevaluated Locations](#name-a-vocabulary-for-unevaluate)
    
    *   [11.1](#section-11.1).  [Keyword Independence](#name-keyword-independence-2)
        
    *   [11.2](#section-11.2).  [unevaluatedItems](#name-unevaluateditems)
        
    *   [11.3](#section-11.3).  [unevaluatedProperties](#name-unevaluatedproperties)
        
*   [12](#section-12). [Output Formatting](#name-output-formatting)
    
    *   [12.1](#section-12.1).  [Format](#name-format)
        
    *   [12.2](#section-12.2).  [Output Formats](#name-output-formats)
        
    *   [12.3](#section-12.3).  [Minimum Information](#name-minimum-information)
        
        *   [12.3.1](#section-12.3.1).  [Keyword Relative Location](#name-keyword-relative-location)
            
        *   [12.3.2](#section-12.3.2).  [Keyword Absolute Location](#name-keyword-absolute-location)
            
        *   [12.3.3](#section-12.3.3).  [Instance Location](#name-instance-location)
            
        *   [12.3.4](#section-12.3.4).  [Error or Annotation](#name-error-or-annotation)
            
        *   [12.3.5](#section-12.3.5).  [Nested Results](#name-nested-results)
            
    *   [12.4](#section-12.4).  [Output Structure](#name-output-structure)
        
        *   [12.4.1](#section-12.4.1).  [Flag](#name-flag)
            
        *   [12.4.2](#section-12.4.2).  [Basic](#name-basic)
            
        *   [12.4.3](#section-12.4.3).  [Detailed](#name-detailed)
            
        *   [12.4.4](#section-12.4.4).  [Verbose](#name-verbose)
            
        *   [12.4.5](#section-12.4.5).  [Output validation schemas](#name-output-validation-schemas)
            
*   [13](#section-13). [Security Considerations](#name-security-considerations)
    
*   [14](#section-14). [IANA Considerations](#name-iana-considerations)
    
    *   [14.1](#section-14.1).  [application/schema+json](#name-application-schemajson)
        
    *   [14.2](#section-14.2).  [application/schema-instance+json](#name-application-schema-instance)
        
*   [15](#section-15). [References](#name-references)
    
    *   [15.1](#section-15.1).  [Normative References](#name-normative-references)
        
    *   [15.2](#section-15.2).  [Informative References](#name-informative-references)
        
*   [Appendix A](#appendix-A).  [Schema identification examples](#name-schema-identification-examp)
    
*   [Appendix B](#appendix-B).  [Manipulating schema documents and references](#name-manipulating-schema-documen)
    
    *   [B.1](#appendix-B.1).  [Bundling schema resources into a single document](#name-bundling-schema-resources-i)
        
    *   [B.2](#appendix-B.2).  [Reference removal is not always safe](#name-reference-removal-is-not-al)
        
*   [Appendix C](#appendix-C).  [Example of recursive schema extension](#name-example-of-recursive-schema)
    
*   [Appendix D](#appendix-D).  [Working with vocabularies](#name-working-with-vocabularies)
    
    *   [D.1](#appendix-D.1).  [Best practices for vocabulary and meta-schema authors](#name-best-practices-for-vocabula)
        
    *   [D.2](#appendix-D.2).  [Example meta-schema with vocabulary declarations](#name-example-meta-schema-with-vo)
        
*   [Appendix E](#appendix-E).  [References and generative use cases](#name-references-and-generative-u)
    
*   [Appendix F](#appendix-F).  [Acknowledgments](#name-acknowledgments)
    
*   [Appendix G](#appendix-G).  [ChangeLog](#name-changelog)
    
*   [](#appendix-H)[Authors' Addresses](#name-authors-addresses)
    

## [1\.](#section-1) [Introduction](#name-introduction)

JSON Schema is a JSON media type for defining the structure of JSON data. JSON Schema is intended to define validation, documentation, hyperlink navigation, and interaction control of JSON data.[¶](#section-1-1)

This specification defines JSON Schema core terminology and mechanisms, including pointing to another JSON Schema by reference, dereferencing a JSON Schema reference, specifying the dialect being used, specifying a dialect's vocabulary requirements, and defining the expected output.[¶](#section-1-2)

Other specifications define the vocabularies that perform assertions about validation, linking, annotation, navigation, and interaction.[¶](#section-1-3)

## [2\.](#section-2) [Conventions and Terminology](#name-conventions-and-terminology)

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC 2119](#RFC2119) \[[RFC2119](#RFC2119)\].[¶](#section-2-1)

The terms "JSON", "JSON text", "JSON value", "member", "element", "object", "array", "number", "string", "boolean", "true", "false", and "null" in this document are to be interpreted as defined in [RFC 8259](#RFC8259) \[[RFC8259](#RFC8259)\].[¶](#section-2-2)

## [3\.](#section-3) [Overview](#name-overview)

This document proposes a new media type "application/schema+json" to identify a JSON Schema for describing JSON data. It also proposes a further optional media type, "application/schema-instance+json", to provide additional integration features. JSON Schemas are themselves JSON documents. This, and related specifications, define keywords allowing authors to describe JSON data in several ways.[¶](#section-3-1)

JSON Schema uses keywords to assert constraints on JSON instances or annotate those instances with additional information. Additional keywords are used to apply assertions and annotations to more complex JSON data structures, or based on some sort of condition.[¶](#section-3-2)

To facilitate re-use, keywords can be organized into vocabularies. A vocabulary consists of a list of keywords, together with their syntax and semantics. A dialect is defined as a set of vocabularies and their required support identified in a meta-schema.[¶](#section-3-3)

JSON Schema can be extended either by defining additional vocabularies, or less formally by defining additional keywords outside of any vocabulary. Unrecognized individual keywords simply have their values collected as annotations, while the behavior with respect to an unrecognized vocabulary can be controlled when declaring which vocabularies are in use.[¶](#section-3-4)

This document defines a core vocabulary that MUST be supported by any implementation, and cannot be disabled. Its keywords are each prefixed with a "$" character to emphasize their required nature. This vocabulary is essential to the functioning of the "application/schema+json" media type, and is used to bootstrap the loading of other vocabularies.[¶](#section-3-5)

Additionally, this document defines a RECOMMENDED vocabulary of keywords for applying subschemas conditionally, and for applying subschemas to the contents of objects and arrays. Either this vocabulary or one very much like it is required to write schemas for non-trivial JSON instances, whether those schemas are intended for assertion validation, annotation, or both. While not part of the required core vocabulary, for maximum interoperability this additional vocabulary is included in this document and its use is strongly encouraged.[¶](#section-3-6)

Further vocabularies for purposes such as structural validation or hypermedia annotation are defined in other documents. These other documents each define a dialect collecting the standard sets of vocabularies needed to write schemas for that document's purpose.[¶](#section-3-7)

## [4\.](#section-4) [Definitions](#name-definitions)

### [4.1.](#section-4.1) [JSON Document](#name-json-document)

A JSON document is an information resource (series of octets) described by the application/json media type.[¶](#section-4.1-1)

In JSON Schema, the terms "JSON document", "JSON text", and "JSON value" are interchangeable because of the data model it defines.[¶](#section-4.1-2)

JSON Schema is only defined over JSON documents. However, any document or memory structure that can be parsed into or processed according to the JSON Schema data model can be interpreted against a JSON Schema, including media types like [CBOR](#RFC7049) \[[RFC7049](#RFC7049)\].[¶](#section-4.1-3)

### [4.2.](#section-4.2) [Instance](#name-instance)

A JSON document to which a schema is applied is known as an "instance".[¶](#section-4.2-1)

JSON Schema is defined over "application/json" or compatible documents, including media types with the "+json" structured syntax suffix.[¶](#section-4.2-2)

Among these, this specification defines the "application/schema-instance+json" media type which defines handling for fragments in the URI.[¶](#section-4.2-3)

#### [4.2.1.](#section-4.2.1) [Instance Data Model](#name-instance-data-model)

JSON Schema interprets documents according to a data model. A JSON value interpreted according to this data model is called an "instance".[¶](#section-4.2.1-1)

An instance has one of six primitive types, and a range of possible values depending on the type:[¶](#section-4.2.1-2)

null:

A JSON "null" value[¶](#section-4.2.1-3.2)

boolean:

A "true" or "false" value, from the JSON "true" or "false" value[¶](#section-4.2.1-3.4)

object:

An unordered set of properties mapping a string to an instance, from the JSON "object" value[¶](#section-4.2.1-3.6)

array:

An ordered list of instances, from the JSON "array" value[¶](#section-4.2.1-3.8)

number:

An arbitrary-precision, base-10 decimal number value, from the JSON "number" value[¶](#section-4.2.1-3.10)

string:

A string of Unicode code points, from the JSON "string" value[¶](#section-4.2.1-3.12)

Whitespace and formatting concerns, including different lexical representations of numbers that are equal within the data model, are thus outside the scope of JSON Schema. JSON Schema [vocabularies](#vocabulary) ([Section 8.1](#vocabulary)) that wish to work with such differences in lexical representations SHOULD define keywords to precisely interpret formatted strings within the data model rather than relying on having the original JSON representation Unicode characters available.[¶](#section-4.2.1-4)

Since an object cannot have two properties with the same key, behavior for a JSON document that tries to define two properties with the same key in a single object is undefined.[¶](#section-4.2.1-5)

Note that JSON Schema vocabularies are free to define their own extended type system. This should not be confused with the core data model types defined here. As an example, "integer" is a reasonable type for a vocabulary to define as a value for a keyword, but the data model makes no distinction between integers and other numbers.[¶](#section-4.2.1-6)

#### [4.2.2.](#section-4.2.2) [Instance Equality](#name-instance-equality)

Two JSON instances are said to be equal if and only if they are of the same type and have the same value according to the data model. Specifically, this means:[¶](#section-4.2.2-1)

*   both are null; or[¶](#section-4.2.2-2.1)
*   both are true; or[¶](#section-4.2.2-2.2)
*   both are false; or[¶](#section-4.2.2-2.3)
*   both are strings, and are the same codepoint-for-codepoint; or[¶](#section-4.2.2-2.4)
*   both are numbers, and have the same mathematical value; or[¶](#section-4.2.2-2.5)
*   both are arrays, and have an equal value item-for-item; or[¶](#section-4.2.2-2.6)
*   both are objects, and each property in one has exactly one property with a key equal to the other's, and that other property has an equal value.[¶](#section-4.2.2-2.7)

Implied in this definition is that arrays must be the same length, objects must have the same number of members, properties in objects are unordered, there is no way to define multiple properties with the same key, and mere formatting differences (indentation, placement of commas, trailing zeros) are insignificant.[¶](#section-4.2.2-3)

#### [4.2.3.](#section-4.2.3) [Non-JSON Instances](#name-non-json-instances)

It is possible to use JSON Schema with a superset of the JSON Schema data model, where an instance may be outside any of the six JSON data types.[¶](#section-4.2.3-1)

In this case, annotations still apply; but most validation keywords will not be useful, as they will always pass or always fail.[¶](#section-4.2.3-2)

A custom vocabulary may define support for a superset of the core data model. The schema itself may only be expressible in this superset; for example, to make use of the "const" keyword.[¶](#section-4.2.3-3)

### [4.3.](#section-4.3) [JSON Schema Documents](#name-json-schema-documents)

A JSON Schema document, or simply a schema, is a JSON document used to describe an instance. A schema can itself be interpreted as an instance, but SHOULD always be given the media type "application/schema+json" rather than "application/schema-instance+json". The "application/schema+json" media type is defined to offer a superset of the fragment identifier syntax and semantics provided by "application/schema-instance+json".[¶](#section-4.3-1)

A JSON Schema MUST be an object or a boolean.[¶](#section-4.3-2)

#### [4.3.1.](#section-4.3.1) [JSON Schema Objects and Keywords](#name-json-schema-objects-and-key)

Object properties that are applied to the instance are called keywords, or schema keywords. Broadly speaking, keywords fall into one of five categories:[¶](#section-4.3.1-1)

identifiers:

control schema identification through setting a URI for the schema and/or changing how the base URI is determined[¶](#section-4.3.1-2.2)

assertions:

produce a boolean result when applied to an instance[¶](#section-4.3.1-2.4)

annotations:

attach information to an instance for application use[¶](#section-4.3.1-2.6)

applicators:

apply one or more subschemas to a particular location in the instance, and combine or modify their results[¶](#section-4.3.1-2.8)

reserved locations:

do not directly affect results, but reserve a place for a specific purpose to ensure interoperability[¶](#section-4.3.1-2.10)

Keywords may fall into multiple categories, although applicators SHOULD only produce assertion results based on their subschemas' results. They should not define additional constraints independent of their subschemas.[¶](#section-4.3.1-3)

Keywords which are properties within the same schema object are referred to as adjacent keywords.[¶](#section-4.3.1-4)

Extension keywords, meaning those defined outside of this document and its companions, are free to define other behaviors as well.[¶](#section-4.3.1-5)

A JSON Schema MAY contain properties which are not schema keywords. Unknown keywords SHOULD be treated as annotations, where the value of the keyword is the value of the annotation.[¶](#section-4.3.1-6)

An empty schema is a JSON Schema with no properties, or only unknown properties.[¶](#section-4.3.1-7)

#### [4.3.2.](#section-4.3.2) [Boolean JSON Schemas](#name-boolean-json-schemas)

The boolean schema values "true" and "false" are trivial schemas that always produce themselves as assertion results, regardless of the instance value. They never produce annotation results.[¶](#section-4.3.2-1)

These boolean schemas exist to clarify schema author intent and facilitate schema processing optimizations. They behave identically to the following schema objects (where "not" is part of the subschema application vocabulary defined in this document).[¶](#section-4.3.2-2)

true:

Always passes validation, as if the empty schema {}[¶](#section-4.3.2-3.2)

false:

Always fails validation, as if the schema { "not": {} }[¶](#section-4.3.2-3.4)

While the empty schema object is unambiguous, there are many possible equivalents to the "false" schema. Using the boolean values ensures that the intent is clear to both human readers and implementations.[¶](#section-4.3.2-4)

#### [4.3.3.](#section-4.3.3) [Schema Vocabularies](#name-schema-vocabularies)

A schema vocabulary, or simply a vocabulary, is a set of keywords, their syntax, and their semantics. A vocabulary is generally organized around a particular purpose. Different uses of JSON Schema, such as validation, hypermedia, or user interface generation, will involve different sets of vocabularies.[¶](#section-4.3.3-1)

Vocabularies are the primary unit of re-use in JSON Schema, as schema authors can indicate what vocabularies are required or optional in order to process the schema. Since vocabularies are identified by URIs in the meta-schema, generic implementations can load extensions to support previously unknown vocabularies. While keywords can be supported outside of any vocabulary, there is no analogous mechanism to indicate individual keyword usage.[¶](#section-4.3.3-2)

A schema vocabulary can be defined by anything from an informal description to a standards proposal, depending on the audience and interoperability expectations. In particular, in order to facilitate vocabulary use within non-public organizations, a vocabulary specification need not be published outside of its scope of use.[¶](#section-4.3.3-3)

#### [4.3.4.](#section-4.3.4) [Meta-Schemas](#name-meta-schemas)

A schema that itself describes a schema is called a meta-schema. Meta-schemas are used to validate JSON Schemas and specify which vocabularies they are using.[¶](#section-4.3.4-1)

Typically, a meta-schema will specify a set of vocabularies, and validate schemas that conform to the syntax of those vocabularies. However, meta-schemas and vocabularies are separate in order to allow meta-schemas to validate schema conformance more strictly or more loosely than the vocabularies' specifications call for. Meta-schemas may also describe and validate additional keywords that are not part of a formal vocabulary.[¶](#section-4.3.4-2)

#### [4.3.5.](#section-4.3.5) [Root Schema and Subschemas and Resources](#name-root-schema-and-subschemas-)

A JSON Schema resource is a schema which is [canonically](#RFC6596) \[[RFC6596](#RFC6596)\] identified by an [absolute URI](#RFC3986) \[[RFC3986](#RFC3986)\]. Schema resources MAY also be identified by URIs, including URIs with fragments, if the resulting secondary resource (as defined by [section 3.5 of RFC 3986](#RFC3986) \[[RFC3986](#RFC3986)\]) is identical to the primary resource. This can occur with the empty fragment, or when one schema resource is embedded in another. Any such URIs with fragments are considered to be non-canonical.[¶](#section-4.3.5-1)

The root schema is the schema that comprises the entire JSON document in question. The root schema is always a schema resource, where the URI is determined as described in section [9.1.1](#initial-base). Note that documents that embed schemas in another format will not have a root schema resource in this sense. Exactly how such usages fit with the JSON Schema document and resource concepts will be clarified in a future draft. [¶](#section-4.3.5-2)

Some keywords take schemas themselves, allowing JSON Schemas to be nested:[¶](#section-4.3.5-3)

{
    "title": "root",
    "items": {
        "title": "array item"
    }
}

[¶](#section-4.3.5-4)

In this example document, the schema titled "array item" is a subschema, and the schema titled "root" is the root schema.[¶](#section-4.3.5-5)

As with the root schema, a subschema is either an object or a boolean.[¶](#section-4.3.5-6)

As discussed in section [8.2.1](#id-keyword), a JSON Schema document can contain multiple JSON Schema resources. When used without qualification, the term "root schema" refers to the document's root schema. In some cases, resource root schemas are discussed. A resource's root schema is its top-level schema object, which would also be a document root schema if the resource were to be extracted to a standalone JSON Schema document.[¶](#section-4.3.5-7)

Whether multiple schema resources are embedded or linked with a reference, they are processed in the same way, with the same available behaviors.[¶](#section-4.3.5-8)

## [5\.](#section-5) [Fragment Identifiers](#name-fragment-identifiers)

In accordance with section 3.1 of [RFC 6839](#RFC6839) \[[RFC6839](#RFC6839)\], the syntax and semantics of fragment identifiers specified for any +json media type SHOULD be as specified for "application/json". (At publication of this document, there is no fragment identification syntax defined for "application/json".)[¶](#section-5-1)

Additionally, the "application/schema+json" media type supports two fragment identifier structures: plain names and JSON Pointers. The "application/schema-instance+json" media type supports one fragment identifier structure: JSON Pointers.[¶](#section-5-2)

The use of JSON Pointers as URI fragment identifiers is described in [RFC 6901](#RFC6901) \[[RFC6901](#RFC6901)\]. For "application/schema+json", which supports two fragment identifier syntaxes, fragment identifiers matching the JSON Pointer syntax, including the empty string, MUST be interpreted as JSON Pointer fragment identifiers.[¶](#section-5-3)

Per the W3C's [best practices for fragment identifiers](#W3C.WD-fragid-best-practices-20121025) \[[W3C.WD-fragid-best-practices-20121025](#W3C.WD-fragid-best-practices-20121025)\], plain name fragment identifiers in "application/schema+json" are reserved for referencing locally named schemas. All fragment identifiers that do not match the JSON Pointer syntax MUST be interpreted as plain name fragment identifiers.[¶](#section-5-4)

Defining and referencing a plain name fragment identifier within an "application/schema+json" document are specified in the ["$anchor" keyword](#anchor) ([Section 8.2.2](#anchor)) section.[¶](#section-5-5)

## [6\.](#section-6) [General Considerations](#name-general-considerations)

### [6.1.](#section-6.1) [Range of JSON Values](#name-range-of-json-values)

An instance may be any valid JSON value as defined by [JSON](#RFC8259) \[[RFC8259](#RFC8259)\]. JSON Schema imposes no restrictions on type: JSON Schema can describe any JSON value, including, for example, null.[¶](#section-6.1-1)

### [6.2.](#section-6.2) [Programming Language Independence](#name-programming-language-indepe)

JSON Schema is programming language agnostic, and supports the full range of values described in the data model. Be aware, however, that some languages and JSON parsers may not be able to represent in memory the full range of values describable by JSON.[¶](#section-6.2-1)

### [6.3.](#section-6.3) [Mathematical Integers](#name-mathematical-integers)

Some programming languages and parsers use different internal representations for floating point numbers than they do for integers.[¶](#section-6.3-1)

For consistency, integer JSON numbers SHOULD NOT be encoded with a fractional part.[¶](#section-6.3-2)

### [6.4.](#section-6.4) [Regular Expressions](#name-regular-expressions)

Keywords MAY use regular expressions to express constraints, or constrain the instance value to be a regular expression. These regular expressions SHOULD be valid according to the regular expression dialect described in [ECMA-262, section 21.2.1](#ecma262) \[[ecma262](#ecma262)\].[¶](#section-6.4-1)

Regular expressions SHOULD be built with the "u" flag (or equivalent) to provide Unicode support, or processed in such a way which provides Unicode support as defined by ECMA-262.[¶](#section-6.4-2)

Furthermore, given the high disparity in regular expression constructs support, schema authors SHOULD limit themselves to the following regular expression tokens:[¶](#section-6.4-3)

*   individual Unicode characters, as defined by the [JSON specification](#RFC8259) \[[RFC8259](#RFC8259)\];[¶](#section-6.4-4.1)
*   simple character classes (\[abc\]), range character classes (\[a-z\]);[¶](#section-6.4-4.2)
*   complemented character classes (\[^abc\], \[^a-z\]);[¶](#section-6.4-4.3)
*   simple quantifiers: "+" (one or more), "\*" (zero or more), "?" (zero or one), and their lazy versions ("+?", "\*?", "??");[¶](#section-6.4-4.4)
*   range quantifiers: "{x}" (exactly x occurrences), "{x,y}" (at least x, at most y, occurrences), {x,} (x occurrences or more), and their lazy versions;[¶](#section-6.4-4.5)
*   the beginning-of-input ("^") and end-of-input ("$") anchors;[¶](#section-6.4-4.6)
*   simple grouping ("(...)") and alternation ("|").[¶](#section-6.4-4.7)

Finally, implementations MUST NOT take regular expressions to be anchored, neither at the beginning nor at the end. This means, for instance, the pattern "es" matches "expression".[¶](#section-6.4-5)

### [6.5.](#section-6.5) [Extending JSON Schema](#name-extending-json-schema)

Additional schema keywords and schema vocabularies MAY be defined by any entity. Save for explicit agreement, schema authors SHALL NOT expect these additional keywords and vocabularies to be supported by implementations that do not explicitly document such support. Implementations SHOULD treat keywords they do not support as annotations, where the value of the keyword is the value of the annotation.[¶](#section-6.5-1)

Implementations MAY provide the ability to register or load handlers for vocabularies that they do not support directly. The exact mechanism for registering and implementing such handlers is implementation-dependent.[¶](#section-6.5-2)

## [7\.](#section-7) [Keyword Behaviors](#name-keyword-behaviors)

JSON Schema keywords fall into several general behavior categories. Assertions validate that an instance satisfies constraints, producing a boolean result. Annotations attach information that applications may use in any way they see fit. Applicators apply subschemas to parts of the instance and combine their results.[¶](#section-7-1)

Extension keywords SHOULD stay within these categories, keeping in mind that annotations in particular are extremely flexible. Complex behavior is usually better delegated to applications on the basis of annotation data than implemented directly as schema keywords. However, extension keywords MAY define other behaviors for specialized purposes.[¶](#section-7-2)

Evaluating an instance against a schema involves processing all of the keywords in the schema against the appropriate locations within the instance. Typically, applicator keywords are processed until a schema object with no applicators (and therefore no subschemas) is reached. The appropriate location in the instance is evaluated against the assertion and annotation keywords in the schema object, and their results are gathered into the parent schema according to the rules of the applicator.[¶](#section-7-3)

Evaluation of a parent schema object can complete once all of its subschemas have been evaluated, although in some circumstances evaluation may be short-circuited due to assertion results. When annotations are being collected, some assertion result short-circuiting is not possible due to the need to examine all subschemas for annotation collection, including those that cannot further change the assertion result.[¶](#section-7-4)

### [7.1.](#section-7.1) [Lexical Scope and Dynamic Scope](#name-lexical-scope-and-dynamic-s)

While most JSON Schema keywords can be evaluated on their own, or at most need to take into account the values or results of adjacent keywords in the same schema object, a few have more complex behavior.[¶](#section-7.1-1)

The lexical scope of a keyword is determined by the nested JSON data structure of objects and arrays. The largest such scope is an entire schema document. The smallest scope is a single schema object with no subschemas.[¶](#section-7.1-2)

Keywords MAY be defined with a partial value, such as a URI-reference, which must be resolved against another value, such as another URI-reference or a full URI, which is found through the lexical structure of the JSON document. The "$id", "$ref", and "$dynamicRef" core keywords, and the "base" JSON Hyper-Schema keyword, are examples of this sort of behavior.[¶](#section-7.1-3)

Note that some keywords, such as "$schema", apply to the lexical scope of the entire schema resource, and therefore MUST only appear in a schema resource's root schema.[¶](#section-7.1-4)

Other keywords may take into account the dynamic scope that exists during the evaluation of a schema, typically together with an instance document. The outermost dynamic scope is the schema object at which processing begins, even if it is not a schema resource root. The path from this root schema to any particular keyword (that includes any "$ref" and "$dynamicRef" keywords that may have been resolved) is considered the keyword's "validation path."[¶](#section-7.1-5)

Lexical and dynamic scopes align until a reference keyword is encountered. While following the reference keyword moves processing from one lexical scope into a different one, from the perspective of dynamic scope, following a reference is no different from descending into a subschema present as a value. A keyword on the far side of that reference that resolves information through the dynamic scope will consider the originating side of the reference to be their dynamic parent, rather than examining the local lexically enclosing parent.[¶](#section-7.1-6)

The concept of dynamic scope is primarily used with "$dynamicRef" and "$dynamicAnchor", and should be considered an advanced feature and used with caution when defining additional keywords. It also appears when reporting errors and collected annotations, as it may be possible to revisit the same lexical scope repeatedly with different dynamic scopes. In such cases, it is important to inform the user of the dynamic path that produced the error or annotation.[¶](#section-7.1-7)

### [7.2.](#section-7.2) [Keyword Interactions](#name-keyword-interactions)

Keyword behavior MAY be defined in terms of the annotation results of [subschemas](#root) ([Section 4.3.5](#root)) and/or adjacent keywords (keywords within the same schema object) and their subschemas. Such keywords MUST NOT result in a circular dependency. Keywords MAY modify their behavior based on the presence or absence of another keyword in the same [schema object](#schema-document) ([Section 4.3](#schema-document)).[¶](#section-7.2-1)

### [7.3.](#section-7.3) [Default Behaviors](#name-default-behaviors)

A missing keyword MUST NOT produce a false assertion result, MUST NOT produce annotation results, and MUST NOT cause any other schema to be evaluated as part of its own behavioral definition. However, given that missing keywords do not contribute annotations, the lack of annotation results may indirectly change the behavior of other keywords.[¶](#section-7.3-1)

In some cases, the missing keyword assertion behavior of a keyword is identical to that produced by a certain value, and keyword definitions SHOULD note such values where known. However, even if the value which produces the default behavior would produce annotation results if present, the default behavior still MUST NOT result in annotations.[¶](#section-7.3-2)

Because annotation collection can add significant cost in terms of both computation and memory, implementations MAY opt out of this feature. Keywords that are specified in terms of collected annotations SHOULD describe reasonable alternate approaches when appropriate. This approach is demonstrated by the "[items](#items)" and "[additionalProperties](#additionalProperties)" keywords in this document.[¶](#section-7.3-3)

Note that when no such alternate approach is possible for a keyword, implementations that do not support annotation collections will not be able to support those keywords or vocabularies that contain them.[¶](#section-7.3-4)

### [7.4.](#section-7.4) [Identifiers](#name-identifiers)

Identifiers define URIs for a schema, or affect how such URIs are resolved in [references](#references) ([Section 8.2.3](#references)), or both. The Core vocabulary defined in this document defines several identifying keywords, most notably "$id".[¶](#section-7.4-1)

Canonical schema URIs MUST NOT change while processing an instance, but keywords that affect URI-reference resolution MAY have behavior that is only fully determined at runtime.[¶](#section-7.4-2)

While custom identifier keywords are possible, vocabulary designers should take care not to disrupt the functioning of core keywords. For example, the "$dynamicAnchor" keyword in this specification limits its URI resolution effects to the matching "$dynamicRef" keyword, leaving the behavior of "$ref" undisturbed.[¶](#section-7.4-3)

### [7.5.](#section-7.5) [Applicators](#name-applicators)

Applicators allow for building more complex schemas than can be accomplished with a single schema object. Evaluation of an instance against a [schema document](#schema-document) ([Section 4.3](#schema-document)) begins by applying the [root schema](#root) ([Section 4.3.5](#root)) to the complete instance document. From there, keywords known as applicators are used to determine which additional schemas are applied. Such schemas may be applied in-place to the current location, or to a child location.[¶](#section-7.5-1)

The schemas to be applied may be present as subschemas comprising all or part of the keyword's value. Alternatively, an applicator may refer to a schema elsewhere in the same schema document, or in a different one. The mechanism for identifying such referenced schemas is defined by the keyword.[¶](#section-7.5-2)

Applicator keywords also define how subschema or referenced schema boolean [assertion](#assertions) ([Section 7.6](#assertions)) results are modified and/or combined to produce the boolean result of the applicator. Applicators may apply any boolean logic operation to the assertion results of subschemas, but MUST NOT introduce new assertion conditions of their own.[¶](#section-7.5-3)

[Annotation](#annotations) ([Section 7.7](#annotations)) results are preserved along with the instance location and the location of the schema keyword, so that applications can decide how to interpret multiple values.[¶](#section-7.5-4)

#### [7.5.1.](#section-7.5.1) [Referenced and Referencing Schemas](#name-referenced-and-referencing-)

As noted in [Section 7.5](#applicators), an applicator keyword may refer to a schema to be applied, rather than including it as a subschema in the applicator's value. In such situations, the schema being applied is known as the referenced schema, while the schema containing the applicator keyword is the referencing schema.[¶](#section-7.5.1-1)

While root schemas and subschemas are static concepts based on a schema's position within a schema document, referenced and referencing schemas are dynamic. Different pairs of schemas may find themselves in various referenced and referencing arrangements during the evaluation of an instance against a schema.[¶](#section-7.5.1-2)

For some by-reference applicators, such as ["$ref"](#ref) ([Section 8.2.3.1](#ref)), the referenced schema can be determined by static analysis of the schema document's lexical scope. Others, such as "$dynamicRef" (with "$dynamicAnchor"), may make use of dynamic scoping, and therefore only be resolvable in the process of evaluating the schema with an instance.[¶](#section-7.5.1-3)

### [7.6.](#section-7.6) [Assertions](#name-assertions)

JSON Schema can be used to assert constraints on a JSON document, which either passes or fails the assertions. This approach can be used to validate conformance with the constraints, or document what is needed to satisfy them.[¶](#section-7.6-1)

JSON Schema implementations produce a single boolean result when evaluating an instance against schema assertions.[¶](#section-7.6-2)

An instance can only fail an assertion that is present in the schema.[¶](#section-7.6-3)

#### [7.6.1.](#section-7.6.1) [Assertions and Instance Primitive Types](#name-assertions-and-instance-pri)

Most assertions only constrain values within a certain primitive type. When the type of the instance is not of the type targeted by the keyword, the instance is considered to conform to the assertion.[¶](#section-7.6.1-1)

For example, the "maxLength" keyword from the companion [validation vocabulary](#json-schema-validation) \[[json-schema-validation](#json-schema-validation)\]: will only restrict certain strings (that are too long) from being valid. If the instance is a number, boolean, null, array, or object, then it is valid against this assertion.[¶](#section-7.6.1-2)

This behavior allows keywords to be used more easily with instances that can be of multiple primitive types. The companion validation vocabulary also includes a "type" keyword which can independently restrict the instance to one or more primitive types. This allows for a concise expression of use cases such as a function that might return either a string of a certain length or a null value:[¶](#section-7.6.1-3)

{
    "type": \["string", "null"\],
    "maxLength": 255
}

[¶](#section-7.6.1-4)

If "maxLength" also restricted the instance type to be a string, then this would be substantially more cumbersome to express because the example as written would not actually allow null values. Each keyword is evaluated separately unless explicitly specified otherwise, so if "maxLength" restricted the instance to strings, then including "null" in "type" would not have any useful effect.[¶](#section-7.6.1-5)

### [7.7.](#section-7.7) [Annotations](#name-annotations)

JSON Schema can annotate an instance with information, whenever the instance validates against the schema object containing the annotation, and all of its parent schema objects. The information can be a simple value, or can be calculated based on the instance contents.[¶](#section-7.7-1)

Annotations are attached to specific locations in an instance. Since many subschemas can be applied to any single location, applications may need to decide how to handle differing annotation values being attached to the same instance location by the same schema keyword in different schema objects.[¶](#section-7.7-2)

Unlike assertion results, annotation data can take a wide variety of forms, which are provided to applications to use as they see fit. JSON Schema implementations are not expected to make use of the collected information on behalf of applications.[¶](#section-7.7-3)

Unless otherwise specified, the value of an annotation keyword is the keyword's value. However, other behaviors are possible. For example, [JSON Hyper-Schema's](#json-hyper-schema) \[[json-hyper-schema](#json-hyper-schema)\] "links" keyword is a complex annotation that produces a value based in part on the instance data.[¶](#section-7.7-4)

While "short-circuit" evaluation is possible for assertions, collecting annotations requires examining all schemas that apply to an instance location, even if they cannot change the overall assertion result. The only exception is that subschemas of a schema object that has failed validation MAY be skipped, as annotations are not retained for failing schemas.[¶](#section-7.7-5)

#### [7.7.1.](#section-7.7.1) [Collecting Annotations](#name-collecting-annotations)

Annotations are collected by keywords that explicitly define annotation-collecting behavior. Note that boolean schemas cannot produce annotations as they do not make use of keywords.[¶](#section-7.7.1-1)

A collected annotation MUST include the following information:[¶](#section-7.7.1-2)

*   The name of the keyword that produces the annotation[¶](#section-7.7.1-3.1)
*   The instance location to which it is attached, as a JSON Pointer[¶](#section-7.7.1-3.2)
*   The schema location path, indicating how reference keywords such as "$ref" were followed to reach the absolute schema location.[¶](#section-7.7.1-3.3)
*   The absolute schema location of the attaching keyword, as a URI. This MAY be omitted if it is the same as the schema location path from above.[¶](#section-7.7.1-3.4)
*   The attached value(s)[¶](#section-7.7.1-3.5)

##### [7.7.1.1.](#section-7.7.1.1) [Distinguishing Among Multiple Values](#name-distinguishing-among-multip)

Applications MAY make decisions on which of multiple annotation values to use based on the schema location that contributed the value. This is intended to allow flexible usage. Collecting the schema location facilitates such usage.[¶](#section-7.7.1.1-1)

For example, consider this schema, which uses annotations and assertions from the [Validation specification](#json-schema-validation) \[[json-schema-validation](#json-schema-validation)\]:[¶](#section-7.7.1.1-2)

Note that some lines are wrapped for clarity.[¶](#section-7.7.1.1-3)

{
    "title": "Feature list",
    "type": "array",
    "prefixItems": \[
        {
            "title": "Feature A",
            "properties": {
                "enabled": {
                    "$ref": "#/$defs/enabledToggle",
                    "default": true
                }
            }
        },
        {
            "title": "Feature B",
            "properties": {
                "enabled": {
                    "description": "If set to null, Feature B
                                    inherits the enabled
                                    value from Feature A",
                    "$ref": "#/$defs/enabledToggle"
                }
            }
        }
    \],
    "$defs": {
        "enabledToggle": {
            "title": "Enabled",
            "description": "Whether the feature is enabled (true),
                            disabled (false), or under
                            automatic control (null)",
            "type": \["boolean", "null"\],
            "default": null
        }
    }
}

[¶](#section-7.7.1.1-4)

In this example, both Feature A and Feature B make use of the re-usable "enabledToggle" schema. That schema uses the "title", "description", and "default" annotations. Therefore the application has to decide how to handle the additional "default" value for Feature A, and the additional "description" value for Feature B.[¶](#section-7.7.1.1-5)

The application programmer and the schema author need to agree on the usage. For this example, let's assume that they agree that the most specific "default" value will be used, and any additional, more generic "default" values will be silently ignored. Let's also assume that they agree that all "description" text is to be used, starting with the most generic, and ending with the most specific. This requires the schema author to write descriptions that work when combined in this way.[¶](#section-7.7.1.1-6)

The application can use the schema location path to determine which values are which. The values in the feature's immediate "enabled" property schema are more specific, while the values under the re-usable schema that is referenced to with "$ref" are more generic. The schema location path will show whether each value was found by crossing a "$ref" or not.[¶](#section-7.7.1.1-7)

Feature A will therefore use a default value of true, while Feature B will use the generic default value of null. Feature A will only have the generic description from the "enabledToggle" schema, while Feature B will use that description, and also append its locally defined description that explains how to interpret a null value.[¶](#section-7.7.1.1-8)

Note that there are other reasonable approaches that a different application might take. For example, an application may consider the presence of two different values for "default" to be an error, regardless of their schema locations.[¶](#section-7.7.1.1-9)

##### [7.7.1.2.](#section-7.7.1.2) [Annotations and Assertions](#name-annotations-and-assertions)

Schema objects that produce a false assertion result MUST NOT produce any annotation results, whether from their own keywords or from keywords in subschemas.[¶](#section-7.7.1.2-1)

Note that the overall schema results may still include annotations collected from other schema locations. Given this schema:[¶](#section-7.7.1.2-2)

{
    "oneOf": \[
        {
            "title": "Integer Value",
            "type": "integer"
        },
        {
            "title": "String Value",
            "type": "string"
        }
    \]
}

[¶](#section-7.7.1.2-3)

Against the instance `"This is a string"`, the title annotation "Integer Value" is discarded because the type assertion in that schema object fails. The title annotation "String Value" is kept, as the instance passes the string type assertions.[¶](#section-7.7.1.2-4)

##### [7.7.1.3.](#section-7.7.1.3) [Annotations and Applicators](#name-annotations-and-applicators)

In addition to possibly defining annotation results of their own, applicator keywords aggregate the annotations collected in their subschema(s) or referenced schema(s).[¶](#section-7.7.1.3-1)

### [7.8.](#section-7.8) [Reserved Locations](#name-reserved-locations)

A fourth category of keywords simply reserve a location to hold re-usable components or data of interest to schema authors that is not suitable for re-use. These keywords do not affect validation or annotation results. Their purpose in the core vocabulary is to ensure that locations are available for certain purposes and will not be redefined by extension keywords.[¶](#section-7.8-1)

While these keywords do not directly affect results, as explained in section [9.4.2](#non-schemas) unrecognized extension keywords that reserve locations for re-usable schemas may have undesirable interactions with references in certain circumstances.[¶](#section-7.8-2)

### [7.9.](#section-7.9) [Loading Instance Data](#name-loading-instance-data)

While none of the vocabularies defined as part of this or the associated documents define a keyword which may target and/or load instance data, it is possible that other vocabularies may wish to do so.[¶](#section-7.9-1)

Keywords MAY be defined to use JSON Pointers or Relative JSON Pointers to examine parts of an instance outside the current evaluation location.[¶](#section-7.9-2)

Keywords that allow adjusting the location using a Relative JSON Pointer SHOULD default to using the current location if a default is desireable.[¶](#section-7.9-3)

## [8\.](#section-8) [The JSON Schema Core Vocabulary](#name-the-json-schema-core-vocabu)

Keywords declared in this section, which all begin with "$", make up the JSON Schema Core vocabulary. These keywords are either required in order to process any schema or meta-schema, including those split across multiple documents, or exist to reserve keywords for purposes that require guaranteed interoperability.[¶](#section-8-1)

The Core vocabulary MUST be considered mandatory at all times, in order to bootstrap the processing of further vocabularies. Meta-schemas that use the ["$vocabulary"](#vocabulary) ([Section 8.1](#vocabulary)) keyword to declare the vocabularies in use MUST explicitly list the Core vocabulary, which MUST have a value of true indicating that it is required.[¶](#section-8-2)

The behavior of a false value for this vocabulary (and only this vocabulary) is undefined, as is the behavior when "$vocabulary" is present but the Core vocabulary is not included. However, it is RECOMMENDED that implementations detect these cases and raise an error when they occur. It is not meaningful to declare that a meta-schema optionally uses Core.[¶](#section-8-3)

Meta-schemas that do not use "$vocabulary" MUST be considered to require the Core vocabulary as if its URI were present with a value of true.[¶](#section-8-4)

The current URI for the Core vocabulary is: <https://json-schema.org/draft/2020-12/vocab/core>.[¶](#section-8-5)

The current URI for the corresponding meta-schema is: [https://json-schema.org/draft/2020-12/meta/core](https://json-schema.org/draft/2020-12/meta/core).[¶](#section-8-6)

While the "$" prefix is not formally reserved for the Core vocabulary, it is RECOMMENDED that extension keywords (in vocabularies or otherwise) begin with a character other than "$" to avoid possible future collisions.[¶](#section-8-7)

### [8.1.](#section-8.1) [Meta-Schemas and Vocabularies](#name-meta-schemas-and-vocabulari)

Two concepts, meta-schemas and vocabularies, are used to inform an implementation how to interpret a schema. Every schema has a meta-schema, which can be declared using the "$schema" keyword.[¶](#section-8.1-1)

The meta-schema serves two purposes:[¶](#section-8.1-2)

Declaring the vocabularies in use

The "$vocabulary" keyword, when it appears in a meta-schema, declares which vocabularies are available to be used in schemas that refer to that meta-schema. Vocabularies define keyword semantics, as well as their general syntax.[¶](#section-8.1-3.2)

Describing valid schema syntax

A schema MUST successfully validate against its meta-schema, which constrains the syntax of the available keywords. The syntax described is expected to be compatible with the vocabularies declared; while it is possible to describe an incompatible syntax, such a meta-schema would be unlikely to be useful.[¶](#section-8.1-3.4)

Meta-schemas are separate from vocabularies to allow for vocabularies to be combined in different ways, and for meta-schema authors to impose additional constraints such as forbidding certain keywords, or performing unusually strict syntactical validation, as might be done during a development and testing cycle. Each vocabulary typically identifies a meta-schema consisting only of the vocabulary's keywords.[¶](#section-8.1-4)

Meta-schema authoring is an advanced usage of JSON Schema, so the design of meta-schema features emphasizes flexibility over simplicity.[¶](#section-8.1-5)

#### [8.1.1.](#section-8.1.1) [The "$schema" Keyword](#name-the-schema-keyword)

The "$schema" keyword is both used as a JSON Schema dialect identifier and as the identifier of a resource which is itself a JSON Schema, which describes the set of valid schemas written for this particular dialect.[¶](#section-8.1.1-1)

The value of this keyword MUST be a [URI](#RFC3986) \[[RFC3986](#RFC3986)\] (containing a scheme) and this URI MUST be normalized. The current schema MUST be valid against the meta-schema identified by this URI.[¶](#section-8.1.1-2)

If this URI identifies a retrievable resource, that resource SHOULD be of media type "application/schema+json".[¶](#section-8.1.1-3)

The "$schema" keyword SHOULD be used in the document root schema object, and MAY be used in the root schema objects of embedded schema resources. It MUST NOT appear in non-resource root schema objects. If absent from the document root schema, the resulting behavior is implementation-defined.[¶](#section-8.1.1-4)

Values for this property are defined elsewhere in this and other documents, and by other parties.[¶](#section-8.1.1-5)

#### [8.1.2.](#section-8.1.2) [The "$vocabulary" Keyword](#name-the-vocabulary-keyword)

The "$vocabulary" keyword is used in meta-schemas to identify the vocabularies available for use in schemas described by that meta-schema. It is also used to indicate whether each vocabulary is required or optional, in the sense that an implementation MUST understand the required vocabularies in order to successfully process the schema. Together, this information forms a dialect. Any vocabulary that is understood by the implementation MUST be processed in a manner consistent with the semantic definitions contained within the vocabulary.[¶](#section-8.1.2-1)

The value of this keyword MUST be an object. The property names in the object MUST be URIs (containing a scheme) and this URI MUST be normalized. Each URI that appears as a property name identifies a specific set of keywords and their semantics.[¶](#section-8.1.2-2)

The URI MAY be a URL, but the nature of the retrievable resource is currently undefined, and reserved for future use. Vocabulary authors MAY use the URL of the vocabulary specification, in a human-readable media type such as text/html or text/plain, as the vocabulary URI. Vocabulary documents may be added in forthcoming drafts. For now, identifying the keyword set is deemed sufficient as that, along with meta-schema validation, is how the current "vocabularies" work today. Any future vocabulary document format will be specified as a JSON document, so using text/html or other non-JSON formats in the meantime will not produce any future ambiguity. [¶](#section-8.1.2-3)

The values of the object properties MUST be booleans. If the value is true, then implementations that do not recognize the vocabulary MUST refuse to process any schemas that declare this meta-schema with "$schema". If the value is false, implementations that do not recognize the vocabulary SHOULD proceed with processing such schemas. The value has no impact if the implementation understands the vocabulary.[¶](#section-8.1.2-4)

Per [6.5](#extending), unrecognized keywords SHOULD be treated as annotations. This remains the case for keywords defined by unrecognized vocabularies. It is not currently possible to distinguish between unrecognized keywords that are defined in vocabularies from those that are not part of any vocabulary.[¶](#section-8.1.2-5)

The "$vocabulary" keyword SHOULD be used in the root schema of any schema document intended for use as a meta-schema. It MUST NOT appear in subschemas.[¶](#section-8.1.2-6)

The "$vocabulary" keyword MUST be ignored in schema documents that are not being processed as a meta-schema. This allows validating a meta-schema M against its own meta-schema M' without requiring the validator to understand the vocabularies declared by M.[¶](#section-8.1.2-7)

##### [8.1.2.1.](#section-8.1.2.1) [Default vocabularies](#name-default-vocabularies)

If "$vocabulary" is absent, an implementation MAY determine behavior based on the meta-schema if it is recognized from the URI value of the referring schema's "$schema" keyword. This is how behavior (such as Hyper-Schema usage) has been recognized prior to the existence of vocabularies.[¶](#section-8.1.2.1-1)

If the meta-schema, as referenced by the schema, is not recognized, or is missing, then the behavior is implementation-defined. If the implementation proceeds with processing the schema, it MUST assume the use of the core vocabulary. If the implementation is built for a specific purpose, then it SHOULD assume the use of all of the most relevant vocabularies for that purpose.[¶](#section-8.1.2.1-2)

For example, an implementation that is a validator SHOULD assume the use of all vocabularies in this specification and the companion Validation specification.[¶](#section-8.1.2.1-3)

##### [8.1.2.2.](#section-8.1.2.2) [Non-inheritability of vocabularies](#name-non-inheritability-of-vocab)

Note that the processing restrictions on "$vocabulary" mean that meta-schemas that reference other meta-schemas using "$ref" or similar keywords do not automatically inherit the vocabulary declarations of those other meta-schemas. All such declarations must be repeated in the root of each schema document intended for use as a meta-schema. This is demonstrated in [the example meta-schema](#example-meta-schema) ([Appendix D.2](#example-meta-schema)). This requirement allows implementations to find all vocabulary requirement information in a single place for each meta-schema. As schema extensibility means that there are endless potential ways to combine more fine-grained meta-schemas by reference, requiring implementations to anticipate all possibilities and search for vocabularies in referenced meta-schemas would be overly burdensome. [¶](#section-8.1.2.2-1)

#### [8.1.3.](#section-8.1.3) [Updates to Meta-Schema and Vocabulary URIs](#name-updates-to-meta-schema-and-)

Updated vocabulary and meta-schema URIs MAY be published between specification drafts in order to correct errors. Implementations SHOULD consider URIs dated after this specification draft and before the next to indicate the same syntax and semantics as those listed here.[¶](#section-8.1.3-1)

### [8.2.](#section-8.2) [Base URI, Anchors, and Dereferencing](#name-base-uri-anchors-and-derefe)

To differentiate between schemas in a vast ecosystem, schemas are identified by [URI](#RFC3986) \[[RFC3986](#RFC3986)\], and can embed references to other schemas by specifying their URI.[¶](#section-8.2-1)

Several keywords can accept a relative [URI-reference](#RFC3986) \[[RFC3986](#RFC3986)\], or a value used to construct a relative URI-reference. For these keywords, it is necessary to establish a base URI in order to resolve the reference.[¶](#section-8.2-2)

#### [8.2.1.](#section-8.2.1) [The "$id" Keyword](#name-the-id-keyword)

The "$id" keyword identifies a schema resource with its [canonical](#RFC6596) \[[RFC6596](#RFC6596)\] URI.[¶](#section-8.2.1-1)

Note that this URI is an identifier and not necessarily a network locator. In the case of a network-addressable URL, a schema need not be downloadable from its canonical URI.[¶](#section-8.2.1-2)

If present, the value for this keyword MUST be a string, and MUST represent a valid [URI-reference](#RFC3986) \[[RFC3986](#RFC3986)\]. This URI-reference SHOULD be normalized, and MUST resolve to an [absolute-URI](#RFC3986) \[[RFC3986](#RFC3986)\] (without a fragment), or to a URI with an empty fragment.[¶](#section-8.2.1-3)

The empty fragment form is NOT RECOMMENDED and is retained only for backwards compatibility, and because the application/schema+json media type defines that a URI with an empty fragment identifies the same resource as the same URI with the fragment removed. However, since this equivalence is not part of the [RFC 3986 normalization process](#RFC3986) \[[RFC3986](#RFC3986)\], implementers and schema authors cannot rely on generic URI libraries understanding it.[¶](#section-8.2.1-4)

Therefore, "$id" MUST NOT contain a non-empty fragment, and SHOULD NOT contain an empty fragment. The absolute-URI form MUST be considered the canonical URI, regardless of the presence or absence of an empty fragment. An empty fragment is currently allowed because older meta-schemas have an empty fragment in their $id (or previously, id). A future draft may outright forbid even empty fragments in "$id". [¶](#section-8.2.1-5)

The absolute-URI also serves as the base URI for relative URI-references in keywords within the schema resource, in accordance with [RFC 3986 section 5.1.1](#RFC3986) \[[RFC3986](#RFC3986)\] regarding base URIs embedded in content.[¶](#section-8.2.1-6)

The presence of "$id" in a subschema indicates that the subschema constitutes a distinct schema resource within a single schema document. Furthermore, in accordance with [RFC 3986 section 5.1.2](#RFC3986) \[[RFC3986](#RFC3986)\] regarding encapsulating entities, if an "$id" in a subschema is a relative URI-reference, the base URI for resolving that reference is the URI of the parent schema resource.[¶](#section-8.2.1-7)

If no parent schema object explicitly identifies itself as a resource with "$id", the base URI is that of the entire document, as established by the steps given in the [previous section.](#initial-base) ([Section 9.1.1](#initial-base))[¶](#section-8.2.1-8)

##### [8.2.1.1.](#section-8.2.1.1) [Identifying the root schema](#name-identifying-the-root-schema)

The root schema of a JSON Schema document SHOULD contain an "$id" keyword with an [absolute-URI](#RFC3986) \[[RFC3986](#RFC3986)\] (containing a scheme, but no fragment).[¶](#section-8.2.1.1-1)

#### [8.2.2.](#section-8.2.2) [Defining location-independent identifiers](#name-defining-location-independe)

Using JSON Pointer fragments requires knowledge of the structure of the schema. When writing schema documents with the intention to provide re-usable schemas, it may be preferable to use a plain name fragment that is not tied to any particular structural location. This allows a subschema to be relocated without requiring JSON Pointer references to be updated.[¶](#section-8.2.2-1)

The "$anchor" and "$dynamicAnchor" keywords are used to specify such fragments. They are identifier keywords that can only be used to create plain name fragments, rather than absolute URIs as seen with "$id".[¶](#section-8.2.2-2)

The base URI to which the resulting fragment is appended is the canonical URI of the schema resource containing the "$anchor" or "$dynamicAnchor" in question. As discussed in the previous section, this is either the nearest "$id" in the same or parent schema object, or the base URI for the document as determined according to RFC 3986.[¶](#section-8.2.2-3)

Separately from the usual usage of URIs, "$dynamicAnchor" indicates that the fragment is an extension point when used with the "$dynamicRef" keyword. This low-level, advanced feature makes it easier to extend recursive schemas such as the meta-schemas, without imposing any particular semantics on that extension. See the section on ["$dynamicRef"](#dynamic-ref) ([Section 8.2.3.2](#dynamic-ref)) for details.[¶](#section-8.2.2-4)

In most cases, the normal fragment behavior both suffices and is more intuitive. Therefore it is RECOMMENDED that "$anchor" be used to create plain name fragments unless there is a clear need for "$dynamicAnchor".[¶](#section-8.2.2-5)

If present, the value of this keyword MUST be a string and MUST start with a letter (\[A-Za-z\]) or underscore ("\_"), followed by any number of letters, digits (\[0-9\]), hyphens ("-"), underscores ("\_"), and periods ("."). This matches the US-ASCII part of XML's [NCName production](#xml-names) \[[xml-names](#xml-names)\]. Note that the anchor string does not include the "#" character, as it is not a URI-reference. An "$anchor": "foo" becomes the fragment "#foo" when used in a URI. See below for full examples. [¶](#section-8.2.2-6)

The effect of specifying the same fragment name multiple times within the same resource, using any combination of "$anchor" and/or "$dynamicAnchor", is undefined. Implementations MAY raise an error if such usage is detected.[¶](#section-8.2.2-7)

#### [8.2.3.](#section-8.2.3) [Schema References](#name-schema-references)

Several keywords can be used to reference a schema which is to be applied to the current instance location. "$ref" and "$dynamicRef" are applicator keywords, applying the referenced schema to the instance.[¶](#section-8.2.3-1)

As the values of "$ref" and "$dynamicRef" are URI References, this allows the possibility to externalise or divide a schema across multiple files, and provides the ability to validate recursive structures through self-reference.[¶](#section-8.2.3-2)

The resolved URI produced by these keywords is not necessarily a network locator, only an identifier. A schema need not be downloadable from the address if it is a network-addressable URL, and implementations SHOULD NOT assume they should perform a network operation when they encounter a network-addressable URI.[¶](#section-8.2.3-3)

##### [8.2.3.1.](#section-8.2.3.1) [Direct References with "$ref"](#name-direct-references-with-ref)

The "$ref" keyword is an applicator that is used to reference a statically identified schema. Its results are the results of the referenced schema. Note that this definition of how the results are determined means that other keywords can appear alongside of "$ref" in the same schema object. [¶](#section-8.2.3.1-1)

The value of the "$ref" keyword MUST be a string which is a URI-Reference. Resolved against the current URI base, it produces the URI of the schema to apply. This resolution is safe to perform on schema load, as the process of evaluating an instance cannot change how the reference resolves.[¶](#section-8.2.3.1-2)

##### [8.2.3.2.](#section-8.2.3.2) [Dynamic References with "$dynamicRef"](#name-dynamic-references-with-dyn)

The "$dynamicRef" keyword is an applicator that allows for deferring the full resolution until runtime, at which point it is resolved each time it is encountered while evaluating an instance.[¶](#section-8.2.3.2-1)

Together with "$dynamicAnchor", "$dynamicRef" implements a cooperative extension mechanism that is primarily useful with recursive schemas (schemas that reference themselves). Both the extension point and the runtime-determined extension target are defined with "$dynamicAnchor", and only exhibit runtime dynamic behavior when referenced with "$dynamicRef".[¶](#section-8.2.3.2-2)

The value of the "$dynamicRef" property MUST be a string which is a URI-Reference. Resolved against the current URI base, it produces the URI used as the starting point for runtime resolution. This initial resolution is safe to perform on schema load.[¶](#section-8.2.3.2-3)

If the initially resolved starting point URI includes a fragment that was created by the "$dynamicAnchor" keyword, the initial URI MUST be replaced by the URI (including the fragment) for the outermost schema resource in the [dynamic scope](#scopes) ([Section 7.1](#scopes)) that defines an identically named fragment with "$dynamicAnchor".[¶](#section-8.2.3.2-4)

Otherwise, its behavior is identical to "$ref", and no runtime resolution is needed.[¶](#section-8.2.3.2-5)

For a full example using these keyword, see appendix [C](#recursive-example). The difference between the hyper-schema meta-schema in pre-2019 drafts and an this draft dramatically demonstrates the utility of these keywords. [¶](#section-8.2.3.2-6)

#### [8.2.4.](#section-8.2.4) [Schema Re-Use With "$defs"](#name-schema-re-use-with-defs)

The "$defs" keyword reserves a location for schema authors to inline re-usable JSON Schemas into a more general schema. The keyword does not directly affect the validation result.[¶](#section-8.2.4-1)

This keyword's value MUST be an object. Each member value of this object MUST be a valid JSON Schema.[¶](#section-8.2.4-2)

As an example, here is a schema describing an array of positive integers, where the positive integer constraint is a subschema in "$defs":[¶](#section-8.2.4-3)

{
    "type": "array",
    "items": { "$ref": "#/$defs/positiveInteger" },
    "$defs": {
        "positiveInteger": {
            "type": "integer",
            "exclusiveMinimum": 0
        }
    }
}

[¶](#section-8.2.4-4)

### [8.3.](#section-8.3) [Comments With "$comment"](#name-comments-with-comment)

This keyword reserves a location for comments from schema authors to readers or maintainers of the schema.[¶](#section-8.3-1)

The value of this keyword MUST be a string. Implementations MUST NOT present this string to end users. Tools for editing schemas SHOULD support displaying and editing this keyword. The value of this keyword MAY be used in debug or error output which is intended for developers making use of schemas.[¶](#section-8.3-2)

Schema vocabularies SHOULD allow "$comment" within any object containing vocabulary keywords. Implementations MAY assume "$comment" is allowed unless the vocabulary specifically forbids it. Vocabularies MUST NOT specify any effect of "$comment" beyond what is described in this specification.[¶](#section-8.3-3)

Tools that translate other media types or programming languages to and from application/schema+json MAY choose to convert that media type or programming language's native comments to or from "$comment" values. The behavior of such translation when both native comments and "$comment" properties are present is implementation-dependent.[¶](#section-8.3-4)

Implementations MAY strip "$comment" values at any point during processing. In particular, this allows for shortening schemas when the size of deployed schemas is a concern.[¶](#section-8.3-5)

Implementations MUST NOT take any other action based on the presence, absence, or contents of "$comment" properties. In particular, the value of "$comment" MUST NOT be collected as an annotation result.[¶](#section-8.3-6)

## [9\.](#section-9) [Loading and Processing Schemas](#name-loading-and-processing-sche)

### [9.1.](#section-9.1) [Loading a Schema](#name-loading-a-schema)

#### [9.1.1.](#section-9.1.1) [Initial Base URI](#name-initial-base-uri)

[RFC3986 Section 5.1](#RFC3986) \[[RFC3986](#RFC3986)\] defines how to determine the default base URI of a document.[¶](#section-9.1.1-1)

Informatively, the initial base URI of a schema is the URI at which it was found, whether that was a network location, a local filesystem, or any other situation identifiable by a URI of any known scheme.[¶](#section-9.1.1-2)

If a schema document defines no explicit base URI with "$id" (embedded in content), the base URI is that determined per [RFC 3986 section 5](#RFC3986) \[[RFC3986](#RFC3986)\].[¶](#section-9.1.1-3)

If no source is known, or no URI scheme is known for the source, a suitable implementation-specific default URI MAY be used as described in [RFC 3986 Section 5.1.4](#RFC3986) \[[RFC3986](#RFC3986)\]. It is RECOMMENDED that implementations document any default base URI that they assume.[¶](#section-9.1.1-4)

If a schema object is embedded in a document of another media type, then the initial base URI is determined according to the rules of that media type.[¶](#section-9.1.1-5)

Unless the "$id" keyword described in an earlier section is present in the root schema, this base URI SHOULD be considered the canonical URI of the schema document's root schema resource.[¶](#section-9.1.1-6)

#### [9.1.2.](#section-9.1.2) [Loading a referenced schema](#name-loading-a-referenced-schema)

The use of URIs to identify remote schemas does not necessarily mean anything is downloaded, but instead JSON Schema implementations SHOULD understand ahead of time which schemas they will be using, and the URIs that identify them.[¶](#section-9.1.2-1)

When schemas are downloaded, for example by a generic user-agent that does not know until runtime which schemas to download, see [Usage for Hypermedia](#hypermedia) ([Section 9.5.1](#hypermedia)).[¶](#section-9.1.2-2)

Implementations SHOULD be able to associate arbitrary URIs with an arbitrary schema and/or automatically associate a schema's "$id"-given URI, depending on the trust that the validator has in the schema. Such URIs and schemas can be supplied to an implementation prior to processing instances, or may be noted within a schema document as it is processed, producing associations as shown in appendix [A](#idExamples).[¶](#section-9.1.2-3)

A schema MAY (and likely will) have multiple URIs, but there is no way for a URI to identify more than one schema. When multiple schemas try to identify as the same URI, validators SHOULD raise an error condition.[¶](#section-9.1.2-4)

#### [9.1.3.](#section-9.1.3) [Detecting a Meta-Schema](#name-detecting-a-meta-schema)

Implementations MUST recognize a schema as a meta-schema if it is being examined because it was identified as such by another schema's "$schema" keyword. This means that a single schema document might sometimes be considered a regular schema, and other times be considered a meta-schema.[¶](#section-9.1.3-1)

In the case of examining a schema which is its own meta-schema, when an implementation begins processing it as a regular schema, it is processed under those rules. However, when loaded a second time as a result of checking its own "$schema" value, it is treated as a meta-schema. So the same document is processed both ways in the course of one session.[¶](#section-9.1.3-2)

Implementations MAY allow a schema to be explicitly passed as a meta-schema, for implementation-specific purposes, such as pre-loading a commonly used meta-schema and checking its vocabulary support requirements up front. Meta-schema authors MUST NOT expect such features to be interoperable across implementations.[¶](#section-9.1.3-3)

### [9.2.](#section-9.2) [Dereferencing](#name-dereferencing)

Schemas can be identified by any URI that has been given to them, including a JSON Pointer or their URI given directly by "$id". In all cases, dereferencing a "$ref" reference involves first resolving its value as a URI reference against the current base URI per [RFC 3986](#RFC3986) \[[RFC3986](#RFC3986)\].[¶](#section-9.2-1)

If the resulting URI identifies a schema within the current document, or within another schema document that has been made available to the implementation, then that schema SHOULD be used automatically.[¶](#section-9.2-2)

For example, consider this schema:[¶](#section-9.2-3)

{
    "$id": "https://example.net/root.json",
    "items": {
        "type": "array",
        "items": { "$ref": "#item" }
    },
    "$defs": {
        "single": {
            "$anchor": "item",
            "type": "object",
            "additionalProperties": { "$ref": "other.json" }
        }
    }
}

[¶](#section-9.2-4)

When an implementation encounters the <#/$defs/single> schema, it resolves the "$anchor" value as a fragment name against the current base URI to form <https://example.net/root.json#item>.[¶](#section-9.2-5)

When an implementation then looks inside the <#/items> schema, it encounters the <#item> reference, and resolves this to <https://example.net/root.json#item>, which it has seen defined in this same document and can therefore use automatically.[¶](#section-9.2-6)

When an implementation encounters the reference to "other.json", it resolves this to <https://example.net/other.json>, which is not defined in this document. If a schema with that identifier has otherwise been supplied to the implementation, it can also be used automatically. What should implementations do when the referenced schema is not known? Are there circumstances in which automatic network dereferencing is allowed? A same origin policy? A user-configurable option? In the case of an evolving API described by Hyper-Schema, it is expected that new schemas will be added to the system dynamically, so placing an absolute requirement of pre-loading schema documents is not feasible. [¶](#section-9.2-7)

#### [9.2.1.](#section-9.2.1) [JSON Pointer fragments and embedded schema resources](#name-json-pointer-fragments-and-)

Since JSON Pointer URI fragments are constructed based on the structure of the schema document, an embedded schema resource and its subschemas can be identified by JSON Pointer fragments relative to either its own canonical URI, or relative to any containing resource's URI.[¶](#section-9.2.1-1)

Conceptually, a set of linked schema resources should behave identically whether each resource is a separate document connected with [schema references](#references) ([Section 8.2.3](#references)), or is structured as a single document with one or more schema resources embedded as subschemas.[¶](#section-9.2.1-2)

Since URIs involving JSON Pointer fragments relative to the parent schema resource's URI cease to be valid when the embedded schema is moved to a separate document and referenced, applications and schemas SHOULD NOT use such URIs to identify embedded schema resources or locations within them.[¶](#section-9.2.1-3)

Consider the following schema document that contains another schema resource embedded within it:[¶](#section-9.2.1-4)

{
    "$id": "https://example.com/foo",
    "items": {
        "$id": "https://example.com/bar",
        "additionalProperties": { }
    }
}

[¶](#section-9.2.1-5)

The URI "https://example.com/foo#/items" points to the "items" schema, which is an embedded resource. The canonical URI of that schema resource, however, is "https://example.com/bar".[¶](#section-9.2.1-6)

For the "additionalProperties" schema within that embedded resource, the URI "https://example.com/foo#/items/additionalProperties" points to the correct object, but that object's URI relative to its resource's canonical URI is "https://example.com/bar#/additionalProperties".[¶](#section-9.2.1-7)

Now consider the following two schema resources linked by reference using a URI value for "$ref":[¶](#section-9.2.1-8)

{
    "$id": "https://example.com/foo",
    "items": {
        "$ref": "bar"
    }
}

{
    "$id": "https://example.com/bar",
    "additionalProperties": { }
}

[¶](#section-9.2.1-9)

Here we see that "https://example.com/bar#/additionalProperties", using a JSON Pointer fragment appended to the canonical URI of the "bar" schema resource, is still valid, while "https://example.com/foo#/items/additionalProperties", which relied on a JSON Pointer fragment appended to the canonical URI of the "foo" schema resource, no longer resolves to anything.[¶](#section-9.2.1-10)

Note also that "https://example.com/foo#/items" is valid in both arrangements, but resolves to a different value. This URI ends up functioning similarly to a retrieval URI for a resource. While this URI is valid, it is more robust to use the "$id" of the embedded or referenced resource unless it is specifically desired to identify the object containing the "$ref" in the second (non-embedded) arrangement.[¶](#section-9.2.1-11)

An implementation MAY choose not to support addressing schema resource contents by URIs using a base other than the resource's canonical URI, plus a JSON Pointer fragment relative to that base. Therefore, schema authors SHOULD NOT rely on such URIs, as using them may reduce interoperability. This is to avoid requiring implementations to keep track of a whole stack of possible base URIs and JSON Pointer fragments for each, given that all but one will be fragile if the schema resources are reorganized. Some have argued that this is easy so there is no point in forbidding it, while others have argued that it complicates schema identification and should be forbidden. Feedback on this topic is encouraged. After some discussion, we feel that we need to remove the use of "canonical" in favour of talking about JSON Pointers which reference across schema resource boundaries as undefined or even forbidden behavior (https://github.com/json-schema-org/json-schema-spec/issues/937, https://github.com/json-schema-org/json-schema-spec/issues/1183) [¶](#section-9.2.1-12)

Further examples of such non-canonical URI construction, as well as the appropriate canonical URI-based fragments to use instead, are provided in appendix [A](#idExamples).[¶](#section-9.2.1-13)

### [9.3.](#section-9.3) [Compound Documents](#name-compound-documents)

A Compound Schema Document is defined as a JSON document (sometimes called a "bundled" schema) which has multiple embedded JSON Schema Resources bundled into the same document to ease transportation.[¶](#section-9.3-1)

Each embedded Schema Resource MUST be treated as an individual Schema Resource, following standard schema loading and processing requirements, including determining vocabulary support.[¶](#section-9.3-2)

#### [9.3.1.](#section-9.3.1) [Bundling](#name-bundling)

The bundling process for creating a Compound Schema Document is defined as taking references (such as "$ref") to an external Schema Resource and embedding the referenced Schema Resources within the referring document. Bundling SHOULD be done in such a way that all URIs (used for referencing) in the base document and any referenced/embedded documents do not require altering.[¶](#section-9.3.1-1)

Each embedded JSON Schema Resource MUST identify itself with a URI using the "$id" keyword, and SHOULD make use of the "$schema" keyword to identify the dialect it is using, in the root of the schema resource. It is RECOMMENDED that the URI identifier value of "$id" be an Absolute URI.[¶](#section-9.3.1-2)

When the Schema Resource referenced by a by-reference applicator is bundled, it is RECOMMENDED that the Schema Resource be located as a value of a "$defs" object at the containing schema's root. The key of the "$defs" for the now embedded Schema Resource MAY be the "$id" of the bundled schema or some other form of application defined unique identifier (such as a UUID). This key is not intended to be referenced in JSON Schema, but may be used by an application to aid the bundling process.[¶](#section-9.3.1-3)

A Schema Resource MAY be embedded in a location other than "$defs" where the location is defined as a schema value.[¶](#section-9.3.1-4)

A Bundled Schema Resource MUST NOT be bundled by replacing the schema object from which it was referenced, or by wrapping the Schema Resource in other applicator keywords.[¶](#section-9.3.1-5)

In order to produce identical output, references in the containing schema document to the previously external Schema Resources MUST NOT be changed, and now resolve to a schema using the "$id" of an embedded Schema Resource. Such identical output includes validation evaluation and URIs or paths used in resulting annotations or errors.[¶](#section-9.3.1-6)

While the bundling process will often be the main method for creating a Compound Schema Document, it is also possible and expected that some will be created by hand, potentially without individual Schema Resources existing on their own previously.[¶](#section-9.3.1-7)

#### [9.3.2.](#section-9.3.2) [Differing and Default Dialects](#name-differing-and-default-diale)

When multiple schema resources are present in a single document, schema resources which do not define with which dialect they should be processed MUST be processed with the same dialect as the enclosing resource.[¶](#section-9.3.2-1)

Since any schema that can be referenced can also be embedded, embedded schema resources MAY specify different processing dialects using the "$schema" values from their enclosing resource.[¶](#section-9.3.2-2)

#### [9.3.3.](#section-9.3.3) [Validating](#name-validating)

Given that a Compound Schema Document may have embedded resources which identify as using different dialects, these documents SHOULD NOT be validated by applying a meta-schema to the Compound Schema Document as an instance. It is RECOMMENDED that an alternate validation process be provided in order to validate Schema Documents. Each Schema Resource SHOULD be separately validated against its associated meta-schema. If you know a schema is what's being validated, you can identify if the schemas is a Compound Schema Document or not, by way of use of "$id", which identifies an embedded resource when used not at the document's root. [¶](#section-9.3.3-1)

A Compound Schema Document in which all embedded resources identify as using the same dialect, or in which "$schema" is omitted and therefore defaults to that of the enclosing resource, MAY be validated by applying the appropriate meta-schema.[¶](#section-9.3.3-2)

### [9.4.](#section-9.4) [Caveats](#name-caveats)

#### [9.4.1.](#section-9.4.1) [Guarding Against Infinite Recursion](#name-guarding-against-infinite-r)

A schema MUST NOT be run into an infinite loop against an instance. For example, if two schemas "#alice" and "#bob" both have an "allOf" property that refers to the other, a naive validator might get stuck in an infinite recursive loop trying to validate the instance. Schemas SHOULD NOT make use of infinite recursive nesting like this; the behavior is undefined.[¶](#section-9.4.1-1)

#### [9.4.2.](#section-9.4.2) [References to Possible Non-Schemas](#name-references-to-possible-non-)

Subschema objects (or booleans) are recognized by their use with known applicator keywords or with location-reserving keywords such as ["$defs"](#defs) ([Section 8.2.4](#defs)) that take one or more subschemas as a value. These keywords may be "$defs" and the standard applicators from this document, or extension keywords from a known vocabulary, or implementation-specific custom keywords.[¶](#section-9.4.2-1)

Multi-level structures of unknown keywords are capable of introducing nested subschemas, which would be subject to the processing rules for "$id". Therefore, having a reference target in such an unrecognized structure cannot be reliably implemented, and the resulting behavior is undefined. Similarly, a reference target under a known keyword, for which the value is known not to be a schema, results in undefined behavior in order to avoid burdening implementations with the need to detect such targets. These scenarios are analogous to fetching a schema over HTTP but receiving a response with a Content-Type other than application/schema+json. An implementation can certainly try to interpret it as a schema, but the origin server offered no guarantee that it actually is any such thing. Therefore, interpreting it as such has security implications and may produce unpredictable results. [¶](#section-9.4.2-2)

Note that single-level custom keywords with identical syntax and semantics to "$defs" do not allow for any intervening "$id" keywords, and therefore will behave correctly under implementations that attempt to use any reference target as a schema. However, this behavior is implementation-specific and MUST NOT be relied upon for interoperability.[¶](#section-9.4.2-3)

### [9.5.](#section-9.5) [Associating Instances and Schemas](#name-associating-instances-and-s)

#### [9.5.1.](#section-9.5.1) [Usage for Hypermedia](#name-usage-for-hypermedia)

JSON has been adopted widely by HTTP servers for automated APIs and robots. This section describes how to enhance processing of JSON documents in a more RESTful manner when used with protocols that support media types and [Web linking](#RFC8288) \[[RFC8288](#RFC8288)\].[¶](#section-9.5.1-1)

##### [9.5.1.1.](#section-9.5.1.1) [Linking to a Schema](#name-linking-to-a-schema)

It is RECOMMENDED that instances described by a schema provide a link to a downloadable JSON Schema using the link relation "describedby", as defined by [Linked Data Protocol 1.0, section 8.1](#W3C.REC-ldp-20150226) \[[W3C.REC-ldp-20150226](#W3C.REC-ldp-20150226)\].[¶](#section-9.5.1.1-1)

In HTTP, such links can be attached to any response using the [Link header](#RFC8288) \[[RFC8288](#RFC8288)\]. An example of such a header would be:[¶](#section-9.5.1.1-2)

        Link: <https://example.com/my-hyper-schema>; rel="describedby"

[¶](#section-9.5.1.1-3)

##### [9.5.1.2.](#section-9.5.1.2) [Usage Over HTTP](#name-usage-over-http)

When used for hypermedia systems over a network, [HTTP](#RFC7231) \[[RFC7231](#RFC7231)\] is frequently the protocol of choice for distributing schemas. Misbehaving clients can pose problems for server maintainers if they pull a schema over the network more frequently than necessary, when it's instead possible to cache a schema for a long period of time.[¶](#section-9.5.1.2-1)

HTTP servers SHOULD set long-lived caching headers on JSON Schemas. HTTP clients SHOULD observe caching headers and not re-request documents within their freshness period. Distributed systems SHOULD make use of a shared cache and/or caching proxy.[¶](#section-9.5.1.2-2)

Clients SHOULD set or prepend a User-Agent header specific to the JSON Schema implementation or software product. Since symbols are listed in decreasing order of significance, the JSON Schema library name/version should precede the more generic HTTP library name (if any). For example:[¶](#section-9.5.1.2-3)

        User-Agent: product-name/5.4.1 so-cool-json-schema/1.0.2 curl/7.43.0

[¶](#section-9.5.1.2-4)

Clients SHOULD be able to make requests with a "From" header so that server operators can contact the owner of a potentially misbehaving script.[¶](#section-9.5.1.2-5)

## [10\.](#section-10) [A Vocabulary for Applying Subschemas](#name-a-vocabulary-for-applying-s)

This section defines a vocabulary of applicator keywords that are RECOMMENDED for use as the basis of other vocabularies.[¶](#section-10-1)

Meta-schemas that do not use "$vocabulary" SHOULD be considered to require this vocabulary as if its URI were present with a value of true.[¶](#section-10-2)

The current URI for this vocabulary, known as the Applicator vocabulary, is: <https://json-schema.org/draft/2020-12/vocab/applicator>.[¶](#section-10-3)

The current URI for the corresponding meta-schema is: [https://json-schema.org/draft/2020-12/meta/applicator](https://json-schema.org/draft/2020-12/meta/applicator).[¶](#section-10-4)

### [10.1.](#section-10.1) [Keyword Independence](#name-keyword-independence)

Schema keywords typically operate independently, without affecting each other's outcomes.[¶](#section-10.1-1)

For schema author convenience, there are some exceptions among the keywords in this vocabulary:[¶](#section-10.1-2)

*   "additionalProperties", whose behavior is defined in terms of "properties" and "patternProperties"[¶](#section-10.1-3.1)
*   "items", whose behavior is defined in terms of "prefixItems"[¶](#section-10.1-3.2)
*   "contains", whose behavior is affected by the presence and value of "minContains", in the Validation vocabulary[¶](#section-10.1-3.3)

### [10.2.](#section-10.2) [Keywords for Applying Subschemas in Place](#name-keywords-for-applying-subsc)

These keywords apply subschemas to the same location in the instance as the parent schema is being applied. They allow combining or modifying the subschema results in various ways.[¶](#section-10.2-1)

Subschemas of these keywords evaluate the instance completely independently such that the results of one such subschema MUST NOT impact the results of sibling subschemas. Therefore subschemas may be applied in any order.[¶](#section-10.2-2)

#### [10.2.1.](#section-10.2.1) [Keywords for Applying Subschemas With Logic](#name-keywords-for-applying-subsch)

These keywords correspond to logical operators for combining or modifying the boolean assertion results of the subschemas. They have no direct impact on annotation collection, although they enable the same annotation keyword to be applied to an instance location with different values. Annotation keywords define their own rules for combining such values.[¶](#section-10.2.1-1)

##### [10.2.1.1.](#section-10.2.1.1) [allOf](#name-allof)

This keyword's value MUST be a non-empty array. Each item of the array MUST be a valid JSON Schema.[¶](#section-10.2.1.1-1)

An instance validates successfully against this keyword if it validates successfully against all schemas defined by this keyword's value.[¶](#section-10.2.1.1-2)

##### [10.2.1.2.](#section-10.2.1.2) [anyOf](#name-anyof)

This keyword's value MUST be a non-empty array. Each item of the array MUST be a valid JSON Schema.[¶](#section-10.2.1.2-1)

An instance validates successfully against this keyword if it validates successfully against at least one schema defined by this keyword's value. Note that when annotations are being collected, all subschemas MUST be examined so that annotations are collected from each subschema that validates successfully.[¶](#section-10.2.1.2-2)

##### [10.2.1.3.](#section-10.2.1.3) [oneOf](#name-oneof)

This keyword's value MUST be a non-empty array. Each item of the array MUST be a valid JSON Schema.[¶](#section-10.2.1.3-1)

An instance validates successfully against this keyword if it validates successfully against exactly one schema defined by this keyword's value.[¶](#section-10.2.1.3-2)

##### [10.2.1.4.](#section-10.2.1.4) [not](#name-not)

This keyword's value MUST be a valid JSON Schema.[¶](#section-10.2.1.4-1)

An instance is valid against this keyword if it fails to validate successfully against the schema defined by this keyword.[¶](#section-10.2.1.4-2)

#### [10.2.2.](#section-10.2.2) [Keywords for Applying Subschemas Conditionally](#name-keywords-for-applying-subsche)

Three of these keywords work together to implement conditional application of a subschema based on the outcome of another subschema. The fourth is a shortcut for a specific conditional case.[¶](#section-10.2.2-1)

"if", "then", and "else" MUST NOT interact with each other across subschema boundaries. In other words, an "if" in one branch of an "allOf" MUST NOT have an impact on a "then" or "else" in another branch.[¶](#section-10.2.2-2)

There is no default behavior for "if", "then", or "else" when they are not present. In particular, they MUST NOT be treated as if present with an empty schema, and when "if" is not present, both "then" and "else" MUST be entirely ignored.[¶](#section-10.2.2-3)

##### [10.2.2.1.](#section-10.2.2.1) [if](#name-if)

This keyword's value MUST be a valid JSON Schema.[¶](#section-10.2.2.1-1)

This validation outcome of this keyword's subschema has no direct effect on the overall validation result. Rather, it controls which of the "then" or "else" keywords are evaluated.[¶](#section-10.2.2.1-2)

Instances that successfully validate against this keyword's subschema MUST also be valid against the subschema value of the "then" keyword, if present.[¶](#section-10.2.2.1-3)

Instances that fail to validate against this keyword's subschema MUST also be valid against the subschema value of the "else" keyword, if present.[¶](#section-10.2.2.1-4)

If [annotations](#annotations) ([Section 7.7](#annotations)) are being collected, they are collected from this keyword's subschema in the usual way, including when the keyword is present without either "then" or "else".[¶](#section-10.2.2.1-5)

##### [10.2.2.2.](#section-10.2.2.2) [then](#name-then)

This keyword's value MUST be a valid JSON Schema.[¶](#section-10.2.2.2-1)

When "if" is present, and the instance successfully validates against its subschema, then validation succeeds against this keyword if the instance also successfully validates against this keyword's subschema.[¶](#section-10.2.2.2-2)

This keyword has no effect when "if" is absent, or when the instance fails to validate against its subschema. Implementations MUST NOT evaluate the instance against this keyword, for either validation or annotation collection purposes, in such cases.[¶](#section-10.2.2.2-3)

##### [10.2.2.3.](#section-10.2.2.3) [else](#name-else)

This keyword's value MUST be a valid JSON Schema.[¶](#section-10.2.2.3-1)

When "if" is present, and the instance fails to validate against its subschema, then validation succeeds against this keyword if the instance successfully validates against this keyword's subschema.[¶](#section-10.2.2.3-2)

This keyword has no effect when "if" is absent, or when the instance successfully validates against its subschema. Implementations MUST NOT evaluate the instance against this keyword, for either validation or annotation collection purposes, in such cases.[¶](#section-10.2.2.3-3)

##### [10.2.2.4.](#section-10.2.2.4) [dependentSchemas](#name-dependentschemas)

This keyword specifies subschemas that are evaluated if the instance is an object and contains a certain property.[¶](#section-10.2.2.4-1)

This keyword's value MUST be an object. Each value in the object MUST be a valid JSON Schema.[¶](#section-10.2.2.4-2)

If the object key is a property in the instance, the entire instance must validate against the subschema. Its use is dependent on the presence of the property.[¶](#section-10.2.2.4-3)

Omitting this keyword has the same behavior as an empty object.[¶](#section-10.2.2.4-4)

### [10.3.](#section-10.3) [Keywords for Applying Subschemas to Child Instances](#name-keywords-for-applying-subschem)

Each of these keywords defines a rule for applying its subschema(s) to child instances, specifically object properties and array items, and combining their results.[¶](#section-10.3-1)

#### [10.3.1.](#section-10.3.1) [Keywords for Applying Subschemas to Arrays](#name-keywords-for-applying-subschema)

##### [10.3.1.1.](#section-10.3.1.1) [prefixItems](#name-prefixitems)

The value of "prefixItems" MUST be a non-empty array of valid JSON Schemas.[¶](#section-10.3.1.1-1)

Validation succeeds if each element of the instance validates against the schema at the same position, if any. This keyword does not constrain the length of the array. If the array is longer than this keyword's value, this keyword validates only the prefix of matching length.[¶](#section-10.3.1.1-2)

This keyword produces an annotation value which is the largest index to which this keyword applied a subschema. The value MAY be a boolean true if a subschema was applied to every index of the instance, such as is produced by the "items" keyword. This annotation affects the behavior of "items" and "unevaluatedItems".[¶](#section-10.3.1.1-3)

Omitting this keyword has the same assertion behavior as an empty array.[¶](#section-10.3.1.1-4)

##### [10.3.1.2.](#section-10.3.1.2) [items](#name-items)

The value of "items" MUST be a valid JSON Schema.[¶](#section-10.3.1.2-1)

This keyword applies its subschema to all instance elements at indexes greater than the length of the "prefixItems" array in the same schema object, as reported by the annotation result of that "prefixItems" keyword. If no such annotation result exists, "items" applies its subschema to all instance array elements. Note that the behavior of "items" without "prefixItems" is identical to that of the schema form of "items" in prior drafts. When "prefixItems" is present, the behavior of "items" is identical to the former "additionalItems" keyword. [¶](#section-10.3.1.2-2)

If the "items" subschema is applied to any positions within the instance array, it produces an annotation result of boolean true, indicating that all remaining array elements have been evaluated against this keyword's subschema. This annotation affects the behavior of "unevaluatedItems" in the Unevaluated vocabulary.[¶](#section-10.3.1.2-3)

Omitting this keyword has the same assertion behavior as an empty schema.[¶](#section-10.3.1.2-4)

Implementations MAY choose to implement or optimize this keyword in another way that produces the same effect, such as by directly checking for the presence and size of a "prefixItems" array. Implementations that do not support annotation collection MUST do so.[¶](#section-10.3.1.2-5)

##### [10.3.1.3.](#section-10.3.1.3) [contains](#name-contains)

The value of this keyword MUST be a valid JSON Schema.[¶](#section-10.3.1.3-1)

An array instance is valid against "contains" if at least one of its elements is valid against the given schema, except when "minContains" is present and has a value of 0, in which case an array instance MUST be considered valid against the "contains" keyword, even if none of its elements is valid against the given schema.[¶](#section-10.3.1.3-2)

This keyword produces an annotation value which is an array of the indexes to which this keyword validates successfully when applying its subschema, in ascending order. The value MAY be a boolean "true" if the subschema validates successfully when applied to every index of the instance. The annotation MUST be present if the instance array to which this keyword's schema applies is empty.[¶](#section-10.3.1.3-3)

This annotation affects the behavior of "unevaluatedItems" in the Unevaluated vocabulary, and MAY also be used to implement the "minContains" and "maxContains" keywords in the Validation vocabulary.[¶](#section-10.3.1.3-4)

The subschema MUST be applied to every array element even after the first match has been found, in order to collect annotations for use by other keywords. This is to ensure that all possible annotations are collected.[¶](#section-10.3.1.3-5)

#### [10.3.2.](#section-10.3.2) [Keywords for Applying Subschemas to Objects](#name-keywords-for-applying-subschemas)

##### [10.3.2.1.](#section-10.3.2.1) [properties](#name-properties)

The value of "properties" MUST be an object. Each value of this object MUST be a valid JSON Schema.[¶](#section-10.3.2.1-1)

Validation succeeds if, for each name that appears in both the instance and as a name within this keyword's value, the child instance for that name successfully validates against the corresponding schema.[¶](#section-10.3.2.1-2)

The annotation result of this keyword is the set of instance property names matched by this keyword. This annotation affects the behavior of "additionalProperties" (in this vocabulary) and "unevaluatedProperties" in the Unevaluated vocabulary.[¶](#section-10.3.2.1-3)

Omitting this keyword has the same assertion behavior as an empty object.[¶](#section-10.3.2.1-4)

##### [10.3.2.2.](#section-10.3.2.2) [patternProperties](#name-patternproperties)

The value of "patternProperties" MUST be an object. Each property name of this object SHOULD be a valid regular expression, according to the ECMA-262 regular expression dialect. Each property value of this object MUST be a valid JSON Schema.[¶](#section-10.3.2.2-1)

Validation succeeds if, for each instance name that matches any regular expressions that appear as a property name in this keyword's value, the child instance for that name successfully validates against each schema that corresponds to a matching regular expression.[¶](#section-10.3.2.2-2)

The annotation result of this keyword is the set of instance property names matched by this keyword. This annotation affects the behavior of "additionalProperties" (in this vocabulary) and "unevaluatedProperties" (in the Unevaluated vocabulary).[¶](#section-10.3.2.2-3)

Omitting this keyword has the same assertion behavior as an empty object.[¶](#section-10.3.2.2-4)

##### [10.3.2.3.](#section-10.3.2.3) [additionalProperties](#name-additionalproperties)

The value of "additionalProperties" MUST be a valid JSON Schema.[¶](#section-10.3.2.3-1)

The behavior of this keyword depends on the presence and annotation results of "properties" and "patternProperties" within the same schema object. Validation with "additionalProperties" applies only to the child values of instance names that do not appear in the annotation results of either "properties" or "patternProperties".[¶](#section-10.3.2.3-2)

For all such properties, validation succeeds if the child instance validates against the "additionalProperties" schema.[¶](#section-10.3.2.3-3)

The annotation result of this keyword is the set of instance property names validated by this keyword's subschema. This annotation affects the behavior of "unevaluatedProperties" in the Unevaluated vocabulary.[¶](#section-10.3.2.3-4)

Omitting this keyword has the same assertion behavior as an empty schema.[¶](#section-10.3.2.3-5)

Implementations MAY choose to implement or optimize this keyword in another way that produces the same effect, such as by directly checking the names in "properties" and the patterns in "patternProperties" against the instance property set. Implementations that do not support annotation collection MUST do so. In defining this option, it seems there is the potential for ambiguity in the output format. The ambiguity does not affect validation results, but it does affect the resulting output format. The ambiguity allows for multiple valid output results depending on whether annotations are used or a solution that "produces the same effect" as draft-07. It is understood that annotations from failing schemas are dropped. See our \[Decision Record\](https://github.com/json-schema-org/json-schema-spec/tree/HEAD/adr/2022-04-08-cref-for-ambiguity-and-fix-later-gh-spec-issue-1172.md) for further details. [¶](#section-10.3.2.3-6)

##### [10.3.2.4.](#section-10.3.2.4) [propertyNames](#name-propertynames)

The value of "propertyNames" MUST be a valid JSON Schema.[¶](#section-10.3.2.4-1)

If the instance is an object, this keyword validates if every property name in the instance validates against the provided schema. Note the property name that the schema is testing will always be a string.[¶](#section-10.3.2.4-2)

Omitting this keyword has the same behavior as an empty schema.[¶](#section-10.3.2.4-3)

## [11\.](#section-11) [A Vocabulary for Unevaluated Locations](#name-a-vocabulary-for-unevaluate)

The purpose of these keywords is to enable schema authors to apply subschemas to array items or object properties that have not been successfully evaluated against any dynamic-scope subschema of any adjacent keywords.[¶](#section-11-1)

These instance items or properties may have been unsuccessfully evaluated against one or more adjacent keyword subschemas, such as when an assertion in a branch of an "anyOf" fails. Such failed evaluations are not considered to contribute to whether or not the item or property has been evaluated. Only successful evaluations are considered.[¶](#section-11-2)

If an item in an array or an object property is "successfully evaluated", it is logically considered to be valid in terms of the representation of the object or array that's expected. For example if a subschema represents a car, which requires between 2-4 wheels, and the value of "wheels" is 6, the instance object is not "evaluated" to be a car, and the "wheels" property is considered "unevaluated (successfully as a known thing)", and does not retain any annotations.[¶](#section-11-3)

Recall that adjacent keywords are keywords within the same schema object, and that the dynamic-scope subschemas include reference targets as well as lexical subschemas.[¶](#section-11-4)

The behavior of these keywords depend on the annotation results of adjacent keywords that apply to the instance location being validated.[¶](#section-11-5)

Meta-schemas that do not use "$vocabulary" SHOULD be considered to require this vocabulary as if its URI were present with a value of true.[¶](#section-11-6)

The current URI for this vocabulary, known as the Unevaluated Applicator vocabulary, is: <https://json-schema.org/draft/2020-12/vocab/unevaluated>.[¶](#section-11-7)

The current URI for the corresponding meta-schema is: [https://json-schema.org/draft/2020-12/meta/unevaluated](https://json-schema.org/draft/2020-12/meta/unevaluated).[¶](#section-11-8)

### [11.1.](#section-11.1) [Keyword Independence](#name-keyword-independence-2)

Schema keywords typically operate independently, without affecting each other's outcomes. However, the keywords in this vocabulary are notable exceptions:[¶](#section-11.1-1)

*   "unevaluatedItems", whose behavior is defined in terms of annotations from "prefixItems", "items", "contains", and itself[¶](#section-11.1-2.1)
*   "unevaluatedProperties", whose behavior is defined in terms of annotations from "properties", "patternProperties", "additionalProperties" and itself[¶](#section-11.1-2.2)

### [11.2.](#section-11.2) [unevaluatedItems](#name-unevaluateditems)

The value of "unevaluatedItems" MUST be a valid JSON Schema.[¶](#section-11.2-1)

The behavior of this keyword depends on the annotation results of adjacent keywords that apply to the instance location being validated. Specifically, the annotations from "prefixItems", "items", and "contains", which can come from those keywords when they are adjacent to the "unevaluatedItems" keyword. Those three annotations, as well as "unevaluatedItems", can also result from any and all adjacent [in-place applicator](#in-place) ([Section 10.2](#in-place)) keywords. This includes but is not limited to the in-place applicators defined in this document.[¶](#section-11.2-2)

If no relevant annotations are present, the "unevaluatedItems" subschema MUST be applied to all locations in the array. If a boolean true value is present from any of the relevant annotations, "unevaluatedItems" MUST be ignored. Otherwise, the subschema MUST be applied to any index greater than the largest annotation value for "prefixItems", which does not appear in any annotation value for "contains".[¶](#section-11.2-3)

This means that "prefixItems", "items", "contains", and all in-place applicators MUST be evaluated before this keyword can be evaluated. Authors of extension keywords MUST NOT define an in-place applicator that would need to be evaluated after this keyword.[¶](#section-11.2-4)

If the "unevaluatedItems" subschema is applied to any positions within the instance array, it produces an annotation result of boolean true, analogous to the behavior of "items". This annotation affects the behavior of "unevaluatedItems" in parent schemas.[¶](#section-11.2-5)

Omitting this keyword has the same assertion behavior as an empty schema.[¶](#section-11.2-6)

### [11.3.](#section-11.3) [unevaluatedProperties](#name-unevaluatedproperties)

The value of "unevaluatedProperties" MUST be a valid JSON Schema.[¶](#section-11.3-1)

The behavior of this keyword depends on the annotation results of adjacent keywords that apply to the instance location being validated. Specifically, the annotations from "properties", "patternProperties", and "additionalProperties", which can come from those keywords when they are adjacent to the "unevaluatedProperties" keyword. Those three annotations, as well as "unevaluatedProperties", can also result from any and all adjacent [in-place applicator](#in-place) ([Section 10.2](#in-place)) keywords. This includes but is not limited to the in-place applicators defined in this document.[¶](#section-11.3-2)

Validation with "unevaluatedProperties" applies only to the child values of instance names that do not appear in the "properties", "patternProperties", "additionalProperties", or "unevaluatedProperties" annotation results that apply to the instance location being validated.[¶](#section-11.3-3)

For all such properties, validation succeeds if the child instance validates against the "unevaluatedProperties" schema.[¶](#section-11.3-4)

This means that "properties", "patternProperties", "additionalProperties", and all in-place applicators MUST be evaluated before this keyword can be evaluated. Authors of extension keywords MUST NOT define an in-place applicator that would need to be evaluated after this keyword.[¶](#section-11.3-5)

The annotation result of this keyword is the set of instance property names validated by this keyword's subschema. This annotation affects the behavior of "unevaluatedProperties" in parent schemas.[¶](#section-11.3-6)

Omitting this keyword has the same assertion behavior as an empty schema.[¶](#section-11.3-7)

## [12\.](#section-12) [Output Formatting](#name-output-formatting)

JSON Schema is defined to be platform-independent. As such, to increase compatibility across platforms, implementations SHOULD conform to a standard validation output format. This section describes the minimum requirements that consumers will need to properly interpret validation results.[¶](#section-12-1)

### [12.1.](#section-12.1) [Format](#name-format)

JSON Schema output is defined using the JSON Schema data instance model as described in section 4.2.1. Implementations MAY deviate from this as supported by their specific languages and platforms, however it is RECOMMENDED that the output be convertible to the JSON format defined herein via serialization or other means.[¶](#section-12.1-1)

### [12.2.](#section-12.2) [Output Formats](#name-output-formats)

This specification defines four output formats. See the "Output Structure" section for the requirements of each format.[¶](#section-12.2-1)

*   Flag - A boolean which simply indicates the overall validation result with no further details.[¶](#section-12.2-2.1)
*   Basic - Provides validation information in a flat list structure.[¶](#section-12.2-2.2)
*   Detailed - Provides validation information in a condensed hierarchical structure based on the structure of the schema.[¶](#section-12.2-2.3)
*   Verbose - Provides validation information in an uncondensed hierarchical structure that matches the exact structure of the schema.[¶](#section-12.2-2.4)

An implementation SHOULD provide at least one of the "flag", "basic", or "detailed" format and MAY provide the "verbose" format. If it provides one or more of the "detailed" or "verbose" formats, it MUST also provide the "flag" format. Implementations SHOULD specify in their documentation which formats they support.[¶](#section-12.2-3)

### [12.3.](#section-12.3) [Minimum Information](#name-minimum-information)

Beyond the simplistic "flag" output, additional information is useful to aid in debugging a schema or instance. Each sub-result SHOULD contain the information contained within this section at a minimum.[¶](#section-12.3-1)

A single object that contains all of these components is considered an output unit.[¶](#section-12.3-2)

Implementations MAY elect to provide additional information.[¶](#section-12.3-3)

#### [12.3.1.](#section-12.3.1) [Keyword Relative Location](#name-keyword-relative-location)

The relative location of the validating keyword that follows the validation path. The value MUST be expressed as a JSON Pointer, and it MUST include any by-reference applicators such as "$ref" or "$dynamicRef".[¶](#section-12.3.1-1)

/properties/width/$ref/minimum

[¶](#section-12.3.1-2)

Note that this pointer may not be resolvable by the normal JSON Pointer process due to the inclusion of these by-reference applicator keywords.[¶](#section-12.3.1-3)

The JSON key for this information is "keywordLocation".[¶](#section-12.3.1-4)

#### [12.3.2.](#section-12.3.2) [Keyword Absolute Location](#name-keyword-absolute-location)

The absolute, dereferenced location of the validating keyword. The value MUST be expressed as a full URI using the canonical URI of the relevant schema resource with a JSON Pointer fragment, and it MUST NOT include by-reference applicators such as "$ref" or "$dynamicRef" as non-terminal path components. It MAY end in such keywords if the error or annotation is for that keyword, such as an unresolvable reference. Note that "absolute" here is in the sense of "absolute filesystem path" (meaning the complete location) rather than the "absolute-URI" terminology from RFC 3986 (meaning with scheme but without fragment). Keyword absolute locations will have a fragment in order to identify the keyword. [¶](#section-12.3.2-1)

https://example.com/schemas/common#/$defs/count/minimum

[¶](#section-12.3.2-2)

This information MAY be omitted only if either the dynamic scope did not pass over a reference or if the schema does not declare an absolute URI as its "$id".[¶](#section-12.3.2-3)

The JSON key for this information is "absoluteKeywordLocation".[¶](#section-12.3.2-4)

#### [12.3.3.](#section-12.3.3) [Instance Location](#name-instance-location)

The location of the JSON value within the instance being validated. The value MUST be expressed as a JSON Pointer.[¶](#section-12.3.3-1)

The JSON key for this information is "instanceLocation".[¶](#section-12.3.3-2)

#### [12.3.4.](#section-12.3.4) [Error or Annotation](#name-error-or-annotation)

The error or annotation that is produced by the validation.[¶](#section-12.3.4-1)

For errors, the specific wording for the message is not defined by this specification. Implementations will need to provide this.[¶](#section-12.3.4-2)

For annotations, each keyword that produces an annotation specifies its format. By default, it is the keyword's value.[¶](#section-12.3.4-3)

The JSON key for failed validations is "error"; for successful validations it is "annotation".[¶](#section-12.3.4-4)

#### [12.3.5.](#section-12.3.5) [Nested Results](#name-nested-results)

For the two hierarchical structures, this property will hold nested errors and annotations.[¶](#section-12.3.5-1)

The JSON key for nested results in failed validations is "errors"; for successful validations it is "annotations". Note the plural forms, as a keyword with nested results can also have a local error or annotation.[¶](#section-12.3.5-2)

### [12.4.](#section-12.4) [Output Structure](#name-output-structure)

The output MUST be an object containing a boolean property named "valid". When additional information about the result is required, the output MUST also contain "errors" or "annotations" as described below.[¶](#section-12.4-1)

*   "valid" - a boolean value indicating the overall validation success or failure[¶](#section-12.4-2.1)
*   "errors" - the collection of errors or annotations produced by a failed validation[¶](#section-12.4-2.2)
*   "annotations" - the collection of errors or annotations produced by a successful validation[¶](#section-12.4-2.3)

For these examples, the following schema and instance will be used.[¶](#section-12.4-3)

{
  "$id": "https://example.com/polygon",
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$defs": {
    "point": {
      "type": "object",
      "properties": {
        "x": { "type": "number" },
        "y": { "type": "number" }
      },
      "additionalProperties": false,
      "required": \[ "x", "y" \]
    }
  },
  "type": "array",
  "items": { "$ref": "#/$defs/point" },
  "minItems": 3
}

\[
  {
    "x": 2.5,
    "y": 1.3
  },
  {
    "x": 1,
    "z": 6.7
  }
\]

[¶](#section-12.4-4)

This instance will fail validation and produce errors, but it's trivial to deduce examples for passing schemas that produce annotations.[¶](#section-12.4-5)

Specifically, the errors it will produce are:[¶](#section-12.4-6)

*   The second object is missing a "y" property.[¶](#section-12.4-7.1)
*   The second object has a disallowed "z" property.[¶](#section-12.4-7.2)
*   There are only two objects, but three are required.[¶](#section-12.4-7.3)

Note that the error message wording as depicted in these examples is not a requirement of this specification. Implementations SHOULD craft error messages tailored for their audience or provide a templating mechanism that allows their users to craft their own messages.[¶](#section-12.4-8)

#### [12.4.1.](#section-12.4.1) [Flag](#name-flag)

In the simplest case, merely the boolean result for the "valid" valid property needs to be fulfilled.[¶](#section-12.4.1-1)

{
  "valid": false
}

[¶](#section-12.4.1-2)

Because no errors or annotations are returned with this format, it is RECOMMENDED that implementations use short-circuiting logic to return failure or success as soon as the outcome can be determined. For example, if an "anyOf" keyword contains five sub-schemas, and the second one passes, there is no need to check the other three. The logic can simply return with success.[¶](#section-12.4.1-3)

#### [12.4.2.](#section-12.4.2) [Basic](#name-basic)

The "Basic" structure is a flat list of output units.[¶](#section-12.4.2-1)

{
  "valid": false,
  "errors": \[
    {
      "keywordLocation": "",
      "instanceLocation": "",
      "error": "A subschema had errors."
    },
    {
      "keywordLocation": "/items/$ref",
      "absoluteKeywordLocation":
        "https://example.com/polygon#/$defs/point",
      "instanceLocation": "/1",
      "error": "A subschema had errors."
    },
    {
      "keywordLocation": "/items/$ref/required",
      "absoluteKeywordLocation":
        "https://example.com/polygon#/$defs/point/required",
      "instanceLocation": "/1",
      "error": "Required property 'y' not found."
    },
    {
      "keywordLocation": "/items/$ref/additionalProperties",
      "absoluteKeywordLocation":
        "https://example.com/polygon#/$defs/point/additionalProperties",
      "instanceLocation": "/1/z",
      "error": "Additional property 'z' found but was invalid."
    },
    {
      "keywordLocation": "/minItems",
      "instanceLocation": "",
      "error": "Expected at least 3 items but found 2"
    }
  \]
}

[¶](#section-12.4.2-2)

#### [12.4.3.](#section-12.4.3) [Detailed](#name-detailed)

The "Detailed" structure is based on the schema and can be more readable for both humans and machines. Having the structure organized this way makes associations between the errors more apparent. For example, the fact that the missing "y" property and the extra "z" property both stem from the same location in the instance is not immediately obvious in the "Basic" structure. In a hierarchy, the correlation is more easily identified.[¶](#section-12.4.3-1)

The following rules govern the construction of the results object:[¶](#section-12.4.3-2)

*   All applicator keywords ("\*Of", "$ref", "if"/"then"/"else", etc.) require a node.[¶](#section-12.4.3-3.1)
*   Nodes that have no children are removed.[¶](#section-12.4.3-3.2)
*   Nodes that have a single child are replaced by the child.[¶](#section-12.4.3-3.3)

Branch nodes do not require an error message or an annotation.[¶](#section-12.4.3-4)

{
  "valid": false,
  "keywordLocation": "",
  "instanceLocation": "",
  "errors": \[
    {
      "valid": false,
      "keywordLocation": "/items/$ref",
      "absoluteKeywordLocation":
        "https://example.com/polygon#/$defs/point",
      "instanceLocation": "/1",
      "errors": \[
        {
          "valid": false,
          "keywordLocation": "/items/$ref/required",
          "absoluteKeywordLocation":
            "https://example.com/polygon#/$defs/point/required",
          "instanceLocation": "/1",
          "error": "Required property 'y' not found."
        },
        {
          "valid": false,
          "keywordLocation": "/items/$ref/additionalProperties",
          "absoluteKeywordLocation":
            "https://example.com/polygon#/$defs/point/additionalProperties",
          "instanceLocation": "/1/z",
          "error": "Additional property 'z' found but was invalid."
        }
      \]
    },
    {
      "valid": false,
      "keywordLocation": "/minItems",
      "instanceLocation": "",
      "error": "Expected at least 3 items but found 2"
    }
  \]
}

[¶](#section-12.4.3-5)

#### [12.4.4.](#section-12.4.4) [Verbose](#name-verbose)

The "Verbose" structure is a fully realized hierarchy that exactly matches that of the schema. This structure has applications in form generation and validation where the error's location is important.[¶](#section-12.4.4-1)

The primary difference between this and the "Detailed" structure is that all results are returned. This includes sub-schema validation results that would otherwise be removed (e.g. annotations for failed validations, successful validations inside a \`not\` keyword, etc.). Because of this, it is RECOMMENDED that each node also carry a \`valid\` property to indicate the validation result for that node.[¶](#section-12.4.4-2)

Because this output structure can be quite large, a smaller example is given here for brevity. The URI of the full output structure of the example above is: [https://json-schema.org/draft/2020-12/output/verbose-example](https://json-schema.org/draft/2020-12/output/verbose-example).[¶](#section-12.4.4-3)

// schema
{
  "$id": "https://example.com/polygon",
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "validProp": true,
  },
  "additionalProperties": false
}

// instance
{
  "validProp": 5,
  "disallowedProp": "value"
}

// result
{
  "valid": false,
  "keywordLocation": "",
  "instanceLocation": "",
  "errors": \[
    {
      "valid": true,
      "keywordLocation": "/type",
      "instanceLocation": ""
    },
    {
      "valid": true,
      "keywordLocation": "/properties",
      "instanceLocation": ""
    },
    {
      "valid": false,
      "keywordLocation": "/additionalProperties",
      "instanceLocation": "",
      "errors": \[
        {
          "valid": false,
          "keywordLocation": "/additionalProperties",
          "instanceLocation": "/disallowedProp",
          "error": "Additional property 'disallowedProp' found but was invalid."
        }
      \]
    }
  \]
}

[¶](#section-12.4.4-4)

#### [12.4.5.](#section-12.4.5) [Output validation schemas](#name-output-validation-schemas)

For convenience, JSON Schema has been provided to validate output generated by implementations. Its URI is: [https://json-schema.org/draft/2020-12/output/schema](https://json-schema.org/draft/2020-12/output/schema).[¶](#section-12.4.5-1)

## [13\.](#section-13) [Security Considerations](#name-security-considerations)

Both schemas and instances are JSON values. As such, all security considerations defined in [RFC 8259](#RFC8259) \[[RFC8259](#RFC8259)\] apply.[¶](#section-13-1)

Instances and schemas are both frequently written by untrusted third parties, to be deployed on public Internet servers. Validators should take care that the parsing and validating against schemas does not consume excessive system resources. Validators MUST NOT fall into an infinite loop.[¶](#section-13-2)

A malicious party could cause an implementation to repeatedly collect a copy of a very large value as an annotation. Implementations SHOULD guard against excessive consumption of system resources in such a scenario.[¶](#section-13-3)

Servers MUST ensure that malicious parties cannot change the functionality of existing schemas by uploading a schema with a pre-existing or very similar "$id".[¶](#section-13-4)

Individual JSON Schema vocabularies are liable to also have their own security considerations. Consult the respective specifications for more information.[¶](#section-13-5)

Schema authors should take care with "$comment" contents, as a malicious implementation can display them to end-users in violation of a spec, or fail to strip them if such behavior is expected.[¶](#section-13-6)

A malicious schema author could place executable code or other dangerous material within a "$comment". Implementations MUST NOT parse or otherwise take action based on "$comment" contents.[¶](#section-13-7)

## [14\.](#section-14) [IANA Considerations](#name-iana-considerations)

### [14.1.](#section-14.1) [application/schema+json](#name-application-schemajson)

The proposed MIME media type for JSON Schema is defined as follows:[¶](#section-14.1-1)

*   Type name: application[¶](#section-14.1-2.1)
*   Subtype name: schema+json[¶](#section-14.1-2.2)
*   Required parameters: N/A[¶](#section-14.1-2.3)
*   Encoding considerations: Encoding considerations are identical to those specified for the "application/json" media type. See [JSON](#RFC8259) \[[RFC8259](#RFC8259)\].[¶](#section-14.1-2.4)
*   Security considerations: See Section [13](#security) above.[¶](#section-14.1-2.5)
*   Interoperability considerations: See Sections [6.2](#language), [6.3](#integers), and [6.4](#regex) above.[¶](#section-14.1-2.6)
*   Fragment identifier considerations: See Section [5](#fragments)[¶](#section-14.1-2.7)

### [14.2.](#section-14.2) [application/schema-instance+json](#name-application-schema-instance)

The proposed MIME media type for JSON Schema Instances that require a JSON Schema-specific media type is defined as follows:[¶](#section-14.2-1)

*   Type name: application[¶](#section-14.2-2.1)
*   Subtype name: schema-instance+json[¶](#section-14.2-2.2)
*   Required parameters: N/A[¶](#section-14.2-2.3)
*   Encoding considerations: Encoding considerations are identical to those specified for the "application/json" media type. See [JSON](#RFC8259) \[[RFC8259](#RFC8259)\].[¶](#section-14.2-2.4)
*   Security considerations: See Section [13](#security) above.[¶](#section-14.2-2.5)
*   Interoperability considerations: See Sections [6.2](#language), [6.3](#integers), and [6.4](#regex) above.[¶](#section-14.2-2.6)
*   Fragment identifier considerations: See Section [5](#fragments)[¶](#section-14.2-2.7)

## [15\.](#section-15) [References](#name-references)

### [15.1.](#section-15.1) [Normative References](#name-normative-references)

\[RFC2119\]

Bradner, S., "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, RFC 2119, DOI 10.17487/RFC2119, March 1997, <[https://www.rfc-editor.org/info/rfc2119](https://www.rfc-editor.org/info/rfc2119)\>.

\[RFC3986\]

Berners-Lee, T., Fielding, R., and L. Masinter, "Uniform Resource Identifier (URI): Generic Syntax", STD 66, RFC 3986, DOI 10.17487/RFC3986, January 2005, <[https://www.rfc-editor.org/info/rfc3986](https://www.rfc-editor.org/info/rfc3986)\>.

\[RFC6839\]

Hansen, T. and A. Melnikov, "Additional Media Type Structured Syntax Suffixes", RFC 6839, DOI 10.17487/RFC6839, January 2013, <[https://www.rfc-editor.org/info/rfc6839](https://www.rfc-editor.org/info/rfc6839)\>.

\[RFC6901\]

Bryan, P., Ed., Zyp, K., and M. Nottingham, Ed., "JavaScript Object Notation (JSON) Pointer", RFC 6901, DOI 10.17487/RFC6901, April 2013, <[https://www.rfc-editor.org/info/rfc6901](https://www.rfc-editor.org/info/rfc6901)\>.

\[RFC8259\]

Bray, T., Ed., "The JavaScript Object Notation (JSON) Data Interchange Format", STD 90, RFC 8259, DOI 10.17487/RFC8259, December 2017, <[https://www.rfc-editor.org/info/rfc8259](https://www.rfc-editor.org/info/rfc8259)\>.

\[W3C.REC-ldp-20150226\]

Speicher, S., Arwe, J., and A. Malhotra, "Linked Data Platform 1.0", World Wide Web Consortium Recommendation REC-ldp-20150226, 26 February 2015, <[https://www.w3.org/TR/2015/REC-ldp-20150226](https://www.w3.org/TR/2015/REC-ldp-20150226)\>.

\[ecma262\]

"ECMA-262, 11th edition specification", June 2020, <[https://www.ecma-international.org/ecma-262/11.0/index.html](https://www.ecma-international.org/ecma-262/11.0/index.html)\>.

### [15.2.](#section-15.2) [Informative References](#name-informative-references)

\[RFC6596\]

Ohye, M. and J. Kupke, "The Canonical Link Relation", RFC 6596, DOI 10.17487/RFC6596, April 2012, <[https://www.rfc-editor.org/info/rfc6596](https://www.rfc-editor.org/info/rfc6596)\>.

\[RFC7049\]

Bormann, C. and P. Hoffman, "Concise Binary Object Representation (CBOR)", RFC 7049, DOI 10.17487/RFC7049, October 2013, <[https://www.rfc-editor.org/info/rfc7049](https://www.rfc-editor.org/info/rfc7049)\>.

\[RFC7231\]

Fielding, R., Ed. and J. Reschke, Ed., "Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content", RFC 7231, DOI 10.17487/RFC7231, June 2014, <[https://www.rfc-editor.org/info/rfc7231](https://www.rfc-editor.org/info/rfc7231)\>.

\[RFC8288\]

Nottingham, M., "Web Linking", RFC 8288, DOI 10.17487/RFC8288, October 2017, <[https://www.rfc-editor.org/info/rfc8288](https://www.rfc-editor.org/info/rfc8288)\>.

\[W3C.WD-fragid-best-practices-20121025\]

Tennison, J., "Best Practices for Fragment Identifiers and Media Type Definitions", World Wide Web Consortium WD WD-fragid-best-practices-20121025, 25 October 2012, <[https://www.w3.org/TR/2012/WD-fragid-best-practices-20121025](https://www.w3.org/TR/2012/WD-fragid-best-practices-20121025)\>.

\[json-schema-validation\]

Wright, A., Andrews, H., and B. Hutton, "JSON Schema Validation: A Vocabulary for Structural Validation of JSON", Work in Progress, Internet-Draft, draft-bhutton-json-schema-validation-01, June 2022, <[https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-01](https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-01)\>.

\[json-hyper-schema\]

Andrews, H. and A. Wright, "JSON Hyper-Schema: A Vocabulary for Hypermedia Annotation of JSON", Work in Progress, Internet-Draft, draft-handrews-json-schema-hyperschema-02, November 2017, <[https://datatracker.ietf.org/doc/html/draft-handrews-json-schema-hyperschema-02](https://datatracker.ietf.org/doc/html/draft-handrews-json-schema-hyperschema-02)\>.

\[xml-names\]

Bray, T., Ed., Hollander, D., Ed., Layman, A., Ed., and R. Tobin, Ed., "Namespaces in XML 1.1 (Second Edition)", August 2006, <[http://www.w3.org/TR/2006/REC-xml-names11-20060816](http://www.w3.org/TR/2006/REC-xml-names11-20060816)\>.

## [Appendix A.](#appendix-A) [Schema identification examples](#name-schema-identification-examp)

Consider the following schema, which shows "$id" being used to identify both the root schema and various subschemas, and "$anchor" being used to define plain name fragment identifiers.[¶](#appendix-A-1)

{
    "$id": "https://example.com/root.json",
    "$defs": {
        "A": { "$anchor": "foo" },
        "B": {
            "$id": "other.json",
            "$defs": {
                "X": { "$anchor": "bar" },
                "Y": {
                    "$id": "t/inner.json",
                    "$anchor": "bar"
                }
            }
        },
        "C": {
            "$id": "urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f"
        }
    }
}

[¶](#appendix-A-2)

The schemas at the following URI-encoded [JSON Pointers](#RFC6901) \[[RFC6901](#RFC6901)\] (relative to the root schema) have the following base URIs, and are identifiable by any listed URI in accordance with sections [5](#fragments) and [9.2.1](#embedded) above.[¶](#appendix-A-3)

\# (document root)

canonical (and base) URI

https://example.com/root.json[¶](#appendix-A-4.2.1.2)

canonical resource URI plus pointer fragment

https://example.com/root.json#[¶](#appendix-A-4.2.1.4)

#/$defs/A

base URI

https://example.com/root.json[¶](#appendix-A-4.4.1.2)

canonical resource URI plus plain fragment

https://example.com/root.json#foo[¶](#appendix-A-4.4.1.4)

canonical resource URI plus pointer fragment

https://example.com/root.json#/$defs/A[¶](#appendix-A-4.4.1.6)

#/$defs/B

canonical (and base) URI

https://example.com/other.json[¶](#appendix-A-4.6.1.2)

canonical resource URI plus pointer fragment

https://example.com/other.json#[¶](#appendix-A-4.6.1.4)

base URI of enclosing (root.json) resource plus fragment

https://example.com/root.json#/$defs/B[¶](#appendix-A-4.6.1.6)

#/$defs/B/$defs/X

base URI

https://example.com/other.json[¶](#appendix-A-4.8.1.2)

canonical resource URI plus plain fragment

https://example.com/other.json#bar[¶](#appendix-A-4.8.1.4)

canonical resource URI plus pointer fragment

https://example.com/other.json#/$defs/X[¶](#appendix-A-4.8.1.6)

base URI of enclosing (root.json) resource plus fragment

https://example.com/root.json#/$defs/B/$defs/X[¶](#appendix-A-4.8.1.8)

#/$defs/B/$defs/Y

canonical (and base) URI

https://example.com/t/inner.json[¶](#appendix-A-4.10.1.2)

canonical URI plus plain fragment

https://example.com/t/inner.json#bar[¶](#appendix-A-4.10.1.4)

canonical URI plus pointer fragment

https://example.com/t/inner.json#[¶](#appendix-A-4.10.1.6)

base URI of enclosing (other.json) resource plus fragment

https://example.com/other.json#/$defs/Y[¶](#appendix-A-4.10.1.8)

base URI of enclosing (root.json) resource plus fragment

https://example.com/root.json#/$defs/B/$defs/Y[¶](#appendix-A-4.10.1.10)

#/$defs/C

canonical (and base) URI

urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f[¶](#appendix-A-4.12.1.2)

canonical URI plus pointer fragment

urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f#[¶](#appendix-A-4.12.1.4)

base URI of enclosing (root.json) resource plus fragment

https://example.com/root.json#/$defs/C[¶](#appendix-A-4.12.1.6)

Note: The fragment part of the URI does not make it canonical or non-canonical, rather, the base URI used (as part of the full URI with any fragment) is what determines the canonical nature of the resulting full URI. Multiple "canonical" URIs? We Acknowledge this is potentially confusing, and direct you to read the CREF located in the [JSON Pointer fragments and embedded schema resources](#embedded) ([Section 9.2.1](#embedded)) section for futher comments. [¶](#appendix-A-5)

## [Appendix B.](#appendix-B) [Manipulating schema documents and references](#name-manipulating-schema-documen)

Various tools have been created to rearrange schema documents based on how and where references ("$ref") appear. This appendix discusses which use cases and actions are compliant with this specification.[¶](#appendix-B-1)

### [B.1.](#appendix-B.1) [Bundling schema resources into a single document](#name-bundling-schema-resources-i)

A set of schema resources intended for use together can be organized with each in its own schema document, all in the same schema document, or any granularity of document grouping in between.[¶](#appendix-B.1-1)

Numerous tools exist to perform various sorts of reference removal. A common case of this is producing a single file where all references can be resolved within that file. This is typically done to simplify distribution, or to simplify coding so that various invocations of JSON Schema libraries do not have to keep track of and load a large number of resources.[¶](#appendix-B.1-2)

This transformation can be safely and reversibly done as long as all static references (e.g. "$ref") use URI-references that resolve to URIs using the canonical resource URI as the base, and all schema resources have an absolute-URI as the "$id" in their root schema.[¶](#appendix-B.1-3)

With these conditions met, each external resource can be copied under "$defs", without breaking any references among the resources' schema objects, and without changing any aspect of validation or annotation results. The names of the schemas under "$defs" do not affect behavior, assuming they are each unique, as they do not appear in the canonical URIs for the embedded resources.[¶](#appendix-B.1-4)

### [B.2.](#appendix-B.2) [Reference removal is not always safe](#name-reference-removal-is-not-al)

Attempting to remove all references and produce a single schema document does not, in all cases, produce a schema with identical behavior to the original form.[¶](#appendix-B.2-1)

Since "$ref" is now treated like any other keyword, with other keywords allowed in the same schema objects, fully supporting non-recursive "$ref" removal in all cases can require relatively complex schema manipulations. It is beyond the scope of this specification to determine or provide a set of safe "$ref" removal transformations, as they depend not only on the schema structure but also on the intended usage.[¶](#appendix-B.2-2)

## [Appendix C.](#appendix-C) [Example of recursive schema extension](#name-example-of-recursive-schema)

Consider the following two schemas describing a simple recursive tree structure, where each node in the tree can have a "data" field of any type. The first schema allows and ignores other instance properties. The second is more strict and only allows the "data" and "children" properties. An example instance with "data" misspelled as "daat" is also shown.[¶](#appendix-C-1)

// tree schema, extensible
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/tree",
    "$dynamicAnchor": "node",

    "type": "object",
    "properties": {
        "data": true,
        "children": {
            "type": "array",
            "items": {
                "$dynamicRef": "#node"
            }
        }
    }
}

// strict-tree schema, guards against misspelled properties
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/strict-tree",
    "$dynamicAnchor": "node",

    "$ref": "tree",
    "unevaluatedProperties": false
}

// instance with misspelled field
{
    "children": \[ { "daat": 1 } \]
}

[¶](#appendix-C-2)

When we load these two schemas, we will notice the "$dynamicAnchor" named "node" (note the lack of "#" as this is just the name) present in each, resulting in the following full schema URIs:[¶](#appendix-C-3)

*   "https://example.com/tree#node"[¶](#appendix-C-4.1)
*   "https://example.com/strict-tree#node"[¶](#appendix-C-4.2)

In addition, JSON Schema implementations keep track of the fact that these fragments were created with "$dynamicAnchor".[¶](#appendix-C-5)

If we apply the "strict-tree" schema to the instance, we will follow the "$ref" to the "tree" schema, examine its "children" subschema, and find the "$dynamicRef": to "#node" (note the "#" for URI fragment syntax) in its "items" subschema. That reference resolves to "https://example.com/tree#node", which is a URI with a fragment created by "$dynamicAnchor". Therefore we must examine the dynamic scope before following the reference.[¶](#appendix-C-6)

At this point, the dynamic path is "#/$ref/properties/children/items/$dynamicRef", with a dynamic scope containing (from the outermost scope to the innermost):[¶](#appendix-C-7)

1.  "https://example.com/strict-tree#"[¶](#appendix-C-8.1)
2.  "https://example.com/tree#"[¶](#appendix-C-8.2)
3.  "https://example.com/tree#/properties/children"[¶](#appendix-C-8.3)
4.  "https://example.com/tree#/properties/children/items"[¶](#appendix-C-8.4)

Since we are looking for a plain name fragment, which can be defined anywhere within a schema resource, the JSON Pointer fragments are irrelevant to this check. That means that we can remove those fragments and eliminate consecutive duplicates, producing:[¶](#appendix-C-9)

1.  "https://example.com/strict-tree"[¶](#appendix-C-10.1)
2.  "https://example.com/tree"[¶](#appendix-C-10.2)

In this case, the outermost resource also has a "node" fragment defined by "$dynamicAnchor". Therefore instead of resolving the "$dynamicRef" to "https://example.com/tree#node", we resolve it to "https://example.com/strict-tree#node".[¶](#appendix-C-11)

This way, the recursion in the "tree" schema recurses to the root of "strict-tree", instead of only applying "strict-tree" to the instance root, but applying "tree" to instance children.[¶](#appendix-C-12)

This example shows both "$dynamicAnchor"s in the same place in each schema, specifically the resource root schema. Since plain-name fragments are independent of the JSON structure, this would work just as well if one or both of the node schema objects were moved under "$defs". It is the matching "$dynamicAnchor" values which tell us how to resolve the dynamic reference, not any sort of correlation in JSON structure.[¶](#appendix-C-13)

## [Appendix D.](#appendix-D) [Working with vocabularies](#name-working-with-vocabularies)

### [D.1.](#appendix-D.1) [Best practices for vocabulary and meta-schema authors](#name-best-practices-for-vocabula)

Vocabulary authors should take care to avoid keyword name collisions if the vocabulary is intended for broad use, and potentially combined with other vocabularies. JSON Schema does not provide any formal namespacing system, but also does not constrain keyword names, allowing for any number of namespacing approaches.[¶](#appendix-D.1-1)

Vocabularies may build on each other, such as by defining the behavior of their keywords with respect to the behavior of keywords from another vocabulary, or by using a keyword from another vocabulary with a restricted or expanded set of acceptable values. Not all such vocabulary re-use will result in a new vocabulary that is compatible with the vocabulary on which it is built. Vocabulary authors should clearly document what level of compatibility, if any, is expected.[¶](#appendix-D.1-2)

Meta-schema authors should not use "$vocabulary" to combine multiple vocabularies that define conflicting syntax or semantics for the same keyword. As semantic conflicts are not generally detectable through schema validation, implementations are not expected to detect such conflicts. If conflicting vocabularies are declared, the resulting behavior is undefined.[¶](#appendix-D.1-3)

Vocabulary authors SHOULD provide a meta-schema that validates the expected usage of the vocabulary's keywords on their own. Such meta-schemas SHOULD not forbid additional keywords, and MUST not forbid any keywords from the Core vocabulary.[¶](#appendix-D.1-4)

It is recommended that meta-schema authors reference each vocabulary's meta-schema using the ["allOf"](#allOf) ([Section 10.2.1.1](#allOf)) keyword, although other mechanisms for constructing the meta-schema may be appropriate for certain use cases.[¶](#appendix-D.1-5)

The recursive nature of meta-schemas makes the "$dynamicAnchor" and "$dynamicRef" keywords particularly useful for extending existing meta-schemas, as can be seen in the JSON Hyper-Schema meta-schema which extends the Validation meta-schema.[¶](#appendix-D.1-6)

Meta-schemas may impose additional constraints, including describing keywords not present in any vocabulary, beyond what the meta-schemas associated with the declared vocabularies describe. This allows for restricting usage to a subset of a vocabulary, and for validating locally defined keywords not intended for re-use.[¶](#appendix-D.1-7)

However, meta-schemas should not contradict any vocabularies that they declare, such as by requiring a different JSON type than the vocabulary expects. The resulting behavior is undefined.[¶](#appendix-D.1-8)

Meta-schemas intended for local use, with no need to test for vocabulary support in arbitrary implementations, can safely omit "$vocabulary" entirely.[¶](#appendix-D.1-9)

### [D.2.](#appendix-D.2) [Example meta-schema with vocabulary declarations](#name-example-meta-schema-with-vo)

This meta-schema explicitly declares both the Core and Applicator vocabularies, together with an extension vocabulary, and combines their meta-schemas with an "allOf". The extension vocabulary's meta-schema, which describes only the keywords in that vocabulary, is shown after the main example meta-schema.[¶](#appendix-D.2-1)

The main example meta-schema also restricts the usage of the Unevaluated vocabulary by forbidding the keywords prefixed with "unevaluated", which are particularly complex to implement. This does not change the semantics or set of keywords defined by the other vocabularies. It just ensures that schemas using this meta-schema that attempt to use the keywords prefixed with "unevaluated" will fail validation against this meta-schema.[¶](#appendix-D.2-2)

Finally, this meta-schema describes the syntax of a keyword, "localKeyword", that is not part of any vocabulary. Presumably, the implementors and users of this meta-schema will understand the semantics of "localKeyword". JSON Schema does not define any mechanism for expressing keyword semantics outside of vocabularies, making them unsuitable for use except in a specific environment in which they are understood.[¶](#appendix-D.2-3)

This meta-schema combines several vocabularies for general use.[¶](#appendix-D.2-4)

{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://example.com/meta/general-use-example",
  "$dynamicAnchor": "meta",
  "$vocabulary": {
    "https://json-schema.org/draft/2020-12/vocab/core": true,
    "https://json-schema.org/draft/2020-12/vocab/applicator": true,
    "https://json-schema.org/draft/2020-12/vocab/validation": true,
    "https://example.com/vocab/example-vocab": true
  },
  "allOf": \[
    {"$ref": "https://json-schema.org/draft/2020-12/meta/core"},
    {"$ref": "https://json-schema.org/draft/2020-12/meta/applicator"},
    {"$ref": "https://json-schema.org/draft/2020-12/meta/validation"},
    {"$ref": "https://example.com/meta/example-vocab"}
  \],
  "patternProperties": {
    "^unevaluated": false
  },
  "properties": {
    "localKeyword": {
      "$comment": "Not in vocabulary, but validated if used",
      "type": "string"
    }
  }
}

[¶](#appendix-D.2-5)

This meta-schema describes only a single extension vocabulary.[¶](#appendix-D.2-6)

{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://example.com/meta/example-vocab",
  "$dynamicAnchor": "meta",
  "$vocabulary": {
    "https://example.com/vocab/example-vocab": true,
  },
  "type": \["object", "boolean"\],
  "properties": {
    "minDate": {
      "type": "string",
      "pattern": "\\d\\d\\d\\d-\\d\\d-\\d\\d",
      "format": "date",
    }
  }
}

[¶](#appendix-D.2-7)

As shown above, even though each of the single-vocabulary meta-schemas referenced in the general-use meta-schema's "allOf" declares its corresponding vocabulary, this new meta-schema must re-declare them.[¶](#appendix-D.2-8)

The standard meta-schemas that combine all vocabularies defined by the Core and Validation specification, and that combine all vocabularies defined by those specifications as well as the Hyper-Schema specification, demonstrate additional complex combinations. These URIs for these meta-schemas may be found in the Validation and Hyper-Schema specifications, respectively.[¶](#appendix-D.2-9)

While the general-use meta-schema can validate the syntax of "minDate", it is the vocabulary that defines the logic behind the semantic meaning of "minDate". Without an understanding of the semantics (in this example, that the instance value must be a date equal to or after the date provided as the keyword's value in the schema), an implementation can only validate the syntactic usage. In this case, that means validating that it is a date-formatted string (using "pattern" to ensure that it is validated even when "format" functions purely as an annotation, as explained in the [Validation specification](#json-schema-validation) \[[json-schema-validation](#json-schema-validation)\].[¶](#appendix-D.2-10)

## [Appendix E.](#appendix-E) [References and generative use cases](#name-references-and-generative-u)

While the presence of references is expected to be transparent to validation results, generative use cases such as code generators and UI renderers often consider references to be semantically significant.[¶](#appendix-E-1)

To make such use case-specific semantics explicit, the best practice is to create an annotation keyword for use in the same schema object alongside of a reference keyword such as "$ref".[¶](#appendix-E-2)

For example, here is a hypothetical keyword for determining whether a code generator should consider the reference target to be a distinct class, and how those classes are related. Note that this example is solely for illustrative purposes, and is not intended to propose a functional code generation keyword.[¶](#appendix-E-3)

{
    "allOf": \[
        {
            "classRelation": "is-a",
            "$ref": "classes/base.json"
        },
        {
            "$ref": "fields/common.json"
        }
    \],
    "properties": {
        "foo": {
            "classRelation": "has-a",
            "$ref": "classes/foo.json"
        },
        "date": {
            "$ref": "types/dateStruct.json",
        }
    }
}

[¶](#appendix-E-4)

Here, this schema represents some sort of object-oriented class. The first reference in the "allOf" is noted as the base class. The second is not assigned a class relationship, meaning that the code generator should combine the target's definition with this one as if no reference were involved.[¶](#appendix-E-5)

Looking at the properties, "foo" is flagged as object composition, while the "date" property is not. It is simply a field with sub-fields, rather than an instance of a distinct class.[¶](#appendix-E-6)

This style of usage requires the annotation to be in the same object as the reference, which must be recognizable as a reference.[¶](#appendix-E-7)

## [Appendix F.](#appendix-F) [Acknowledgments](#name-acknowledgments)

Thanks to Gary Court, Francis Galiegue, Kris Zyp, and Geraint Luff for their work on the initial drafts of JSON Schema.[¶](#appendix-F-1)

Thanks to Jason Desrosiers, Daniel Perrett, Erik Wilde, Evgeny Poberezkin, Brad Bowman, Gowry Sankar, Donald Pipowitch, Dave Finlay, Denis Laxalde, Phil Sturgeon, Shawn Silverman, and Karen Etheridge for their submissions and patches to the document.[¶](#appendix-F-2)

## [Appendix G.](#appendix-G) [ChangeLog](#name-changelog)

This section to be removed before leaving Internet-Draft status.[¶](#appendix-G-1)

draft-bhutton-json-schema-01

*   Improve and clarify the "type", "contains", "unevaluatedProperties", and "unevaluatedItems" keyword explanations[¶](#appendix-G-2.2.1.1)
*   Clarify various aspects of "canonical URIs"[¶](#appendix-G-2.2.1.2)
*   Comment on ambiguity around annotations and "additionalProperties"[¶](#appendix-G-2.2.1.3)
*   Clarify Vocabularies need not be formally defined[¶](#appendix-G-2.2.1.4)
*   Remove references to remaining media-type parameters[¶](#appendix-G-2.2.1.5)
*   Fix multiple examples[¶](#appendix-G-2.2.1.6)

draft-bhutton-json-schema-00

*   "$schema" MAY change for embedded resources[¶](#appendix-G-2.4.1.1)
*   Array-value "items" functionality is now "prefixItems"[¶](#appendix-G-2.4.1.2)
*   "items" subsumes the old function of "additionalItems"[¶](#appendix-G-2.4.1.3)
*   "contains" annotation behavior, and "contains" and "unevaluatedItems" interactions now specified[¶](#appendix-G-2.4.1.4)
*   Rename $recursive\* to $dynamic\*, with behavior modification[¶](#appendix-G-2.4.1.5)
*   $dynamicAnchor defines a fragment like $anchor[¶](#appendix-G-2.4.1.6)
*   $dynamic\* (previously $recursive) no longer use runtime base URI determination[¶](#appendix-G-2.4.1.7)
*   Define Compound Schema Documents (bundle) and processing[¶](#appendix-G-2.4.1.8)
*   Reference ECMA-262, 11th edition for regular expression support[¶](#appendix-G-2.4.1.9)
*   Regular expression should support unicode[¶](#appendix-G-2.4.1.10)
*   Remove media type parameters[¶](#appendix-G-2.4.1.11)
*   Specify Unknown keywords are collected as annotations[¶](#appendix-G-2.4.1.12)
*   Moved "unevaluatedItems" and "unevaluatedProperties" from core into their own vocabulary[¶](#appendix-G-2.4.1.13)

draft-handrews-json-schema-02

*   Update to RFC 8259 for JSON specification[¶](#appendix-G-2.6.1.1)
*   Moved "definitions" from the Validation specification here as "$defs"[¶](#appendix-G-2.6.1.2)
*   Moved applicator keywords from the Validation specification as their own vocabulary[¶](#appendix-G-2.6.1.3)
*   Moved the schema form of "dependencies" from the Validation specification as "dependentSchemas"[¶](#appendix-G-2.6.1.4)
*   Formalized annotation collection[¶](#appendix-G-2.6.1.5)
*   Specified recommended output formats[¶](#appendix-G-2.6.1.6)
*   Defined keyword interactions in terms of annotation and assertion results[¶](#appendix-G-2.6.1.7)
*   Added "unevaluatedProperties" and "unevaluatedItems"[¶](#appendix-G-2.6.1.8)
*   Define "$ref" behavior in terms of the assertion, applicator, and annotation model[¶](#appendix-G-2.6.1.9)
*   Allow keywords adjacent to "$ref"[¶](#appendix-G-2.6.1.10)
*   Note undefined behavior for "$ref" targets involving unknown keywords[¶](#appendix-G-2.6.1.11)
*   Add recursive referencing, primarily for meta-schema extension[¶](#appendix-G-2.6.1.12)
*   Add the concept of formal vocabularies, and how they can be recognized through meta-schemas[¶](#appendix-G-2.6.1.13)
*   Additional guidance on initial base URIs beyond network retrieval[¶](#appendix-G-2.6.1.14)
*   Allow "schema" media type parameter for "application/schema+json"[¶](#appendix-G-2.6.1.15)
*   Better explanation of media type parameters and the HTTP Accept header[¶](#appendix-G-2.6.1.16)
*   Use "$id" to establish canonical and base absolute-URIs only, no fragments[¶](#appendix-G-2.6.1.17)
*   Replace plain-name-fragment-only form of "$id" with "$anchor"[¶](#appendix-G-2.6.1.18)
*   Clarified that the behavior of JSON Pointers across "$id" boundary is unreliable[¶](#appendix-G-2.6.1.19)

draft-handrews-json-schema-01

*   This draft is purely a clarification with no functional changes[¶](#appendix-G-2.8.1.1)
*   Emphasized annotations as a primary usage of JSON Schema[¶](#appendix-G-2.8.1.2)
*   Clarified $id by use cases[¶](#appendix-G-2.8.1.3)
*   Exhaustive schema identification examples[¶](#appendix-G-2.8.1.4)
*   Replaced "external referencing" with how and when an implementation might know of a schema from another document[¶](#appendix-G-2.8.1.5)
*   Replaced "internal referencing" with how an implementation should recognized schema identifiers during parsing[¶](#appendix-G-2.8.1.6)
*   Dereferencing the former "internal" or "external" references is always the same process[¶](#appendix-G-2.8.1.7)
*   Minor formatting improvements[¶](#appendix-G-2.8.1.8)

draft-handrews-json-schema-00

*   Make the concept of a schema keyword vocabulary more clear[¶](#appendix-G-2.10.1.1)
*   Note that the concept of "integer" is from a vocabulary, not the data model[¶](#appendix-G-2.10.1.2)
*   Classify keywords as assertions or annotations and describe their general behavior[¶](#appendix-G-2.10.1.3)
*   Explain the boolean schemas in terms of generalized assertions[¶](#appendix-G-2.10.1.4)
*   Reserve "$comment" for non-user-visible notes about the schema[¶](#appendix-G-2.10.1.5)
*   Wording improvements around "$id" and fragments[¶](#appendix-G-2.10.1.6)
*   Note the challenges of extending meta-schemas with recursive references[¶](#appendix-G-2.10.1.7)
*   Add "application/schema-instance+json" media type[¶](#appendix-G-2.10.1.8)
*   Recommend a "schema" link relation / parameter instead of "profile"[¶](#appendix-G-2.10.1.9)

draft-wright-json-schema-01

*   Updated intro[¶](#appendix-G-2.12.1.1)
*   Allowed for any schema to be a boolean[¶](#appendix-G-2.12.1.2)
*   "$schema" SHOULD NOT appear in subschemas, although that may change[¶](#appendix-G-2.12.1.3)
*   Changed "id" to "$id"; all core keywords prefixed with "$"[¶](#appendix-G-2.12.1.4)
*   Clarify and formalize fragments for application/schema+json[¶](#appendix-G-2.12.1.5)
*   Note applicability to formats such as CBOR that can be represented in the JSON data model[¶](#appendix-G-2.12.1.6)

draft-wright-json-schema-00

*   Updated references to JSON[¶](#appendix-G-2.14.1.1)
*   Updated references to HTTP[¶](#appendix-G-2.14.1.2)
*   Updated references to JSON Pointer[¶](#appendix-G-2.14.1.3)
*   Behavior for "id" is now specified in terms of RFC3986[¶](#appendix-G-2.14.1.4)
*   Aligned vocabulary usage for URIs with RFC3986[¶](#appendix-G-2.14.1.5)
*   Removed reference to draft-pbryan-zyp-json-ref-03[¶](#appendix-G-2.14.1.6)
*   Limited use of "$ref" to wherever a schema is expected[¶](#appendix-G-2.14.1.7)
*   Added definition of the "JSON Schema data model"[¶](#appendix-G-2.14.1.8)
*   Added additional security considerations[¶](#appendix-G-2.14.1.9)
*   Defined use of subschema identifiers for "id"[¶](#appendix-G-2.14.1.10)
*   Rewrote section on usage with HTTP[¶](#appendix-G-2.14.1.11)
*   Rewrote section on usage with rel="describedBy" and rel="profile"[¶](#appendix-G-2.14.1.12)
*   Fixed numerous invalid examples[¶](#appendix-G-2.14.1.13)

draft-zyp-json-schema-04

*   Salvaged from draft v3.[¶](#appendix-G-2.16.1.1)
*   Split validation keywords into separate document.[¶](#appendix-G-2.16.1.2)
*   Split hypermedia keywords into separate document.[¶](#appendix-G-2.16.1.3)
*   Initial post-split draft.[¶](#appendix-G-2.16.1.4)
*   Mandate the use of JSON Reference, JSON Pointer.[¶](#appendix-G-2.16.1.5)
*   Define the role of "id". Define URI resolution scope.[¶](#appendix-G-2.16.1.6)
*   Add interoperability considerations.[¶](#appendix-G-2.16.1.7)

draft-zyp-json-schema-00

*   Initial draft.[¶](#appendix-G-2.18.1.1)

## [Authors' Addresses](#name-authors-addresses)

Austin Wright (editor)

Email: [\[email protected\]](/cdn-cgi/l/email-protection#03626262436179657b2d6d6677)

Henry Andrews (editor)

Email: [\[email protected\]](/cdn-cgi/l/email-protection#e2838c8690879591bd8a878c909ba29b838a8d8dcc818d8f)

Ben Hutton (editor)

Postman

Email: [\[email protected\]](/cdn-cgi/l/email-protection#0361666d4369706c6d70606b666e622d676675)

URI: [https://jsonschema.dev](https://jsonschema.dev)

Greg Dennis

Email: [\[email protected\]](/cdn-cgi/l/email-protection#2d4a5f484a5e49484343445e6d544c454242034e4240)

URI: [https://github.com/gregsdennis](https://github.com/gregsdennis)