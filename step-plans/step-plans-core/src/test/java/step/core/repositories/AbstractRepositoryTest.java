package step.core.repositories;

import org.junit.Test;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectPredicate;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbstractRepositoryTest {

    @Test
    public void compareRepositoryObjectReference() {
        Map<String, String> repositoryParams1 = Map.of("key1", "value1");
        Map<String, String> repositoryParams2 = Map.of("key1", "value1", "key2", "value1");
        Map<String, String> repositoryParams3 = null;

        // The list of canonical parameters is empty
        assertTrue(getAbstractRepository(Set.of()).compareCanonicalRepositoryParameters(repositoryParams1, repositoryParams2));

        // key1 is the same in both ref. repositoryParams2 contains an additional parameter that isn't part of the canonical list of parameters
        assertTrue(getAbstractRepository(Set.of("key1")).compareCanonicalRepositoryParameters(repositoryParams1, repositoryParams2));

        // The key2 doesn't exist in repositoryParams1
        assertFalse(getAbstractRepository(Set.of("key1", "key2")).compareCanonicalRepositoryParameters(repositoryParams1, repositoryParams2));

        // The key3 doesn't exist in both ref
        assertTrue(getAbstractRepository(Set.of("key3")).compareCanonicalRepositoryParameters(repositoryParams1, repositoryParams2));

        // One list is null
        assertFalse(getAbstractRepository(Set.of()).compareCanonicalRepositoryParameters(repositoryParams1, repositoryParams3));
        assertTrue(getAbstractRepository(Set.of()).compareCanonicalRepositoryParameters(repositoryParams3, repositoryParams3));
    }

    private static AbstractRepository getAbstractRepository(Set<String> canonicalRepositoryParameters) {
        return new AbstractRepository(canonicalRepositoryParameters) {
            @Override
            public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
                return null;
            }

            @Override
            public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
                return null;
            }

            @Override
            public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
                return null;
            }

            @Override
            public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {

            }
        };
    }
}