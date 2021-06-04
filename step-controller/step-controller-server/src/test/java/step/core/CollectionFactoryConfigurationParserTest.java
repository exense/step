package step.core;

import static org.junit.Assert.*;
import static step.core.CollectionFactoryConfigurationParser.*;

import org.junit.Test;

import ch.exense.commons.app.Configuration;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.delegating.DelegatingCollectionFactory;
import step.core.collections.mongodb.MongoDBCollection;
import step.core.collections.mongodb.MongoDBCollectionFactory;

public class CollectionFactoryConfigurationParserTest {

	@Test
	public void testLegacyConfiguration() {
		Configuration configuration = new Configuration();
		configuration.putProperty("db.host", "localhost");
		configuration.putProperty("database", "test");
		DelegatingCollectionFactory factory = CollectionFactoryConfigurationParser.parseConfiguration(configuration);
		
		Collection<Document> collection = factory.getCollection("test", Document.class);
		assertTrue(collection instanceof MongoDBCollection);
	}
	
	@Test
	public void testConfiguration() {
		Configuration configuration = new Configuration();
		configuration.putProperty(PREFIX + "myCollection" + TYPE, MongoDBCollectionFactory.class.getName());
		configuration.putProperty(PREFIX + "myCollection" + APPLIES_TO, "test");
		configuration.putProperty(PREFIX + "myCollection" + PROPERTIES + "host", "localhost");
		configuration.putProperty(PREFIX + "myCollection" + PROPERTIES + "database", "test");
		
		DelegatingCollectionFactory factory = CollectionFactoryConfigurationParser.parseConfiguration(configuration);
		
		Collection<Document> collection = factory.getCollection("test", Document.class);
		assertTrue(collection instanceof MongoDBCollection);
	}

}
