package step.artefacts.handlers.functions;

import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.TokenLifecycleInterceptor;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.Token;
import step.grid.TokenPretender;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AbstractTokenNumberCalculationContext implements TokenNumberCalculationContext {

    // TODO read this from config
    protected final Map<String, Map<String, String>> pools = Map.of("pool1", Map.of("$agenttype", "default"));
    protected final Map<String, PoolResourceReservationManager> poolResourceReservations = new HashMap<>();
    protected TokenNumberCalculationContext parentContext;

    public void setParent(TokenNumberCalculationContext parentContext) {
        this.parentContext = parentContext;
    }

    public String requireToken(Map<String, Interest> criteria, int count) {
        String bestMatchingPool = getBestMatchingPool(criteria);
        if (bestMatchingPool != null) {
            requireToken(bestMatchingPool, count);
            return bestMatchingPool;
        } else {
            // TODO
            throw new RuntimeException("No matching pool for token...");
        }
    }

    public void requireToken(String pool, int count) {
        poolResourceReservations.computeIfAbsent(pool, k -> new PoolResourceReservationManager()).reserve(count);
    }

    public void releaseRequiredToken(String pool, int count) {
        poolResourceReservations.computeIfAbsent(pool, k -> new PoolResourceReservationManager()).release(count);
    }

    private String getBestMatchingPool(Map<String, Interest> criteria) {
        SimpleAffinityEvaluator affinityEvaluator = new SimpleAffinityEvaluator();
        return pools.entrySet().stream()
                .map(entry -> new Object[]{affinityEvaluator.getAffinityScore(new TokenPretender(Map.of(), criteria), new TokenPretender(entry.getValue(), Map.of())), entry.getKey()})
                .filter(o -> ((int) o[0]) >= 0).sorted(Comparator.comparingInt(o -> (int) o[0])).map(o -> (String) o[1])
                .findFirst().orElse(null);
    }

    public Map<String, Integer> getRequiredTokensPerPool() {
        return poolResourceReservations.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().maxReservationCount));
    }

    public FunctionExecutionService getFunctionExecutionServiceForTokenRequirementCalculation() {
        return new FunctionExecutionService() {

            @Override
            public void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

            }

            @Override
            public void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

            }

            @Override
            public TokenWrapper getLocalTokenHandle() {
                return newTokenWrapper(true);
            }

            private final ConcurrentHashMap<String, String> tokenRequirements = new ConcurrentHashMap<>();

            @Override
            public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
                String pool = AbstractTokenNumberCalculationContext.this.requireToken(interests, 1);
                TokenWrapper tokenWrapper = newTokenWrapper(false);
                tokenRequirements.put(tokenWrapper.getID(), pool);
                return tokenWrapper;
            }

            private TokenWrapper newTokenWrapper(boolean isLocal) {
                TokenWrapper tokenWrapper = new TokenWrapper();
                Token token = new Token();
                token.setAgentid(isLocal ? "local" : "remote");
                token.setAttributes(Map.of());
                token.setId(UUID.randomUUID().toString());
                tokenWrapper.setToken(token);
                return tokenWrapper;
            }

            @Override
            public void returnTokenHandle(String tokenHandleId) throws FunctionExecutionServiceException {
                String pool = tokenRequirements.remove(tokenHandleId);
                if (pool != null) {
                    AbstractTokenNumberCalculationContext.this.releaseRequiredToken(pool, 1);
                }
            }

            @Override
            public <IN, OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass) {
                throw new IllegalStateException("This method shouldn't be called");
            }
        };
    }

    private class PoolResourceReservationManager {

        private int currentReservationCount = 0;
        private int maxReservationCount = 0;

        public void reserve(int count) {
            currentReservationCount += count;
            if (currentReservationCount > maxReservationCount) {
                maxReservationCount = currentReservationCount;
            }
        }

        public void release(int count) {
            currentReservationCount -= count;
        }

    }
}
