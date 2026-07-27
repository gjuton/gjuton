package io.github.gjuton;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.oas.OpenApi30;
import com.networknt.schema.oas.OpenApi31;
import io.github.gjuton.api.GenerationMode;
import io.github.gjuton.api.Gjuton;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.LoaderOptions;

/**
 * Exercises the OpenAPI use case: take a real API description, pick one of its reusable
 * schemas, and generate a payload that the description itself accepts.
 *
 * <p>Adding a {@code .yaml} description under {@code src/test/resources/openapi} is all
 * that is needed to add test cases — one per reusable schema the description declares, up
 * to {@value #MAX_SCHEMAS_PER_DESCRIPTION} of them. Subdirectories are scanned too, so a
 * collection can be copied in as it is laid out.
 *
 * <p>Cases are independent and run concurrently: each one seeds its own generator, so the
 * values a case sees do not depend on which other cases are in the corpus or on the order
 * they happen to run in.
 */
@Slf4j
@Execution(ExecutionMode.CONCURRENT)
class OpenApiIntegrationTest {

    private static final String CORPUS_RESOURCE = "openapi";

    private static final long SEED = 42L;
    private static final int ITERATIONS = 10;

    // Well above what any curated description reaches, so it never hides a failure. It only
    // bounds a bulk copy of machine-generated descriptions, which run to thousands each.
    private static final int MAX_SCHEMAS_PER_DESCRIPTION = 100;

    // Bugs in gjuton itself. Dropping an entry is the acceptance test for the fix it names.
    private static final Set<String> NON_WORKING_DESCRIPTIONS = Set.of(
            // Schema build fails on a percent-encoded pointer in a $ref into #/paths/..., which
            // path templates put {braces} in. Parked in IntegrationTest too. See issue #150.
            "conjur.local-5.3.0.yaml",

            // Generated output silently violates the schema: four of its schemas declare
            // properties without a type, and components/schemas is never inferred. See #161.
            "6-dot-authentiqio.appspot.com-6.yaml",

            // Schema build fails: $ref strings in its example payloads are resolved as schema
            // references, and one lands on a null, surfacing as a bare NPE. See issue #163.
            "viator.com-1.0.0.yaml",

            // Schema build fails for every schema it declares: one draft-04 boolean
            // exclusiveMinimum, which gjuton takes only as a number, sinks it. See issue #157.
            "openbankingproject.ch-1.3.8.yaml"
    );

    // Descriptions are parsed before anything is generated, so these bound gjuton alone.
    private static final long BUILD_TIMEOUT_SECONDS = 10;
    private static final long GENERATION_TIMEOUT_SECONDS = 2;
    private static final long VALIDATION_TIMEOUT_SECONDS = 2;

    private static final YAMLMapper YAML = yamlMapper();
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<Path, SpecDocument> DOCUMENTS = new ConcurrentHashMap<>();
    private static final Map<VersionFlag, JsonSchemaFactory> VALIDATORS = new ConcurrentHashMap<>();

    // Daemon threads so a runaway generation we cannot interrupt never blocks JVM exit.
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        var thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Everything needed to exercise one OpenAPI description: its JSON form, and a
     * validator configured for the OpenAPI dialect the description declares.
     */
    private record SpecDocument(String json, JsonSchemaFactory validator) {}

    @AfterAll
    static void shutdownExecutor() {
        EXECUTOR.shutdownNow();
    }

    /**
     * One row per reusable schema in the corpus, identified as
     * {@code <path relative to the corpus root>#<json pointer>}. A description contributing
     * more than {@value #MAX_SCHEMAS_PER_DESCRIPTION} of them is covered by its first
     * {@value #MAX_SCHEMAS_PER_DESCRIPTION}.
     */
    static List<Arguments> componentSchemas() throws IOException {
        var corpus = corpusDirectory();
        log.info("OpenAPI corpus: {}", corpus);

        var rows = new ArrayList<Arguments>();
        try (Stream<Path> files = Files.walk(corpus)) {
            var specs = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                    .filter(path -> !NON_WORKING_DESCRIPTIONS.contains(path.getFileName().toString()))
                    .sorted()
                    .toList();
            for (var spec : specs) {
                // Reusable schemas live under components/schemas in OpenAPI 3, definitions in Swagger 2.0.
                var root = JSON.readTree(specDocument(spec).json());
                var container = root.at("/components/schemas");
                var prefix = "/components/schemas/";
                if (container.isMissingNode()) {
                    container = root.at("/definitions");
                    prefix = "/definitions/";
                }
                if (!container.isObject()) {
                    continue;
                }
                var names = container.fieldNames();
                for (int taken = 0; names.hasNext() && taken < MAX_SCHEMAS_PER_DESCRIPTION; taken++) {
                    var name = names.next();
                    var pointer = prefix + name.replace("~", "~0").replace("/", "~1");
                    // Relative to the corpus root, not the bare name: a copied-in collection holds
                    // many descriptions all called openapi.yaml, which would share a case id.
                    var location = corpus.relativize(spec).toString().replace('\\', '/');
                    rows.add(Arguments.of(location + "#" + pointer, spec, pointer));
                }
            }
        }
        return rows;
    }

