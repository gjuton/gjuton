package io.github.gjuton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import io.github.gjuton.api.GenerationMode;
import io.github.gjuton.api.Gjuton;
import io.github.gjuton.internal.generator.GjutonMdc;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.slf4j.MDC;

@Execution(ExecutionMode.CONCURRENT)
@Slf4j
class IntegrationTest {
    // Each location under src/test/resources is scanned for .json schema files,
    // run for its own number of iterations and novelty-budget iterations. Real-world
    // corpora (e.g. schemastore) are larger and slower to resolve than our
    // hand-written fixtures, so they run far fewer iterations.
    private static final List<SchemaLocation> SCHEMA_LOCATIONS = List.of(
            new SchemaLocation("schemas", 100, 1000),
            new SchemaLocation("schemas/schemastore", 10, 1000)
    );

    // Excluded for time, not correctness: these generate valid JSON, but one value
    // costs enough that it trips GENERATION_TIMEOUT_SECONDS once the suite runs them
    // in parallel. A schema that generates wrong output belongs in one of the sets below.
    private static final Set<String> SLOW_SCHEMAS = Set.of(
            "sigmacv.json" // ~225ms/value single-threaded; huge values from sentinel maxItems/maxLength (#168)
    );

    // Schemas that cannot be built without reaching the network. Ignored because
    // they would cause a hassle in CI or when running in a sandbox. When tested
    // these schemas fail the test.
    private static final Set<String> SCHEMAS_THAT_NEED_NETWORK_NON_WORKING = Set.of(
            "catalog-info.json", "foundryvtt-module-manifest.json", "foundryvtt-system-manifest.json",
            "foundryvtt-world-manifest.json", "lsdlschema.json", "web-manifest-combined.json"
    );

    // Schemas that cannot be built without reaching the network but generate valid JSON
    // once it is reachable. Kept out of the run so CI and sandboxes stay offline-clean.
    // When tested these schemas pass the test.
    private static final Set<String> SCHEMAS_THAT_NEED_NETWORK_WORKING = Set.of(
            "anywork-ac-1.0.json", "azure-deviceupdate-import-manifest-4.0.json",
            "azure-deviceupdate-import-manifest-5.0.json", "azure-deviceupdate-manifest-definitions-4.0.json",
            "azure-deviceupdate-manifest-definitions-5.0.json", "azure-deviceupdate-update-manifest-4.json",
            "azure-deviceupdate-update-manifest-5.json", "azure-iot-edge-deployment-template-1.0.json",
            "azure-iot-edge-deployment-template-2.0.json", "azure-iot-edge-deployment-template-3.0.json",
            "azure-iot-edge-deployment-template-4.0.json", "bitrise.json", "cheatsheets.json", "cibuildwheel.json",
            "cinnamon-spice.info.json", "clang-format.json", "clangd.json", "clasp.json", "coffeelint.json",
            "cryproj.52.schema.json", "cryproj.53.schema.json", "drone.json", "eslintrc.json",
            "gematik-test-hcpis.json", "gematik-test-hcps.json", "github-pages-jekyll.json", "grunt-clean-task.json",
            "grunt-copy-task.json", "grunt-cssmin-task.json", "grunt-jshint-task.json", "hammerkit.json",
            "jekyll.json", "jsbeautifyrc-nested.json", "minecraft-advancement.json", "minecraft-pack-mcmeta.json",
            "minecraft-texture-mcmeta.json", "mta.json", "mtaext.json", "partial-pdm.json", "partial-tox.json",
            "pep-723.json", "poetry.json", "pre-commit-config.json", "prisma.json", "rancher-fleet-0.5.json",
            "rancher-fleet-0.8.json", "rc3-collection-0.0.3.json", "rc3-folder-0.0.3.json", "rc3-request-0.0.3.json",
            "replit.json", "rudder-techniques.json", "rust-toolchain.json",
            "sarif-external-property-file-2.1.0-rtm.0.json", "sarif-external-property-file-2.1.0-rtm.1.json",
            "sarif-external-property-file-2.1.0-rtm.2.json", "sarif-external-property-file-2.1.0-rtm.3.json",
            "sarif-external-property-file-2.1.0-rtm.4.json", "sarif-external-property-file-2.1.0-rtm.5.json",
            "sarif-external-property-file.json", "scarb.json", "schema-org-action.json",
            "schema-org-contact-point.json", "schema-org-place.json", "schema-org-thing.json", "scikit-build.json",
            "setuptools.json", "ti8m-cdk-concrete-environment-config.json", "ti8m-cdk-concrete-environments.json",
            "tsoa.json", "vs-2017.3.host.json", "web-manifest-app-info.json"
    );

