package io.github.gjuton.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.gjuton.api.GenerationMode;
import io.github.gjuton.api.Gjuton;
import io.github.gjuton.internal.extension.GjutonExtensions;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.model.SchemaDocument;
import io.github.gjuton.internal.parser.SchemaParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Measures gjuton against the official JSON Schema Test Suite, vendored under
 * {@code src/test/resources/json-schema-test-suite}.
 *
 * <p>The suite serves two independent purposes here. Every case pins what
 * {@link SchemaValidator} decides about one instance, and every group with a
 * known-valid instance — and therefore a provably satisfiable schema — is a
 * generation target whose output is checked against a third-party validator.
 *
 * <p>What gjuton gets wrong is recorded in the ignore sets below, grouped by
 * cause. The sets are checked in both directions: an entry that stops failing
 * fails the build, so they cannot quietly become a mute button.
 */
@Execution(ExecutionMode.CONCURRENT)
class JsonSchemaSuiteTest {

    private static final String SUITE_ROOT = "json-schema-test-suite";

    private static final long SEED = 42L;

    // Keyword breadth, not sampling depth.
    private static final int VALUES_PER_GROUP = 3;

    // Validation sets hold case ids, generation sets group ids; NEEDS_NETWORK is
    // per file. Regenerate a set: drop its entries, rerun, paste the failures.

    // Absolute http(s) $ref or $id the parser resolves by fetching. Skipped both
    // directions so verdicts never depend on what a machine can reach.
    private static final Set<String> NEEDS_NETWORK = Set.of(
            "draft2019-09/anchor.json", "draft2020-12/anchor.json",
            "draft4/definitions.json", "draft6/definitions.json", "draft7/definitions.json",
            "draft2019-09/defs.json", "draft2020-12/defs.json",
            "draft2020-12/dynamicRef.json",
            "draft2019-09/recursiveRef.json",
            "draft4/ref.json", "draft6/ref.json", "draft7/ref.json", "draft2019-09/ref.json", "draft2020-12/ref.json",
            "draft4/refRemote.json", "draft6/refRemote.json", "draft7/refRemote.json", "draft2019-09/refRemote.json", "draft2020-12/refRemote.json",
            "draft2019-09/unevaluatedItems.json", "draft2020-12/unevaluatedItems.json",
            "draft2019-09/unevaluatedProperties.json", "draft2020-12/unevaluatedProperties.json",
            "draft2019-09/vocabulary.json", "draft2020-12/vocabulary.json"
    );

    // The parser refuses a schema spelled as a bare boolean, at the root or inside
    // an `items` array.
    private static final Set<String> PARSER_REJECTS_SCHEMA = Set.of(
            "draft2019-09/boolean_schema.json # boolean schema 'false' # array is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # boolean false is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # boolean true is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # empty array is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # empty object is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # null is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # number is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # object is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'false' # string is invalid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # array is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # boolean false is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # boolean true is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # empty array is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # empty object is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # null is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # number is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # object is valid",
            "draft2019-09/boolean_schema.json # boolean schema 'true' # string is valid",
            "draft2019-09/items.json # items with boolean schemas # array with one item is valid",
            "draft2019-09/items.json # items with boolean schemas # array with two items is invalid",
            "draft2019-09/items.json # items with boolean schemas # empty array is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # array is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # boolean false is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # boolean true is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # empty array is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # empty object is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # null is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # number is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # object is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'false' # string is invalid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # array is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # boolean false is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # boolean true is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # empty array is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # empty object is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # null is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # number is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # object is valid",
            "draft2020-12/boolean_schema.json # boolean schema 'true' # string is valid",
            "draft6/boolean_schema.json # boolean schema 'false' # array is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # boolean false is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # boolean true is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # empty array is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # empty object is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # null is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # number is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # object is invalid",
            "draft6/boolean_schema.json # boolean schema 'false' # string is invalid",
            "draft6/boolean_schema.json # boolean schema 'true' # array is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # boolean false is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # boolean true is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # empty array is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # empty object is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # null is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # number is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # object is valid",
            "draft6/boolean_schema.json # boolean schema 'true' # string is valid",
            "draft6/items.json # items with boolean schemas # array with one item is valid",
            "draft6/items.json # items with boolean schemas # array with two items is invalid",
            "draft6/items.json # items with boolean schemas # empty array is valid",
            "draft7/boolean_schema.json # boolean schema 'false' # array is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # boolean false is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # boolean true is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # empty array is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # empty object is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # null is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # number is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # object is invalid",
            "draft7/boolean_schema.json # boolean schema 'false' # string is invalid",
            "draft7/boolean_schema.json # boolean schema 'true' # array is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # boolean false is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # boolean true is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # empty array is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # empty object is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # null is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # number is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # object is valid",
            "draft7/boolean_schema.json # boolean schema 'true' # string is valid",
            "draft7/items.json # items with boolean schemas # array with one item is valid",
            "draft7/items.json # items with boolean schemas # array with two items is invalid",
            "draft7/items.json # items with boolean schemas # empty array is valid"
    );

    // The parser drops the keyword.
    private static final Set<String> KEYWORD_NOT_RECOGNIZED = Set.of(
            "draft2019-09/maxContains.json # maxContains = 0 with minContains = 0 # empty array",
            "draft2019-09/maxContains.json # maxContains = 0 with minContains = 0 # one matching item",
            "draft2019-09/maxContains.json # maxContains with contains # all elements match, invalid maxContains",
            "draft2019-09/maxContains.json # maxContains with contains # some elements match, invalid maxContains",
            "draft2019-09/maxContains.json # maxContains with contains, value with a decimal # too many elements match, invalid maxContains",
            "draft2019-09/maxContains.json # minContains < maxContains # minContains < maxContains < actual",
            "draft2019-09/minContains.json # maxContains < minContains # invalid maxContains",
            "draft2019-09/minContains.json # maxContains < minContains # invalid maxContains and minContains",
            "draft2019-09/minContains.json # maxContains < minContains # invalid minContains",
            "draft2019-09/minContains.json # maxContains = minContains # all elements match, invalid maxContains",
            "draft2019-09/minContains.json # maxContains = minContains # all elements match, invalid minContains",
            "draft2019-09/minContains.json # minContains = 0 with maxContains # empty data",
            "draft2019-09/minContains.json # minContains = 0 with maxContains # too many",
            "draft2019-09/minContains.json # minContains = 0 with no maxContains # empty data",
            "draft2019-09/minContains.json # minContains = 0 with no maxContains # minContains = 0 makes contains always pass",
            "draft2019-09/minContains.json # minContains=2 with contains # all elements match, invalid minContains",
            "draft2019-09/minContains.json # minContains=2 with contains # some elements match, invalid minContains",
            "draft2019-09/minContains.json # minContains=2 with contains with a decimal value # one element matches, invalid minContains",
            "draft2019-09/not.json # collect annotations inside a 'not', even if collection is disabled # unevaluated property",
            "draft2020-12/maxContains.json # maxContains = 0 with minContains = 0 # empty array",
            "draft2020-12/maxContains.json # maxContains = 0 with minContains = 0 # one matching item",
            "draft2020-12/maxContains.json # maxContains with contains # all elements match, invalid maxContains",
            "draft2020-12/maxContains.json # maxContains with contains # some elements match, invalid maxContains",
            "draft2020-12/maxContains.json # maxContains with contains, value with a decimal # too many elements match, invalid maxContains",
            "draft2020-12/maxContains.json # minContains < maxContains # minContains < maxContains < actual",
            "draft2020-12/minContains.json # maxContains < minContains # invalid maxContains",
            "draft2020-12/minContains.json # maxContains < minContains # invalid maxContains and minContains",
            "draft2020-12/minContains.json # maxContains < minContains # invalid minContains",
            "draft2020-12/minContains.json # maxContains = minContains # all elements match, invalid maxContains",
            "draft2020-12/minContains.json # maxContains = minContains # all elements match, invalid minContains",
            "draft2020-12/minContains.json # minContains = 0 # empty data",
            "draft2020-12/minContains.json # minContains = 0 # minContains = 0 makes contains always pass",
            "draft2020-12/minContains.json # minContains = 0 with maxContains # empty data",
            "draft2020-12/minContains.json # minContains = 0 with maxContains # too many",
            "draft2020-12/minContains.json # minContains=2 with contains # all elements match, invalid minContains",
            "draft2020-12/minContains.json # minContains=2 with contains # some elements match, invalid minContains",
            "draft2020-12/minContains.json # minContains=2 with contains with a decimal value # one element matches, invalid minContains",
            "draft2020-12/not.json # collect annotations inside a 'not', even if collection is disabled # unevaluated property"
    );