    /**
     * Generating from a reusable schema of an OpenAPI description produces JSON the
     * description itself accepts, in every generation mode.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("componentSchemas")
    void generatesValidJson(String entry, Path spec, String pointer) {
        // given
        var document = specDocument(spec);
        // The validator gets the very document Gjuton generates from, so they cannot disagree.
        var schemaDocument = schemaDocumentFor(document, pointer);
        var config = SchemaValidatorsConfig.builder().preloadJsonSchema(false).build();
        var validator = document.validator().getSchema(schemaDocument, config);

        for (var mode : GenerationMode.values()) {
            // when
            var context = entry + " [" + mode + "]";
            var gjuton = callWithTimeout(context + " schema build",
                    () -> Gjuton.of(schemaDocument).withSeed(SEED).withGenerationMode(mode), BUILD_TIMEOUT_SECONDS);
            for (int invocation = 1; invocation <= ITERATIONS; invocation++) {
                var json = callWithTimeout(context + " generation at invocation " + invocation,
                        gjuton::generate, GENERATION_TIMEOUT_SECONDS);
                Callable<Set<ValidationMessage>> validation = () -> validator.validate(json, InputFormat.JSON);
                var errors = callWithTimeout(context + " validation at invocation " + invocation,
                        validation, VALIDATION_TIMEOUT_SECONDS);

                // then
                assertThat(errors)
                        .as("%s [%s] invocation=%d generated %s", entry, mode, invocation, json)
                        .isEmpty();
            }
        }
    }

    /**
     * The directory holding the OpenAPI descriptions to run against.
     */
    private static Path corpusDirectory() throws IOException {
        try {
            var resource = OpenApiIntegrationTest.class.getClassLoader().getResource(CORPUS_RESOURCE);
            if (resource == null) {
                throw new IOException("No OpenAPI corpus on the classpath at " + CORPUS_RESOURCE);
            }
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * The description at {@code spec}, converted and prepared for generation and
     * validation. Calls for the same description yield the same instance, at the cost of
     * a single conversion however many schemas that description holds.
     *
     * @throws IllegalArgumentException if the description is not readable as YAML
     */
    private static SpecDocument specDocument(Path spec) {
        return DOCUMENTS.computeIfAbsent(spec, path -> {
            JsonNode root;
            try {
                root = YAML.readTree(path.toFile());
            } catch (IOException e) {
                throw new IllegalArgumentException("Not readable as YAML: " + path, e);
            }
            try {
                return new SpecDocument(JSON.writeValueAsString(root), validatorFor(root));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Not convertible to JSON: " + path, e);
            }
        });
    }

    /**
     * A schema document whose root is the reusable schema at {@code pointer}, expressed
     * so that every {@code $ref} the description makes to its own components still
     * resolves. This is what a consumer would hand Gjuton to generate a payload for one
     * named schema of an API.
     */
    private static String schemaDocumentFor(SpecDocument document, String pointer) {
        // Serialized rather than quoted by hand: a schema name may contain characters that
        // need escaping inside a JSON string, such as the backslashes of a PHP namespace.
        var ref = JSON.getNodeFactory().textNode("#" + pointer);
        return "{\"$ref\":" + ref + "," + document.json().substring(1);
    }

    /**
     * Runs {@code task}, failing rather than hanging the suite if it does not finish
     * within {@code timeoutSeconds}. Any failure is reported against {@code context},
     * which has to name the corpus entry: test reports identify a row only by its index,
     * so the message is all that ties a failure back to the schema that caused it.
     */
    private static <T> T callWithTimeout(String context, Callable<T> task, long timeoutSeconds) {
        var future = EXECUTOR.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AssertionError(context + ": timed out after " + timeoutSeconds + "s");
        } catch (ExecutionException e) {
            throw new AssertionError(context + ": " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(context + ": interrupted", e);
        }
    }

    /**
     * A validator for the schema dialect {@code spec} declares — the two OpenAPI
     * dialects, or draft-04 for Swagger 2.0 descriptions. Keywords of the surrounding
     * description that are not schema keywords are accepted and ignored.
     */
    private static JsonSchemaFactory validatorFor(JsonNode spec) {
        JsonMetaSchema blueprint;
        VersionFlag specification;
        if (spec.hasNonNull("swagger")) {
            blueprint = JsonMetaSchema.getV4();
            specification = VersionFlag.V4;
        } else if (spec.path("openapi").asText("3.0.0").startsWith("3.1")) {
            blueprint = OpenApi31.getInstance();
            specification = VersionFlag.V202012;
        } else {
            blueprint = OpenApi30.getInstance();
            specification = VersionFlag.V7;
        }
        return VALIDATORS.computeIfAbsent(specification, flag -> {
            // A whole API description, so its top-level keywords (openapi, paths, info, x-...)
            // are not schema keywords.
            var dialect = JsonMetaSchema.builder(blueprint)
                    .unknownKeywordFactory((keyword, context) -> new NonValidationKeyword(keyword))
                    .build();
            return JsonSchemaFactory.getInstance(flag, builder -> builder
                    .metaSchema(dialect)
                    .defaultMetaSchemaIri(dialect.getIri()));
        });
    }

    /**
     * A YAML reader that accepts descriptions of the size, nesting and anchor reuse real
     * APIs reach. The defaults are sized for untrusted input and reject documents this
     * corpus legitimately contains — several run past 3MB.
     */
    private static YAMLMapper yamlMapper() {
        var options = new LoaderOptions();
        options.setCodePointLimit(Integer.MAX_VALUE);
        options.setNestingDepthLimit(1000);
        options.setMaxAliasesForCollections(1000);
        var factory = YAMLFactory.builder().loaderOptions(options).build();
        return YAMLMapper.builder(factory).build();
    }
}
