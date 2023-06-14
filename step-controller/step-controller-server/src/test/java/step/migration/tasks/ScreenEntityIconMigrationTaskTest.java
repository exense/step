package step.migration.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.migration.MigrationContext;
import step.plugins.screentemplating.ScreenInput;

import java.util.List;

public class ScreenEntityIconMigrationTaskTest {

	public static List<String> jsons = List.of("{ \"id\" : \"617c13a1ba679e55b77d1e48\", \"attributes\" : { \"project\" : \"617c13a2ba679e55b77d1e55\" }, \"screenId\" : \"functionTable\", \"position\" : 0, \"input\" : { \"id\" : \"attributes.name\", \"type\" : \"TEXT\", \"label\" : \"Name\", \"customUIComponents\" : [ \"functionEntityIcon\", \"functionLink\" ] } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ed6\", \"attributes\" : { \"project\" : \"617c13a2ba679e55b77d1e55\" }, \"screenId\" : \"planTable\", \"position\" : 0, \"input\" : { \"id\" : \"attributes.name\", \"type\" : \"TEXT\", \"label\" : \"Name\", \"customUIComponents\" : [ \"planEntityIcon\", \"planLink\" ] } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ed7\", \"attributes\" : { \"project\" : \"617c13a2ba679e55b77d1e55\" }, \"screenId\" : \"schedulerTable\", \"position\" : 0, \"input\" : { \"id\" : \"attributes.name\", \"type\" : \"TEXT\", \"label\" : \"Name\", \"customUIComponents\" : [ \"taskEntityIcon\", \"schedulerTaskLink\" ] } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ede\", \"attributes\" : { \"project\" : \"617c13a2ba679e55b77d1e55\" }, \"screenId\" : \"parameterTable\", \"position\" : 0, \"input\" : { \"id\" : \"key\", \"type\" : \"TEXT\", \"label\" : \"Key\", \"description\" : \"Keys containing 'pwd' or 'password' will be automatically protected\", \"customUIComponents\" : [ \"parameterEntityIcon\", \"parameterKey\" ] } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ee2\", \"screenId\" : \"parameterDialog\", \"input\" : { \"id\" : \"key\", \"type\" : \"TEXT\", \"label\" : \"Key\", \"description\" : \"Keys containing 'pwd' or 'password' will be automatically protected\" }, \"attributes\" : { \"project\" : \"617c13a2ba679e55b77d1e55\" }, \"position\" : 0 }");


	@Test
	public void screenEntityIconMigrationTaskTest() {
		InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
		Collection<Document> screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
		ObjectMapper objectMapper = new ObjectMapper();
		jsons.forEach(j -> {
			try {
				Document document = objectMapper.readValue(j, Document.class);
				screenInputs.save(document);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		});

		ScreenEntityIconMigrationTask screenEntityIconMigrationTask = new ScreenEntityIconMigrationTask(collectionFactory, new MigrationContext());
		screenEntityIconMigrationTask.runUpgradeScript();

		Collection<ScreenInput> screenInputs1 = collectionFactory.getCollection("screenInputs", ScreenInput.class);
		ScreenInput screenInput;
		screenInput= screenInputs1.find(Filters.equals("id", "617c13a1ba679e55b77d1e48"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals(1, screenInput.getInput().getCustomUIComponents().size());
		Assert.assertEquals("functionLink", screenInput.getInput().getCustomUIComponents().get(0));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ed6"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals(1, screenInput.getInput().getCustomUIComponents().size());
		Assert.assertEquals("planLink", screenInput.getInput().getCustomUIComponents().get(0));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ed7"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals(1, screenInput.getInput().getCustomUIComponents().size());
		Assert.assertEquals("schedulerTaskLink", screenInput.getInput().getCustomUIComponents().get(0));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ede"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals(1, screenInput.getInput().getCustomUIComponents().size());
		Assert.assertEquals("parameterKey", screenInput.getInput().getCustomUIComponents().get(0));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ee2"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertNull(screenInput.getInput().getCustomUIComponents());
	}
}