    // SchemaNormalizer.inferMissingTypes adds the type the keywords imply and the
    // validator dispatches on it; the suite expects a keyword to ignore other types.
    // `format` is one of those keywords, which is why format cases sit here:
    // gjuton never checks the format itself.
    private static final Set<String> INFERRED_TYPE_REJECTS_VALID = Set.of(
            "draft2019-09/additionalItems.json # additionalItems as false without items # ignores non-arrays",
            "draft2019-09/additionalProperties.json # additionalProperties being false does not allow other properties # ignores arrays",
            "draft2019-09/additionalProperties.json # additionalProperties being false does not allow other properties # ignores other non-objects",
            "draft2019-09/additionalProperties.json # additionalProperties being false does not allow other properties # ignores strings",
            "draft2019-09/contains.json # contains keyword validation # not array is valid",
            "draft2019-09/contains.json # contains keyword with boolean schema false # non-arrays are valid",
            "draft2019-09/dependentRequired.json # empty dependents # non-object is valid",
            "draft2019-09/dependentRequired.json # single dependency # ignores arrays",
            "draft2019-09/dependentRequired.json # single dependency # ignores other non-objects",
            "draft2019-09/dependentRequired.json # single dependency # ignores strings",
            "draft2019-09/dependentSchemas.json # single dependency # ignores arrays",
            "draft2019-09/dependentSchemas.json # single dependency # ignores other non-objects",
            "draft2019-09/dependentSchemas.json # single dependency # ignores strings",
            "draft2019-09/exclusiveMaximum.json # exclusiveMaximum validation # ignores non-numbers",
            "draft2019-09/exclusiveMinimum.json # exclusiveMinimum validation # ignores non-numbers",
            "draft2019-09/format.json # date format # all string formats ignore arrays",
            "draft2019-09/format.json # date format # all string formats ignore booleans",
            "draft2019-09/format.json # date format # all string formats ignore floats",
            "draft2019-09/format.json # date format # all string formats ignore integers",
            "draft2019-09/format.json # date format # all string formats ignore nulls",
            "draft2019-09/format.json # date format # all string formats ignore objects",
            "draft2019-09/format.json # date-time format # all string formats ignore arrays",
            "draft2019-09/format.json # date-time format # all string formats ignore booleans",
            "draft2019-09/format.json # date-time format # all string formats ignore floats",
            "draft2019-09/format.json # date-time format # all string formats ignore integers",
            "draft2019-09/format.json # date-time format # all string formats ignore nulls",
            "draft2019-09/format.json # date-time format # all string formats ignore objects",
            "draft2019-09/format.json # duration format # all string formats ignore arrays",
            "draft2019-09/format.json # duration format # all string formats ignore booleans",
            "draft2019-09/format.json # duration format # all string formats ignore floats",
            "draft2019-09/format.json # duration format # all string formats ignore integers",
            "draft2019-09/format.json # duration format # all string formats ignore nulls",
            "draft2019-09/format.json # duration format # all string formats ignore objects",
            "draft2019-09/format.json # email format # all string formats ignore arrays",
            "draft2019-09/format.json # email format # all string formats ignore booleans",
            "draft2019-09/format.json # email format # all string formats ignore floats",
            "draft2019-09/format.json # email format # all string formats ignore integers",
            "draft2019-09/format.json # email format # all string formats ignore nulls",
            "draft2019-09/format.json # email format # all string formats ignore objects",
            "draft2019-09/format.json # hostname format # all string formats ignore arrays",
            "draft2019-09/format.json # hostname format # all string formats ignore booleans",
            "draft2019-09/format.json # hostname format # all string formats ignore floats",
            "draft2019-09/format.json # hostname format # all string formats ignore integers",
            "draft2019-09/format.json # hostname format # all string formats ignore nulls",
            "draft2019-09/format.json # hostname format # all string formats ignore objects",
            "draft2019-09/format.json # idn-email format # all string formats ignore arrays",
            "draft2019-09/format.json # idn-email format # all string formats ignore booleans",
            "draft2019-09/format.json # idn-email format # all string formats ignore floats",
            "draft2019-09/format.json # idn-email format # all string formats ignore integers",
            "draft2019-09/format.json # idn-email format # all string formats ignore nulls",
            "draft2019-09/format.json # idn-email format # all string formats ignore objects",
            "draft2019-09/format.json # idn-hostname format # all string formats ignore arrays",
            "draft2019-09/format.json # idn-hostname format # all string formats ignore booleans",
            "draft2019-09/format.json # idn-hostname format # all string formats ignore floats",
            "draft2019-09/format.json # idn-hostname format # all string formats ignore integers",
            "draft2019-09/format.json # idn-hostname format # all string formats ignore nulls",
            "draft2019-09/format.json # idn-hostname format # all string formats ignore objects",
            "draft2019-09/format.json # ipv4 format # all string formats ignore arrays",
            "draft2019-09/format.json # ipv4 format # all string formats ignore booleans",
            "draft2019-09/format.json # ipv4 format # all string formats ignore floats",
            "draft2019-09/format.json # ipv4 format # all string formats ignore integers",
            "draft2019-09/format.json # ipv4 format # all string formats ignore nulls",
            "draft2019-09/format.json # ipv4 format # all string formats ignore objects",
            "draft2019-09/format.json # ipv6 format # all string formats ignore arrays",
            "draft2019-09/format.json # ipv6 format # all string formats ignore booleans",
            "draft2019-09/format.json # ipv6 format # all string formats ignore floats",
            "draft2019-09/format.json # ipv6 format # all string formats ignore integers",
            "draft2019-09/format.json # ipv6 format # all string formats ignore nulls",
            "draft2019-09/format.json # ipv6 format # all string formats ignore objects",
            "draft2019-09/format.json # iri format # all string formats ignore arrays",
            "draft2019-09/format.json # iri format # all string formats ignore booleans",
            "draft2019-09/format.json # iri format # all string formats ignore floats",
            "draft2019-09/format.json # iri format # all string formats ignore integers",
            "draft2019-09/format.json # iri format # all string formats ignore nulls",
            "draft2019-09/format.json # iri format # all string formats ignore objects",
            "draft2019-09/format.json # iri-reference format # all string formats ignore arrays",
            "draft2019-09/format.json # iri-reference format # all string formats ignore booleans",
            "draft2019-09/format.json # iri-reference format # all string formats ignore floats",
            "draft2019-09/format.json # iri-reference format # all string formats ignore integers",
            "draft2019-09/format.json # iri-reference format # all string formats ignore nulls",
            "draft2019-09/format.json # iri-reference format # all string formats ignore objects",
            "draft2019-09/format.json # json-pointer format # all string formats ignore arrays",
            "draft2019-09/format.json # json-pointer format # all string formats ignore booleans",
            "draft2019-09/format.json # json-pointer format # all string formats ignore floats",
            "draft2019-09/format.json # json-pointer format # all string formats ignore integers",
            "draft2019-09/format.json # json-pointer format # all string formats ignore nulls",
            "draft2019-09/format.json # json-pointer format # all string formats ignore objects",
            "draft2019-09/format.json # regex format # all string formats ignore arrays",
            "draft2019-09/format.json # regex format # all string formats ignore booleans",
            "draft2019-09/format.json # regex format # all string formats ignore floats",
            "draft2019-09/format.json # regex format # all string formats ignore integers",
            "draft2019-09/format.json # regex format # all string formats ignore nulls",
            "draft2019-09/format.json # regex format # all string formats ignore objects",
            "draft2019-09/format.json # relative-json-pointer format # all string formats ignore arrays",
            "draft2019-09/format.json # relative-json-pointer format # all string formats ignore booleans",
            "draft2019-09/format.json # relative-json-pointer format # all string formats ignore floats",
            "draft2019-09/format.json # relative-json-pointer format # all string formats ignore integers",
            "draft2019-09/format.json # relative-json-pointer format # all string formats ignore nulls",
            "draft2019-09/format.json # relative-json-pointer format # all string formats ignore objects",
            "draft2019-09/format.json # time format # all string formats ignore arrays",
            "draft2019-09/format.json # time format # all string formats ignore booleans",
            "draft2019-09/format.json # time format # all string formats ignore floats",
            "draft2019-09/format.json # time format # all string formats ignore integers",
            "draft2019-09/format.json # time format # all string formats ignore nulls",
            "draft2019-09/format.json # time format # all string formats ignore objects",
            "draft2019-09/format.json # uri format # all string formats ignore arrays",
            "draft2019-09/format.json # uri format # all string formats ignore booleans",
            "draft2019-09/format.json # uri format # all string formats ignore floats",
            "draft2019-09/format.json # uri format # all string formats ignore integers",
            "draft2019-09/format.json # uri format # all string formats ignore nulls",
            "draft2019-09/format.json # uri format # all string formats ignore objects",
            "draft2019-09/format.json # uri-reference format # all string formats ignore arrays",
            "draft2019-09/format.json # uri-reference format # all string formats ignore booleans",
            "draft2019-09/format.json # uri-reference format # all string formats ignore floats",
            "draft2019-09/format.json # uri-reference format # all string formats ignore integers",
            "draft2019-09/format.json # uri-reference format # all string formats ignore nulls",
            "draft2019-09/format.json # uri-reference format # all string formats ignore objects",
            "draft2019-09/format.json # uri-template format # all string formats ignore arrays",
            "draft2019-09/format.json # uri-template format # all string formats ignore booleans",
            "draft2019-09/format.json # uri-template format # all string formats ignore floats",
            "draft2019-09/format.json # uri-template format # all string formats ignore integers",
            "draft2019-09/format.json # uri-template format # all string formats ignore nulls",
            "draft2019-09/format.json # uri-template format # all string formats ignore objects",
            "draft2019-09/format.json # uuid format # all string formats ignore arrays",
            "draft2019-09/format.json # uuid format # all string formats ignore booleans",
            "draft2019-09/format.json # uuid format # all string formats ignore floats",
            "draft2019-09/format.json # uuid format # all string formats ignore integers",
            "draft2019-09/format.json # uuid format # all string formats ignore nulls",
            "draft2019-09/format.json # uuid format # all string formats ignore objects",
            "draft2019-09/items.json # a schema given for items # JavaScript pseudo-array is valid",
            "draft2019-09/items.json # a schema given for items # ignores non-arrays",
            "draft2019-09/items.json # an array of schemas for items # JavaScript pseudo-array is valid",
            "draft2019-09/maxItems.json # maxItems validation # ignores non-arrays",
            "draft2019-09/maxLength.json # maxLength validation # ignores non-strings",
            "draft2019-09/maxProperties.json # maxProperties validation # ignores arrays",
            "draft2019-09/maxProperties.json # maxProperties validation # ignores other non-objects",
            "draft2019-09/maxProperties.json # maxProperties validation # ignores strings",
            "draft2019-09/maximum.json # maximum validation # ignores non-numbers",
            "draft2019-09/minItems.json # minItems validation # ignores non-arrays",
            "draft2019-09/minLength.json # minLength validation # ignores non-strings",
            "draft2019-09/minProperties.json # minProperties validation # ignores arrays",
            "draft2019-09/minProperties.json # minProperties validation # ignores booleans",
            "draft2019-09/minProperties.json # minProperties validation # ignores null",
            "draft2019-09/minProperties.json # minProperties validation # ignores other non-objects",
            "draft2019-09/minProperties.json # minProperties validation # ignores strings",
            "draft2019-09/minimum.json # minimum validation # ignores non-numbers",
            "draft2019-09/minimum.json # minimum validation with signed integer # ignores non-numbers",
            "draft2019-09/multipleOf.json # by int # ignores non-numbers",
            "draft2019-09/pattern.json # pattern validation # ignores arrays",
            "draft2019-09/pattern.json # pattern validation # ignores booleans",
            "draft2019-09/pattern.json # pattern validation # ignores floats",
            "draft2019-09/pattern.json # pattern validation # ignores integers",
            "draft2019-09/pattern.json # pattern validation # ignores null",
            "draft2019-09/pattern.json # pattern validation # ignores objects",
            "draft2019-09/patternProperties.json # patternProperties validates properties matching a regex # ignores arrays",
            "draft2019-09/patternProperties.json # patternProperties validates properties matching a regex # ignores other non-objects",
            "draft2019-09/patternProperties.json # patternProperties validates properties matching a regex # ignores strings",
            "draft2019-09/properties.json # object properties validation # ignores arrays",
            "draft2019-09/properties.json # object properties validation # ignores other non-objects",
            "draft2019-09/properties.json # properties whose names are Javascript object property names # ignores arrays",
            "draft2019-09/properties.json # properties whose names are Javascript object property names # ignores other non-objects",
            "draft2019-09/propertyNames.json # propertyNames validation # ignores arrays",
            "draft2019-09/propertyNames.json # propertyNames validation # ignores booleans",
            "draft2019-09/propertyNames.json # propertyNames validation # ignores null",
            "draft2019-09/propertyNames.json # propertyNames validation # ignores other non-objects",
            "draft2019-09/propertyNames.json # propertyNames validation # ignores strings",
            "draft2019-09/required.json # required properties whose names are Javascript object property names # ignores arrays",
            "draft2019-09/required.json # required properties whose names are Javascript object property names # ignores other non-objects",
            "draft2019-09/required.json # required validation # ignores arrays",
            "draft2019-09/required.json # required validation # ignores boolean",
            "draft2019-09/required.json # required validation # ignores null",
            "draft2019-09/required.json # required validation # ignores other non-objects",
            "draft2019-09/required.json # required validation # ignores strings",
            "draft2020-12/additionalProperties.json # additionalProperties being false does not allow other properties # ignores arrays",
            "draft2020-12/additionalProperties.json # additionalProperties being false does not allow other properties # ignores other non-objects",
            "draft2020-12/additionalProperties.json # additionalProperties being false does not allow other properties # ignores strings",
            "draft2020-12/contains.json # contains keyword validation # not array is valid",
            "draft2020-12/contains.json # contains keyword with boolean schema false # non-arrays are valid",
            "draft2020-12/dependentRequired.json # empty dependents # non-object is valid",
            "draft2020-12/dependentRequired.json # single dependency # ignores arrays",
            "draft2020-12/dependentRequired.json # single dependency # ignores other non-objects",
            "draft2020-12/dependentRequired.json # single dependency # ignores strings",
            "draft2020-12/dependentSchemas.json # single dependency # ignores arrays",
            "draft2020-12/dependentSchemas.json # single dependency # ignores other non-objects",
            "draft2020-12/dependentSchemas.json # single dependency # ignores strings",
            "draft2020-12/exclusiveMaximum.json # exclusiveMaximum validation # ignores non-numbers",
            "draft2020-12/exclusiveMinimum.json # exclusiveMinimum validation # ignores non-numbers",
            "draft2020-12/format.json # date format # all string formats ignore arrays",
            "draft2020-12/format.json # date format # all string formats ignore booleans",
            "draft2020-12/format.json # date format # all string formats ignore floats",
            "draft2020-12/format.json # date format # all string formats ignore integers",
            "draft2020-12/format.json # date format # all string formats ignore nulls",
            "draft2020-12/format.json # date format # all string formats ignore objects",
            "draft2020-12/format.json # date-time format # all string formats ignore arrays",
            "draft2020-12/format.json # date-time format # all string formats ignore booleans",
            "draft2020-12/format.json # date-time format # all string formats ignore floats",
            "draft2020-12/format.json # date-time format # all string formats ignore integers",
            "draft2020-12/format.json # date-time format # all string formats ignore nulls",
            "draft2020-12/format.json # date-time format # all string formats ignore objects",
            "draft2020-12/format.json # duration format # all string formats ignore arrays",
            "draft2020-12/format.json # duration format # all string formats ignore booleans",
            "draft2020-12/format.json # duration format # all string formats ignore floats",
            "draft2020-12/format.json # duration format # all string formats ignore integers",
            "draft2020-12/format.json # duration format # all string formats ignore nulls",
            "draft2020-12/format.json # duration format # all string formats ignore objects",
            "draft2020-12/format.json # email format # all string formats ignore arrays",
            "draft2020-12/format.json # email format # all string formats ignore booleans",
            "draft2020-12/format.json # email format # all string formats ignore floats",
            "draft2020-12/format.json # email format # all string formats ignore integers",
            "draft2020-12/format.json # email format # all string formats ignore nulls",
            "draft2020-12/format.json # email format # all string formats ignore objects",
            "draft2020-12/format.json # hostname format # all string formats ignore arrays",
            "draft2020-12/format.json # hostname format # all string formats ignore booleans",
            "draft2020-12/format.json # hostname format # all string formats ignore floats",
            "draft2020-12/format.json # hostname format # all string formats ignore integers",
            "draft2020-12/format.json # hostname format # all string formats ignore nulls",
            "draft2020-12/format.json # hostname format # all string formats ignore objects",
            "draft2020-12/format.json # idn-email format # all string formats ignore arrays",
            "draft2020-12/format.json # idn-email format # all string formats ignore booleans",
            "draft2020-12/format.json # idn-email format # all string formats ignore floats",
            "draft2020-12/format.json # idn-email format # all string formats ignore integers",
            "draft2020-12/format.json # idn-email format # all string formats ignore nulls",
            "draft2020-12/format.json # idn-email format # all string formats ignore objects",
            "draft2020-12/format.json # idn-hostname format # all string formats ignore arrays",
            "draft2020-12/format.json # idn-hostname format # all string formats ignore booleans",
            "draft2020-12/format.json # idn-hostname format # all string formats ignore floats",
            "draft2020-12/format.json # idn-hostname format # all string formats ignore integers",
            "draft2020-12/format.json # idn-hostname format # all string formats ignore nulls",
            "draft2020-12/format.json # idn-hostname format # all string formats ignore objects",
            "draft2020-12/format.json # ipv4 format # all string formats ignore arrays",
            "draft2020-12/format.json # ipv4 format # all string formats ignore booleans",
            "draft2020-12/format.json # ipv4 format # all string formats ignore floats",
            "draft2020-12/format.json # ipv4 format # all string formats ignore integers",
            "draft2020-12/format.json # ipv4 format # all string formats ignore nulls",
            "draft2020-12/format.json # ipv4 format # all string formats ignore objects",
            "draft2020-12/format.json # ipv6 format # all string formats ignore arrays",
            "draft2020-12/format.json # ipv6 format # all string formats ignore booleans",
            "draft2020-12/format.json # ipv6 format # all string formats ignore floats",
            "draft2020-12/format.json # ipv6 format # all string formats ignore integers",
            "draft2020-12/format.json # ipv6 format # all string formats ignore nulls",
            "draft2020-12/format.json # ipv6 format # all string formats ignore objects",
            "draft2020-12/format.json # iri format # all string formats ignore arrays",
            "draft2020-12/format.json # iri format # all string formats ignore booleans",
            "draft2020-12/format.json # iri format # all string formats ignore floats",
            "draft2020-12/format.json # iri format # all string formats ignore integers",
            "draft2020-12/format.json # iri format # all string formats ignore nulls",
            "draft2020-12/format.json # iri format # all string formats ignore objects",
            "draft2020-12/format.json # iri-reference format # all string formats ignore arrays",
            "draft2020-12/format.json # iri-reference format # all string formats ignore booleans",
            "draft2020-12/format.json # iri-reference format # all string formats ignore floats",
            "draft2020-12/format.json # iri-reference format # all string formats ignore integers",
            "draft2020-12/format.json # iri-reference format # all string formats ignore nulls",
            "draft2020-12/format.json # iri-reference format # all string formats ignore objects",
            "draft2020-12/format.json # json-pointer format # all string formats ignore arrays",
            "draft2020-12/format.json # json-pointer format # all string formats ignore booleans",
            "draft2020-12/format.json # json-pointer format # all string formats ignore floats",
            "draft2020-12/format.json # json-pointer format # all string formats ignore integers",
            "draft2020-12/format.json # json-pointer format # all string formats ignore nulls",
            "draft2020-12/format.json # json-pointer format # all string formats ignore objects",
            "draft2020-12/format.json # regex format # all string formats ignore arrays",
            "draft2020-12/format.json # regex format # all string formats ignore booleans",
            "draft2020-12/format.json # regex format # all string formats ignore floats",
            "draft2020-12/format.json # regex format # all string formats ignore integers",
            "draft2020-12/format.json # regex format # all string formats ignore nulls",
            "draft2020-12/format.json # regex format # all string formats ignore objects",
            "draft2020-12/format.json # relative-json-pointer format # all string formats ignore arrays",
            "draft2020-12/format.json # relative-json-pointer format # all string formats ignore booleans",
            "draft2020-12/format.json # relative-json-pointer format # all string formats ignore floats",
            "draft2020-12/format.json # relative-json-pointer format # all string formats ignore integers",
            "draft2020-12/format.json # relative-json-pointer format # all string formats ignore nulls",
            "draft2020-12/format.json # relative-json-pointer format # all string formats ignore objects",
            "draft2020-12/format.json # time format # all string formats ignore arrays",
            "draft2020-12/format.json # time format # all string formats ignore booleans",
            "draft2020-12/format.json # time format # all string formats ignore floats",
            "draft2020-12/format.json # time format # all string formats ignore integers",
            "draft2020-12/format.json # time format # all string formats ignore nulls",
            "draft2020-12/format.json # time format # all string formats ignore objects",
            "draft2020-12/format.json # uri format # all string formats ignore arrays",
            "draft2020-12/format.json # uri format # all string formats ignore booleans",
            "draft2020-12/format.json # uri format # all string formats ignore floats",
            "draft2020-12/format.json # uri format # all string formats ignore integers",
            "draft2020-12/format.json # uri format # all string formats ignore nulls",
            "draft2020-12/format.json # uri format # all string formats ignore objects",
            "draft2020-12/format.json # uri-reference format # all string formats ignore arrays",
            "draft2020-12/format.json # uri-reference format # all string formats ignore booleans",
            "draft2020-12/format.json # uri-reference format # all string formats ignore floats",
            "draft2020-12/format.json # uri-reference format # all string formats ignore integers",
            "draft2020-12/format.json # uri-reference format # all string formats ignore nulls",
            "draft2020-12/format.json # uri-reference format # all string formats ignore objects",
            "draft2020-12/format.json # uri-template format # all string formats ignore arrays",
            "draft2020-12/format.json # uri-template format # all string formats ignore booleans",
            "draft2020-12/format.json # uri-template format # all string formats ignore floats",
            "draft2020-12/format.json # uri-template format # all string formats ignore integers",
            "draft2020-12/format.json # uri-template format # all string formats ignore nulls",
            "draft2020-12/format.json # uri-template format # all string formats ignore objects",
            "draft2020-12/format.json # uuid format # all string formats ignore arrays",
            "draft2020-12/format.json # uuid format # all string formats ignore booleans",
            "draft2020-12/format.json # uuid format # all string formats ignore floats",
            "draft2020-12/format.json # uuid format # all string formats ignore integers",
            "draft2020-12/format.json # uuid format # all string formats ignore nulls",
            "draft2020-12/format.json # uuid format # all string formats ignore objects",
            "draft2020-12/items.json # a schema given for items # JavaScript pseudo-array is valid",
            "draft2020-12/items.json # a schema given for items # ignores non-arrays",
            "draft2020-12/maxItems.json # maxItems validation # ignores non-arrays",
            "draft2020-12/maxLength.json # maxLength validation # ignores non-strings",
            "draft2020-12/maxProperties.json # maxProperties validation # ignores arrays",
            "draft2020-12/maxProperties.json # maxProperties validation # ignores other non-objects",
            "draft2020-12/maxProperties.json # maxProperties validation # ignores strings",
            "draft2020-12/maximum.json # maximum validation # ignores non-numbers",
            "draft2020-12/minItems.json # minItems validation # ignores non-arrays",
            "draft2020-12/minLength.json # minLength validation # ignores non-strings",
            "draft2020-12/minProperties.json # minProperties validation # ignores arrays",
            "draft2020-12/minProperties.json # minProperties validation # ignores booleans",
            "draft2020-12/minProperties.json # minProperties validation # ignores null",
            "draft2020-12/minProperties.json # minProperties validation # ignores other non-objects",
            "draft2020-12/minProperties.json # minProperties validation # ignores strings",
            "draft2020-12/minimum.json # minimum validation # ignores non-numbers",
            "draft2020-12/minimum.json # minimum validation with signed integer # ignores non-numbers",
            "draft2020-12/multipleOf.json # by int # ignores non-numbers",
            "draft2020-12/pattern.json # pattern validation # ignores arrays",
            "draft2020-12/pattern.json # pattern validation # ignores booleans",
            "draft2020-12/pattern.json # pattern validation # ignores floats",
            "draft2020-12/pattern.json # pattern validation # ignores integers",
            "draft2020-12/pattern.json # pattern validation # ignores null",
            "draft2020-12/pattern.json # pattern validation # ignores objects",
            "draft2020-12/patternProperties.json # patternProperties validates properties matching a regex # ignores arrays",
            "draft2020-12/patternProperties.json # patternProperties validates properties matching a regex # ignores other non-objects",
            "draft2020-12/patternProperties.json # patternProperties validates properties matching a regex # ignores strings",
            "draft2020-12/prefixItems.json # a schema given for prefixItems # JavaScript pseudo-array is valid",
            "draft2020-12/properties.json # object properties validation # ignores arrays",
            "draft2020-12/properties.json # object properties validation # ignores other non-objects",
            "draft2020-12/properties.json # properties whose names are Javascript object property names # ignores arrays",
            "draft2020-12/properties.json # properties whose names are Javascript object property names # ignores other non-objects",
            "draft2020-12/propertyNames.json # propertyNames validation # ignores arrays",
            "draft2020-12/propertyNames.json # propertyNames validation # ignores booleans",
            "draft2020-12/propertyNames.json # propertyNames validation # ignores null",
            "draft2020-12/propertyNames.json # propertyNames validation # ignores other non-objects",
            "draft2020-12/propertyNames.json # propertyNames validation # ignores strings",
            "draft2020-12/required.json # required properties whose names are Javascript object property names # ignores arrays",
            "draft2020-12/required.json # required properties whose names are Javascript object property names # ignores other non-objects",
            "draft2020-12/required.json # required validation # ignores arrays",
            "draft2020-12/required.json # required validation # ignores boolean",
            "draft2020-12/required.json # required validation # ignores null",
            "draft2020-12/required.json # required validation # ignores other non-objects",
            "draft2020-12/required.json # required validation # ignores strings",
            "draft4/additionalItems.json # additionalItems as false without items # ignores non-arrays",
            "draft4/additionalProperties.json # additionalProperties being false does not allow other properties # ignores arrays",
            "draft4/additionalProperties.json # additionalProperties being false does not allow other properties # ignores other non-objects",
            "draft4/additionalProperties.json # additionalProperties being false does not allow other properties # ignores strings",
            "draft4/dependencies.json # dependencies # ignores arrays",
            "draft4/dependencies.json # dependencies # ignores other non-objects",
            "draft4/dependencies.json # dependencies # ignores strings",
            "draft4/format.json # date-time format # all string formats ignore arrays",
            "draft4/format.json # date-time format # all string formats ignore booleans",
            "draft4/format.json # date-time format # all string formats ignore floats",
            "draft4/format.json # date-time format # all string formats ignore integers",
            "draft4/format.json # date-time format # all string formats ignore nulls",
            "draft4/format.json # date-time format # all string formats ignore objects",
            "draft4/format.json # email format # all string formats ignore arrays",
            "draft4/format.json # email format # all string formats ignore booleans",
            "draft4/format.json # email format # all string formats ignore floats",
            "draft4/format.json # email format # all string formats ignore integers",
            "draft4/format.json # email format # all string formats ignore nulls",
            "draft4/format.json # email format # all string formats ignore objects",
            "draft4/format.json # hostname format # all string formats ignore arrays",
            "draft4/format.json # hostname format # all string formats ignore booleans",
            "draft4/format.json # hostname format # all string formats ignore floats",
            "draft4/format.json # hostname format # all string formats ignore integers",
            "draft4/format.json # hostname format # all string formats ignore nulls",
            "draft4/format.json # hostname format # all string formats ignore objects",
            "draft4/format.json # ipv4 format # all string formats ignore arrays",
            "draft4/format.json # ipv4 format # all string formats ignore booleans",
            "draft4/format.json # ipv4 format # all string formats ignore floats",
            "draft4/format.json # ipv4 format # all string formats ignore integers",
            "draft4/format.json # ipv4 format # all string formats ignore nulls",
            "draft4/format.json # ipv4 format # all string formats ignore objects",
            "draft4/format.json # ipv6 format # all string formats ignore arrays",
            "draft4/format.json # ipv6 format # all string formats ignore booleans",
            "draft4/format.json # ipv6 format # all string formats ignore floats",
            "draft4/format.json # ipv6 format # all string formats ignore integers",
            "draft4/format.json # ipv6 format # all string formats ignore nulls",
            "draft4/format.json # ipv6 format # all string formats ignore objects",
            "draft4/format.json # uri format # all string formats ignore arrays",
            "draft4/format.json # uri format # all string formats ignore booleans",
            "draft4/format.json # uri format # all string formats ignore floats",
            "draft4/format.json # uri format # all string formats ignore integers",
            "draft4/format.json # uri format # all string formats ignore nulls",
            "draft4/format.json # uri format # all string formats ignore objects",
            "draft4/items.json # a schema given for items # JavaScript pseudo-array is valid",
            "draft4/items.json # a schema given for items # ignores non-arrays",
            "draft4/items.json # an array of schemas for items # JavaScript pseudo-array is valid",
            "draft4/maxItems.json # maxItems validation # ignores non-arrays",
            "draft4/maxLength.json # maxLength validation # ignores non-strings",
            "draft4/maxProperties.json # maxProperties validation # ignores arrays",
            "draft4/maxProperties.json # maxProperties validation # ignores other non-objects",
            "draft4/maxProperties.json # maxProperties validation # ignores strings",
            "draft4/maximum.json # maximum validation # ignores non-numbers",
            "draft4/maximum.json # maximum validation (explicit false exclusivity) # ignores non-numbers",
            "draft4/minItems.json # minItems validation # ignores non-arrays",
            "draft4/minLength.json # minLength validation # ignores non-strings",
            "draft4/minProperties.json # minProperties validation # ignores arrays",
            "draft4/minProperties.json # minProperties validation # ignores booleans",
            "draft4/minProperties.json # minProperties validation # ignores null",
            "draft4/minProperties.json # minProperties validation # ignores other non-objects",
            "draft4/minProperties.json # minProperties validation # ignores strings",
            "draft4/minimum.json # minimum validation # ignores non-numbers",
            "draft4/minimum.json # minimum validation (explicit false exclusivity) # ignores non-numbers",
            "draft4/minimum.json # minimum validation with signed integer # ignores non-numbers",
            "draft4/multipleOf.json # by int # ignores non-numbers",
            "draft4/pattern.json # pattern validation # ignores arrays",
            "draft4/pattern.json # pattern validation # ignores booleans",
            "draft4/pattern.json # pattern validation # ignores floats",
            "draft4/pattern.json # pattern validation # ignores integers",
            "draft4/pattern.json # pattern validation # ignores null",
            "draft4/pattern.json # pattern validation # ignores objects",
            "draft4/patternProperties.json # patternProperties validates properties matching a regex # ignores arrays",
            "draft4/patternProperties.json # patternProperties validates properties matching a regex # ignores other non-objects",
            "draft4/patternProperties.json # patternProperties validates properties matching a regex # ignores strings",
            "draft4/properties.json # object properties validation # ignores arrays",
            "draft4/properties.json # object properties validation # ignores other non-objects",
            "draft4/properties.json # properties whose names are Javascript object property names # ignores arrays",
            "draft4/properties.json # properties whose names are Javascript object property names # ignores other non-objects",
            "draft4/required.json # required properties whose names are Javascript object property names # ignores arrays",
            "draft4/required.json # required properties whose names are Javascript object property names # ignores other non-objects",
            "draft4/required.json # required validation # ignores arrays",
            "draft4/required.json # required validation # ignores boolean",
            "draft4/required.json # required validation # ignores null",
            "draft4/required.json # required validation # ignores other non-objects",
            "draft4/required.json # required validation # ignores strings",
            "draft6/additionalItems.json # additionalItems as false without items # ignores non-arrays",
            "draft6/additionalProperties.json # additionalProperties being false does not allow other properties # ignores arrays",
            "draft6/additionalProperties.json # additionalProperties being false does not allow other properties # ignores other non-objects",
            "draft6/additionalProperties.json # additionalProperties being false does not allow other properties # ignores strings",
            "draft6/contains.json # contains keyword validation # not array is valid",
            "draft6/contains.json # contains keyword with boolean schema false # non-arrays are valid",
            "draft6/dependencies.json # dependencies # ignores arrays",
            "draft6/dependencies.json # dependencies # ignores other non-objects",
            "draft6/dependencies.json # dependencies # ignores strings",
            "draft6/dependencies.json # dependencies with empty array # non-object is valid",
            "draft6/exclusiveMaximum.json # exclusiveMaximum validation # ignores non-numbers",
            "draft6/exclusiveMinimum.json # exclusiveMinimum validation # ignores non-numbers",
            "draft6/format.json # date-time format # all string formats ignore arrays",
            "draft6/format.json # date-time format # all string formats ignore booleans",
            "draft6/format.json # date-time format # all string formats ignore floats",
            "draft6/format.json # date-time format # all string formats ignore integers",
            "draft6/format.json # date-time format # all string formats ignore nulls",
            "draft6/format.json # date-time format # all string formats ignore objects",
            "draft6/format.json # email format # all string formats ignore arrays",
            "draft6/format.json # email format # all string formats ignore booleans",
            "draft6/format.json # email format # all string formats ignore floats",
            "draft6/format.json # email format # all string formats ignore integers",
            "draft6/format.json # email format # all string formats ignore nulls",
            "draft6/format.json # email format # all string formats ignore objects",
            "draft6/format.json # hostname format # all string formats ignore arrays",
            "draft6/format.json # hostname format # all string formats ignore booleans",
            "draft6/format.json # hostname format # all string formats ignore floats",
            "draft6/format.json # hostname format # all string formats ignore integers",
            "draft6/format.json # hostname format # all string formats ignore nulls",
            "draft6/format.json # hostname format # all string formats ignore objects",
            "draft6/format.json # ipv4 format # all string formats ignore arrays",
            "draft6/format.json # ipv4 format # all string formats ignore booleans",
            "draft6/format.json # ipv4 format # all string formats ignore floats",
            "draft6/format.json # ipv4 format # all string formats ignore integers",
            "draft6/format.json # ipv4 format # all string formats ignore nulls",
            "draft6/format.json # ipv4 format # all string formats ignore objects",
            "draft6/format.json # ipv6 format # all string formats ignore arrays",
            "draft6/format.json # ipv6 format # all string formats ignore booleans",
            "draft6/format.json # ipv6 format # all string formats ignore floats",
            "draft6/format.json # ipv6 format # all string formats ignore integers",
            "draft6/format.json # ipv6 format # all string formats ignore nulls",
            "draft6/format.json # ipv6 format # all string formats ignore objects",
            "draft6/format.json # json-pointer format # all string formats ignore arrays",
            "draft6/format.json # json-pointer format # all string formats ignore booleans",
            "draft6/format.json # json-pointer format # all string formats ignore floats",
            "draft6/format.json # json-pointer format # all string formats ignore integers",
            "draft6/format.json # json-pointer format # all string formats ignore nulls",
            "draft6/format.json # json-pointer format # all string formats ignore objects",
            "draft6/format.json # uri format # all string formats ignore arrays",
            "draft6/format.json # uri format # all string formats ignore booleans",
            "draft6/format.json # uri format # all string formats ignore floats",
            "draft6/format.json # uri format # all string formats ignore integers",
            "draft6/format.json # uri format # all string formats ignore nulls",
            "draft6/format.json # uri format # all string formats ignore objects",
            "draft6/format.json # uri-reference format # all string formats ignore arrays",
            "draft6/format.json # uri-reference format # all string formats ignore booleans",
            "draft6/format.json # uri-reference format # all string formats ignore floats",
            "draft6/format.json # uri-reference format # all string formats ignore integers",
            "draft6/format.json # uri-reference format # all string formats ignore nulls",
            "draft6/format.json # uri-reference format # all string formats ignore objects",
            "draft6/format.json # uri-template format # all string formats ignore arrays",
            "draft6/format.json # uri-template format # all string formats ignore booleans",
            "draft6/format.json # uri-template format # all string formats ignore floats",
            "draft6/format.json # uri-template format # all string formats ignore integers",
            "draft6/format.json # uri-template format # all string formats ignore nulls",
            "draft6/format.json # uri-template format # all string formats ignore objects",
            "draft6/items.json # a schema given for items # JavaScript pseudo-array is valid",
            "draft6/items.json # a schema given for items # ignores non-arrays",
            "draft6/items.json # an array of schemas for items # JavaScript pseudo-array is valid",
            "draft6/maxItems.json # maxItems validation # ignores non-arrays",
            "draft6/maxLength.json # maxLength validation # ignores non-strings",
            "draft6/maxProperties.json # maxProperties validation # ignores arrays",
            "draft6/maxProperties.json # maxProperties validation # ignores other non-objects",
            "draft6/maxProperties.json # maxProperties validation # ignores strings",
            "draft6/maximum.json # maximum validation # ignores non-numbers",
            "draft6/minItems.json # minItems validation # ignores non-arrays",
            "draft6/minLength.json # minLength validation # ignores non-strings",
            "draft6/minProperties.json # minProperties validation # ignores arrays",
            "draft6/minProperties.json # minProperties validation # ignores booleans",
            "draft6/minProperties.json # minProperties validation # ignores null",
            "draft6/minProperties.json # minProperties validation # ignores other non-objects",
            "draft6/minProperties.json # minProperties validation # ignores strings",
            "draft6/minimum.json # minimum validation # ignores non-numbers",
            "draft6/minimum.json # minimum validation with signed integer # ignores non-numbers",
            "draft6/multipleOf.json # by int # ignores non-numbers",
            "draft6/pattern.json # pattern validation # ignores arrays",
            "draft6/pattern.json # pattern validation # ignores booleans",
            "draft6/pattern.json # pattern validation # ignores floats",
            "draft6/pattern.json # pattern validation # ignores integers",
            "draft6/pattern.json # pattern validation # ignores null",
            "draft6/pattern.json # pattern validation # ignores objects",
            "draft6/patternProperties.json # patternProperties validates properties matching a regex # ignores arrays",
            "draft6/patternProperties.json # patternProperties validates properties matching a regex # ignores other non-objects",
            "draft6/patternProperties.json # patternProperties validates properties matching a regex # ignores strings",
            "draft6/properties.json # object properties validation # ignores arrays",
            "draft6/properties.json # object properties validation # ignores other non-objects",
            "draft6/properties.json # properties whose names are Javascript object property names # ignores arrays",
            "draft6/properties.json # properties whose names are Javascript object property names # ignores other non-objects",
            "draft6/propertyNames.json # propertyNames validation # ignores arrays",
            "draft6/propertyNames.json # propertyNames validation # ignores booleans",
            "draft6/propertyNames.json # propertyNames validation # ignores null",
            "draft6/propertyNames.json # propertyNames validation # ignores other non-objects",
            "draft6/propertyNames.json # propertyNames validation # ignores strings",
            "draft6/required.json # required properties whose names are Javascript object property names # ignores arrays",
            "draft6/required.json # required properties whose names are Javascript object property names # ignores other non-objects",
            "draft6/required.json # required validation # ignores arrays",
            "draft6/required.json # required validation # ignores boolean",
            "draft6/required.json # required validation # ignores null",
            "draft6/required.json # required validation # ignores other non-objects",
            "draft6/required.json # required validation # ignores strings",
            "draft7/additionalItems.json # additionalItems as false without items # ignores non-arrays",
            "draft7/additionalProperties.json # additionalProperties being false does not allow other properties # ignores arrays",
            "draft7/additionalProperties.json # additionalProperties being false does not allow other properties # ignores other non-objects",
            "draft7/additionalProperties.json # additionalProperties being false does not allow other properties # ignores strings",
            "draft7/contains.json # contains keyword validation # not array is valid",
            "draft7/contains.json # contains keyword with boolean schema false # non-arrays are valid",
            "draft7/dependencies.json # dependencies # ignores arrays",
            "draft7/dependencies.json # dependencies # ignores other non-objects",
            "draft7/dependencies.json # dependencies # ignores strings",
            "draft7/dependencies.json # dependencies with empty array # non-object is valid",
            "draft7/exclusiveMaximum.json # exclusiveMaximum validation # ignores non-numbers",
            "draft7/exclusiveMinimum.json # exclusiveMinimum validation # ignores non-numbers",
            "draft7/format.json # date format # all string formats ignore arrays",
            "draft7/format.json # date format # all string formats ignore booleans",
            "draft7/format.json # date format # all string formats ignore floats",
            "draft7/format.json # date format # all string formats ignore integers",
            "draft7/format.json # date format # all string formats ignore nulls",
            "draft7/format.json # date format # all string formats ignore objects",
            "draft7/format.json # date-time format # all string formats ignore arrays",
            "draft7/format.json # date-time format # all string formats ignore booleans",
            "draft7/format.json # date-time format # all string formats ignore floats",
            "draft7/format.json # date-time format # all string formats ignore integers",
            "draft7/format.json # date-time format # all string formats ignore nulls",
            "draft7/format.json # date-time format # all string formats ignore objects",
            "draft7/format.json # email format # all string formats ignore arrays",
            "draft7/format.json # email format # all string formats ignore booleans",
            "draft7/format.json # email format # all string formats ignore floats",
            "draft7/format.json # email format # all string formats ignore integers",
            "draft7/format.json # email format # all string formats ignore nulls",
            "draft7/format.json # email format # all string formats ignore objects",
            "draft7/format.json # hostname format # all string formats ignore arrays",
            "draft7/format.json # hostname format # all string formats ignore booleans",
            "draft7/format.json # hostname format # all string formats ignore floats",
            "draft7/format.json # hostname format # all string formats ignore integers",
            "draft7/format.json # hostname format # all string formats ignore nulls",
            "draft7/format.json # hostname format # all string formats ignore objects",
            "draft7/format.json # idn-email format # all string formats ignore arrays",
            "draft7/format.json # idn-email format # all string formats ignore booleans",
            "draft7/format.json # idn-email format # all string formats ignore floats",
            "draft7/format.json # idn-email format # all string formats ignore integers",
            "draft7/format.json # idn-email format # all string formats ignore nulls",
            "draft7/format.json # idn-email format # all string formats ignore objects",
            "draft7/format.json # idn-hostname format # all string formats ignore arrays",
            "draft7/format.json # idn-hostname format # all string formats ignore booleans",
            "draft7/format.json # idn-hostname format # all string formats ignore floats",
            "draft7/format.json # idn-hostname format # all string formats ignore integers",
            "draft7/format.json # idn-hostname format # all string formats ignore nulls",
            "draft7/format.json # idn-hostname format # all string formats ignore objects",
            "draft7/format.json # ipv4 format # all string formats ignore arrays",
            "draft7/format.json # ipv4 format # all string formats ignore booleans",
            "draft7/format.json # ipv4 format # all string formats ignore floats",
            "draft7/format.json # ipv4 format # all string formats ignore integers",
            "draft7/format.json # ipv4 format # all string formats ignore nulls",
            "draft7/format.json # ipv4 format # all string formats ignore objects",
            "draft7/format.json # ipv6 format # all string formats ignore arrays",
            "draft7/format.json # ipv6 format # all string formats ignore booleans",
            "draft7/format.json # ipv6 format # all string formats ignore floats",
            "draft7/format.json # ipv6 format # all string formats ignore integers",
            "draft7/format.json # ipv6 format # all string formats ignore nulls",
            "draft7/format.json # ipv6 format # all string formats ignore objects",
            "draft7/format.json # iri format # all string formats ignore arrays",
            "draft7/format.json # iri format # all string formats ignore booleans",
            "draft7/format.json # iri format # all string formats ignore floats",
            "draft7/format.json # iri format # all string formats ignore integers",
            "draft7/format.json # iri format # all string formats ignore nulls",
            "draft7/format.json # iri format # all string formats ignore objects",
            "draft7/format.json # iri-reference format # all string formats ignore arrays",
            "draft7/format.json # iri-reference format # all string formats ignore booleans",
            "draft7/format.json # iri-reference format # all string formats ignore floats",
            "draft7/format.json # iri-reference format # all string formats ignore integers",
            "draft7/format.json # iri-reference format # all string formats ignore nulls",
            "draft7/format.json # iri-reference format # all string formats ignore objects",
            "draft7/format.json # json-pointer format # all string formats ignore arrays",
            "draft7/format.json # json-pointer format # all string formats ignore booleans",
            "draft7/format.json # json-pointer format # all string formats ignore floats",
            "draft7/format.json # json-pointer format # all string formats ignore integers",
            "draft7/format.json # json-pointer format # all string formats ignore nulls",
            "draft7/format.json # json-pointer format # all string formats ignore objects",
            "draft7/format.json # regex format # all string formats ignore arrays",
            "draft7/format.json # regex format # all string formats ignore booleans",
            "draft7/format.json # regex format # all string formats ignore floats",
            "draft7/format.json # regex format # all string formats ignore integers",
            "draft7/format.json # regex format # all string formats ignore nulls",
            "draft7/format.json # regex format # all string formats ignore objects",
            "draft7/format.json # relative-json-pointer format # all string formats ignore arrays",
            "draft7/format.json # relative-json-pointer format # all string formats ignore booleans",
            "draft7/format.json # relative-json-pointer format # all string formats ignore floats",
            "draft7/format.json # relative-json-pointer format # all string formats ignore integers",
            "draft7/format.json # relative-json-pointer format # all string formats ignore nulls",
            "draft7/format.json # relative-json-pointer format # all string formats ignore objects",
            "draft7/format.json # time format # all string formats ignore arrays",
            "draft7/format.json # time format # all string formats ignore booleans",
            "draft7/format.json # time format # all string formats ignore floats",
            "draft7/format.json # time format # all string formats ignore integers",
            "draft7/format.json # time format # all string formats ignore nulls",
            "draft7/format.json # time format # all string formats ignore objects",
            "draft7/format.json # uri format # all string formats ignore arrays",
            "draft7/format.json # uri format # all string formats ignore booleans",
            "draft7/format.json # uri format # all string formats ignore floats",
            "draft7/format.json # uri format # all string formats ignore integers",
            "draft7/format.json # uri format # all string formats ignore nulls",
            "draft7/format.json # uri format # all string formats ignore objects",
            "draft7/format.json # uri-reference format # all string formats ignore arrays",
            "draft7/format.json # uri-reference format # all string formats ignore booleans",
            "draft7/format.json # uri-reference format # all string formats ignore floats",
            "draft7/format.json # uri-reference format # all string formats ignore integers",
            "draft7/format.json # uri-reference format # all string formats ignore nulls",
            "draft7/format.json # uri-reference format # all string formats ignore objects",
            "draft7/format.json # uri-template format # all string formats ignore arrays",
            "draft7/format.json # uri-template format # all string formats ignore booleans",
            "draft7/format.json # uri-template format # all string formats ignore floats",
            "draft7/format.json # uri-template format # all string formats ignore integers",
            "draft7/format.json # uri-template format # all string formats ignore nulls",
            "draft7/format.json # uri-template format # all string formats ignore objects",
            "draft7/items.json # a schema given for items # JavaScript pseudo-array is valid",
            "draft7/items.json # a schema given for items # ignores non-arrays",
            "draft7/items.json # an array of schemas for items # JavaScript pseudo-array is valid",
            "draft7/maxItems.json # maxItems validation # ignores non-arrays",
            "draft7/maxLength.json # maxLength validation # ignores non-strings",
            "draft7/maxProperties.json # maxProperties validation # ignores arrays",
            "draft7/maxProperties.json # maxProperties validation # ignores other non-objects",
            "draft7/maxProperties.json # maxProperties validation # ignores strings",
            "draft7/maximum.json # maximum validation # ignores non-numbers",
            "draft7/minItems.json # minItems validation # ignores non-arrays",
            "draft7/minLength.json # minLength validation # ignores non-strings",
            "draft7/minProperties.json # minProperties validation # ignores arrays",
            "draft7/minProperties.json # minProperties validation # ignores booleans",
            "draft7/minProperties.json # minProperties validation # ignores null",
            "draft7/minProperties.json # minProperties validation # ignores other non-objects",
            "draft7/minProperties.json # minProperties validation # ignores strings",
            "draft7/minimum.json # minimum validation # ignores non-numbers",
            "draft7/minimum.json # minimum validation with signed integer # ignores non-numbers",
            "draft7/multipleOf.json # by int # ignores non-numbers",
            "draft7/pattern.json # pattern validation # ignores arrays",
            "draft7/pattern.json # pattern validation # ignores booleans",
            "draft7/pattern.json # pattern validation # ignores floats",
            "draft7/pattern.json # pattern validation # ignores integers",
            "draft7/pattern.json # pattern validation # ignores null",
            "draft7/pattern.json # pattern validation # ignores objects",
            "draft7/patternProperties.json # patternProperties validates properties matching a regex # ignores arrays",
            "draft7/patternProperties.json # patternProperties validates properties matching a regex # ignores other non-objects",
            "draft7/patternProperties.json # patternProperties validates properties matching a regex # ignores strings",
            "draft7/properties.json # object properties validation # ignores arrays",
            "draft7/properties.json # object properties validation # ignores other non-objects",
            "draft7/properties.json # properties whose names are Javascript object property names # ignores arrays",
            "draft7/properties.json # properties whose names are Javascript object property names # ignores other non-objects",
            "draft7/propertyNames.json # propertyNames validation # ignores arrays",
            "draft7/propertyNames.json # propertyNames validation # ignores booleans",
            "draft7/propertyNames.json # propertyNames validation # ignores null",
            "draft7/propertyNames.json # propertyNames validation # ignores other non-objects",
            "draft7/propertyNames.json # propertyNames validation # ignores strings",
            "draft7/required.json # required properties whose names are Javascript object property names # ignores arrays",
            "draft7/required.json # required properties whose names are Javascript object property names # ignores other non-objects",
            "draft7/required.json # required validation # ignores arrays",
            "draft7/required.json # required validation # ignores boolean",
            "draft7/required.json # required validation # ignores null",
            "draft7/required.json # required validation # ignores other non-objects",
            "draft7/required.json # required validation # ignores strings"
    );

