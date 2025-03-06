package step.core.execution.model;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple data class to keep track of which agents (identified by their URLs)
 * participated in an execution.
 */
public class ExecutionAgentUrls {
    private final Set<String> backingSet = ConcurrentHashMap.newKeySet();

    public void add(String agentUrl) {
        backingSet.add(agentUrl);
    }

    @Override
    // this directly returns the data in the output format the frontend expects:
    // space-separated, and for convenience, sorted alphabetically (case-insensitive)
    public String toString() {
        return backingSet.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(" "));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionAgentUrls that = (ExecutionAgentUrls) o;
        return Objects.equals(backingSet, that.backingSet);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(backingSet);
    }
}
