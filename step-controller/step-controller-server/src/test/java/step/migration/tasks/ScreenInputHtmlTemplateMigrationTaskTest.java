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

import static org.junit.Assert.assertNull;

public class ScreenInputHtmlTemplateMigrationTaskTest {

	public static List<String> jsons = List.of("{ \"id\" : \"617c13a1ba679e55b77d1e48\", \"screenId\" : \"functionTable\", \"input\" : { \"id\" : \"attributes.name\", \"valueHtmlTemplate\" : \"<entity-icon entity=\\\"stBean\\\" entity-name=\\\"'functions'\\\"/> <function-link function_=\\\"stBean\\\" st-options=\\\"stOptions\\\" />\" } }",
			"{ \"id\" : \"617c13a4ba679e55b77d1e5d\", \"screenId\" : \"functionTableExtensions\", \"input\" : { \"id\" : \"customFields.functionPackageId\", \"valueHtmlTemplate\" : \"<function-package-link id='stBean.customFields.functionPackageId' />\" } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ed6\", \"screenId\" : \"planTable\", \"input\" : { \"id\" : \"attributes.name\", \"valueHtmlTemplate\" : \"<entity-icon entity=\\\"stBean\\\" entity-name=\\\"'plans'\\\"/> <plan-link plan-id=\\\"stBean.id\\\" description=\\\"stBean.attributes.name\\\" st-options=\\\"stOptions\\\"/>\" } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ed7\", \"screenId\" : \"schedulerTable\", \"input\" : { \"id\" : \"attributes.name\", \"valueHtmlTemplate\" : \"<entity-icon entity=\\\"stBean\\\" entity-name=\\\"'tasks'\\\"/> <scheduler-task-link scheduler-task=\\\"stBean\\\" />\" } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ede\", \"screenId\" : \"parameterTable\", \"input\" : { \"id\" : \"key\", \"valueHtmlTemplate\" : \"<entity-icon entity=\\\"stBean\\\" entity-name=\\\"'parameters'\\\"/> <parameter-key parameter=\\\"stBean\\\" st-options=\\\"stOptions\\\" />\" } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1eae\", \"screenId\" : \"parameterTable\", \"input\" : { \"id\" : \"noHtml\" } }",
			"{ \"id\" : \"617c13b2ba679e55b77d1ead\", \"screenId\" : \"parameterTable\", \"input\" : { \"id\" : \"unkownHtml\", \"valueHtmlTemplate\" : \"<strong>sometext</strong>\" } }");


	@Test
	public void screenInputHtmlTemplateMigrationTaskTest() {
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

		ScreenInputHtmlTemplateMigrationTask screenInputHtmlTemplateMigrationTask = new ScreenInputHtmlTemplateMigrationTask(collectionFactory, new MigrationContext());
		screenInputHtmlTemplateMigrationTask.runUpgradeScript();

		Collection<ScreenInput> screenInputs1 = collectionFactory.getCollection("screenInputs", ScreenInput.class);
		ScreenInput screenInput;
		screenInput= screenInputs1.find(Filters.equals("id", "617c13a1ba679e55b77d1e48"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals("functionEntityIcon", screenInput.getInput().getCustomUIComponents().get(0));
		Assert.assertEquals("functionLink", screenInput.getInput().getCustomUIComponents().get(1));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13a4ba679e55b77d1e5d"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals("functionPackageLink", screenInput.getInput().getCustomUIComponents().get(0));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ed6"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals("planEntityIcon", screenInput.getInput().getCustomUIComponents().get(0));
		Assert.assertEquals("planLink", screenInput.getInput().getCustomUIComponents().get(1));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ed7"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals("taskEntityIcon", screenInput.getInput().getCustomUIComponents().get(0));
		Assert.assertEquals("schedulerTaskLink", screenInput.getInput().getCustomUIComponents().get(1));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ede"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals("parameterEntityIcon", screenInput.getInput().getCustomUIComponents().get(0));
		Assert.assertEquals("parameterKey", screenInput.getInput().getCustomUIComponents().get(1));

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1eae"), null, null, null, 0).findFirst().orElseThrow();
		assertNull(screenInput.getInput().getCustomUIComponents());

		screenInput= screenInputs1.find(Filters.equals("id", "617c13b2ba679e55b77d1ead"), null, null, null, 0).findFirst().orElseThrow();
		Assert.assertEquals("Migration of custom html template to new UI failed. Source html template: <strong>sometext</strong>", screenInput.getInput().getCustomUIComponents().get(0));
	}
}
