package step.core.collections;

import org.junit.Assert;
import org.junit.Test;
import step.artefacts.TestCase;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AutomationPackageCollectionFactoryTest extends AutomationPackageCollectionTestBase {

    @Test
    public void testCollectionIdempotence() {
        AutomationPackageCollectionFactory cf = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        Collection<Plan> c1 = cf.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
        Assert.assertTrue(c1.estimatedCount() > 0); // just for good measure
        Collection<Plan> c2 = cf.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
        Assert.assertSame(c1, c2);
    }

    @Test
    public void testFindPlansByRootArtefactType() {
        AutomationPackageCollectionFactory cf = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        Collection<Plan> plans = cf.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);

        // The type of an artefact is its name, as in the other collection implementations
        List<Plan> testCases = plans.find(Filters.equals("root._class", "TestCase"), null, null, null, 0).collect(Collectors.toList());
        Assert.assertFalse(testCases.isEmpty());
        Assert.assertTrue(testCases.stream().allMatch(plan -> plan.getRoot() instanceof TestCase));
        Assert.assertTrue(testCases.stream().anyMatch(plan -> "Test Plan".equals(plan.getAttribute(AbstractOrganizableObject.NAME))));

        Assert.assertEquals(0, plans.count(Filters.equals("root._class", TestCase.class.getName()), null));
    }
}
