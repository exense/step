package step.migration.tasks;

import org.junit.Test;
import step.artefacts.Sleep;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.BaseArtefacts;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MigrateSleepArtefactInPlansTest {

    @Test
    public void test() {
        CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);

        Sleep sleep = new Sleep();
        sleep.setDuration(new DynamicValue<>(10L));
        sleep.setUnit(new DynamicValue<>("s"));
        Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sleep).endBlock().build();
        collectionFactory.getCollection("plans", Plan.class).save(plan);

        sleep = new Sleep();
        sleep.setDuration(new DynamicValue<>(10L));
        sleep.setUnit(new DynamicValue<>("m"));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sleep).endBlock().build();
        collectionFactory.getCollection("plans", Plan.class).save(plan);

        sleep = new Sleep();
        sleep.setDuration(new DynamicValue<>(10L));
        sleep.setUnit(new DynamicValue<>("ms"));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sleep).endBlock().build();
        collectionFactory.getCollection("plans", Plan.class).save(plan);

        sleep = new Sleep();
        sleep.setDuration(new DynamicValue<>("10",""));
        sleep.setUnit(new DynamicValue<>("ms"));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sleep).endBlock().build();
        collectionFactory.getCollection("plans", Plan.class).save(plan);

        sleep = new Sleep();
        sleep.setDuration(new DynamicValue<>("10",""));
        sleep.setUnit(new DynamicValue<>("s"));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sleep).endBlock().build();
        collectionFactory.getCollection("plans", Plan.class).save(plan);

        sleep = new Sleep();
        sleep.setDuration(new DynamicValue<>("10",""));
        sleep.setUnit(new DynamicValue<>("s",""));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sleep).endBlock().build();
        collectionFactory.getCollection("plans", Plan.class).save(plan);


        MigrateSleepArtefactInPlans task = new MigrateSleepArtefactInPlans(collectionFactory, null);
        task.runUpgradeScript();

        List<Plan> plans = collectionFactory.getCollection("plans", Plan.class).find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
        Plan plan1 = plans.get(0);
        assertEquals("ms", ((Sleep)plan1.getRoot().getChildren().get(0)).getUnit().get());
        assertEquals(10000L, (long) ((Sleep) plan1.getRoot().getChildren().get(0)).getDuration().get());

        plan1 = plans.get(1);
        assertEquals("ms", ((Sleep)plan1.getRoot().getChildren().get(0)).getUnit().get());
        assertEquals(600000L, (long) ((Sleep) plan1.getRoot().getChildren().get(0)).getDuration().get());

        plan1 = plans.get(2);
        assertEquals("ms", ((Sleep)plan1.getRoot().getChildren().get(0)).getUnit().get());
        assertEquals(10L, (long) ((Sleep) plan1.getRoot().getChildren().get(0)).getDuration().get());

        plan1 = plans.get(3);
        assertEquals("ms", ((Sleep)plan1.getRoot().getChildren().get(0)).getUnit().get());
        assertTrue(((Sleep) plan1.getRoot().getChildren().get(0)).getDuration().isDynamic());
        assertEquals("10", ((Sleep)plan1.getRoot().getChildren().get(0)).getDuration().getExpression());

        plan1 = plans.get(4);
        assertEquals("s", ((Sleep)plan1.getRoot().getChildren().get(0)).getUnit().get());
        assertTrue(((Sleep) plan1.getRoot().getChildren().get(0)).getDuration().isDynamic());
        assertEquals("10", ((Sleep)plan1.getRoot().getChildren().get(0)).getDuration().getExpression());

        plan1 = plans.get(5);
        assertTrue(((Sleep) plan1.getRoot().getChildren().get(0)).getDuration().isDynamic());
        assertTrue(((Sleep) plan1.getRoot().getChildren().get(0)).getUnit().isDynamic());
        assertEquals("s", ((Sleep)plan1.getRoot().getChildren().get(0)).getUnit().getExpression());
        assertEquals("10", ((Sleep)plan1.getRoot().getChildren().get(0)).getDuration().getExpression());
    }
}
