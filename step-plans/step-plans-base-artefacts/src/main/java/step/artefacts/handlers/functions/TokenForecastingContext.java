package step.artefacts.handlers.functions;

import step.artefacts.handlers.functions.autoscaler.AgentPoolRequirementSpec;
import step.artefacts.handlers.functions.autoscaler.AgentPoolSpec;
import step.artefacts.handlers.functions.autoscaler.TemplateStsAgentPoolRequirementSpec;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.TokenLifecycleInterceptor;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.Token;
import step.grid.TokenPretender;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TokenForecastingContext {

    protected final Map<String, PoolReservationTracker> poolResourceReservations = new HashMap<>();
    protected final Set<AgentPoolSpec> availableAgentPools;
    protected final TokenForecastingContext parentContext;
    protected Set<Map<String, Interest>> criteriaWithoutMatch = new HashSet<>();

    public TokenForecastingContext(Set<AgentPoolSpec> availableAgentPools) {
        this.availableAgentPools = availableAgentPools;
        this.parentContext = null;
    }

    public TokenForecastingContext(TokenForecastingContext parentContext) {
        this.parentContext = parentContext;
        this.availableAgentPools = parentContext == null ? new HashSet<>() : parentContext.availableAgentPools;
    }

    protected String requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        AgentPoolSpec bestMatchingPool = getBestMatchingPool(criteria).orElseThrow(() -> new NoMatchingTokenPoolException());
        String poolName = bestMatchingPool.name;
        requireToken(poolName, count);
        return poolName;
    }

    protected void requireToken(String pool, int count) {
        poolResourceReservations.computeIfAbsent(pool, k -> new PoolReservationTracker()).reserve(count);
    }

    protected void releaseRequiredToken(String pool, int count) {
        poolResourceReservations.computeIfAbsent(pool, k -> new PoolReservationTracker()).release(count);
    }

    private Optional<AgentPoolSpec> getBestMatchingPool(Map<String, Interest> criteria) {
        SimpleAffinityEvaluator<Identity, Identity> affinityEvaluator = new SimpleAffinityEvaluator<>();
        return availableAgentPools.stream()
                .map(entry -> new Object[]{affinityEvaluator.getAffinityScore(new TokenPretender(Map.of(), criteria), new TokenPretender(entry.attributes, Map.of())), entry})
                .filter(o -> ((int) o[0]) >= 0).sorted(Comparator.comparingInt(o -> (int) o[0])).map(o -> (AgentPoolSpec) o[1])
                .findFirst();
    }

    /**
     * @return the forecasted number of tokens required per pool
     */
    public Map<String, Integer> getTokenForecastPerPool() {
        return poolResourceReservations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().maxReservationCount));
    }

    public Set<AgentPoolRequirementSpec> getAgentPoolRequirementSpec() {
        return getTokenForecastPerPool().entrySet().stream().map(s -> {
            AgentPoolSpec agentPoolSpec = availableAgentPools.stream().filter(p -> p.name.equals(s.getKey())).findFirst().orElseThrow();
            int requiredReplicas = calculateRequiredReplicas(s.getValue(), agentPoolSpec.numberOfTokens);
            return new TemplateStsAgentPoolRequirementSpec(s.getKey(), requiredReplicas);
        } ).collect(Collectors.toSet());
    }

    private int calculateRequiredReplicas(int requiredTokens, int tokenGroupCapacity) {
        int desiredReplicas = requiredTokens / tokenGroupCapacity;
        return requiredTokens % tokenGroupCapacity > 0 ? desiredReplicas + 1 : desiredReplicas;
    }

    public Set<Map<String, Interest>> getCriteriaWithoutMatch() {
        return criteriaWithoutMatch;
    }

    public FunctionExecutionService getFunctionExecutionServiceForTokenForecasting() {
        return new FunctionExecutionService() {

            @Override
            public void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {
            }

            @Override
            public void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {
            }

            @Override
            public TokenWrapper getLocalTokenHandle() {
                return newTokenWrapper(true, null);
            }

            private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

            @Override
            public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
                String pool;
                TokenWrapper tokenWrapper;
                try {
                    pool = TokenForecastingContext.this.requireToken(interests, 1);
                    tokenWrapper = newTokenWrapper(false, pool);
                    // Keep track of the pool associated to this token. We need this information in the release() method
                    tokens.put(tokenWrapper.getID(), pool);
                } catch (NoMatchingTokenPoolException e) {
                    // No token pool matches the selection criteria. Keep track of these criteria
                    reportFailedSelection(interests);
                    tokenWrapper = newTokenWrapper(false, null);
                }
                return tokenWrapper;
            }

            private TokenWrapper newTokenWrapper(boolean isLocal, String pool) {
                TokenWrapper tokenWrapper = new TokenWrapper();
                Token token = new Token();
                token.setAgentid(isLocal ? "local" : "remote");
                token.setAttributes(pool != null ? availableAgentPools.stream().filter(p -> p.name.equals(pool)).findFirst().orElseThrow().attributes : Map.of());
                token.setId(UUID.randomUUID().toString());
                tokenWrapper.setToken(token);
                return tokenWrapper;
            }

            @Override
            public void returnTokenHandle(String tokenHandleId) {
                String pool = tokens.remove(tokenHandleId);
                if (pool != null) {
                    TokenForecastingContext.this.releaseRequiredToken(pool, 1);
                }
            }

            @Override
            public <IN, OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass) {
                throw new IllegalStateException("This method shouldn't be called");
            }
        };
    }

    private void reportFailedSelection(Map<String, Interest> interests) {
        if(parentContext != null) {
            parentContext.reportFailedSelection(interests);
        } else {
            criteriaWithoutMatch.add(interests);
        }
    }

    private static class PoolReservationTracker {

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
