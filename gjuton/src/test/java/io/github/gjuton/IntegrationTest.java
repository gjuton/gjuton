package io.github.gjuton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import io.github.gjuton.api.GenerationMode;
import io.github.gjuton.api.Gjuton;
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

@Execution(ExecutionMode.CONCURRENT)
@Slf4j
class IntegrationTest {
    // Each location under src/test/resources is scanned for .json schema files,
    // run for its own number of iterations and novelty-budget iterations. Real-world
    // corpora (e.g. schemastore) are larger and slower to resolve than our
    // hand-written fixtures, so they run far fewer iterations.
    private static final List<SchemaLocation> SCHEMA_LOCATIONS = List.of(
            new SchemaLocation("schemas", 100, 1000),
            new SchemaLocation("schemas/schemastore", 10, 100)
    );

    // Timings indicate a test run of instantiating Gjuton, generating 10 values and
    // validating those 10 values. Measurement was capped at 1s, so entries marked ">1s"
    // are only known to be at least that slow, not exactly that slow.
    // Format is given as total (instantiate Gjuton/generate 10/validate 10).
    private static final Set<String> SLOW_SCHEMAS = Set.of(
            "sigmacv.json", // >1s
            "venvironment-schema-v5.0.0.json", // >1s
            "venvironment-schema-v3.0.0.json", // 196ms (5ms/186ms/4ms)
            "lsdlschema-4.1.json", // 899ms (1ms/894ms/2ms)
            "lsdlschema-4.0.json", // >1s
            "lsdlschema-3.5.json", // 309ms (4ms/297ms/7ms)
            "lsdlschema-3.4.json", // 973ms (1ms/968ms/3ms)
            "lsdlschema-3.3.json", // 953ms (1ms/948ms/2ms)
            "lsdlschema-3.2.json", // >1s
            "lsdlschema-3.1.json", // 747ms (1ms/744ms/2ms)
            "lsdlschema-3.0.json", // >1s
            "lsdlschema-2.0.json", // 289ms (1ms/284ms/2ms)
            "jfrog-pipelines.json", // >1s
            "jsconfig.json", // 280ms (2ms/275ms/2ms)
            "winget-pkgs-installer-1.0.0.json", // 258ms (1ms/188ms/68ms)
            "youtrack-app.json", // 379ms (0ms/377ms/0ms)
            "codeship-steps.json", // >1s
            "airlock-microgateway-3.2.json", // 128ms (7ms/115ms/4ms)
            "utcm-monitor.json", // 135ms (86ms/0ms/41ms)
            "vector.json", // 196ms (125ms/3ms/63ms)
            "bosh-bpm-config.json", // 108ms (0ms/106ms/0ms)
            "ruff.json", // 174ms (7ms/64ms/13ms)
            "renovate-39.json", // >1s
            "renovate-40.json", // >1s
            "renovate-41.json", // >1s
            "renovate-42.json", // >1s
            "renovate-global-schema-41.json", // >1s
            "renovate-global-schema-42.json", // >1s
            "renovate-inherited-schema-42.json", // >1s
            "sarif-2.0.0-csd.2.beta.2018-10-10.json", // >1s
            "sarif-2.0.0-csd.2.beta.2019-01-09.json", // >1s
            "sarif-2.0.0-csd.2.beta.2019-01-24.json", // >1s
            "sarif-2.0.0.json", // >1s
            "sarif-2.1.0.json", // >1s
            "sarif-2.1.0-rtm.0.json", // >1s
            "sarif-2.1.0-rtm.1.json", // >1s
            "sarif-2.1.0-rtm.2.json", // >1s
            "sarif-2.1.0-rtm.3.json", // >1s
            "sarif-2.1.0-rtm.4.json", // >1s
            "sarif-2.1.0-rtm.5.json", // >1s
            "sarif-2.1.0-rtm.6.json", // >1s
            "sarif.json", // >1s
            "stylelintrc.json", // >1s
            "workflows.json", // >1s
            "jsdoc-1.0.0.json", // 203ms (124ms/22ms/56ms)
            "mkdocs-1.0.json", // 103ms (102ms/0ms/0ms)
            "openutau-character.json", // 79ms (0ms/0ms/79ms)
            "apollo-router-2.9.0.json", // 51ms (11ms/19ms/19ms)
            "dotnet-releases-index.json", // 58ms (6ms/32ms/19ms)
            "tsconfig.json", // 67ms (16ms/42ms/6ms)
            "venvironment-schema-v4.1.0.json", // 88ms (4ms/76ms/7ms)
            "venvironment-schema-v3.2.0.json", // 96ms (5ms/85ms/4ms)
            "venvironment-schema-v3.1.0.json", // 95ms (2ms/89ms/2ms)
            "cryproj.54.schema.json", // 72ms (1ms/67ms/3ms)
            "cryproj.55.schema.json", // 71ms (1ms/66ms/3ms)
            "cryproj.json", // 54ms (2ms/46ms/4ms)
            "cryproj.dev.schema.json", // 70ms (1ms/65ms/3ms)
            "partial-eslint-plugins.json", // 90ms (11ms/38ms/38ms)
            "claude-code-settings.json", // 75ms (5ms/49ms/20ms)
            "cargo-lints-clippy.json" // 80ms (8ms/29ms/41ms)
    );

