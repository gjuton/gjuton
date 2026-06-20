package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.skip;
import static se.plilja.jsonschemagen.internal.util.FunctionalUtil.coalesce;

import java.util.ArrayList;
import java.util.List;
import se.plilja.jsonschemagen.internal.model.ArraySchema;
import se.plilja.jsonschemagen.internal.model.NullSchema;
import se.plilja.jsonschemagen.internal.model.Schema;

/**
 * Generator for {@code "type": "array"} schemas. Varies array length
 * across successive calls to cover the allowed range.
 */
final class ArrayGenerator extends PhaseGenerator<ArrayGenerator.GenerationPhase, List<Object>> {

    private static final int DEFAULT_LENGTH_BUFFER = 5;

    private final ArraySchema schema;
    private final Schema itemSchema;

    enum GenerationPhase {
        MIN_LENGTH, MAX_LENGTH, RANDOM
    }

    ArrayGenerator(GeneratorContext context, ArraySchema schema) {
        super(GenerationPhase.class, context);
        this.schema = schema;
        // TODO: when items is absent, JSON Schema allows any element type. Emitting nulls is valid
        // but boring; a varied-type generator (cycling string/int/bool/...) would surface more bugs.
        this.itemSchema = coalesce(schema.getItems(), new NullSchema());
    }

    @Override
    protected GenerationPhase minimalPhase() {
        return GenerationPhase.MIN_LENGTH;
    }

    @Override
    protected GenerationResult<List<Object>> generatePhase(GenerationPhase phase) {
        return switch (phase) {
            case MIN_LENGTH -> result(buildList(coalesce(schema.getMinItems(), 0)));
            case MAX_LENGTH -> schema.getMaxItems() != null ? result(buildList(schema.getMaxItems())) : skip();
            case RANDOM -> {
                var min = coalesce(schema.getMinItems(), 0);
                var max = coalesce(schema.getMaxItems(), min + DEFAULT_LENGTH_BUFFER);
                var length = min + context.random().nextInt(max - min + 1);
                yield result(buildList(length));
            }
        };
    }

    private List<Object> buildList(int length) {
        // cr I don't like this. The method says to build a list of a particaular length, then we should not build of another length, we should push the minimum length to the call site
        int effectiveLength = schema.getContains() != null ? Math.max(length, 1) : length;
        var list = new ArrayList<>();
        for (var i = 0; i < effectiveLength; i++) {
            list.add(context.generatorFor(itemSchema).generate());
        }
        satisfyContains(list); // CR i dont like overwriting here, for example lets say the generator of the for loop were generating a particular value of an enum cycle. Here we would overwrite that. This means that not all enum values would have been emitted. Better to decide which index before the for loop should have the contains. And then put an if-statemtn in thhe for loop
        return list;
    }

    private void satisfyContains(ArrayList<Object> list) {

        // CR this method should be inlined
        if (schema.getContains() == null) {
            return;
        }
        var containsValue = context.generatorFor(schema.getContains()).generate();
        int index = context.random().nextInt(list.size());
        list.set(index, containsValue);
    }
}
