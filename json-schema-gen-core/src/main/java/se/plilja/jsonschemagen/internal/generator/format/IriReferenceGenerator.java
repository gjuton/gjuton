package se.plilja.jsonschemagen.internal.generator.format;

import se.plilja.jsonschemagen.internal.generator.GenerationResult;
import se.plilja.jsonschemagen.internal.generator.GeneratorContext;
import se.plilja.jsonschemagen.internal.generator.PhaseGenerator;
import se.plilja.jsonschemagen.internal.model.StringSchema;

public final class IriReferenceGenerator extends PhaseGenerator<UriReferenceGenerator.UriReferencePhase, String> {

    private final UriReferenceGenerator delegate;

    public IriReferenceGenerator(GeneratorContext context, StringSchema schema) {
        super(UriReferenceGenerator.UriReferencePhase.class, context);
        this.delegate = new UriReferenceGenerator(context, schema, Alphabets.IDN_POOL);
    }

    @Override
    public String generate() {
        return delegate.generate();
    }

    @Override
    protected UriReferenceGenerator.UriReferencePhase minimalPhase() {
        return delegate.minimalPhase();
    }

    @Override
    protected GenerationResult<String> generatePhase(UriReferenceGenerator.UriReferencePhase phase) {
        return delegate.generatePhase(phase);
    }
}