    // These schemas declare a remote $id and reference a sibling schema by relative $ref.
    // Per the JSON Schema spec, that $ref must resolve against the declared $id (or, for
    // Draft 4 schemas, the bare "id" keyword), so the validator tries to fetch the sibling
    // over the network instead of finding it on disk.
    // Excluded here because this sandbox has no network access; run manually outside it.
    // See ticket #156
    private static final Set<String> REMOTE_ID_REF_SCHEMAS = Set.of(
            "anywork-ac-1.0.json", "azure-deviceupdate-import-manifest-4.0.json",
            "azure-deviceupdate-import-manifest-5.0.json", "azure-deviceupdate-manifest-definitions-4.0.json",
            "azure-deviceupdate-manifest-definitions-5.0.json", "azure-deviceupdate-update-manifest-4.json",
            "azure-deviceupdate-update-manifest-5.json", "azure-iot-edge-deployment-template-1.0.json",
            "azure-iot-edge-deployment-template-2.0.json", "azure-iot-edge-deployment-template-3.0.json",
            "azure-iot-edge-deployment-template-4.0.json",
            "bitrise.json", "catalog-info.json", "cheatsheets.json", "cibuildwheel.json",
            "cinnamon-spice.info.json", "clang-format.json", "clangd.json", "drone.json", "eslintrc.json",
            "foundryvtt-module-manifest.json", "foundryvtt-system-manifest.json", "foundryvtt-world-manifest.json",
            "gematik-test-hcpis.json", "gematik-test-hcps.json", "github-pages-jekyll.json",
            "grunt-clean-task.json", "grunt-copy-task.json", "grunt-cssmin-task.json", "grunt-jshint-task.json",
            "hammerkit.json", "jekyll.json", "jsbeautifyrc-nested.json", "lsdlschema.json",
            "minecraft-advancement.json", "minecraft-pack-mcmeta.json", "minecraft-texture-mcmeta.json",
            "mta.json", "mtaext.json", "partial-pdm.json", "partial-tox.json", "pep-723.json", "poetry.json",
            "pre-commit-config.json", "prisma.json", "rancher-fleet-0.5.json", "rancher-fleet-0.8.json",
            "rc3-collection-0.0.3.json", "rc3-folder-0.0.3.json", "rc3-request-0.0.3.json",
            "sarif-external-property-file-2.1.0-rtm.0.json", "sarif-external-property-file-2.1.0-rtm.1.json",
            "sarif-external-property-file-2.1.0-rtm.2.json", "sarif-external-property-file-2.1.0-rtm.3.json",
            "sarif-external-property-file-2.1.0-rtm.4.json", "sarif-external-property-file-2.1.0-rtm.5.json",
            "sarif-external-property-file.json", "schema-org-action.json", "schema-org-contact-point.json",
            "schema-org-place.json", "schema-org-thing.json", "scikit-build.json", "setuptools.json",
            "ti8m-cdk-concrete-environment-config.json", "ti8m-cdk-concrete-environments.json",
            "vs-2017.3.host.json", "web-manifest-combined.json"
    );

