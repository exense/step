package step.artefacts.handlers.functions;

import step.artefacts.handlers.functions.autoscaler.AgentPoolRequirementSpec;
import step.artefacts.handlers.functions.autoscaler.AgentPoolSpec;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.TokenLifecycleInterceptor;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.Token;
import step.grid.TokenPretender;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static step.artefacts.handlers.functions.autoscaler.AgentPoolProvisioningParameters.supportedParameters;

public class TokenForecastingContext {
    protected final Map<Key, PoolReservationTracker> poolResourceReservations = new HashMap<>();
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

    protected Key requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        AgentPoolSpec bestMatchingPool = getBestMatchingPool(criteria).orElseThrow(NoMatchingTokenPoolException::new);
        String poolName = bestMatchingPool.name;

        // Delegate the creation of the provisioning parameters map to the registered parameter types
        HashMap<String, String> provisioningParameters = new HashMap<>();
        supportedParameters.forEach(p -> p.tokenSelectionCriteriaToAgentPoolProvisioningParameters.accept(criteria, provisioningParameters));

        Key key = new Key(poolName, provisioningParameters);
        requireToken(key, count);
        return key;
    }

    protected void requireToken(Key key, int count) {
        poolResourceReservations.computeIfAbsent(key, k -> new PoolReservationTracker()).reserve(count);
    }

    protected static class Key {
        String poolTemplateName;
        Map<String, String> provisioningParameters;

        public Key(String poolTemplateName, Map<String, String> provisioningParameters) {
            this.poolTemplateName = poolTemplateName;
            this.provisioningParameters = provisioningParameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(poolTemplateName, key.poolTemplateName) && Objects.equals(provisioningParameters, key.provisioningParameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(poolTemplateName, provisioningParameters);
        }
    }

    protected void releaseRequiredToken(Key key, int count) {
        poolResourceReservations.computeIfAbsent(key, k -> new PoolReservationTracker()).release(count);
    }

    private Optional<AgentPoolSpec> getBestMatchingPool(Map<String, Interest> criteria) {
        PreProvisioningTokenAffinityEvaluator affinityEvaluator = new PreProvisioningTokenAffinityEvaluator();
        // Find the agent pool that best matches the criteria among the available agent pools
        // Technically we search the pool that has with the highest affinity score as follows:
        // - for each available pool we calculate the affinity score with the criteria using the configured affinityEvaluator
        // - we sort the resulting stream by descending score
        // - from this list we take the first element
        return availableAgentPools.stream()
                .map(entry -> new Object[]{affinityEvaluator.getAffinityScore(new TokenPretender(Map.of(), criteria), new TokenPretender(entry.attributes, Map.of())), entry})
                .filter(o -> ((int) o[0]) >= 0).sorted(Comparator.comparingInt(o -> (int) o[0])).map(o -> (AgentPoolSpec) o[1])
                .findFirst();
    }

    /**
     * @return the forecasted number of tokens required per pool
     */
    protected Map<Key, Integer> getTokenForecastPerPool() {
        return poolResourceReservations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().maxReservationCount));
    }

    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpec() {
        return getTokenForecastPerPool().entrySet().stream().map(s -> {
            AgentPoolSpec agentPoolSpec = availableAgentPools.stream().filter(p -> p.name.equals(s.getKey().poolTemplateName)).findFirst().orElseThrow();
            int requiredReplicas = calculateRequiredReplicas(s.getValue(), agentPoolSpec.numberOfTokens);
            return new AgentPoolRequirementSpec(s.getKey().poolTemplateName, s.getKey().provisioningParameters, requiredReplicas);
        }).collect(Collectors.toList());
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

            private final ConcurrentHashMap<String, Key> tokens = new ConcurrentHashMap<>();

            @Override
            public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
                Key pool;
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

            private TokenWrapper newTokenWrapper(boolean isLocal, Key pool) {
                TokenWrapper tokenWrapper = new TokenWrapper();
                Token token = new Token();
                token.setAgentid(isLocal ? "local" : "remote");
                token.setAttributes(pool != null ? availableAgentPools.stream().filter(p -> p.name.equals(pool.poolTemplateName)).findFirst().orElseThrow().attributes : Map.of());
                token.setId(UUID.randomUUID().toString());
                tokenWrapper.setToken(token);
                return tokenWrapper;
            }

            @Override
            public void returnTokenHandle(String tokenHandleId) {
                Key key = tokens.remove(tokenHandleId);
                if (key != null) {
                    TokenForecastingContext.this.releaseRequiredToken(key, 1);
                }
            }

            @Override
            public <IN, OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass) {
                throw new IllegalStateException("This method shouldn't be called");
            }
        };
    }

    private void reportFailedSelection(Map<String, Interest> interests) {
        if (parentContext != null) {
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
