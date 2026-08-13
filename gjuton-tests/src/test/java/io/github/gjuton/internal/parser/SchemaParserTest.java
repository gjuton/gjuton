package io.github.gjuton.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.github.gjuton.api.Gjuton;
import io.github.gjuton.internal.extension.GjutonExtensions;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.NullSchema;
import io.github.gjuton.internal.model.NumericSchema;
import io.github.gjuton.internal.model.ObjectSchema;
import io.github.gjuton.internal.model.StringSchema;
import io.github.gjuton.internal.model.UnsatisfiableSchema;
import io.github.gjuton.internal.model.UntypedSchema;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaParserTest {

    private static final JsonConverter JSON = GjutonExtensions.locator().find(JsonConverter.class).orElseThrow();
    private static final SchemaParser PARSER = new SchemaParser(JSON);

    @Nested
    class RefResolution {

        @Test
        void selfRefResolvesToRootSchema() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "self": {"$ref": "#"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#");

            // then
            assertThat(resolved).isSameAs(document.getRoot());
        }

        @Test
        void refInDefinitionsIsCollected() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "billing": {"$ref": "#/definitions/Address"}
                        },
                        "definitions": {
                            "Address": {
                                "type": "object",
                                "properties": {"street": {"type": "string"}}
                            }
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/definitions/Address");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void refInDefsIsCollected() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "tag": {"$ref": "#/$defs/Tag"}
                        },
                        "$defs": {
                            "Tag": {"type": "string"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/$defs/Tag");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void refNestedInsideArrayItemsIsCollected() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "tags": {
                                "type": "array",
                                "items": {"$ref": "#/definitions/Tag"}
                            }
                        },
                        "definitions": {
                            "Tag": {"type": "string"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/definitions/Tag");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void refInsideArrayElementIsCollected() {
            // $ref lives as an element of a JSON array (oneOf), not as a property
            // value. Exercises the array-recursion branch of collectRefs.
            var document = PARSER.parse("""
                    {
                        "oneOf": [
                            {"$ref": "#/definitions/Tag"}
                        ],
                        "definitions": {
                            "Tag": {"type": "string"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/definitions/Tag");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void refNestedDeepInsideObjectPropertyIsCollected() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "outer": {
                                "type": "object",
                                "properties": {
                                    "inner": {
                                        "type": "object",
                                        "properties": {
                                            "leaf": {"$ref": "#/definitions/Leaf"}
                                        }
                                    }
                                }
                            }
                        },
                        "definitions": {
                            "Leaf": {"type": "string"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/definitions/Leaf");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void multipleRefsToSameTargetStringResolveToSameSchemaInstance() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "billing": {"$ref": "#/definitions/Address"},
                            "shipping": {"$ref": "#/definitions/Address"}
                        },
                        "definitions": {
                            "Address": {
                                "type": "object",
                                "properties": {"street": {"type": "string"}}
                            }
                        }
                    }
                    """);

            // when
            var first = document.resolveRef("#/definitions/Address");
            var second = document.resolveRef("#/definitions/Address");

            // then
            assertThat(first).isSameAs(second);
        }

        @Test
        void distinctRefsAreEachCollected() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "a": {"$ref": "#/definitions/A"},
                            "b": {"$ref": "#/definitions/B"}
                        },
                        "definitions": {
                            "A": {"type": "string"},
                            "B": {"type": "integer"}
                        }
                    }
                    """);

            // when
            var a = document.resolveRef("#/definitions/A");
            var b = document.resolveRef("#/definitions/B");

            // then
            assertThat(a).isNotNull();
            assertThat(b).isNotNull();
            assertThat(a).isNotSameAs(b);
        }

        @Test
        void fragmentEscapeResolvesToTheKeyItNames() {
            // The escape a fragment carries stands for the character in the key, and
            // RFC 6901's own escaping is read afterwards.
            var ref = "#/definitions/a~1b%3Ac";
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "x": {"$ref": "%s"}
                        },
                        "definitions": {
                            "a/b:c": {"type": "string"}
                        }
                    }
                    """.formatted(ref));

            // when
            var resolved = document.resolveRef(ref);

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void percentEncodedPatternPropertiesKeyResolves() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "y": {"$ref": "#/patternProperties/%5B-_.a-zA-Z0-9%5D%2B"}
                        },
                        "patternProperties": {
                            "[-_.a-zA-Z0-9]+": {"type": "string"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/patternProperties/%5B-_.a-zA-Z0-9%5D%2B");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void unresolvedPercentEncodedRefIsReportedDecoded() {
            // when / then
            assertThatThrownBy(() -> PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "missing": {"$ref": "#/definitions/a%3Ab"}
                        }
                    }
                    """))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("#/definitions/a:b");
        }

        @Test
        void unresolvedRefThrowsIllegalArgumentException() {
            // when / then
            assertThatThrownBy(() -> PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "missing": {"$ref": "#/definitions/DoesNotExist"}
                        }
                    }
                    """))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("#/definitions/DoesNotExist");
        }

        @Test
        void unresolvedRefIsDetectedAtParseTimeNotGenerationTime() {
            // A $ref nested deep inside an unreachable branch is still walked by
            // collectRefs, so a typo surfaces at parse time even if generation
            // would never reach that branch.

            // when / then
            assertThatThrownBy(() -> PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "deeply": {
                                "type": "array",
                                "items": {
                                    "type": "object",
                                    "properties": {
                                        "x": {"$ref": "#/definitions/Missing"}
                                    }
                                }
                            }
                        }
                    }
                    """))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("#/definitions/Missing");
        }

        @Test
        void refOntoNonSchemaTargetThrowsNamingTheRef() {
            // when / then
            assertThatThrownBy(() -> PARSER.parse("""
                    {
                        "$ref": "#/definitions/x",
                        "definitions": {"x": null}
                    }
                    """))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("#/definitions/x");
        }

        @Test
        void refInsideExamplePayloadIsNotResolved() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {"a": {"type": "string"}},
                        "example": {"a": {"$ref": "#/nope/x"}}
                    }
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(ObjectSchema.class);
        }

        @Test
        void refNestedInsideRefTargetIsCollected() {
            // The target sits under a container that is not a schema keyword, so it is
            // only reachable by following the ref that points at it.
            var document = PARSER.parse("""
                    {
                        "$ref": "#/components/schemas/Order",
                        "components": {
                            "schemas": {
                                "Order": {
                                    "type": "object",
                                    "properties": {
                                        "customer": {"$ref": "#/components/schemas/Customer"}
                                    }
                                },
                                "Customer": {
                                    "type": "object",
                                    "properties": {"name": {"type": "string"}}
                                }
                            }
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/components/schemas/Customer");

            // then
            assertThat(resolved).isNotNull();
        }

        @Test
        void refInsideDependenciesSubSchemaIsCollected() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {"a": {"type": "string"}},
                        "dependencies": {
                            "a": {
                                "properties": {"b": {"$ref": "#/definitions/B"}}
                            }
                        },
                        "definitions": {
                            "B": {"type": "string"}
                        }
                    }
                    """);

            // when
            var resolved = document.resolveRef("#/definitions/B");

            // then
            assertThat(resolved).isNotNull();
        }
    }

    @Nested
    class ExternalRefs {

        @Test
        void httpRefResolvesViaNetwork() throws IOException {
            var server = startSchemaServer("/schema.json", """
                    {"type": "string", "minLength": 1}
                    """);
            try {
                int port = server.getAddress().getPort();

                // when
                var document = PARSER.parse("""
                        {
                            "type": "object",
                            "properties": {
                                "name": {"$ref": "http://localhost:%d/schema.json"}
                            }
                        }
                        """.formatted(port));

                // then
                var resolved = document.resolveRef("http://localhost:%d/schema.json".formatted(port));
                assertThat(resolved).isNotNull().isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void httpRefWithFragmentResolvesDefinition() throws IOException {
            var server = startSchemaServer("/defs.json", """
                    {
                        "definitions": {
                            "Tag": {"type": "string", "maxLength": 50}
                        }
                    }
                    """);
            try {
                int port = server.getAddress().getPort();

                // when
                var document = PARSER.parse("""
                        {
                            "type": "object",
                            "properties": {
                                "tag": {"$ref": "http://localhost:%d/defs.json#/definitions/Tag"}
                            }
                        }
                        """.formatted(port));

                // then
                var resolved = document.resolveRef("http://localhost:%d/defs.json#/definitions/Tag".formatted(port));
                assertThat(resolved).isNotNull().isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void relativeRefWithNoBaseDirThrowsIllegalArgumentException() {
            // when / then
            assertThatThrownBy(() -> PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "external": {"$ref": "other-schema.json#/definitions/Foo"}
                        }
                    }
                    """))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("other-schema.json")
                    .hasMessageContaining("no base URI");
        }

        @Test
        void fileRefResolvesToExternalSchema() throws URISyntaxException {
            var schemaFile = testResourcePath("schemas/ref-external-file.json");

            // when
            var document = PARSER.parse(schemaFile);

            // then
            var resolved = document.resolveRef("external/defs.json#/definitions/Address");
            assertThat(resolved).isNotNull();
            assertThat(resolved).isInstanceOf(ObjectSchema.class);
        }

        @Test
        void twoFragmentsIntoSameExternalFileBothResolve(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("defs.json"), """
                    {
                        "definitions": {
                            "Address": {
                                "type": "object",
                                "properties": {"street": {"type": "string"}},
                                "required": ["street"]
                            },
                            "ZipCode": {
                                "type": "string",
                                "minLength": 5
                            }
                        }
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "address": {"$ref": "defs.json#/definitions/Address"},
                            "zip": {"$ref": "defs.json#/definitions/ZipCode"}
                        }
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            var address = document.resolveRef("defs.json#/definitions/Address");
            var zip = document.resolveRef("defs.json#/definitions/ZipCode");
            assertThat(address).isNotNull().isInstanceOf(ObjectSchema.class);
            assertThat(zip).isNotNull().isInstanceOf(StringSchema.class);
        }

        @Test
        void fileRefWithoutFragmentResolvesToExternalRoot(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("leaf.json"), """
                    {"type": "string", "minLength": 1}
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "name": {"$ref": "leaf.json"}
                        }
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            var resolved = document.resolveRef("leaf.json");
            assertThat(resolved).isNotNull();
            assertThat(resolved).isInstanceOf(StringSchema.class);
        }

        @Test
        void refsWithinExternalSchemaAreResolved(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("defs.json"), """
                    {
                        "definitions": {
                            "Order": {
                                "type": "object",
                                "properties": {
                                    "id": {"type": "integer"},
                                    "address": {"$ref": "#/definitions/Address"}
                                },
                                "required": ["id", "address"]
                            },
                            "Address": {
                                "type": "object",
                                "properties": {
                                    "street": {"type": "string"},
                                    "city": {"type": "string"}
                                },
                                "required": ["street", "city"]
                            }
                        }
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "order": {"$ref": "defs.json#/definitions/Order"}
                        },
                        "required": ["order"]
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            var order = document.resolveRef("defs.json#/definitions/Order");
            assertThat(order).isNotNull().isInstanceOf(ObjectSchema.class);
            // The ref inside defs.json is named by the document it belongs to.
            var defsUri = tempDir.toUri().resolve("defs.json");
            var addressRef = document.resolveRef(defsUri + "#/definitions/Address");
            assertThat(addressRef).isNotNull().isInstanceOf(ObjectSchema.class);
        }

        @Test
        void generationWorksWithRefsWithinExternalSchema(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("defs.json"), """
                    {
                        "definitions": {
                            "Order": {
                                "type": "object",
                                "properties": {
                                    "id": {"type": "integer"},
                                    "address": {"$ref": "#/definitions/Address"}
                                },
                                "required": ["id", "address"]
                            },
                            "Address": {
                                "type": "object",
                                "properties": {
                                    "street": {"type": "string"},
                                    "city": {"type": "string"}
                                },
                                "required": ["street", "city"]
                            }
                        }
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "order": {"$ref": "defs.json#/definitions/Order"}
                        },
                        "required": ["order"]
                    }
                    """);

            // when
            var gen = Gjuton.of(schemaFile.toFile()).withSeed(42);
            var json = gen.generate();

            // then
            var tree = (Map<?, ?>) JSON.readTree(json);
            var order = (Map<?, ?>) tree.get("order");
            assertThat(order).isNotNull();
            assertThat(order.get("id")).isNotNull();
            var address = (Map<?, ?>) order.get("address");
            assertThat(address).isNotNull();
            assertThat(address.get("street")).isNotNull();
            assertThat(address.get("city")).isNotNull();
        }

        @Test
        void transitiveRefsWithinExternalSchemaAreResolved(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("defs.json"), """
                    {
                        "definitions": {
                            "Order": {
                                "type": "object",
                                "properties": {
                                    "address": {"$ref": "#/definitions/Address"}
                                },
                                "required": ["address"]
                            },
                            "Address": {
                                "type": "object",
                                "properties": {
                                    "zip": {"$ref": "#/definitions/ZipCode"}
                                },
                                "required": ["zip"]
                            },
                            "ZipCode": {
                                "type": "string",
                                "minLength": 5,
                                "maxLength": 10
                            }
                        }
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "order": {"$ref": "defs.json#/definitions/Order"}
                        },
                        "required": ["order"]
                    }
                    """);

            // when
            var gen = Gjuton.of(schemaFile.toFile()).withSeed(42);
            var json = gen.generate();

            // then
            var tree = (Map<?, ?>) JSON.readTree(json);
            var order = (Map<?, ?>) tree.get("order");
            var address = (Map<?, ?>) order.get("address");
            var zip = address.get("zip");
            assertThat(zip).isInstanceOf(String.class);
            assertThat((String) zip).hasSizeBetween(5, 10);
        }

        @Test
        void overlappingDefinitionNamesInMainAndExternalDocDoNotCollide(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("defs.json"), """
                    {
                        "definitions": {
                            "Thing": {
                                "type": "object",
                                "properties": {
                                    "name": {"$ref": "#/definitions/Name"}
                                },
                                "required": ["name"]
                            },
                            "Name": {
                                "type": "string",
                                "minLength": 10,
                                "maxLength": 20
                            }
                        }
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "thing": {"$ref": "defs.json#/definitions/Thing"},
                            "localName": {"$ref": "#/definitions/Name"}
                        },
                        "definitions": {
                            "Name": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 3
                            }
                        },
                        "required": ["thing", "localName"]
                    }
                    """);

            // when
            var gen = Gjuton.of(schemaFile.toFile()).withSeed(42);
            var json = gen.generate();

            // then
            var tree = (Map<?, ?>) JSON.readTree(json);
            var thing = (Map<?, ?>) tree.get("thing");
            var externalName = (String) thing.get("name");
            var localName = (String) tree.get("localName");
            assertThat(externalName.length()).isBetween(10, 20);
            assertThat(localName.length()).isBetween(1, 3);
        }

        @Test
        void externalSchemaReferencingAnotherExternalSchema(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("address.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "street": {"type": "string"},
                            "zip": {"$ref": "zipcode.json"}
                        },
                        "required": ["street", "zip"]
                    }
                    """);
            Files.writeString(tempDir.resolve("zipcode.json"), """
                    {
                        "type": "string",
                        "minLength": 5,
                        "maxLength": 10
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "type": "object",
                        "properties": {
                            "address": {"$ref": "address.json"}
                        },
                        "required": ["address"]
                    }
                    """);

            // when
            var gen = Gjuton.of(schemaFile.toFile()).withSeed(42);
            var json = gen.generate();

            // then
            var tree = (Map<?, ?>) JSON.readTree(json);
            var address = (Map<?, ?>) tree.get("address");
            assertThat(address.get("street")).isInstanceOf(String.class);
            var zip = (String) address.get("zip");
            assertThat(zip.length()).isBetween(5, 10);
        }

        @Test
        void relativeRefResolvesAgainstDeclaredIdRatherThanFileLocation(@TempDir Path tempDir) throws IOException {
            var server = startSchemaServer("/sub/other.json", """
                    {
                        "type": "string",
                        "minLength": 1
                    }
                    """);
            try {
                int port = server.getAddress().getPort();
                // A decoy next to the entry file: resolving against the file's own
                // directory would find this instead of the $id-implied target.
                Files.writeString(tempDir.resolve("other.json"), """
                        {
                            "type": "integer"
                        }
                        """);
                var schemaFile = Files.writeString(tempDir.resolve("root.json"), """
                        {
                            "$id": "http://localhost:%d/sub/root.json",
                            "properties": {
                                "thing": {
                                    "$ref": "other.json"
                                }
                            }
                        }
                        """.formatted(port));

                // when
                var document = PARSER.parse(schemaFile);

                // then
                assertThat(document.resolveRef("other.json")).isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void draft4BareIdActsAsBaseUriForRelativeRefs(@TempDir Path tempDir) throws IOException {
            var server = startSchemaServer("/sub/other.json", """
                    {
                        "type": "string",
                        "minLength": 1
                    }
                    """);
            try {
                int port = server.getAddress().getPort();
                Files.writeString(tempDir.resolve("other.json"), """
                        {
                            "type": "integer"
                        }
                        """);
                var schemaFile = Files.writeString(tempDir.resolve("root.json"), """
                        {
                            "id": "http://localhost:%d/sub/root.json",
                            "properties": {
                                "thing": {
                                    "$ref": "other.json"
                                }
                            }
                        }
                        """.formatted(port));

                // when
                var document = PARSER.parse(schemaFile);

                // then
                assertThat(document.resolveRef("other.json")).isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void refInsideExternalDocumentResolvesAgainstThatDocumentsDirectory(@TempDir Path tempDir) throws IOException {
            var subDir = Files.createDirectory(tempDir.resolve("sub"));
            Files.writeString(subDir.resolve("address.json"), """
                    {
                        "properties": {
                            "zip": {
                                "$ref": "zipcode.json"
                            }
                        }
                    }
                    """);
            Files.writeString(subDir.resolve("zipcode.json"), """
                    {
                        "type": "string",
                        "minLength": 5
                    }
                    """);
            // A decoy in the entry document's directory: resolving the nested ref
            // against the entry file rather than address.json would find this.
            Files.writeString(tempDir.resolve("zipcode.json"), """
                    {
                        "type": "integer"
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "properties": {
                            "address": {
                                "$ref": "sub/address.json"
                            }
                        }
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            var zipcodeUri = tempDir.toUri().resolve("sub/zipcode.json");
            assertThat(document.resolveRef(zipcodeUri.toString())).isInstanceOf(StringSchema.class);
        }

        @Test
        void theSameRefSpellingInTwoDocumentsResolvesToEachDocumentsOwnTarget(@TempDir Path tempDir) throws IOException {
            var subDir = Files.createDirectory(tempDir.resolve("sub"));
            Files.writeString(subDir.resolve("a.json"), """
                    {
                        "properties": {
                            "inner": {
                                "$ref": "target.json"
                            }
                        }
                    }
                    """);
            Files.writeString(subDir.resolve("target.json"), """
                    {
                        "type": "string",
                        "minLength": 5
                    }
                    """);
            // Named by the same spelling as the ref inside sub/a.json, but a different file.
            Files.writeString(tempDir.resolve("target.json"), """
                    {
                        "type": "integer"
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                    {
                        "properties": {
                            "own": {
                                "$ref": "target.json"
                            },
                            "nested": {
                                "$ref": "sub/a.json"
                            }
                        }
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            assertThat(document.resolveRef("target.json")).isInstanceOf(NumericSchema.class);
            var nestedTargetUri = tempDir.toUri().resolve("sub/target.json");
            assertThat(document.resolveRef(nestedTargetUri.toString())).isInstanceOf(StringSchema.class);
        }

        @Test
        void refInsideExternalDocumentResolvesAgainstThatDocumentsOwnId(@TempDir Path tempDir) throws IOException {
            var server = startSchemaServer("/deep/target.json", """
                    {
                        "type": "string",
                        "minLength": 5
                    }
                    """);
            try {
                int port = server.getAddress().getPort();
                var subDir = Files.createDirectory(tempDir.resolve("sub"));
                Files.writeString(subDir.resolve("a.json"), """
                        {
                            "$id": "http://localhost:%d/deep/a.json",
                            "properties": {
                                "inner": {
                                    "$ref": "target.json"
                                }
                            }
                        }
                        """.formatted(port));
                // A decoy beside a.json: the ref belongs to the $id's directory, not this one.
                Files.writeString(subDir.resolve("target.json"), """
                        {
                            "type": "integer"
                        }
                        """);
                var schemaFile = Files.writeString(tempDir.resolve("main.json"), """
                        {
                            "properties": {
                                "nested": {
                                    "$ref": "sub/a.json"
                                }
                            }
                        }
                        """);

                // when
                var document = PARSER.parse(schemaFile);

                // then
                assertThat(document.resolveRef("http://localhost:%d/deep/target.json".formatted(port)))
                        .isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void refNamingTheDocumentsOwnIdIsResolvedWithinIt() {
            // The host does not resolve, so any attempt to retrieve the document
            // named by the ref fails rather than finding it in the parse.
            // when
            var document = PARSER.parse("""
                    {
                        "$id": "https://example.invalid/root.json",
                        "definitions": {
                            "Name": {
                                "type": "string",
                                "minLength": 3
                            }
                        },
                        "properties": {
                            "name": {
                                "$ref": "https://example.invalid/root.json#/definitions/Name"
                            }
                        }
                    }
                    """);

            // then
            assertThat(document.resolveRef("https://example.invalid/root.json#/definitions/Name"))
                    .isInstanceOf(StringSchema.class);
        }

        @Test
        void refNamingTheDocumentsOwnIdWithoutFragmentNamesTheDocumentItself() {
            // The host does not resolve, so any attempt to retrieve the document
            // named by the ref fails rather than finding it in the parse.
            // when
            var document = PARSER.parse("""
                    {
                        "$id": "https://example.invalid/root.json",
                        "type": "object",
                        "properties": {
                            "child": {
                                "$ref": "https://example.invalid/root.json"
                            }
                        }
                    }
                    """);

            // then
            assertThat(document.resolveRef("https://example.invalid/root.json"))
                    .isInstanceOf(ObjectSchema.class);
        }

        @Test
        void documentNamedBySeveralRefsIsRetrievedOnce() throws IOException {
            var fetches = new AtomicInteger();
            var defs = """
                    {
                        "definitions": {
                            "Address": {
                                "type": "object"
                            },
                            "ZipCode": {
                                "type": "string",
                                "minLength": 5
                            }
                        }
                    }
                    """;
            var server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/defs.json", exchange -> {
                fetches.incrementAndGet();
                var bytes = defs.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            });
            server.start();
            try {
                int port = server.getAddress().getPort();

                // when
                var document = PARSER.parse("""
                        {
                            "properties": {
                                "address": {
                                    "$ref": "http://localhost:%d/defs.json#/definitions/Address"
                                },
                                "zip": {
                                    "$ref": "http://localhost:%d/defs.json#/definitions/ZipCode"
                                }
                            }
                        }
                        """.formatted(port, port));

                // then
                assertThat(fetches).hasValue(1);
                assertThat(document.resolveRef("http://localhost:%d/defs.json#/definitions/ZipCode".formatted(port)))
                        .isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void nearestEnclosingIdRebasesRefsInItsOwnSubtree(@TempDir Path tempDir) throws IOException {
            var server = startSchemaServer("/nested/other.json", """
                    {
                        "type": "string",
                        "minLength": 1
                    }
                    """);
            try {
                int port = server.getAddress().getPort();
                Files.writeString(tempDir.resolve("other.json"), """
                        {
                            "type": "integer"
                        }
                        """);
                // Only the subschema declares an $id; the document itself has none,
                // so the ref inside that subtree resolves against the subschema.
                var schemaFile = Files.writeString(tempDir.resolve("root.json"), """
                        {
                            "properties": {
                                "wrapper": {
                                    "$id": "http://localhost:%d/nested/wrapper.json",
                                    "properties": {
                                        "thing": {
                                            "$ref": "other.json"
                                        }
                                    }
                                }
                            }
                        }
                        """.formatted(port));

                // when
                var document = PARSER.parse(schemaFile);

                // then
                assertThat(document.resolveRef("other.json")).isInstanceOf(StringSchema.class);
            } finally {
                server.stop(0);
            }
        }

        @Test
        void refShapedValueInsideAnExternalDocumentsDataIsLeftAlone(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("defs.json"), """
                    {
                        "definitions": {
                            "Kind": {
                                "enum": [
                                    {
                                        "$ref": "local.json"
                                    }
                                ]
                            }
                        }
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("root.json"), """
                    {
                        "properties": {
                            "kind": {
                                "$ref": "defs.json#/definitions/Kind"
                            }
                        }
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            var kind = document.resolveRef("defs.json#/definitions/Kind");
            assertThat(kind.getEnumValues()).containsExactly(Map.of("$ref", "local.json"));
        }

        @Test
        void nestedIdsComposeSoRefResolvesAgainstAllOfThemInTurn(@TempDir Path tempDir) throws IOException {
            var oneDir = Files.createDirectory(tempDir.resolve("one"));
            var twoDir = Files.createDirectory(oneDir.resolve("two"));
            Files.writeString(twoDir.resolve("target.json"), """
                    {
                        "type": "string",
                        "minLength": 1
                    }
                    """);
            // Decoys at each level the chain passes through: reachable only by
            // stopping short of the innermost $id.
            Files.writeString(tempDir.resolve("target.json"), """
                    {
                        "type": "integer"
                    }
                    """);
            Files.writeString(oneDir.resolve("target.json"), """
                    {
                        "type": "boolean"
                    }
                    """);
            var schemaFile = Files.writeString(tempDir.resolve("root.json"), """
                    {
                        "properties": {
                            "outer": {
                                "$id": "one/mid.json",
                                "properties": {
                                    "inner": {
                                        "$id": "two/leaf.json",
                                        "properties": {
                                            "thing": {
                                                "$ref": "target.json"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    """);

            // when
            var document = PARSER.parse(schemaFile);

            // then
            assertThat(document.resolveRef("target.json")).isInstanceOf(StringSchema.class);
        }
    }

    @Nested
    class TypeArrayRewriting {

        @Test
        void typeArrayIsRewrittenToOneOf() {
            // when
            var document = PARSER.parse("""
                    {"type": ["string", "null"]}
                    """);

            // then
            var root = document.getRoot();
            assertThat(root).isInstanceOf(UntypedSchema.class);
            assertThat(root.getOneOf()).hasSize(1);
            assertThat(root.getOneOf().getFirst()).hasSize(2);
            assertThat(root.getOneOf().getFirst().get(0)).isInstanceOf(StringSchema.class);
            assertThat(root.getOneOf().getFirst().get(1)).isInstanceOf(NullSchema.class);
        }

        @Test
        void typeArrayPreservesConstraintsOnRelevantBranch() {
            // when
            var document = PARSER.parse("""
                    {"type": ["integer", "string"], "minLength": 3}
                    """);

            // then
            var root = document.getRoot();
            assertThat(root.getOneOf()).hasSize(1);
            assertThat(root.getOneOf().getFirst()).hasSize(2);
            assertThat(root.getOneOf().getFirst().get(0)).isInstanceOf(NumericSchema.class);
            var stringBranch = (StringSchema) root.getOneOf().getFirst().get(1);
            assertThat(stringBranch.getMinLength()).isEqualTo(3);
        }

    }

    @Nested
    class TypeParsing {

        @Test
        void formatGjutonDoesNotModelHasNoModelledConstant() {
            var document = PARSER.parse("""
                    {
                        "type": "string",
                        "format": "made-up"
                    }
                    """);

            // when
            var schema = (StringSchema) document.getRoot();

            // then
            assertThat(schema.getFormat()).isNull();
            assertThat(schema.getRawFormat()).isEqualTo("made-up");
        }

        @Test
        void formatIsRetainedAsWrittenEvenWhenNotModelled() {
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "iban": {"type": "string", "format": "iban"},
                            "when": {"type": "string", "format": "date-time"},
                            "plain": {"type": "string"}
                        }
                    }
                    """);

            // when
            var properties = ((ObjectSchema) document.getRoot()).getProperties();

            // then
            assertThat(((StringSchema) properties.get("iban")).getRawFormat()).isEqualTo("iban");
            assertThat(((StringSchema) properties.get("when")).getRawFormat()).isEqualTo("date-time");
            assertThat(((StringSchema) properties.get("plain")).getRawFormat()).isNull();
        }

        @Test
        void numberTypeParsesAsNumericSchema() {
            // when
            var document = PARSER.parse("""
                    {"type": "number"}
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(NumericSchema.class);
            var schema = (NumericSchema) document.getRoot();
            assertThat(schema.getType()).isEqualTo("number");
        }

        @Test
        void integerTypePreservesTypeField() {
            // when
            var document = PARSER.parse("""
                    {"type": "integer"}
                    """);

            // then
            var schema = (NumericSchema) document.getRoot();
            assertThat(schema.getType()).isEqualTo("integer");
        }

        @Test
        void numberTypeParsesFractionalConstraints() {
            // when
            var document = PARSER.parse("""
                    {"type": "number", "minimum": 1.5, "maximum": 10.5}
                    """);

            // then
            var schema = (NumericSchema) document.getRoot();
            assertThat(schema.getMinimum()).isEqualByComparingTo(new BigDecimal("1.5"));
            assertThat(schema.getMaximum()).isEqualByComparingTo(new BigDecimal("10.5"));
        }
    }

    @Nested
    class Dependencies {

        @Test
        void dependentRequiredKeywordIsParsedDirectly() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "foo": { "type": "string" },
                            "bar": { "type": "integer" }
                        },
                        "dependentRequired": {
                            "foo": ["bar"]
                        }
                    }
                    """);

            // then
            var root = (ObjectSchema) document.getRoot();
            assertThat(root.getDependentRequired())
                    .containsEntry("foo", List.of("bar"));
        }

        @Test
        void dependentSchemasKeywordIsParsedDirectly() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "foo": { "type": "string" }
                        },
                        "dependentSchemas": {
                            "foo": {
                                "type": "object",
                                "properties": {
                                    "bar": { "type": "integer" }
                                },
                                "required": ["bar"]
                            }
                        }
                    }
                    """);

            // then
            var root = (ObjectSchema) document.getRoot();
            assertThat(root.getDependentSchemas()).containsKey("foo");
            var depSchema = (ObjectSchema) root.getDependentSchemas().get("foo");
            assertThat(depSchema.getRequired()).containsExactly("bar");
        }

        @Test
        void draft7DependenciesSchemaFormIsNormalisedToDependentSchemas() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "foo": { "type": "string" }
                        },
                        "dependencies": {
                            "foo": {
                                "properties": {
                                    "bar": { "type": "integer" }
                                },
                                "required": ["bar"]
                            }
                        }
                    }
                    """);

            // then
            var root = (ObjectSchema) document.getRoot();
            assertThat(root.getDependentSchemas()).containsKey("foo");
            var depSchema = (ObjectSchema) root.getDependentSchemas().get("foo");
            assertThat(depSchema.getRequired()).containsExactly("bar");
        }

        @Test
        void draft7DependenciesArrayFormIsNormalisedToDependentRequired() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "foo": { "type": "string" },
                            "bar": { "type": "integer" }
                        },
                        "dependencies": {
                            "foo": ["bar"]
                        }
                    }
                    """);

            // then
            var root = (ObjectSchema) document.getRoot();
            assertThat(root.getDependentRequired())
                    .containsEntry("foo", List.of("bar"));
        }

        @Test
        void draft7DependenciesMixedFormIsSplitCorrectly() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "a": { "type": "string" },
                            "b": { "type": "string" },
                            "c": { "type": "string" }
                        },
                        "dependencies": {
                            "a": ["b"],
                            "b": {
                                "properties": { "c": { "type": "string" } },
                                "required": ["c"]
                            }
                        }
                    }
                    """);

            // then
            var root = (ObjectSchema) document.getRoot();
            assertThat(root.getDependentRequired())
                    .containsEntry("a", List.of("b"));
            assertThat(root.getDependentSchemas()).containsKey("b");
            var depSchema = (ObjectSchema) root.getDependentSchemas().get("b");
            assertThat(depSchema.getRequired()).containsExactly("c");
        }
    }

    @Nested
    class Conditional {

        @Test
        void ifThenElseSubSchemasAreParsedOntoTheBaseSchema() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "status": {"type": "string"}
                        },
                        "if": {
                            "properties": {"status": {"const": "ok"}}
                        },
                        "then": {
                            "required": ["data"]
                        },
                        "else": {
                            "required": ["error"]
                        }
                    }
                    """);

            // then
            var root = document.getRoot();
            assertThat(root.getIfSchema()).isInstanceOf(ObjectSchema.class);
            assertThat(root.getThenSchema()).isInstanceOf(ObjectSchema.class);
            assertThat(root.getElseSchema()).isInstanceOf(ObjectSchema.class);
            assertThat(((ObjectSchema) root.getThenSchema()).getRequired()).containsExactly("data");
            assertThat(((ObjectSchema) root.getElseSchema()).getRequired()).containsExactly("error");
        }

        @Test
        void booleanSchemaFormsAreParsedForThenAndElse() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "if": {"properties": {"status": {"const": "ok"}}},
                        "then": false,
                        "else": true
                    }
                    """);

            // then
            var root = document.getRoot();
            assertThat(root.getThenSchema()).isInstanceOf(UnsatisfiableSchema.class);
            assertThat(root.getElseSchema()).isInstanceOf(UntypedSchema.class);
        }

        @Test
        void conditionalKeywordsSurviveOnAnUntypedParent() {
            // A parent carrying only if/then/else has no type-implying keyword,
            // so it stays untyped — the conditional fields must still be present.

            // when
            var document = PARSER.parse("""
                    {
                        "if": {"properties": {"kind": {"const": "a"}}},
                        "then": {"required": ["x"]},
                        "else": {"required": ["y"]}
                    }
                    """);

            // then
            var root = document.getRoot();
            assertThat(root).isInstanceOf(UntypedSchema.class);
            assertThat(root.getIfSchema()).isNotNull();
            assertThat(root.getThenSchema()).isNotNull();
            assertThat(root.getElseSchema()).isNotNull();
        }
    }

    @Nested
    class TypeInference {

        @Test
        void untypedSchemaWithPropertiesIsInferredAsObject() {
            // when
            var document = PARSER.parse("""
                    {
                        "properties": {
                            "name": {"type": "string"}
                        }
                    }
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(ObjectSchema.class);
            var schema = (ObjectSchema) document.getRoot();
            assertThat(schema.getProperties()).containsKey("name");
        }

        @Test
        void untypedSchemaWithPatternIsInferredAsString() {
            // when
            var document = PARSER.parse("""
                    {"pattern": "^abc$"}
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(StringSchema.class);
            var schema = (StringSchema) document.getRoot();
            assertThat(schema.getPattern()).isEqualTo("^abc$");
        }

        @Test
        void untypedSchemaWithMinimumIsInferredAsNumber() {
            // when
            var document = PARSER.parse("""
                    {"minimum": 5}
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(NumericSchema.class);
            var schema = (NumericSchema) document.getRoot();
            assertThat(schema.getMinimum()).isEqualByComparingTo(new BigDecimal("5"));
        }

        @Test
        void untypedSchemaWithItemsIsInferredAsArray() {
            // when
            var document = PARSER.parse("""
                    {"items": {"type": "string"}}
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(ArraySchema.class);
        }

        @Test
        void schemaWithKeywordsFromMultipleTypesStaysUntyped() {
            // when
            var document = PARSER.parse("""
                    {"pattern": "^abc$", "minimum": 5}
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(UntypedSchema.class);
        }

        @Test
        void untypedSchemaNestedInsidePropertiesIsInferred() {
            // when
            var document = PARSER.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "file": {"pattern": "\\\\.css$"}
                        }
                    }
                    """);

            // then
            var root = (ObjectSchema) document.getRoot();
            var file = root.getProperties().get("file");
            assertThat(file).isInstanceOf(StringSchema.class);
            assertThat(((StringSchema) file).getPattern()).isEqualTo("\\.css$");
        }

        @Test
        void constPayloadResemblingSchemaIsNotWalkedForTypeInference() {
            // when
            var document = PARSER.parse("""
                    {
                        "const": {"pattern": "^abc$"}
                    }
                    """);

            // then
            assertThat(document.getRoot().getConstValue())
                    .isEqualTo(java.util.Map.of("pattern", "^abc$"));
        }

        @Test
        void untypedSchemaNestedInsideOneOfBranchIsInferred() {
            // when
            var document = PARSER.parse("""
                    {
                        "oneOf": [
                            {"properties": {"file": {"pattern": "\\\\.css$"}}}
                        ]
                    }
                    """);

            // then
            var branch = (ObjectSchema) document.getRoot().getOneOf().getFirst().get(0);
            var file = branch.getProperties().get("file");
            assertThat(file).isInstanceOf(StringSchema.class);
            assertThat(((StringSchema) file).getPattern()).isEqualTo("\\.css$");
        }

        @Test
        void schemaWithNoTypeImplyingKeywordsStaysUntyped() {
            // when
            var document = PARSER.parse("""
                    {
                        "enum": ["a", "b"]
                    }
                    """);

            // then
            assertThat(document.getRoot()).isInstanceOf(UntypedSchema.class);
        }

        @Test
        void untypedRefTargetUnderNonKeywordContainerIsInferred() {
            // when
            var document = PARSER.parse("""
                    {
                        "$ref": "#/components/schemas/Address",
                        "components": {
                            "schemas": {
                                "Address": {
                                    "properties": {"city": {"type": "string"}},
                                    "required": ["city"]
                                }
                            }
                        }
                    }
                    """);

            // then
            var target = document.resolveRef("#/components/schemas/Address");
            assertThat(target).isInstanceOf(ObjectSchema.class);
            assertThat(((ObjectSchema) target).getProperties()).containsKey("city");
        }

        @Test
        void untypedSchemaNestedInsideRefTargetUnderNonKeywordContainerIsInferred() {
            // when
            var document = PARSER.parse("""
                    {
                        "$ref": "#/components/schemas/Address",
                        "components": {
                            "schemas": {
                                "Address": {
                                    "properties": {
                                        "postcode": {"pattern": "^[0-9]{5}$"}
                                    }
                                }
                            }
                        }
                    }
                    """);

            // then
            var target = (ObjectSchema) document.resolveRef("#/components/schemas/Address");
            var postcode = target.getProperties().get("postcode");
            assertThat(postcode).isInstanceOf(StringSchema.class);
            assertThat(((StringSchema) postcode).getPattern()).isEqualTo("^[0-9]{5}$");
        }

        @Test
        void selfRecursiveRefTargetUnderNonKeywordContainerIsInferred() {
            // when
            var document = PARSER.parse("""
                    {
                        "components": {
                            "schemas": {
                                "Node": {
                                    "properties": {
                                        "child": {"$ref": "#/components/schemas/Node"}
                                    },
                                    "required": ["child"]
                                }
                            }
                        },
                        "type": "object",
                        "properties": {
                            "root": {"$ref": "#/components/schemas/Node"}
                        }
                    }
                    """);

            // then
            var target = document.resolveRef("#/components/schemas/Node");
            assertThat(target).isInstanceOf(ObjectSchema.class);
            assertThat(((ObjectSchema) target).getProperties()).containsKey("child");
        }

        @Test
        void constPayloadInsideRefTargetUnderNonKeywordContainerIsNotWalked() {
            // when
            var document = PARSER.parse("""
                    {
                        "$ref": "#/components/schemas/Marker",
                        "components": {
                            "schemas": {
                                "Marker": {"const": {"pattern": "^abc$"}}
                            }
                        }
                    }
                    """);

            // then
            var target = document.resolveRef("#/components/schemas/Marker");
            assertThat(target.getConstValue()).isEqualTo(java.util.Map.of("pattern", "^abc$"));
        }

        @Test
        void enumPayloadInsideRefTargetUnderNonKeywordContainerIsNotWalked() {
            // when
            var document = PARSER.parse("""
                    {
                        "$ref": "#/components/schemas/Marker",
                        "components": {
                            "schemas": {
                                "Marker": {"enum": [{"properties": {"a": 1}}]}
                            }
                        }
                    }
                    """);

            // then
            var target = document.resolveRef("#/components/schemas/Marker");
            assertThat(target.getEnumValues())
                    .containsExactly(java.util.Map.of("properties", java.util.Map.of("a", 1)));
        }
    }

    private static Path testResourcePath(String relativePath) throws URISyntaxException {
        return Paths.get(SchemaParserTest.class.getClassLoader().getResource(relativePath).toURI());
    }

    private static HttpServer startSchemaServer(String path, String body) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            var bytes = body.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.start();
        return server;
    }
}
