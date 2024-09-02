package step.artefacts.handlers.functions;

import step.grid.tokenpool.Interest;

import java.util.*;

/**
 * This implementation of the {@link TokenForecastingContext} tries to calculate the
 * required number of tokens for Artefacts that run their children in parallel with a defined number of threads.
 * For this estimation it calculates the maximum number of token required for each available pool and for each child artefact.
 * For each pool it then takes the sum of the required number of tokens for the n first children where n is the number of threads
 */
public class MaxAndMultiplyingTokenForecastingContext extends TokenForecastingContext {

    private final int numberOfThreads;

    private final List<TokenForecastingContext> iterations = new ArrayList<>();
    private final Set<Key> keys = new HashSet<>();
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
    public Key requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        Key key = currentIteration.requireToken(criteria, count);
        keys.add(key);
        return key;
    }

    @Override
    public void releaseRequiredToken(Key key, int count) {
        currentIteration.releaseRequiredToken(key, count);
    }

    public void end() {
        keys.forEach(key -> {
            Integer sum = iterations.stream().map(i -> i.getTokenForecastPerKey().get(key)).filter(Objects::nonNull).sorted(Comparator.reverseOrder()).limit(numberOfThreads).reduce(0, Integer::sum);
            if(sum > 0) {
                parentContext.requireToken(key, sum);
                parentContext.releaseRequiredToken(key, sum);
            }
        });
    }
}
