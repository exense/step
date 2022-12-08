package step.migration.tasks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.scheduler.ExecutiontTaskParameters;

public class SetSchedulerTaskAttributesTest {

	private static final String NAME = "Name";

	@Test
	public void test() {
		CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);

		Document task1 = new Document();
		task1.put(AbstractOrganizableObject.NAME, NAME);

		collectionFactory.getCollection("tasks", Document.class).save(task1);
		SetSchedulerTaskAttributes task = new SetSchedulerTaskAttributes(collectionFactory, null);
		task.runUpgradeScript();

		ExecutiontTaskParameters plan1 = collectionFactory.getCollection("tasks", ExecutiontTaskParameters.class)
				.find(Filters.empty(), null, null, null, 0).findFirst().get();
		assertEquals(NAME, plan1.getName());
	}

}