    // Bugs/gaps in gjuton itself: parser limitations, generation crashes, or generated
    // output that violates the schema it was generated from. Each needs triage to
    // confirm whether it's a real gjuton defect and, if so, get fixed.
    private static final Set<String> NON_WORKING_SCHEMAS = Set.of(
            // Schema build fails: missing local $ref target file
            "base-04.json", // schema build: missing local ref target "path" (NoSuchFileException)
            "feed.json", // schema build: missing local ref target "feed-1" (NoSuchFileException)

            // Schema build fails: unresolved $ref fragment
            "dss-2.0.0.json", // schema build: unresolved percent-encoded $ref fragment
            "opspec-io-0.1.7.json", // schema build: unresolved percent-encoded $ref fragment

            // Generates JSON violating other constraints (oneOf/const/required/type/dependentSchemas)
            "es6importsorterrc.json", // generates /preCommands/0 violating its oneOf
            "prometheus.json", // generates /remote_write/0/authorization violating its const constraint
            "tmlanguage.json", // generates /patterns entries missing required 'begin'/'end'
            "tslint.json", // generates /rules/* entries with null where boolean is required
            "vim-addon-info.json", // generates /repository violating dependentSchemas constraint
            "web-manifest.json", // generates /orientation ambiguously valid under 2 oneOf branches

            // Throws UnsatisfiableSchemaException; not yet triaged to confirm whether the
            // schema is genuinely unsatisfiable or gjuton's generator/solver is at fault.
            // Known gjuton bugs already traced for some of these:
            //  - format generator (uri/regex) ignores the schema's `pattern` when generating
            //    candidates, then retries blindly against it (specif-1.0/1.1, foundryvtt-base-package-manifest)
            //  - IfThenElseGenerator merges all allOf if/then branches instead of trying one at
            //    a time, when if/then is used as a type-discriminated union (bmml, gitea-issue-forms,
            //    likely github-issue-forms)
            "bmml.json", // UnsatisfiableSchemaException at /meta
            "codeship-steps.json", // UnsatisfiableSchemaException at /0; regressed in #184, passed before it
            "compilerconfig.json", // UnsatisfiableSchemaException at /0
            "flatpak-manifest.json", // UnsatisfiableSchemaException
            "foundryvtt-base-package-manifest.json", // UnsatisfiableSchemaException at /id (pattern+length)
            "gitea-issue-forms.json", // UnsatisfiableSchemaException at /body/0
            "github-issue-forms.json", // UnsatisfiableSchemaException at /body/0
            "pnpm-workspace.json", // UnsatisfiableSchemaException at /catalog
            "popxf-1.0.json", // UnsatisfiableSchemaException, and generates values violating property-name regex patterns
            // All renovate variants fail in RANDOM mode only, within the first few invocations,
            // and only at soft nesting depth >= 2 — their optional properties recurse into
            // themselves (/ansible/ansible/ansible/...). Passes throughout at soft depth 1.
            "renovate-39.json", // UnsatisfiableSchemaException: no enum value satisfies /ansible/ansible/autodiscoverRepoOrder
            "renovate-40.json", // UnsatisfiableSchemaException: no enum value satisfies /ansible/argocd/autodiscoverRepoSort
            "renovate-41.json", // UnsatisfiableSchemaException at /ansible/ansible-galaxy/encrypted
            "renovate-42.json", // UnsatisfiableSchemaException: no oneOf branch merges with the parent at /ansible/ansible/autodiscoverFilter
            "renovate-global-schema-41.json", // UnsatisfiableSchemaException at /ansible/ansible-galaxy/encrypted
            "renovate-global-schema-42.json", // UnsatisfiableSchemaException: no oneOf branch merges at /ansible/ansible/autodiscoverFilter
            "renovate-inherited-schema-42.json", // UnsatisfiableSchemaException: no oneOf branch merges at /ansible/ansible/autodiscoverFilter
            "specif-1.0.json", // UnsatisfiableSchemaException at /$schema (pattern+length)
            "specif-1.1.json", // UnsatisfiableSchemaException at /$schema (pattern+length)
            "starlake.json", // UnsatisfiableSchemaException
            "venvironment-schema-v4.0.0.json", // UnsatisfiableSchemaException at /can-networks/0/database
            "vhwdebugger-binding-schema.json", // UnsatisfiableSchemaException
            // Also slow, but excluded for failing: generates 8 valid values, then throws on the
            // 9th in EXHAUSTIVE mode. RANDOM survives 20 invocations but is slow at deep limits.
            "workflows.json" // UnsatisfiableSchemaException at $; see #168
    );

