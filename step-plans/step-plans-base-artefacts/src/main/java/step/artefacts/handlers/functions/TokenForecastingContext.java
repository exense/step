package step.artefacts.handlers.functions;

import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
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

import static step.core.agents.provisioning.AgentPoolProvisioningParameters.supportedParameters;

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
        Set<AgentPoolSpec> bestMatchingPools = getBestMatchingPools(criteria);

        // Delegate the creation of the provisioning parameters map to the registered parameter types
        HashMap<String, String> provisioningParameters = new HashMap<>();
        supportedParameters.forEach(p -> p.tokenSelectionCriteriaToAgentPoolProvisioningParameters.accept(criteria, provisioningParameters));

        Key key = new Key(bestMatchingPools, provisioningParameters);
        requireToken(key, count);
        return key;
    }

    protected void requireToken(Key key, int count) {
        poolResourceReservations.computeIfAbsent(key, k -> new PoolReservationTracker()).reserve(count);
    }

    protected static class Key {
        Set<AgentPoolSpec> matchingPools;
        Map<String, String> provisioningParameters;

        public Key(Set<AgentPoolSpec> matchingPools, Map<String, String> provisioningParameters) {
            this.matchingPools = matchingPools;
            this.provisioningParameters = provisioningParameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(matchingPools, key.matchingPools) && Objects.equals(provisioningParameters, key.provisioningParameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(matchingPools, provisioningParameters);
        }
    }

    protected void releaseRequiredToken(Key key, int count) {
        poolResourceReservations.computeIfAbsent(key, k -> new PoolReservationTracker()).release(count);
    }

    private static final class AgentPoolSpecAndScore {
        final AgentPoolSpec agentPoolSpec;
        final int score;

        private AgentPoolSpecAndScore(AgentPoolSpec agentPoolSpec, int score) {
            this.agentPoolSpec = agentPoolSpec;
            this.score = score;
        }
    }
    
    private Set<AgentPoolSpec> getBestMatchingPools(Map<String, Interest> criteria) throws NoMatchingTokenPoolException {
        PreProvisioningTokenAffinityEvaluator affinityEvaluator = new PreProvisioningTokenAffinityEvaluator();
        // Find all the agent pools that match the criteria among the available agent pools
        // - for each available pool we calculate the affinity score with the criteria using the configured affinityEvaluator
        // - we filter out the agent pools that have a score lower than 1
        // - we order the result by decreasing score
        List<AgentPoolSpecAndScore> matchingAgentPools = availableAgentPools.stream()
                .map(entry -> new AgentPoolSpecAndScore(entry, affinityEvaluator.getAffinityScore(new TokenPretender(Map.of(), criteria), new TokenPretender(entry.attributes, Map.of()))))
                .filter(o -> o.score > 0).sorted(Comparator.comparingInt(o -> o.score)).collect(Collectors.toList());

        int size = matchingAgentPools.size();
        if(size == 0) {
            // No matching agent pool could be found
            throw new NoMatchingTokenPoolException();
        } else if (size == 1) {
            // Exactly one agent pool could be found, return it directly
            return Set.of(matchingAgentPools.get(0).agentPoolSpec);
        } else {
            // More than one agent pool could be found. We return the pools with the highest score
            int bestScore = matchingAgentPools.get(0).score;
            return matchingAgentPools.stream().filter(o -> o.score == bestScore).map(o -> o.agentPoolSpec).collect(Collectors.toSet());
        }
    }

    /**
     * @return the forecasted number of tokens required per pool
     */
    protected Map<Key, Integer> getTokenForecastPerKey() {
        return poolResourceReservations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().maxReservationCount));
    }

    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpec() {
        ArrayList<AgentPoolRequirementSpec> result = new ArrayList<>();
        getTokenForecastPerKey().forEach((key, requiredNumberOfTokens) -> {
            // Sort the matching pools by descending number of tokens
            List<AgentPoolSpec> matchingPools = key.matchingPools.stream()
                    .sorted(Comparator.comparingInt(o -> -o.numberOfTokens)).collect(Collectors.toList());

            // If we have more than one matching pool, we calculate the combination than minimizes the total number of agents
            int remainingTokenCount = requiredNumberOfTokens;
            for (AgentPoolSpec pool : matchingPools.subList(0, matchingPools.size() - 1)) {
                int nAgents = (remainingTokenCount - (remainingTokenCount % pool.numberOfTokens)) / pool.numberOfTokens;
                if(nAgents > 0) {
                    result.add(new AgentPoolRequirementSpec(pool.name, key.provisioningParameters, nAgents));
                    remainingTokenCount = remainingTokenCount - nAgents * pool.numberOfTokens;
                }
            }
            // For the last pool (the one with the lowest number of tokens), we take the rounded up number of agents
            // to guaranty the total number of tokens
            AgentPoolSpec lastAgentPool = matchingPools.get(matchingPools.size() - 1);
            int nAgents = (int) Math.ceil((1.0 * remainingTokenCount) / lastAgentPool.numberOfTokens);
            if(nAgents > 0) {
                result.add(new AgentPoolRequirementSpec(lastAgentPool.name, key.provisioningParameters, nAgents));
            }
        });
        return result;
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

            private TokenWrapper newTokenWrapper(boolean isLocal, Key key) {
                TokenWrapper tokenWrapper = new TokenWrapper();
                Token token = new Token();
                token.setAgentid(isLocal ? "local" : "remote");
                token.setAttributes(key != null ? key.matchingPools.stream().findFirst().orElseThrow().attributes : Map.of());
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