    // Modelled but never checked: patternProperties, a null `const`.
    private static final Set<String> VALIDATOR_BLIND_SPOT = Set.of(
            "draft2019-09/additionalProperties.json # additionalProperties being false does not allow other properties "
                    + "# patternProperties are not additional properties",
            "draft2019-09/additionalProperties.json # non-ASCII pattern with additionalProperties # matching the pattern is valid",
            "draft2019-09/const.json # const with null # not null is invalid",
            "draft2019-09/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to both is invalid",
            "draft2019-09/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to one is invalid",
            "draft2019-09/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to the other is invalid",
            "draft2019-09/patternProperties.json # patternProperties validates properties matching a regex # a single invalid match is invalid",
            "draft2019-09/patternProperties.json # patternProperties validates properties matching a regex # multiple invalid matches is invalid",
            "draft2019-09/patternProperties.json # patternProperties with boolean schemas # object with a property matching both true and false is invalid",
            "draft2019-09/patternProperties.json # patternProperties with boolean schemas # object with both properties is invalid",
            "draft2019-09/patternProperties.json # patternProperties with boolean schemas # object with property matching schema false is invalid",
            "draft2019-09/patternProperties.json # regexes are not anchored by default and are case sensitive # recognized members are accounted for",
            "draft2019-09/patternProperties.json # regexes are not anchored by default and are case sensitive # regexes are case sensitive, 2",
            "draft2019-09/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty invalidates property",
            "draft2019-09/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty validates nonproperty",
            "draft2020-12/additionalProperties.json # additionalProperties being false does not allow other properties "
                    + "# patternProperties are not additional properties",
            "draft2020-12/additionalProperties.json # non-ASCII pattern with additionalProperties # matching the pattern is valid",
            "draft2020-12/const.json # const with null # not null is invalid",
            "draft2020-12/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to both is invalid",
            "draft2020-12/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to one is invalid",
            "draft2020-12/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to the other is invalid",
            "draft2020-12/patternProperties.json # patternProperties validates properties matching a regex # a single invalid match is invalid",
            "draft2020-12/patternProperties.json # patternProperties validates properties matching a regex # multiple invalid matches is invalid",
            "draft2020-12/patternProperties.json # patternProperties with boolean schemas # object with a property matching both true and false is invalid",
            "draft2020-12/patternProperties.json # patternProperties with boolean schemas # object with both properties is invalid",
            "draft2020-12/patternProperties.json # patternProperties with boolean schemas # object with property matching schema false is invalid",
            "draft2020-12/patternProperties.json # regexes are not anchored by default and are case sensitive # recognized members are accounted for",
            "draft2020-12/patternProperties.json # regexes are not anchored by default and are case sensitive # regexes are case sensitive, 2",
            "draft2020-12/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty invalidates property",
            "draft2020-12/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty validates nonproperty",
            "draft4/additionalProperties.json # additionalProperties being false does not allow other properties "
                    + "# patternProperties are not additional properties",
            "draft4/additionalProperties.json # non-ASCII pattern with additionalProperties # matching the pattern is valid",
            "draft4/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to both is invalid",
            "draft4/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to one is invalid",
            "draft4/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to the other is invalid",
            "draft4/patternProperties.json # patternProperties validates properties matching a regex # a single invalid match is invalid",
            "draft4/patternProperties.json # patternProperties validates properties matching a regex # multiple invalid matches is invalid",
            "draft4/patternProperties.json # regexes are not anchored by default and are case sensitive # recognized members are accounted for",
            "draft4/patternProperties.json # regexes are not anchored by default and are case sensitive # regexes are case sensitive, 2",
            "draft4/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty invalidates property",
            "draft4/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty validates nonproperty",
            "draft6/additionalProperties.json # additionalProperties being false does not allow other properties "
                    + "# patternProperties are not additional properties",
            "draft6/additionalProperties.json # non-ASCII pattern with additionalProperties # matching the pattern is valid",
            "draft6/const.json # const with null # not null is invalid",
            "draft6/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to both is invalid",
            "draft6/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to one is invalid",
            "draft6/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to the other is invalid",
            "draft6/patternProperties.json # patternProperties validates properties matching a regex # a single invalid match is invalid",
            "draft6/patternProperties.json # patternProperties validates properties matching a regex # multiple invalid matches is invalid",
            "draft6/patternProperties.json # patternProperties with boolean schemas # object with a property matching both true and false is invalid",
            "draft6/patternProperties.json # patternProperties with boolean schemas # object with both properties is invalid",
            "draft6/patternProperties.json # patternProperties with boolean schemas # object with property matching schema false is invalid",
            "draft6/patternProperties.json # regexes are not anchored by default and are case sensitive # recognized members are accounted for",
            "draft6/patternProperties.json # regexes are not anchored by default and are case sensitive # regexes are case sensitive, 2",
            "draft6/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty invalidates property",
            "draft6/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty validates nonproperty",
            "draft7/additionalProperties.json # additionalProperties being false does not allow other properties "
                    + "# patternProperties are not additional properties",
            "draft7/additionalProperties.json # non-ASCII pattern with additionalProperties # matching the pattern is valid",
            "draft7/const.json # const with null # not null is invalid",
            "draft7/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to both is invalid",
            "draft7/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to one is invalid",
            "draft7/patternProperties.json # multiple simultaneous patternProperties are validated # an invalid due to the other is invalid",
            "draft7/patternProperties.json # patternProperties validates properties matching a regex # a single invalid match is invalid",
            "draft7/patternProperties.json # patternProperties validates properties matching a regex # multiple invalid matches is invalid",
            "draft7/patternProperties.json # patternProperties with boolean schemas # object with a property matching both true and false is invalid",
            "draft7/patternProperties.json # patternProperties with boolean schemas # object with both properties is invalid",
            "draft7/patternProperties.json # patternProperties with boolean schemas # object with property matching schema false is invalid",
            "draft7/patternProperties.json # regexes are not anchored by default and are case sensitive # recognized members are accounted for",
            "draft7/patternProperties.json # regexes are not anchored by default and are case sensitive # regexes are case sensitive, 2",
            "draft7/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty invalidates property",
            "draft7/properties.json # properties, patternProperties, additionalProperties interaction # patternProperty validates nonproperty"
    );

