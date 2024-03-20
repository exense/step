package step.artefacts.handlers.functions;

import step.grid.tokenpool.Interest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MaxAndMultiplyingTokenNumberCalculationContext extends AbstractTokenNumberCalculationContext {

    private final int numberOfThreads;

    private final List<RootTokenNumberCalculationContext> iterations = new ArrayList<>();
    private RootTokenNumberCalculationContext currentIteration = new RootTokenNumberCalculationContext();

    public MaxAndMultiplyingTokenNumberCalculationContext(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public void nextIteration() {
        iterations.add(currentIteration);
        currentIteration = new RootTokenNumberCalculationContext();
    }

    @Override
    public String requireToken(Map<String, Interest> criteria, int count) {
        return currentIteration.requireToken(criteria, count);
    }

    @Override
    public void releaseRequiredToken(String pool, int count) {
        currentIteration.releaseRequiredToken(pool, count);
    }

    public void end() {
        pools.keySet().forEach(poolName -> {
            Integer sum = iterations.stream().map(i -> i.getRequiredTokensPerPool().get(poolName)).filter(Objects::nonNull).sorted().limit(numberOfThreads).reduce(0, Integer::sum);
            if(sum > 0) {
                parentContext.requireToken(poolName, sum);
                parentContext.releaseRequiredToken(poolName, sum);
            }
        });
    }
}
