package step.artefacts.handlers.functions;

import step.grid.tokenpool.Interest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This implementation of the {@link TokenForecastingContext} tries to calculate the
 * required number of tokens for Artefacts that run their children in parallel with a defined number of threads.
 * For this estimation it calculates the maximum number of token required for each available pool and for each child artefact.
 * For each pool it then takes the sum of the required number of tokens for the n first children where n is the number of threads
 */
public class MaxAndMultiplyingTokenForecastingContext extends TokenForecastingContext {

    private final int numberOfThreads;

    private final List<TokenForecastingContext> iterations = new ArrayList<>();
    private TokenForecastingContext currentIteration;

    public MaxAndMultiplyingTokenForecastingContext(TokenForecastingContext parentContext, int numberOfThreads) {
        super(parentContext);
        this.numberOfThreads = numberOfThreads;
        currentIteration = new TokenForecastingContext(parentContext);
    }

    public void nextIteration() {
        iterations.add(currentIteration);
        currentIteration = new TokenForecastingContext(parentContext);
    }

    @Override
    public String requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        return currentIteration.requireToken(criteria, count);
    }

    @Override
    public void releaseRequiredToken(String pool, int count) {
        currentIteration.releaseRequiredToken(pool, count);
    }

    public void end() {
        pools.keySet().forEach(poolName -> {
            Integer sum = iterations.stream().map(i -> i.getTokenForecastPerPool().get(poolName)).filter(Objects::nonNull).sorted().limit(numberOfThreads).reduce(0, Integer::sum);
            if(sum > 0) {
                parentContext.requireToken(poolName, sum);
                parentContext.releaseRequiredToken(poolName, sum);
            }
        });
    }
}