    // Bugs/gaps in gjuton itself: parser limitations, generation crashes, or generated
    // output that violates the schema it was generated from. Each needs triage to
    // confirm whether it's a real gjuton defect and, if so, get fixed.
    private static final Set<String> NON_WORKING_SCHEMAS = Set.of(
            // Schema build fails: missing local $ref target file
            "base-04.json", // schema build: missing local ref target "path" (NoSuchFileException)
            "clasp.json", // schema build: missing local ref target "path" (NoSuchFileException)
            "feed.json", // schema build: missing local ref target "feed-1" (NoSuchFileException)
            "tsoa.json", // schema build: missing local ref target "tsconfig" (NoSuchFileException)

            // Schema build fails: unresolved $ref fragment
            "dss-2.0.0.json", // schema build: unresolved percent-encoded $ref fragment
            "opspec-io-0.1.7.json", // schema build: unresolved percent-encoded $ref fragment
            "schema-draft-v4.json", // schema build failed: cannot parse as JSON Schema

            // Crashes during generation (StackOverflowError)
            "json-patch.json", // StackOverflowError during generation
            "okh.json", // StackOverflowError during generation

            // Generates JSON violating other constraints (oneOf/const/required/type/dependentSchemas/contentMediaType)
            "es6importsorterrc.json", // generates /preCommands/0 violating its oneOf
            "prometheus.json", // generates /remote_write/0/authorization violating its const constraint
            "tmlanguage.json", // generates /patterns entries missing required 'begin'/'end'
            "tslint.json", // generates /rules/* entries with null where boolean is required
            "venvplus-schema-v1.0.0.json", // generates /offline-config/source-files entries violating oneOf
            "venvplus-schema-v1.1.0.json", // generates /offline-config/source-files and file-path properties violating constraints
            "vim-addon-info.json", // generates /repository violating dependentSchemas constraint
            "web-manifest.json", // generates /orientation ambiguously valid under 2 oneOf branches
            "azure-iot-edge-deployment-1.0.json", // generates createOptions violating 'is not a content media type'
            "azure-iot-edgeagent-deployment-1.0.json", // generates createOptions violating 'is not a content media type'
            "vega.json", // generates /data/0 missing required 'name', invalid under its oneOf

            // Novelty score never reaches zero within the iteration budget
            "pre-commit-hooks.json",
            "coffeelint.json",
            "scarb.json",
            "replit.json",
            "cryproj.52.schema.json",
            "cryproj.53.schema.json",

            // Throws UnsatisfiableSchemaException; not yet triaged to confirm whether the
            // schema is genuinely unsatisfiable or gjuton's generator/solver is at fault.
            // Known gjuton bugs already traced for some of these:
            //  - format generator (uri/regex) ignores the schema's `pattern` when generating
            //    candidates, then retries blindly against it (specif-1.0/1.1, foundryvtt-base-package-manifest)
            //  - IfThenElseGenerator merges all allOf if/then branches instead of trying one at
            //    a time, when if/then is used as a type-discriminated union (bmml, gitea-issue-forms,
            //    likely github-issue-forms)
            "bmml.json", // UnsatisfiableSchemaException at /meta
            "bundleconfig.json", // UnsatisfiableSchemaException at /0
            "compilerconfig.json", // UnsatisfiableSchemaException at /0
            "flatpak-manifest.json", // UnsatisfiableSchemaException
            "foundryvtt-base-package-manifest.json", // UnsatisfiableSchemaException at /id (pattern+length)
            "gitea-issue-forms.json", // UnsatisfiableSchemaException at /body/0
            "github-issue-forms.json", // UnsatisfiableSchemaException at /body/0
            "pnpm-workspace.json", // UnsatisfiableSchemaException at /catalog
            "popxf-1.0.json", // UnsatisfiableSchemaException, and generates values violating property-name regex patterns
            "pylock.json", // UnsatisfiableSchemaException
            "rudder-techniques.json", // UnsatisfiableSchemaException at /items/0
            "rust-toolchain.json", // UnsatisfiableSchemaException at /toolchain
            "specif-1.0.json", // UnsatisfiableSchemaException at /$schema (pattern+length)
            "specif-1.1.json", // UnsatisfiableSchemaException at /$schema (pattern+length)
            "starlake.json", // UnsatisfiableSchemaException
            "venvironment-schema-v4.0.0.json", // UnsatisfiableSchemaException at /can-networks/0/database
            "vhwdebugger-binding-schema.json", // UnsatisfiableSchemaException
            "vtestunit-schema.json", // UnsatisfiableSchemaException
            "webjob-publish-settings.json" // UnsatisfiableSchemaException
    );