    // additionalItems applied where the spec says it does nothing.
    private static final Set<String> ADDITIONAL_ITEMS_OVER_APPLIED = Set.of(
            "draft2019-09/additionalItems.json # additionalItems as false without items # items defaults to empty schema so everything is valid",
            "draft2019-09/additionalItems.json # when items is schema, additionalItems does nothing # valid with a array of type integers",
            "draft2019-09/additionalItems.json # when items is schema, boolean additionalItems does nothing # all items match schema",
            "draft4/additionalItems.json # additionalItems as false without items # items defaults to empty schema so everything is valid",
            "draft4/additionalItems.json # when items is schema, additionalItems does nothing # all items match schema",
            "draft6/additionalItems.json # additionalItems as false without items # items defaults to empty schema so everything is valid",
            "draft6/additionalItems.json # when items is schema, additionalItems does nothing # valid with a array of type integers",
            "draft6/additionalItems.json # when items is schema, boolean additionalItems does nothing # all items match schema",
            "draft7/additionalItems.json # additionalItems as false without items # items defaults to empty schema so everything is valid",
            "draft7/additionalItems.json # when items is schema, additionalItems does nothing # valid with a array of type integers",
            "draft7/additionalItems.json # when items is schema, boolean additionalItems does nothing # all items match schema"
    );

