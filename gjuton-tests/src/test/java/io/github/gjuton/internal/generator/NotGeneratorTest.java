package io.github.gjuton.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gjuton.internal.extension.GjutonExtensions;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.parser.SchemaParser;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NotGeneratorTest {

    private static final SchemaParser PARSER = new SchemaParser(GjutonExtensions.locator().find(JsonConverter.class).orElseThrow());

    private static final String NOT_NULL = """
            {
                "not": { "type": "null" }
            }
            """;

    @Test
    void firstCallRegistersAsNovel() {
        var document = PARSER.parse(NOT_NULL);
        var context = GeneratorContext.testContext(document, new Random(42));
        var generator = new NotGenerator(context, document.getRoot());

        // when
        context.startRun();
        generator.generate();
        context.completeRun();

        // then
        assertThat(context.noveltyScore()).isEqualTo(1.0);
    }

    @Test
    void secondCallIsNotNovel() {
        var document = PARSER.parse(NOT_NULL);
        var context = GeneratorContext.testContext(document, new Random(42));
        var generator = new NotGenerator(context, document.getRoot());

        // when
        context.startRun();
        generator.generate();
        context.completeRun();
        context.startRun();
        generator.generate();
        context.completeRun();

        // then
        assertThat(context.noveltyScore()).isEqualTo(0.5);
    }
}
