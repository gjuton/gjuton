package io.github.gjuton.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.SchemaDocument;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses a JSON Schema document into the internal model. Supports the
 * most common keywords across drafts rather than strict compliance with
 * any single draft version.
 */
public final class SchemaParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SchemaParser() {
    }

    /**
     * Parses a JSON Schema string into a {@link SchemaDocument} containing
     * the root schema and all resolved {@code $ref} targets.
     *
     * @throws IllegalArgumentException if the input is not valid JSON or
     *     contains an unresolvable {@code $ref}
     */
    public static SchemaDocument parse(String jsonSchema) {
        return doParse(jsonSchema, null);
    }

    /**
     * Parses a JSON Schema file into a {@link SchemaDocument} containing
     * the root schema and all resolved {@code $ref} targets. External
     * {@code $ref} values are resolved relative to the {@code $id} the schema
     * declares, or to the file's own location when it declares none.
     *
     * @throws IllegalArgumentException if the schema is not valid JSON or
     *     contains an unresolvable {@code $ref}
     * @throws UncheckedIOException if reading the file fails
     */
    public static SchemaDocument parse(Path schemaFile) {
        try {
            var jsonSchema = Files.readString(schemaFile);
            var absolutePath = schemaFile.toAbsolutePath();
            return doParse(jsonSchema, absolutePath.toUri());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SchemaDocument doParse(String jsonSchema, URI retrievalUri) {
        try {
            var rootNode = MAPPER.readValue(jsonSchema, Object.class);
            SchemaNormalizer.normalize(rootNode);
            var rootSchema = MAPPER.convertValue(rootNode, Schema.class);
            var refs = RefCollector.collect(rootNode, rootSchema, retrievalUri);
            return new SchemaDocument(rootSchema, refs);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON Schema", e);
        }
    }
}
