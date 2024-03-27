package step.artefacts.handlers.functions;

import step.grid.tokenpool.Interest;

import java.util.Map;

public class MultiplyingTokenForecastingContext extends TokenForecastingContext {

    private final int numberOfThreads;

    public MultiplyingTokenForecastingContext(TokenForecastingContext parentContext, int numberOfThreads) {
        super(parentContext);
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public String requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        return parentContext.requireToken(criteria, count * numberOfThreads);
    }

    @Override
    public void releaseRequiredToken(String pool, int count) {
        parentContext.releaseRequiredToken(pool, count * numberOfThreads);
    }
}
