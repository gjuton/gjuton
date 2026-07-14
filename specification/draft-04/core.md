 JSON Schema: core definitions and terminology                                                  

<table class="header"><tbody><tr><td class="left">Internet Engineering Task Force</td><td class="right">F. Galiegue, Ed.</td></tr><tr><td class="left">Internet-Draft</td><td class="right"></td></tr><tr><td class="left">Intended status: Informational</td><td class="right">K. Zyp, Ed.</td></tr><tr><td class="left">Expires: August 26, 2017</td><td class="right">SitePen (USA)</td></tr><tr><td class="left"></td><td class="right">G. Court</td></tr><tr><td class="left"></td><td class="right">2013</td></tr></tbody></table>

JSON Schema: core definitions and terminology  
draft-zyp-json-schema-04

# [Abstract](#rfc.abstract)

JSON Schema defines the media type "application/schema+json", a JSON based format for defining the structure of JSON data. JSON Schema provides a contract for what JSON data is required for a given application and how to interact with it. JSON Schema is intended to define validation, documentation, hyperlink navigation, and interaction control of JSON data.

# [Status of This Memo](#rfc.status)

This Internet-Draft is submitted in full conformance with the provisions of BCP 78 and BCP 79.

Internet-Drafts are working documents of the Internet Engineering Task Force (IETF). Note that other groups may also distribute working documents as Internet-Drafts. The list of current Internet-Drafts is at http://datatracker.ietf.org/drafts/current/.

Internet-Drafts are draft documents valid for a maximum of six months and may be updated, replaced, or obsoleted by other documents at any time. It is inappropriate to use Internet-Drafts as reference material or to cite them other than as "work in progress."

This Internet-Draft will expire on August 26, 2017.

# [Copyright Notice](#rfc.copyrightnotice)

Copyright (c) 2013 IETF Trust and the persons identified as the document authors. All rights reserved.

This document is subject to BCP 78 and the IETF Trust's Legal Provisions Relating to IETF Documents (http://trustee.ietf.org/license-info) in effect on the date of publication of this document. Please review these documents carefully, as they describe your rights and restrictions with respect to this document. Code Components extracted from this document must include Simplified BSD License text as described in Section 4.e of the Trust Legal Provisions and are provided without warranty as described in the Simplified BSD License.

* * *

# [Table of Contents](#rfc.toc)