    // Failures caused by a third-party validation library bug, not gjuton.
    private static final Set<String> FAILS_IN_VALIDATION_LIBRARY = Set.of(
            "vtesttree-schema-v2.2.0.json", // validation lib mis-resolves leading-zero $ref key "04112" as "4112"

            // Same leading-zero bug: gjuton generates these fine, but the validation lib
            // reads a numeric $ref token as an integer, so "04874" resolves as "4874".
            // RFC 6901 makes the token a literal member name when it addresses an object.
            "venvironment-schema-v3.0.0.json", // "04874" mis-resolved as "4874"
            "venvironment-schema-v3.1.0.json", // "04874" mis-resolved as "4874"
            "venvironment-schema-v3.2.0.json", // "09047" mis-resolved as "9047"
            "venvironment-schema-v4.1.0.json", // "09693" mis-resolved as "9693"
            "venvironment-schema-v5.0.0.json" // "09693" mis-resolved as "9693"
    );

    // Generation fails because the rgxgen library used for `pattern` generation can't
    // produce a matching string, or produces one that doesn't actually match the pattern.
    // Not a Gjuton defect - it's the third-party regex-generation library's limitation.
    private static final Set<String> UNSUPPORTED_REGEX_GENERATION = Set.of(
            "global.json", // rgxgen RgxGenParseException: unexpected symbol in pattern
            "mongodb-atlas-search-index-definition.json", // rgxgen PatternDoesNotMatchAnythingException
            "bukkit-plugin.json", // generates /main violating its own regex pattern
            "paper-plugin.json", // [$.main: does not match the regex pattern ^(?!io\.papermc\.)([a-zA-Z_$][a-zA-Z\d_$]*\.)*[a-zA-Z_$][a-zA-Z\d_$]*$]
            "expo-50.0.0.json", // generates /expo/android/package violating its regex pattern
            "expo-52.0.0.json", // generates /expo/android/package violating its regex pattern
            "expo-53.0.0.json", // generates /expo/android/package violating its regex pattern
            "venvironment-schema-v1.0.0.json", // generates /application-models/0/file-path violating its regex pattern
            "venvironment-schema-v1.1.0.json", // generates file-path properties violating their regex patterns
            "venvironment-schema-v1.1.1.json", // generates file-path/type properties violating their regex patterns
            "venvironment-schema-v2.0.0.json", // generates file-path/type properties violating their regex patterns
            "venvironment-schema-v2.1.0.json", // generates file-path properties violating their regex patterns
            "venvironment-schema-v2.2.0.json", // generates file-path properties violating their regex patterns
            "venvironment-schema-v4.2.0.json" // generates properties violating oneOf/regex constraints
    );

    private static final long DEFAULT_SEED = 42L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Building a generator resolves remote $refs over the network, so this is far
    // more generous than the others: single fetches have been observed to take
    // ~6s, and a schema with several remote refs resolves them one at a time.
    private static final long BUILD_TIMEOUT_SECONDS = 10;

    // Generous enough to avoid false positives at ITERATIONS while keeping a
    // pathological schema from hanging the suite. A single generate() call or
    // validation should complete in milliseconds; these bound the outliers.
    private static final long GENERATION_TIMEOUT_SECONDS = 2;
    private static final long VALIDATION_TIMEOUT_SECONDS = 2;

    // Daemon threads so a runaway generation/validation we can't interrupt never
    // blocks JVM exit. Shut down in @AfterAll to avoid leaking the pool.
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        var thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<VersionFlag, JsonSchemaFactory> FACTORIES = new EnumMap<>(VersionFlag.class);

    /**
     * The validator each schema is checked against, keyed by the schema's path.
     * Compiling one costs far more than running it, and every invocation of a
     * schema is validated against the same one.
     */
    private static final Map<Path, JsonSchema> VALIDATORS = new ConcurrentHashMap<>();

    static {
        for (var flag : VersionFlag.values()) {
            FACTORIES.put(flag, JsonSchemaFactory.getInstance(flag));
        }
    }