    // Numbers compare numerically only at the top level; inside an array or object
    // equality falls through to Java equals, so [1.0] does not match [1].
    private static final Set<String> NUMBER_EQUALITY_INSIDE_COMPOSITES = Set.of(
            "draft2019-09/enum.json # enum with [0] does not match [false] # [0.0] is valid",
            "draft2019-09/enum.json # enum with [1] does not match [true] # [1.0] is valid",
            "draft2020-12/enum.json # enum with [0] does not match [false] # [0.0] is valid",
            "draft2020-12/enum.json # enum with [1] does not match [true] # [1.0] is valid",
            "draft4/enum.json # enum with [0] does not match [false] # [0.0] is valid",
            "draft4/enum.json # enum with [1] does not match [true] # [1.0] is valid",
            "draft6/enum.json # enum with [0] does not match [false] # [0.0] is valid",
            "draft6/enum.json # enum with [1] does not match [true] # [1.0] is valid",
            "draft7/enum.json # enum with [0] does not match [false] # [0.0] is valid",
            "draft7/enum.json # enum with [1] does not match [true] # [1.0] is valid"
    );

    // minLength/maxLength count Java chars, so an astral character counts twice.
    private static final Set<String> LENGTH_COUNTED_IN_UTF16_UNITS = Set.of(
            "draft2019-09/maxLength.json # maxLength validation # two graphemes is long enough",
            "draft2019-09/minLength.json # minLength validation # one grapheme is not long enough",
            "draft2020-12/maxLength.json # maxLength validation # two graphemes is long enough",
            "draft2020-12/minLength.json # minLength validation # one grapheme is not long enough",
            "draft4/maxLength.json # maxLength validation # two graphemes is long enough",
            "draft4/minLength.json # minLength validation # one grapheme is not long enough",
            "draft6/maxLength.json # maxLength validation # two graphemes is long enough",
            "draft6/minLength.json # minLength validation # one grapheme is not long enough",
            "draft7/maxLength.json # maxLength validation # two graphemes is long enough",
            "draft7/minLength.json # minLength validation # one grapheme is not long enough"
    );