*   1\. [Introduction](#rfc.section.1)
*   2\. [Conventions and Terminology](#rfc.section.2)
*   3\. [Core terminology](#rfc.section.3)

*   3.1. [Property, item](#rfc.section.3.1)
*   3.2. [JSON Schema, keywords](#rfc.section.3.2)
*   3.3. [Empty schema](#rfc.section.3.3)
*   3.4. [Root schema, subschema](#rfc.section.3.4)
*   3.5. [JSON Schema primitive types](#rfc.section.3.5)
*   3.6. [JSON value equality](#rfc.section.3.6)
*   3.7. [Instance](#rfc.section.3.7)

*   4\. [Overview](#rfc.section.4)

*   4.1. [Validation](#rfc.section.4.1)
*   4.2. [Hypermedia and linking](#rfc.section.4.2)

*   5\. [General considerations](#rfc.section.5)

*   5.1. [Applicability to all JSON values](#rfc.section.5.1)
*   5.2. [Programming language independence](#rfc.section.5.2)
*   5.3. [JSON Schema and HTTP](#rfc.section.5.3)
*   5.4. [JSON Schema and other protocols](#rfc.section.5.4)
*   5.5. [Mathematical integers](#rfc.section.5.5)
*   5.6. [Extending JSON Schema](#rfc.section.5.6)
*   5.7. [Security considerations](#rfc.section.5.7)

*   6\. [The "$schema" keyword](#rfc.section.6)

*   6.1. [Purpose](#rfc.section.6.1)
*   6.2. [Customization](#rfc.section.6.2)

*   7\. [URI resolution scopes and dereferencing](#rfc.section.7)

*   7.1. [Definition](#rfc.section.7.1)
*   7.2. [URI resolution scope alteration with the "id" keyword](#rfc.section.7.2)

*   7.2.1. [Valid values](#rfc.section.7.2.1)
*   7.2.2. [Usage](#rfc.section.7.2.2)
*   7.2.3. [Canonical dereferencing and inline dereferencing](#rfc.section.7.2.3)
*   7.2.4. [Inline dereferencing and fragments](#rfc.section.7.2.4)

*   7.3. [Interoperability considerations](#rfc.section.7.3)

*   8\. [Recommended correlation mechanisms for use with the HTTP protocol](#rfc.section.8)

*   8.1. [Correlation by means of the "Content-Type" header](#rfc.section.8.1)
*   8.2. [Correlation by means of the "Link" header](#rfc.section.8.2)

*   9\. [IANA Considerations](#rfc.section.9)
*   10\. [References](#rfc.references)

*   10.1. [Normative References](#rfc.references.1)
*   10.2. [Informative References](#rfc.references.2)

*   Appendix A. [ChangeLog](#rfc.appendix.A)
*   [Authors' Addresses](#rfc.authors)

# [1.](#rfc.section.1) Introduction

JSON Schema is a JSON media type for defining the structure of JSON data. JSON Schema provides a contract for what JSON data is required for a given application and how to interact with it. JSON Schema is intended to define validation, documentation, hyperlink navigation, and interaction control of JSON data.

This specification defines JSON Schema core terminology and mechanisms; related specifications build upon this specification and define different applications of JSON Schema.

# [2.](#rfc.section.2) Conventions and Terminology

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC 2119](#RFC2119) \[RFC2119\].

The terms "JSON", "JSON text", "JSON value", "member", "element", "object", "array", "number", "string", "boolean", "true", "false", and "null" in this document are to be interpreted as defined in [RFC 4627](#RFC4627) \[RFC4627\].

# [3.](#rfc.section.3) Core terminology

# [3.1.](#rfc.section.3.1) Property, item

When refering to a JSON Object, as defined by [\[RFC4627\]](#RFC4627), the terms "member" and "property" may be used interchangeably.

When refering to a JSON Array, as defined by [\[RFC4627\]](#RFC4627), the terms "element" and "item" may be used interchangeably.

# [3.2.](#rfc.section.3.2) JSON Schema, keywords

A JSON Schema is a JSON document, and that document MUST be an object. Object members (or properties) defined by JSON Schema (this specification, or related specifications) are called keywords, or schema keywords.

A JSON Schema MAY contain properties which are not schema keywords.

# [3.3.](#rfc.section.3.3) Empty schema

An empty schema is a JSON Schema with no properties, or with properties which are not schema keywords.

# [3.4.](#rfc.section.3.4) Root schema, subschema

This example of a JSON Schema has no subschemas:

{
    "title": "root"
}

                    

JSON Schemas can also be nested, as in this example:

{
    "title": "root",
    "otherSchema": {
        "title": "nested",
        "anotherSchema": {
            "title": "alsoNested"
        }
    }
}

                    

In this example, "nested" and "alsoNested" are subschemas, and "root" is a root schema.

# [3.5.](#rfc.section.3.5) JSON Schema primitive types

JSON Schema defines seven primitive types for JSON values:

array

A JSON array.

boolean

A JSON boolean.

integer

A JSON number without a fraction or exponent part.

number

Any JSON number. Number includes integer.

null

The JSON null value.

object

A JSON object.

string

A JSON string.

# [3.6.](#rfc.section.3.6) JSON value equality

Two JSON values are said to be equal if and only if:

*   both are nulls; or
*   both are booleans, and have the same value; or
*   both are strings, and have the same value; or
*   both are numbers, and have the same mathematical value; or
*   both are arrays, and:
    
    *   have the same number of items; and
    *   items at the same index are equal according to this definition; or
    
*   both are objects, and:
    
    *   have the same set of property names; and
    *   values for a same property name are equal according to this definition.
    

# [3.7.](#rfc.section.3.7) Instance

An instance is any JSON value. An instance may be described by one or more schemas.

An instance may also be referred to as "JSON instance", or "JSON data".

# [4.](#rfc.section.4) Overview

This document proposes a new media type "application/schema+json" to identify JSON Schema for describing JSON data. JSON Schemas are themselves written in JSON. This, and related specifications, define keywords allowing to describe this data in terms of allowable values, textual descriptions and interpreting relations with other resources. The following sections are a summary of features defined by related specifications.

# [4.1.](#rfc.section.4.1) Validation

JSON Schema allows applications to validate instances, either non interactively or interactively. For instance, an application may collect JSON data and check that this data matches a given set of constraints; another application may use a JSON Schema to build an interactive interface in order to collect user input according to constraints described by JSON Schema.

# [4.2.](#rfc.section.4.2) Hypermedia and linking

JSON Schema provides a method for extracting link relations from instances to other resources, as well as describing interpretations of instances as multimedia data. This allows JSON data to be interpreted as rich hypermedia documents, placed in the context of a larger set of related resources.

# [5.](#rfc.section.5) General considerations

# [5.1.](#rfc.section.5.1) Applicability to all JSON values

It is acknowledged that an instance may be any valid JSON value as defined by [\[RFC4627\]](#RFC4627). As such, JSON Schema does not mandate that an instance be of a particular type: JSON Schema can describe any JSON value, including null.

# [5.2.](#rfc.section.5.2) Programming language independence

JSON Schema is programming language agnostic. The only limitations are the ones expressed by [\[RFC4627\]](#RFC4627) and those of the host programming language.

# [5.3.](#rfc.section.5.3) JSON Schema and HTTP

This specification acknowledges the role of [HTTP](#RFC2616) \[RFC2616\] as the dominant protocol in use on the Internet, and the wealth of official specifications related to it.

This specification uses a subset of these specifications to recommend a set of mechanisms, usable by this protocol, to associate JSON instances to one or more schemas.

# [5.4.](#rfc.section.5.4) JSON Schema and other protocols

JSON Schema does not define any semantics for the client-server interface for any other protocols than HTTP. These semantics are application dependent, or subject to agreement between the parties involved in the use of JSON Schema for their own needs.

# [5.5.](#rfc.section.5.5) Mathematical integers

It is acknowledged by this specification that some programming languages, and their associated parsers, use different internal representations for floating point numbers and integers, while others do not.

As a consequence, for interoperability reasons, JSON values used in the context of JSON Schema, whether that JSON be a JSON Schema or an instance, SHOULD ensure that mathematical integers be represented as integers as defined by this specification.

# [5.6.](#rfc.section.5.6) Extending JSON Schema

Implementations MAY choose to define additional keywords to JSON Schema. Save for explicit agreement, schema authors SHALL NOT expect these additional keywords to be supported by peer implementations. Implementations SHOULD ignore keywords they do not support.

# [5.7.](#rfc.section.5.7) Security considerations

Both schemas and instances are JSON values. As such, all security considerations defined in [RFC 4627](#RFC4627) \[RFC4627\] apply.

# [6.](#rfc.section.6) The "$schema" keyword

# [6.1.](#rfc.section.6.1) Purpose

The "$schema" keyword is both used as a JSON Schema version identifier and the location of a resource which is itself a JSON Schema, which describes any schema written for this particular version.

This keyword MUST be located at the root of a JSON Schema. The value of this keyword MUST be a [URI](#RFC3986) \[RFC3986\] and a valid [JSON Reference](#json-reference) \[json-reference\]; this URI MUST be both absolute and normalized. The resource located at this URI MUST successfully describe itself. It is RECOMMENDED that schema authors include this keyword in their schemas.

The following values are predefined:

http://json-schema.org/schema#

JSON Schema written against the current version of the specification.

http://json-schema.org/hyper-schema#

JSON Schema written against the current version of the specification.

http://json-schema.org/draft-04/schema#

JSON Schema written against this version.

http://json-schema.org/draft-04/hyper-schema#

JSON Schema hyperschema written against this version.

http://json-schema.org/draft-03/schema#

JSON Schema written against [JSON Schema, draft v3](#json-schema-03) \[json-schema-03\].

http://json-schema.org/draft-03/hyper-schema#

JSON Schema hyperschema written against [JSON Schema, draft v3](#json-schema-03) \[json-schema-03\].

# [6.2.](#rfc.section.6.2) Customization

When extending JSON Schema with custom keywords, schema authors SHOULD define a custom URI for "$schema". This custom URI MUST NOT be one of the predefined values.

# [7.](#rfc.section.7) URI resolution scopes and dereferencing

# [7.1.](#rfc.section.7.1) Definition

JSON Schema uses [JSON Reference](#json-reference) \[json-reference\] as a mechanism for schema addressing. It extends this specification in two ways:

*   JSON Schema offers facilities to alter the base URI against which a reference must resolve by the means of the "id" keyword;
*   it defines a specific dereferencing mechanism extending JSON Reference to accept arbitrary fragment parts.

Altering the URI within a schema is called defining a new resolution scope. The initial resolution scope of a schema is the URI of the schema itself, if any, or the empty URI if the schema was not loaded from a URI.

# [7.2.](#rfc.section.7.2) URI resolution scope alteration with the "id" keyword

# [7.2.1.](#rfc.section.7.2.1) Valid values

The value for this keyword MUST be a string, and MUST be a valid URI. This URI MUST be normalized, and SHOULD NOT be an empty fragment (#) or the empty URI.

# [7.2.2.](#rfc.section.7.2.2) Usage

The "id" keyword (or "id", for short) is used to alter the resolution scope. When an id is encountered, an implementation MUST resolve this id against the most immediate parent scope. The resolved URI will be the new resolution scope for this subschema and all its children, until another id is encountered.

When using "id" to alter resolution scopes, schema authors SHOULD ensure that resolution scopes are unique within the schema.

This schema will be taken as an example:

{
    "id": "http://x.y.z/rootschema.json#",
    "schema1": {
        "id": "#foo"
    },
    "schema2": {
        "id": "otherschema.json",
        "nested": {
            "id": "#bar"
        },
        "alsonested": {
            "id": "t/inner.json#a"
        }
    },
    "schema3": {
        "id": "some://where.else/completely#"
    }
}

                        

Subschemas at the following URI-encoded [JSON Pointer](#json-pointer) \[json-pointer\]s (starting from the root schema) define the following resolution scopes:

\# (document root)

http://x.y.z/rootschema.json#

#/schema1

http://x.y.z/rootschema.json#foo

#/schema2

http://x.y.z/otherschema.json#

#/schema2/nested

http://x.y.z/otherschema.json#bar

#/schema2/alsonested

http://x.y.z/t/inner.json#a

#/schema3

some://where.else/completely#

# [7.2.3.](#rfc.section.7.2.3) Canonical dereferencing and inline dereferencing

When resolving a URI against a resolution scope, an implementation may choose two modes of operation:

canonical dereferencing

The implementation dereferences all resolved URIs.

inline dereferencing

The implementation chooses to dereference URIs within the schema.

Implementations MUST support canonical dereferencing, and MAY support inline dereferencing.

For example, consider this schema:

{
    "id": "http://my.site/myschema#",
    "definitions": {
        "schema1": {
            "id": "schema1",
            "type": "integer"
        },
        "schema2", {
            "type": "array",
            "items": { "$ref": "schema1" }
        }
    }
}

                        

When an implementation encounters the "schema1" reference, it resolves it against the most immediate parent scope, leading to URI "http://my.site/schema1#". The way to process this URI will differ according to the chosen dereferencing mode:

*   if canonical dereferencing is used, the implementation will dereference this URI, and fetch the content at this URI;
*   if inline dereferencing is used, the implementation will notice that URI scope "http://my.site/schema1#" is already defined within the schema, and choose to use the appropriate subschema.

# [7.2.4.](#rfc.section.7.2.4) Inline dereferencing and fragments

When using inline dereferencing, a resolution scope may lead to a URI which has a non empty fragment part which is not a JSON Pointer, as in this example:

{
    "id": "http://some.site/schema#",
    "not": { "$ref": "#inner" },
    "definitions": {
        "schema1": {
            "id": "#inner",
            "type": "boolean"
        }
    }
}

                        

An implementation choosing to support inline dereferencing SHOULD be able to use this kind of reference. Implementations choosing to use canonical dereferencing, however, are not required to support it.

# [7.3.](#rfc.section.7.3) Interoperability considerations

Inline dereferencing can produce canonical URIs which differ from the canonical URI of the root schema. Schema authors SHOULD ensure that implementations using canonical dereferencing obtain the same content as implementations using inline dereferencing.

Extended JSON References using fragments which are not JSON Pointers are not dereferenceable by implementations choosing not to support inline dereferencing. This kind of reference is defined for backwards compatibility, and SHOULD NOT be used in new schemas.

# [8.](#rfc.section.8) Recommended correlation mechanisms for use with the HTTP protocol

It is acknowledged by this specification that the majority of interactive JSON Schema processing will be over HTTP. This section therefore gives recommendations for materializing an instance/schema correlation using mechanisms currently available for this protocol. An instance is said to be described by one (or more) schema(s).

# [8.1.](#rfc.section.8.1) Correlation by means of the "Content-Type" header

It is RECOMMENDED that a MIME type parameter by the name of "profile" be appended to the "Content-Type" header of the instance being processed. If present, the value of this parameter MUST be a valid URI, and this URI SHOULD resolve to a valid JSON Schema. The MIME type MUST be "application/json", or any other subtype.

An example of such a header would be:

Content-Type: application/my-media-type+json;
          profile=http://example.com/my-hyper-schema#

                    

# [8.2.](#rfc.section.8.2) Correlation by means of the "Link" header

When using the "Link" header, the relation type used MUST be "describedBy", as defined by [RFC 5988, section 5.3](#RFC5988) \[RFC5988\]. The target URI of the "Link" header MUST be a valid JSON Schema.

An example of such a header would be:

Link: <http://example.com/my-hyper-schema#>; rel="describedBy"

                    

# [9.](#rfc.section.9) IANA Considerations

The proposed MIME media type for JSON Schema is defined as follows:

*   type name: application;
*   subtype name: schema+json.

# [10.](#rfc.references) References

# [10.1.](#rfc.references.1) Normative References

<table><tbody><tr><td class="reference"><b id="RFC2119">[RFC2119]</b></td><td class="top"><a>Bradner, S.</a>, "<a href="http://tools.ietf.org/html/rfc2119">Key words for use in RFCs to Indicate Requirement Levels</a>", BCP 14, RFC 2119, DOI 10.17487/RFC2119, March 1997.</td></tr></tbody></table>

# [10.2.](#rfc.references.2) Informative References

<table><tbody><tr><td class="reference"><b id="RFC2616">[RFC2616]</b></td><td class="top"><a>Fielding, R.</a>, <a>Gettys, J.</a>, <a>Mogul, J.</a>, <a>Frystyk, H.</a>, <a>Masinter, L.</a>, <a>Leach, P.</a> and <a>T. Berners-Lee</a>, "<a href="http://tools.ietf.org/html/rfc2616">Hypertext Transfer Protocol -- HTTP/1.1</a>", RFC 2616, DOI 10.17487/RFC2616, June 1999.</td></tr><tr><td class="reference"><b id="RFC3986">[RFC3986]</b></td><td class="top"><a>Berners-Lee, T.</a>, <a>Fielding, R.</a> and <a>L. Masinter</a>, "<a href="http://tools.ietf.org/html/rfc3986">Uniform Resource Identifier (URI): Generic Syntax</a>", STD 66, RFC 3986, DOI 10.17487/RFC3986, January 2005.</td></tr><tr><td class="reference"><b id="RFC4627">[RFC4627]</b></td><td class="top"><a>Crockford, D.</a>, "<a href="http://tools.ietf.org/html/rfc4627">The application/json Media Type for JavaScript Object Notation (JSON)</a>", RFC 4627, DOI 10.17487/RFC4627, July 2006.</td></tr><tr><td class="reference"><b id="RFC5988">[RFC5988]</b></td><td class="top"><a>Nottingham, M.</a>, "<a href="http://tools.ietf.org/html/rfc5988">Web Linking</a>", RFC 5988, DOI 10.17487/RFC5988, October 2010.</td></tr><tr><td class="reference"><b id="json-reference">[json-reference]</b></td><td class="top"><a title="ForgeRock US, Inc.">Bryan, P.</a> and <a title="SitePen (USA)">K. Zyp</a>, "<a href="http://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03">JSON Reference (work in progress)</a>", September 2012.</td></tr><tr><td class="reference"><b id="json-pointer">[json-pointer]</b></td><td class="top"><a title="ForgeRock US, Inc.">Bryan, P.</a> and <a title="SitePen (USA)">K. Zyp</a>, "<a href="http://tools.ietf.org/html/draft-ietf-appsawg-json-pointer-07">JSON Pointer (work in progress)</a>", September 2012.</td></tr><tr><td class="reference"><b id="json-schema-03">[json-schema-03]</b></td><td class="top"><a>Court, G.</a> and <a title="SitePen (USA)">K. Zyp</a>, "<a href="http://tools.ietf.org/html/draft-zyp-json-schema-03">JSON Schema, draft 3</a>", September 2012.</td></tr></tbody></table>

# [Appendix A.](#rfc.appendix.A) ChangeLog

draft-00

*   Initial draft.
*   Salvaged from draft v3.
*   Mandate the use of JSON Reference, JSON Pointer.
*   Define the role of "id". Define URI resolution scope.
*   Add interoperability considerations.

# [Authors' Addresses](#rfc.authors)

Francis Galiegue (editor) Galiegue EMail: [\[email protected\]](/cdn-cgi/l/email-protection#593f3e3835303c3e2c3c193e34383035773a3634)

Kris Zyp (editor) Zyp SitePen (USA) 530 Lytton Avenue Palo Alto, CA 94301, USA Phone: +1 650 968 8787 EMail: [\[email protected\]](/cdn-cgi/l/email-protection#1378617a6053607a677663767d3d707c7e)

Gary Court Court Calgary, AB, Canada EMail: [\[email protected\]](/cdn-cgi/l/email-protection#2c4b4d5e55024f43595e586c4b414d4540024f4341)