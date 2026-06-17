package se.plilja.jsonschemagen.internal.generator.format;

import se.plilja.jsonschemagen.internal.generator.GenerationResult;
import se.plilja.jsonschemagen.internal.generator.GeneratorContext;
import se.plilja.jsonschemagen.internal.generator.PhaseGenerator;
import se.plilja.jsonschemagen.internal.model.StringSchema;

public final class IriGenerator extends PhaseGenerator<UriGenerator.UriPhase, String> {

    private final UriGenerator delegate;

    public IriGenerator(GeneratorContext context, StringSchema schema) {
        super(UriGenerator.UriPhase.class, context);
        this.delegate = new UriGenerator(context, schema, Alphabets.IDN_POOL);
    }

    @Override
    public String generate() {
        return delegate.generate();
    }

    @Override
    protected UriGenerator.UriPhase minimalPhase() {
        return delegate.minimalPhase();
    }

    @Override
    protected GenerationResult<String> generatePhase(UriGenerator.UriPhase phase) {
        return delegate.generatePhase(phase);
    }
}