    // A subschema spelled `false` is dropped instead of rejecting everything.
    private static final Set<String> BOOLEAN_SUBSCHEMA_IGNORED = Set.of(
            "draft6/dependencies.json # dependencies with boolean subschemas # object with both properties is invalid",
            "draft6/dependencies.json # dependencies with boolean subschemas # object with property having schema false is invalid",
            "draft7/dependencies.json # dependencies with boolean subschemas # object with both properties is invalid",
            "draft7/dependencies.json # dependencies with boolean subschemas # object with property having schema false is invalid"
    );

    // \p{Letter} is ECMA-only; java.util.regex throws instead of answering.
    private static final Set<String> REGEX_UNSUPPORTED_BY_JAVA = Set.of(
            "draft2020-12/pattern.json # pattern with Unicode property escape requires unicode mode # ASCII letters match",
            "draft2020-12/pattern.json # pattern with Unicode property escape requires unicode mode # Digits do not match",
            "draft2020-12/pattern.json # pattern with Unicode property escape requires unicode mode # Non-ASCII letters match"
    );

    // Kept apart from the validation sets: different code, different facts.

    // The parser refuses the schema, so no generator is built.
    private static final Set<String> GENERATION_SCHEMA_REJECTED = Set.of(
            "draft2019-09/boolean_schema.json # boolean schema 'true'",
            "draft2019-09/items.json # items with boolean schemas",
            "draft2020-12/boolean_schema.json # boolean schema 'true'",
            "draft6/boolean_schema.json # boolean schema 'true'",
            "draft6/items.json # items with boolean schemas",
            "draft7/boolean_schema.json # boolean schema 'true'",
            "draft7/items.json # items with boolean schemas"
    );

