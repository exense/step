package step.migration.tasks;

import org.junit.Test;
import step.core.collections.*;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.parameter.Parameter;

import static org.junit.Assert.*;

public class MigrateParametersToDynamicValueTest {

	@Test
	public void test() {
		CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
		
		Document parameter1 = new Document();
		parameter1.put("key", "Param1");
		parameter1.put("value", "some value");

		Document parameter2 = new Document();
		parameter2.put("key", "Param2");
		parameter2.put("encryptedValue", "123435");

		Collection<Document> parameters = collectionFactory.getCollection("parameters", Document.class);
		parameters.save(parameter1);
		parameters.save(parameter2);

		Document version1 = new Document();
		Document parameterVersion1 = new Document();
		parameterVersion1.put("key", "Param1");
		parameterVersion1.put("value", "some value");
		parameterVersion1.put("_entityClass", "step.parameter.Parameter");
		version1.put("entity", parameterVersion1);

		Document version2 = new Document();
		Document parameterVersion2 = new Document();
		parameterVersion2.put("key", "Param2");
		parameterVersion2.put("encryptedValue", "123435");
		parameterVersion2.put("_entityClass", "step.parameter.Parameter");
		version2.put("entity", parameterVersion2);
		Collection<Document> parametersversions = collectionFactory.getCollection("parametersversions", Document.class);
		parametersversions.save(version1);
		parametersversions.save(version2);

		MigrateParametersToDynamicValues task = new MigrateParametersToDynamicValues(collectionFactory, null);
		task.runUpgradeScript();

		Parameter actualParameter1 = collectionFactory.getCollection("parameters", Parameter.class).find(Filters.equals("key", "Param1"), null, null, null, 0).findFirst().get();
		Parameter actualParameter2 = collectionFactory.getCollection("parameters", Parameter.class).find(Filters.equals("key", "Param2"), null, null, null, 0).findFirst().get();
		EntityVersion versionParameter1 = collectionFactory.getCollection("parametersversions", EntityVersion.class).find(Filters.equals("entity.key", "Param1"), null, null, null, 0).findFirst().get();
		EntityVersion versionParameter2 = collectionFactory.getCollection("parametersversions", EntityVersion.class).find(Filters.equals("entity.key", "Param2"), null, null, null, 0).findFirst().get();
		assertEquals("some value", actualParameter1.getValue().get());
		assertNull(actualParameter2.getValue());
		assertEquals("some value", ((Parameter) versionParameter1.getEntity()).getValue().get());
		assertNull(((Parameter) versionParameter2.getEntity()).getValue());
	}



}