    // Failures caused by a third-party validation library bug, not gjuton.
    private static final Set<String> FAILS_IN_VALIDATION_LIBRARY = Set.of(
            "vtesttree-schema-v2.2.0.json" // validation lib mis-resolves leading-zero $ref key "04112" as "4112"
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
            String json;
            try {
                json = callWithTimeout(gen::generate, GENERATION_TIMEOUT_SECONDS);
            } catch (RuntimeException e) {
                return List.of(failureRow(name, content, path, "generation at invocation " + i + " " + e.getMessage()));
            }
            rows.add(Arguments.of(name, content, path, i, json, null));
        }
        return rows;
    }

    private static Arguments failureRow(String name, String content, Path path, String detail) {
        return Arguments.of(name, content, path, 0, null, name + ": " + detail);
    }

    static List<Arguments> schemaFiles() throws IOException, URISyntaxException {
        return schemaEntries().stream()
                .map(entry -> Arguments.of(entry.path().getFileName().toString(), entry.path(), entry.noveltyIterations()))
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
                        .filter(p -> !REMOTE_ID_REF_SCHEMAS.contains(p.getFileName().toString()))
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
    void generatesValidJson(String schemaName, String schemaContent, Path schemaPath, int invocation, String json, String generationError) throws Exception {
        // when
        if (generationError != null) {
            fail(generationError);
        }
        var factory = schemaFactoryFor(schemaContent);
        Set<ValidationMessage> errors = validateOrFail(factory, schemaPath, json, schemaName, invocation);

        // then
        assertThat(errors)
                .as("%s invocation=%d", schemaName, invocation)
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
            gen.generate();
            invocation++;
        } while (gen.noveltyScore() > 0.0 && invocation < noveltyIterations);

        // then 2
        assertThat(gen.noveltyScore())
                .as("%s's novelty score dropped to zero within %d iterations", schemaName, noveltyIterations)
                .isEqualTo(0.0);
    }

    private static Set<ValidationMessage> validateOrFail(
            JsonSchemaFactory factory, Path schemaPath, String json, String schemaName, int invocation) {
        var config = SchemaValidatorsConfig.builder().preloadJsonSchema(false).build();
        var location = com.networknt.schema.SchemaLocation.of(schemaPath.toUri().toString());
        Callable<Set<ValidationMessage>> task = () -> factory.getSchema(location, config).validate(json, InputFormat.JSON);
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

    private static JsonSchemaFactory schemaFactoryFor(String schemaContent) throws Exception {
        var tree = MAPPER.readTree(schemaContent);
        var version = SpecVersionDetector.detectOptionalVersion(tree, false)
                .orElse(VersionFlag.V7);
        return FACTORIES.get(version);
    }
}