    // The generator does not know the keyword.
    private static final Set<String> GENERATION_KEYWORD_NOT_RECOGNIZED = Set.of(
            "draft2019-09/maxContains.json # maxContains = 0 with minContains = 0",
            "draft2019-09/minContains.json # maxContains = minContains",
            "draft2019-09/minContains.json # minContains=2 with contains",
            "draft2019-09/minContains.json # minContains=2 with contains with a decimal value",
            "draft2020-12/maxContains.json # maxContains = 0 with minContains = 0",
            "draft2020-12/minContains.json # maxContains = minContains",
            "draft2020-12/minContains.json # minContains=2 with contains",
            "draft2020-12/minContains.json # minContains=2 with contains with a decimal value"
    );

    // Reported unsatisfiable, though the suite asserts a valid instance against it.
    private static final Set<String> GENERATION_GIVES_UP = Set.of(
            "draft2019-09/contains.json # contains keyword with boolean schema false",
            "draft2019-09/not.json # collect annotations inside a 'not', even if collection is disabled",
            "draft2019-09/not.json # forbidden property",
            "draft2020-12/contains.json # contains keyword with boolean schema false",
            "draft2020-12/not.json # collect annotations inside a 'not', even if collection is disabled",
            "draft2020-12/not.json # forbidden property",
            "draft4/not.json # forbidden property",
            "draft6/contains.json # contains keyword with boolean schema false",
            "draft6/not.json # forbidden property",
            "draft7/contains.json # contains keyword with boolean schema false",
            "draft7/not.json # forbidden property"
    );

    // The generator emits a value the schema rejects.
    private static final Set<String> GENERATION_PRODUCES_INVALID_VALUE = Set.of(
            "draft2019-09/additionalItems.json # when items is schema, additionalItems does nothing",
            "draft2019-09/const.json # const with null",
            "draft2019-09/contains.json # items + contains",
            "draft2019-09/dependentSchemas.json # dependent subschema incompatible with root",
            "draft2019-09/multipleOf.json # by number",
            "draft2020-12/const.json # const with null",
            "draft2020-12/contains.json # items + contains",
            "draft2020-12/dependentSchemas.json # dependent subschema incompatible with root",
            "draft2020-12/multipleOf.json # by number",
            "draft4/dependencies.json # dependent subschema incompatible with root",
            "draft4/multipleOf.json # by number",
            "draft6/additionalItems.json # when items is schema, additionalItems does nothing",
            "draft6/const.json # const with null",
            "draft6/contains.json # items + contains",
            "draft6/dependencies.json # dependent subschema incompatible with root",
            "draft6/multipleOf.json # by number",
            "draft7/additionalItems.json # when items is schema, additionalItems does nothing",
            "draft7/const.json # const with null",
            "draft7/contains.json # items + contains",
            "draft7/dependencies.json # dependent subschema incompatible with root",
            "draft7/multipleOf.json # by number"
    );

    // networknt cannot compile \p{Letter}. Not a gjuton defect.
    private static final Set<String> FAILS_IN_VALIDATION_LIBRARY = Set.of(
            "draft2020-12/patternProperties.json # patternProperties with Unicode property escape"
    );

    private static final List<Set<String>> VALIDATION_CAUSES = List.of(
            PARSER_REJECTS_SCHEMA, KEYWORD_NOT_RECOGNIZED, INFERRED_TYPE_REJECTS_VALID, VALIDATOR_BLIND_SPOT,
            ADDITIONAL_ITEMS_OVER_APPLIED, NUMBER_EQUALITY_INSIDE_COMPOSITES, LENGTH_COUNTED_IN_UTF16_UNITS,
            BOOLEAN_SUBSCHEMA_IGNORED, REGEX_UNSUPPORTED_BY_JAVA);

