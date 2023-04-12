package step.core.repositories;

import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractRepository implements Repository {

    private final Set<String> canonicalRepositoryParameters;

    /**
     * @param canonicalRepositoryParameters the minimal set of keys that uniquely identifies a repository object in this repository
     */
    protected AbstractRepository(Set<String> canonicalRepositoryParameters) {
        this.canonicalRepositoryParameters = canonicalRepositoryParameters;
    }

    protected void enrichPlan(ExecutionContext context, Plan plan) {
        context.getObjectEnricher().accept(plan);
    }

    @Override
    public boolean compareCanonicalRepositoryParameters(Map<String, String> repositoryParameters1, Map<String, String> repositoryParameters2) {
        return compareRepositoryObjectReference(repositoryParameters1, repositoryParameters2, canonicalRepositoryParameters);
    }

    private static boolean compareRepositoryObjectReference(Map<String, String> repositoryParameters1, Map<String, String> repositoryParameters2, Set<String> canonicalKeys) {
        Map<String, String> canonicalRepositoryParameters1 = getCanonicalRepositoryParameters(canonicalKeys, repositoryParameters1);
        Map<String, String> canonicalRepositoryParameters2 = getCanonicalRepositoryParameters(canonicalKeys, repositoryParameters2);
        return Objects.equals(canonicalRepositoryParameters1, canonicalRepositoryParameters2);
    }

    private static Map<String, String> getCanonicalRepositoryParameters(Set<String> canonicalKeys, Map<String, String> repositoryParameters) {
        return repositoryParameters != null ? repositoryParameters.entrySet().stream().filter(e -> canonicalKeys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) : null;
    }

    @Override
    public Set<String> getCanonicalRepositoryParameters() {
        return canonicalRepositoryParameters;
    }
}
