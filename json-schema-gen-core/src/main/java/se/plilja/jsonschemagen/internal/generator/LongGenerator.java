package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.FunctionalUtil.coalesce;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.skip;

import java.util.Random;
import se.plilja.jsonschemagen.internal.model.IntegerSchema;

final class LongGenerator extends PhaseGenerator<LongGenerator.GenerationPhase, Long> {

    private final Random random;
    private final IntegerSchema schema;

    enum GenerationPhase {
        MIN,
        MAX,
        ZERO,
        NEAR_MIN,
        NEAR_MAX,
        RANDOM
    }

    LongGenerator(Random random, IntegerSchema schema) {
        super(GenerationPhase.class);
        this.random = random;
        this.schema = schema;
    }

    @Override
    protected GenerationResult<Long> generatePhase(GenerationPhase phase) {
        return switch (phase) {
            case MIN -> schema.getMinimum() != null ? result(schema.getMinimum()) : skip();
            case MAX -> schema.getMaximum() != null ? result(schema.getMaximum()) : skip();
            // TODO consider generating boundary values even when min/max are not declared
            case ZERO -> {
                if (!isInRange(0)) {
                    yield skip();
                }
                // Skip when 0 equals min or max since those phases already emit it
                if (Long.valueOf(0).equals(schema.getMinimum())
                        || Long.valueOf(0).equals(schema.getMaximum())) {
                    yield skip();
                }
                yield result(0L);
            }
            case NEAR_MIN -> {
                if (schema.getMinimum() == null) {
                    yield skip();
                }
                long nearMin = schema.getMinimum() + 1;
                yield isInRange(nearMin) ? result(nearMin) : skip();
            }
            case NEAR_MAX -> {
                if (schema.getMaximum() == null) {
                    yield skip();
                }
                long nearMax = schema.getMaximum() - 1;
                yield isInRange(nearMax) ? result(nearMax) : skip();
            }
            case RANDOM -> result(randomLong());
        };
    }

    private boolean isInRange(long value) {
        return value >= coalesce(schema.getMinimum(), Long.MIN_VALUE)
                && value <= coalesce(schema.getMaximum(), Long.MAX_VALUE - 1);
    }

    private long randomLong() {
        long min = coalesce(schema.getMinimum(), Long.MIN_VALUE);
        long max = coalesce(schema.getMaximum(), Long.MAX_VALUE - 1);
        return random.nextLong(min, max + 1);
    }
}
