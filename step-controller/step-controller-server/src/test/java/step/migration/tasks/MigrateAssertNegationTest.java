package step.migration.tasks;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import step.artefacts.Assert;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.plans.Plan;

public class MigrateAssertNegationTest {

	@Test
	public void test() {
		CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
		
		Document assertNode = new Document();
		assertNode.put("negate", true);
		assertNode.put("_class", "Assert");
		
		Document root = new Document();
		root.put("children", List.of(assertNode));
		root.put("_class", "Sequence");

		Document plan = new Document();
		plan.put("root", root);
		plan.put("_class", Plan.class.getName());
		
		collectionFactory.getCollection("plans", Document.class).save(plan);
		MigrateAssertNegation task = new MigrateAssertNegation(collectionFactory, null);
		task.runUpgradeScript();
		
		Plan plan1 = collectionFactory.getCollection("plans", Plan.class).find(Filters.empty(), null, null, null, 0).findFirst().get();
		assertTrue(((Assert)plan1.getRoot().getChildren().get(0)).getDoNegate().getValue());
	}

}