    @AfterAll
    static void shutdownExecutor() {
        EXECUTOR.shutdownNow();
    }

    static List<Arguments> parameters() throws IOException, URISyntaxException {
        long seed = resolveSeed();
        log.info("IntegrationTest seed: {} (override with -Dtest.seed=<long>)", seed);
        return schemaEntries().parallelStream()
                .flatMap(entry -> {
                    try {
                        var content = Files.readString(entry.path());
                        // Both generation modes must always produce schema-valid JSON.
                        return Stream.of(GenerationMode.values())
                                .flatMap(mode -> {
                                    var name = entry.path().getFileName() + " [" + mode + "]";
                                    return generateRows(name, content, entry.path(), seed, mode, entry.iterations()).stream();
                                });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    /**
     * Produces the parameterized rows for one schema in one generation mode: one
     * row per iteration on success, or a single failure row (attributed to the
     * schema) if building the generator or any generation call throws or times out.
     */
    private static List<Arguments> generateRows(
            String name, String content, Path path, long seed, GenerationMode mode, int iterations) {
        Gjuton gen;
        try {
            gen = callWithTimeout(() -> Gjuton.of(path.toFile()).withSeed(seed).withGenerationMode(mode), BUILD_TIMEOUT_SECONDS);
        } catch (RuntimeException e) {
            return List.of(failureRow(name, content, path, "schema build " + e.getMessage()));
        }

        var rows = new ArrayList<Arguments>(iterations);
        for (int i = 1; i <= iterations; i++) {
            var runId = name + "#" + i;
            String json;
            try {
                json = callWithTimeout(() -> {
                    // Generation happens on the executor's thread, and the run id is
                    // read there, so putting it in place on this one would not reach it.
                    MDC.put(GjutonMdc.RUN_ID_KEY, runId);
                    try {
                        return gen.generate();
                    } finally {
                        MDC.remove(GjutonMdc.RUN_ID_KEY);
                    }
                }, GENERATION_TIMEOUT_SECONDS);
            } catch (RuntimeException e) {
                return List.of(failureRow(name, content, path, "generation at invocation " + i + " " + e.getMessage()));
            }
            rows.add(Arguments.of(name, content, path, i, json, null, runId));
        }
        return rows;
    }

    private static Arguments failureRow(String name, String content, Path path, String detail) {
        return Arguments.of(name, content, path, 0, null, name + ": " + detail, name + "#0");
    }

    static List<Arguments> schemaFiles() throws IOException, URISyntaxException {
        return schemaEntries().stream()
                .map(entry -> Arguments.of(entry.path().getFileName().toString(), entry.path(), entry.noveltyIterations()))
                .toList();
    }

    static List<Arguments> schemaFilesAndModes() throws IOException, URISyntaxException {
        return schemaEntries().stream()
                .flatMap(entry -> Stream.of(GenerationMode.values())
                        .map(mode -> Arguments.of(entry.path().getFileName() + " [" + mode + "]", entry.path(), mode)))
                .toList();
    }

    /**
     * The {@code .json} schema files under every {@link #SCHEMA_LOCATIONS} resource
     * directory, the fixtures every parameterized integration test runs against,
     * each paired with the iteration counts configured for its location.
     */
    private static List<SchemaEntry> schemaEntries() throws IOException, URISyntaxException {
        var entries = new ArrayList<SchemaEntry>();
        for (var location : SCHEMA_LOCATIONS) {
            var resource = IntegrationTest.class.getClassLoader().getResource(location.resourcePath());
            var dir = Paths.get(resource.toURI());
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> !Files.isDirectory(p))
                        .filter(p -> p.toString().endsWith(".json"))
                        .filter(p -> !SLOW_SCHEMAS.contains(p.getFileName().toString()))
                        .filter(p -> !SCHEMAS_THAT_NEED_NETWORK_NON_WORKING.contains(p.getFileName().toString()))
                        .filter(p -> !SCHEMAS_THAT_NEED_NETWORK_WORKING.contains(p.getFileName().toString()))
                        .filter(p -> !NON_WORKING_SCHEMAS.contains(p.getFileName().toString()))
                        .filter(p -> !FAILS_IN_VALIDATION_LIBRARY.contains(p.getFileName().toString()))
                        .filter(p -> !UNSUPPORTED_REGEX_GENERATION.contains(p.getFileName().toString()))
                        .forEach(p -> entries.add(new SchemaEntry(p, location.iterations(), location.noveltyIterations())));
            }
        }
        return entries;
    }

    private record SchemaLocation(String resourcePath, int iterations, int noveltyIterations) {}

    private record SchemaEntry(Path path, int iterations, int noveltyIterations) {}

    private static long resolveSeed() {
        String value = System.getProperty("test.seed");
        if (value == null || value.equals("random")) {
            return DEFAULT_SEED;
        }
        return Long.parseLong(value);
    }

    @ParameterizedTest(name = "{0} invocation={3}")
    @MethodSource("parameters")
    void generatesValidJson(String schemaName, String schemaContent, Path schemaPath, int invocation, String json, String generationError, String runId)
            throws Exception {
        // when
        if (generationError != null) {
            fail(generationError);
        }
        var validator = validatorFor(schemaPath, schemaContent);
        var errors = validateOrFail(validator, json, schemaName, invocation);

        // then
        assertThat(errors)
                .as("%s invocation=%d runId=%s", schemaName, invocation, runId)
                .isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("schemaFiles")
    void noveltyReachesZeroWithinIterationBudget(String schemaName, Path schemaPath, int noveltyIterations) throws IOException {
        // given
        var gen = Gjuton.of(schemaPath.toFile()).withSeed(DEFAULT_SEED).withGenerationMode(GenerationMode.EXHAUSTIVE);

        // then 1 -- nothing generated yet, so novelty defaults to 1.0
        assertThat(gen.noveltyScore())
                .as("%s reports full novelty before any generation", schemaName)
                .isEqualTo(1.0);

        // when
        int invocation = 0;
        do {
            MDC.put(GjutonMdc.RUN_ID_KEY, schemaName + "#" + (invocation + 1));
            try {
                gen.generate();
            } finally {
                MDC.remove(GjutonMdc.RUN_ID_KEY);
            }
            invocation++;
        } while (gen.noveltyScore() > 0.0 && invocation < noveltyIterations);

        // then 2
        assertThat(gen.noveltyScore())
                .as("%s's novelty score dropped to zero within %d iterations", schemaName, noveltyIterations)
                .isEqualTo(0.0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("schemaFilesAndModes")
    void sameSeedProducesIdenticalValues(String schemaName, Path schemaPath, GenerationMode mode) throws IOException {
        // given
        var first = Gjuton.of(schemaPath.toFile()).withSeed(DEFAULT_SEED).withGenerationMode(mode);
        var second = Gjuton.of(schemaPath.toFile()).withSeed(DEFAULT_SEED).withGenerationMode(mode);

        // then
        for (int i = 1; i <= 10; i++) {
            assertThat(first.generate())
                    .as("%s invocation=%d", schemaName, i)
                    .isEqualTo(second.generate());
        }
    }

    private static Set<ValidationMessage> validateOrFail(
            JsonSchema validator, String json, String schemaName, int invocation) {
        Callable<Set<ValidationMessage>> task = () -> validator.validate(json, InputFormat.JSON);
        try {
            return callWithTimeout(task, VALIDATION_TIMEOUT_SECONDS);
        } catch (RuntimeException e) {
            return fail("%s invocation=%d: validation %s".formatted(schemaName, invocation, e.getMessage()));
        }
    }

    private static <T> T callWithTimeout(Callable<T> task, long timeoutSeconds) {
        Future<T> future = EXECUTOR.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("timed out after " + timeoutSeconds + "s");
        } catch (ExecutionException e) {
            throw new RuntimeException("failed: " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static JsonSchema validatorFor(Path schemaPath, String schemaContent) throws IOException {
        var cached = VALIDATORS.get(schemaPath);
        if (cached != null) {
            return cached;
        }
        var tree = MAPPER.readTree(schemaContent);
        var version = SpecVersionDetector.detectOptionalVersion(tree, false)
                .orElse(VersionFlag.V7);
        var factory = FACTORIES.get(version);
        // Preload what can be resolved up front, so the shared validator does as
        // little lazy initialisation as possible while several threads use it.
        var config = SchemaValidatorsConfig.builder().preloadJsonSchema(true).build();
        var location = com.networknt.schema.SchemaLocation.of(schemaPath.toUri().toString());
        var validator = factory.getSchema(location, config);
        var raced = VALIDATORS.putIfAbsent(schemaPath, validator);
        return raced != null ? raced : validator;
    }
}
