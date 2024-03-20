package step.artefacts.handlers.functions;

import step.grid.tokenpool.Interest;

import java.util.Map;

public class MultiplyingTokenNumberCalculationContext extends AbstractTokenNumberCalculationContext {

    private final int numberOfThreads;

    public MultiplyingTokenNumberCalculationContext(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public String requireToken(Map<String, Interest> criteria, int count) {
        return parentContext.requireToken(criteria, count * numberOfThreads);
    }

    @Override
    public void releaseRequiredToken(String pool, int count) {
        parentContext.releaseRequiredToken(pool, count * numberOfThreads);
    }
}
