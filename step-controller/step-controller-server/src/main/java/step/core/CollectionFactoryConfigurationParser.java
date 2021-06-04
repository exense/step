package step.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import ch.exense.commons.app.Configuration;
import step.core.collections.CollectionFactory;
import step.core.collections.delegating.DelegatingCollectionFactory;

public class CollectionFactoryConfigurationParser {

	protected static final String PREFIX = "datasource.";
	protected static final String TYPE = ".type";
	protected static final String APPLIES_TO = ".collections";
	protected static final String PROPERTIES = ".properties.";
	protected static final String ALL = "all";
	protected static final String SEPARATOR = ",";
	protected static final String DOT = "\\.";

	protected static final String DB_PREFIX = "db.";

	public static DelegatingCollectionFactory parseConfiguration(Configuration configuration) {
		DelegatingCollectionFactory delegatingCollectionFactory = new DelegatingCollectionFactory();
		// Iterate over all configured collection factories and instantiate them
		Set<Object> propertyNames = configuration.getPropertyNames();
		propertyNames.stream().filter(p -> p.toString().startsWith(PREFIX)).map(p -> p.toString().split(DOT)[1])
				.distinct().forEach(collectionFactoryId -> createCollectionFactory(configuration,
						delegatingCollectionFactory, collectionFactoryId));

		// Legacy DB configuration mode
		Properties dbProperties = new Properties();
		propertyNames.stream().filter(p -> p.toString().startsWith(DB_PREFIX))
				.map(p -> p.toString().replace(DB_PREFIX, ""))
				.forEach(p -> dbProperties.put(p, configuration.getProperty(DB_PREFIX + p)));
		if (dbProperties.size() > 0) {
			addCollectionFactory(delegatingCollectionFactory, "mongodb",
					"step.core.collections.mongodb.MongoDBCollectionFactory", dbProperties, ALL);
		}

		return delegatingCollectionFactory;

	}

	private static CollectionFactory createCollectionFactory(Configuration configuration,
			DelegatingCollectionFactory delegatingCollectionFactory, String collectionFactoryId) {
		Properties properties = new Properties();
		configuration.getPropertyNames().stream()
				.filter(p -> p.toString().startsWith(PREFIX + collectionFactoryId + PROPERTIES))
				.forEach(p -> properties.put(p.toString().replace(PREFIX + collectionFactoryId + PROPERTIES, ""),
						configuration.getProperty(p.toString())));
		String type = configuration.getProperty(PREFIX + collectionFactoryId + TYPE);
		String collectionNameList = configuration.getProperty(PREFIX + collectionFactoryId + APPLIES_TO);

		CollectionFactory collectionFactory = addCollectionFactory(delegatingCollectionFactory, collectionFactoryId,
				type, properties, collectionNameList);

		return collectionFactory;
	}

	private static CollectionFactory addCollectionFactory(DelegatingCollectionFactory delegatingCollectionFactory,
			String collectionFactoryId, String type, Properties properties, String collectionNameList) {
		CollectionFactory collectionFactory;
		try {
			collectionFactory = (CollectionFactory) Class.forName(type).getConstructor(Properties.class)
					.newInstance(properties);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			throw new RuntimeException("Error while creating instance of collection factory " + collectionFactoryId, e);
		}

		delegatingCollectionFactory.addCollectionFactory(collectionFactoryId, collectionFactory);

		String[] collectionNames = collectionNameList.split(SEPARATOR);
		Arrays.asList(collectionNames).stream().forEach(collectionName -> {
			if (!collectionName.equals(ALL)) {
				delegatingCollectionFactory.addRoute(collectionName, collectionFactoryId);
			} else {
				delegatingCollectionFactory.setDefaultRoute(collectionFactoryId);
			}
		});
		return collectionFactory;
	}
}
