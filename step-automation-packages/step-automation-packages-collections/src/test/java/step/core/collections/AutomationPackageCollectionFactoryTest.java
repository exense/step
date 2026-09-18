package step.core.collections;

import org.junit.Assert;
import org.junit.Test;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

import java.util.Properties;

public class AutomationPackageCollectionFactoryTest extends AutomationPackageCollectionTestBase {

    @Test
    public void testCollectionIdempotence() {
        AutomationPackageCollectionFactory cf = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        Collection<Plan> c1 = cf.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
        Assert.assertTrue(c1.estimatedCount() > 0); // just for good measure
        Collection<Plan> c2 = cf.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
        Assert.assertSame(c1, c2);
    }
}