    private static final List<Set<String>> GENERATION_CAUSES = List.of(
            GENERATION_SCHEMA_REJECTED, GENERATION_KEYWORD_NOT_RECOGNIZED, GENERATION_GIVES_UP,
            GENERATION_PRODUCES_INVALID_VALUE, FAILS_IN_VALIDATION_LIBRARY);

    private static final Set<String> IGNORED_FOR_VALIDATION = union(VALIDATION_CAUSES);

    private static final Set<String> IGNORED_FOR_GENERATION = union(GENERATION_CAUSES);

    private static final JsonConverter CONVERTER = GjutonExtensions.locator().find(JsonConverter.class).orElseThrow();

    private static final SchemaParser PARSER = new SchemaParser(CONVERTER);

    private static final Map<SpecificationVersion, SchemaRegistry> REGISTRIES = new EnumMap<>(SpecificationVersion.class);

    static {
        for (var version : SpecificationVersion.values()) {
            REGISTRIES.put(version, SchemaRegistry.withDefaultDialect(version));
        }
    }

    /**
     * One instance from the suite, the schema it is asserted against, and the
     * verdict the suite expects.
     *
     * <p>The id reads {@code draft7/required.json # required validation # non-present
     * required property is invalid} and is unique across the suite.
     */
    record SuiteCase(String groupId, String id, SpecificationVersion dialect, String schemaJson, Object data, boolean valid) {

        @Override
        public String toString() {
            return id;
        }
    }

    /**
     * A suite schema at least one case asserts an instance valid against, which
     * makes it satisfiable and therefore a fair generation target.
     */
    record SuiteGroup(String id, SpecificationVersion dialect, String schemaJson) {

        @Override
        public String toString() {
            return id;
        }
    }

    private static final Path ROOT = suiteRoot();

    /**
     * Every case of every vendored suite file that can be run offline, in file
     * order.
     */
    private static final List<SuiteCase> SUITE = readSuite();

    private static final List<SuiteGroup> SATISFIABLE_GROUPS = readSatisfiableGroups();

    private static Path suiteRoot() {
        try {
            var root = JsonSchemaSuiteTest.class.getClassLoader().getResource(SUITE_ROOT);
            return Paths.get(root.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<SuiteCase> readSuite() {
        var cases = new ArrayList<SuiteCase>();
        try (var files = Files.walk(ROOT)) {
            var paths = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
            for (var path : paths) {
                var file = ROOT.relativize(path).toString();
                if (!NEEDS_NETWORK.contains(file)) {
                    readFile(file, path, cases);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return cases;
    }

    /**
     * Appends every case the given suite file declares, in file order.
     */
    @SuppressWarnings("unchecked")
    private static void readFile(String file, Path path, List<SuiteCase> cases) {
        var dialect = dialectOf(file);
        List<Object> groups;
        try {
            var json = Files.readString(path);
            groups = (List<Object>) CONVERTER.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (var group : groups) {
            var groupNode = (Map<String, Object>) group;
            var groupId = file + " # " + groupNode.get("description");
            var schemaJson = CONVERTER.write(groupNode.get("schema"));
            for (var test : (List<Object>) groupNode.get("tests")) {
                var testNode = (Map<String, Object>) test;
                var id = groupId + " # " + testNode.get("description");
                cases.add(new SuiteCase(groupId, id, dialect, schemaJson, testNode.get("data"), (Boolean) testNode.get("valid")));
            }
        }
    }

    /**
     * The draft a suite file is written against, which its directory names — the
     * schemas themselves mostly carry no {@code $schema}.
     */
    private static SpecificationVersion dialectOf(String file) {
        var draft = file.substring(0, file.indexOf('/'));
        return switch (draft) {
            case "draft4" -> SpecificationVersion.DRAFT_4;
            case "draft6" -> SpecificationVersion.DRAFT_6;
            case "draft7" -> SpecificationVersion.DRAFT_7;
            case "draft2019-09" -> SpecificationVersion.DRAFT_2019_09;
            case "draft2020-12" -> SpecificationVersion.DRAFT_2020_12;
            default -> throw new IllegalStateException("no dialect for " + draft);
        };
    }

    /**
     * The cases whose verdict is asserted: everything the ignore lists leave.
     */
    static Stream<SuiteCase> cases() {
        return SUITE.stream().filter(c -> !IGNORED_FOR_VALIDATION.contains(c.id()));
    }

    /**
     * The satisfiable groups that are generated from: everything the ignore lists
     * leave, in file order.
     */
    static Stream<SuiteGroup> groups() {
        return SATISFIABLE_GROUPS.stream().filter(g -> !IGNORED_FOR_GENERATION.contains(g.id()));
    }

    static Stream<String> ignoredForValidation() {
        return IGNORED_FOR_VALIDATION.stream().sorted();
    }

    static Stream<String> ignoredForGeneration() {
        return IGNORED_FOR_GENERATION.stream().sorted();
    }

    @ParameterizedTest
    @MethodSource("cases")
    void validatesAsTheSuiteExpects(SuiteCase suiteCase) {
        var document = PARSER.parse(suiteCase.schemaJson());
        var context = GeneratorContext.testContext(document, new Random(SEED));
        var validator = new SchemaValidator(context);

        // when
        var violation = validator.violation(suiteCase.data(), document.getRoot());

        // then
        var satisfied = violation == null;
        assertThat(satisfied).as("%s, violation: %s", suiteCase, violation).isEqualTo(suiteCase.valid());
    }

    @ParameterizedTest
    @MethodSource("groups")
    void generatesValidJson(SuiteGroup group) {
        var reference = referenceValidator(group);
        for (var mode : GenerationMode.values()) {
            var gjuton = Gjuton.of(group.schemaJson()).withSeed(SEED).withGenerationMode(mode);
            for (int i = 1; i <= VALUES_PER_GROUP; i++) {
                // when
                var json = gjuton.generate();

                // then
                var errors = reference.validate(json, InputFormat.JSON);
                assertThat(errors).as("%s %s invocation %d generated %s", group, mode, i, json).isEmpty();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("ignoredForValidation")
    void ignoredCaseStillFails(String id) {
        // when
        var ignored = SUITE.stream().filter(c -> c.id().equals(id)).findFirst();
        var mismatch = ignored.map(JsonSchemaSuiteTest::validationStillFails);

        // then
        assertThat(ignored).as("%s is on an ignore list but no longer exists in the suite; remove it", id).isPresent();
        assertThat(mismatch).as("%s is on an ignore list but now passes; remove it", id).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("ignoredForGeneration")
    void ignoredGroupStillFails(String id) {
        // when
        var ignored = SATISFIABLE_GROUPS.stream().filter(g -> g.id().equals(id)).findFirst();
        var failure = ignored.map(JsonSchemaSuiteTest::generationStillFails);

        // then
        assertThat(ignored).as("%s is on the generation ignore list but no longer exists in the suite; remove it", id).isPresent();
        assertThat(failure).as("%s is on the generation ignore list but now passes; remove it", id).isNotEmpty();
    }

    @Test
    void everyIgnoredEntryHasOneCause() {
        // when
        int validationEntries = VALIDATION_CAUSES.stream().mapToInt(Set::size).sum();
        int generationEntries = GENERATION_CAUSES.stream().mapToInt(Set::size).sum();

        // then
        assertThat(IGNORED_FOR_VALIDATION).as("a case id is listed under more than one cause").hasSize(validationEntries);
        assertThat(IGNORED_FOR_GENERATION).as("a group id is listed under more than one cause").hasSize(generationEntries);
    }

    /**
     * Describes how gjuton's verdict on a case differs from the suite's, or
     * {@code null} when they agree. A schema the parser refuses is a mismatch of
     * its own, distinct from a wrong verdict.
     */
    private static String validationStillFails(SuiteCase suiteCase) {
        SchemaDocument document;
        try {
            document = PARSER.parse(suiteCase.schemaJson());
        } catch (RuntimeException e) {
            return "parse threw " + e;
        }
        try {
            var context = GeneratorContext.testContext(document, new Random(SEED));
            var validator = new SchemaValidator(context);
            var violation = validator.violation(suiteCase.data(), document.getRoot());
            if ((violation == null) == suiteCase.valid()) {
                return null;
            }
            if (violation == null) {
                return "expected invalid, but no constraint was violated by " + suiteCase.data();
            }
            return "expected valid, but " + violation;
        } catch (RuntimeException e) {
            return "validator threw " + e;
        }
    }

    /**
     * The reference implementation's view of a group's schema, against which
     * generated values are judged.
     *
     * @throws RuntimeException if the reference implementation rejects the schema
     */
    private static Schema referenceValidator(SuiteGroup group) {
        // The dialect comes from the directory the file sits in: suite schemas
        // mostly carry no $schema of their own.
        var registry = REGISTRIES.get(group.dialect());
        return registry.getSchema(group.schemaJson());
    }

    /**
     * Describes why generating from a group's schema does not produce a
     * schema-valid value, or {@code null} when every generated value validates.
     * Both {@link GenerationMode}s are exercised.
     */
    private static String generationStillFails(SuiteGroup group) {
        // Compiled apart from generation so a third-party failure never reads as gjuton's.
        Schema reference;
        try {
            reference = referenceValidator(group);
        } catch (RuntimeException e) {
            return "reference validator threw " + e;
        }
        for (var mode : GenerationMode.values()) {
            try {
                var gjuton = Gjuton.of(group.schemaJson()).withSeed(SEED).withGenerationMode(mode);
                for (int i = 1; i <= VALUES_PER_GROUP; i++) {
                    var json = gjuton.generate();
                    var errors = reference.validate(json, InputFormat.JSON);
                    if (!errors.isEmpty()) {
                        return mode + " invocation " + i + " generated " + json + " violating " + errors;
                    }
                }
            } catch (RuntimeException e) {
                return mode + " threw " + e;
            }
        }
        return null;
    }

    /**
     * The groups of the vendored suite that at least one case asserts an instance
     * valid against.
     */
    private static List<SuiteGroup> readSatisfiableGroups() {
        var groups = new LinkedHashMap<String, SuiteGroup>();
        for (var suiteCase : SUITE) {
            if (suiteCase.valid()) {
                var group = new SuiteGroup(suiteCase.groupId(), suiteCase.dialect(), suiteCase.schemaJson());
                groups.putIfAbsent(group.id(), group);
            }
        }
        return List.copyOf(groups.values());
    }

    private static Set<String> union(List<Set<String>> sets) {
        return sets.stream().flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    }
}
