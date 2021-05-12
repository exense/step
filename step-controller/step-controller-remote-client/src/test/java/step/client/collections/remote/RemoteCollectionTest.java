package step.client.collections.remote;

import org.junit.Test;
import step.core.collections.AbstractCollectionTest;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.plans.Plan;

import java.util.stream.Stream;

public class RemoteCollectionTest extends AbstractCollectionTest {
    public RemoteCollectionTest() {
        super(new RemoteCollectionFactory());
    }

//    @Test
    public void testGetAllPlans() {
        Collection<Plan> plans = this.collectionFactory.getCollection("plans", Plan.class);
        Stream<Plan> planStream = plans.find(Filters.empty(), null, null, null, 0);

    }
}
