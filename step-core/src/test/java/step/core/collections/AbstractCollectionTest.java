package step.core.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.Test;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.serialization.DottedKeyMap;
import step.core.entities.Bean;

public abstract class AbstractCollectionTest {

	private static final String COLLECTION = "beans";
	private static final String NEW_VALUE = "newValue";
	private static final String VALUE1 = "Test1";
	private static final String VALUE2 = "Test2";
	private static final String VALUE3 = "Test3";
	private static final String PROPERTY1 = "property1";

	protected final CollectionFactory collectionFactory;

	public AbstractCollectionTest(CollectionFactory collectionFactory) {
		super();
		this.collectionFactory = collectionFactory;
	}

	@Test
	public void testGetById() throws Exception {
		// Bean collection
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());
		Bean bean1 = new Bean(VALUE1);
		beanCollection.save(bean1);

		// Id as ObjectId
		Bean actualBean = beanCollection.find(Filters.id(bean1.getId()), null, null, null, 0).findFirst().get();
		assertEquals(bean1, actualBean);

		// Id as string
		actualBean = beanCollection.find(Filters.id(bean1.getId().toString()), null, null, null, 0).findFirst().get();
		assertEquals(bean1, actualBean);

		// Id as ObjectId in equals filter
		actualBean = beanCollection
				.find(Filters.equals(AbstractIdentifiableObject.ID, bean1.getId()), null, null, null, 0).findFirst()
				.get();
		assertEquals(bean1, actualBean);

		// Id as string in equals filter
		actualBean = beanCollection
				.find(Filters.equals(AbstractIdentifiableObject.ID, bean1.getId().toString()), null, null, null, 0)
				.findFirst().get();
		assertEquals(bean1, actualBean);

		// Document collection
		Collection<Document> documentCollection = collectionFactory.getCollection(COLLECTION, Document.class);

		// Id as ObjectId
		Document actualDocument = documentCollection.find(Filters.id(bean1.getId()), null, null, null, 0).findFirst()
				.get();
		assertEquals(bean1.getId(), actualDocument.getId());

		// Id as ObjectId
		actualDocument = documentCollection.find(Filters.id(bean1.getId().toString()), null, null, null, 0).findFirst()
				.get();
		assertEquals(bean1.getId(), actualDocument.getId());
	}

	@Test
	public void test() throws Exception {
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());

		Bean bean1 = new Bean(VALUE1);
		beanCollection.save(bean1);
		Collection<Document> mapCollection = collectionFactory.getCollection(COLLECTION, Document.class);

		Document document = mapCollection.find(Filters.equals(PROPERTY1, VALUE1), null, null, null, 0).findFirst()
				.get();

		Object id = document.get(AbstractIdentifiableObject.ID);
		assertTrue(id instanceof String);
		assertEquals(bean1.getId(), document.getId());
		assertEquals(VALUE1, document.get(PROPERTY1));

		// The returned document should be convertible to the bean with the default mapper
		Bean bean = DefaultJacksonMapperProvider.getObjectMapper().convertValue(document, Bean.class);
		assertEquals(bean1, bean);

		document.put(PROPERTY1, NEW_VALUE);
		mapCollection.save(document);

		Bean actualBean = beanCollection
				.find(Filters.equals(AbstractIdentifiableObject.ID,
						new ObjectId(document.get(AbstractIdentifiableObject.ID).toString())), null, null, null, 0)
				.findFirst().get();
		assertEquals(NEW_VALUE, actualBean.getProperty1());
	}

	@Test
	public void testSave() throws Exception {
		// Bean collection
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());

		Bean bean1 = new Bean(VALUE1);
		// Reset id
		bean1.setId(null);
		beanCollection.save(bean1);
		Bean bean1AfterSave = beanCollection.save(bean1);
		// An Id should have been generated
		assertNotNull(bean1AfterSave.getId());

		// Document collection
		Collection<Document> documentCollection = collectionFactory.getCollection(COLLECTION, Document.class);
		documentCollection.remove(Filters.empty());
		Document document = documentCollection.save(new Document());
		assertNotNull(document.get(AbstractIdentifiableObject.ID));
	}

	@Test
	public void testFind() throws Exception {
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());

		Bean bean1 = new Bean(VALUE1);
		Bean bean2 = new Bean(VALUE2);
		Bean bean3 = new Bean(VALUE3);
		beanCollection.save(List.of(bean1, bean3, bean2));

		// Sort ascending
		List<Bean> result = beanCollection.find(Filters.empty(), new SearchOrder(PROPERTY1, 1), null, null, 0)
				.collect(Collectors.toList());
		assertEquals(List.of(bean1, bean2, bean3), result);

		// Sort ascending by ID
		result = beanCollection.find(Filters.empty(), new SearchOrder(AbstractIdentifiableObject.ID, 1), null, null, 0)
				.collect(Collectors.toList());
		assertEquals(List.of(bean1, bean2, bean3), result);

		// Sort descending
		result = beanCollection.find(Filters.empty(), new SearchOrder(PROPERTY1, -1), null, null, 0)
				.collect(Collectors.toList());
		assertEquals(List.of(bean3, bean2, bean1), result);

		// Skip limit
		result = beanCollection.find(Filters.empty(), new SearchOrder(PROPERTY1, 1), 1, 2, 0)
				.collect(Collectors.toList());
		assertEquals(List.of(bean2, bean3), result);
	}
	
	@Test
	public void testFindFilters() {
		Collection<Bean> collection = collectionFactory.getCollection("beans", Bean.class);
		collection.remove(Filters.empty());
		
		Bean bean = new Bean();
		bean.setProperty1("My property 1");
		bean.setLongProperty(11l);
		bean.setBooleanProperty(false);
		bean.addAttribute("MyAtt1", "My value 1");
		collection.save(bean);

		Bean bean2 = new Bean();
		bean2.setProperty1("My property 2");
		bean2.setLongProperty(21l);
		bean2.setBooleanProperty(false);
		bean2.addAttribute("MyAtt1", "My value 2");
		collection.save(bean2);
		
		// Find by regex
		List<Bean> result = collection.find(Filters.regex("property1", "My", true), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());
		
		// Find by regex with dotted key
		result = collection.find(Filters.regex("attributes.MyAtt1", "My", true), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());
		
		// Equals String
		result = collection.find(Filters.equals("property1", "My property 1"), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());
		
		// Equals String with dotted key
		result = collection.find(Filters.equals("attributes.MyAtt1", "My value 1"), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());
		
		// Equals boolean
		result = collection.find(Filters.equals("booleanProperty", false), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());
		
		// Equals long
		result = collection.find(Filters.equals("longProperty", 11), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());

		//is null
		result = collection.find(Filters.equals("missingField", (String) null), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());

		result = collection.find(Filters.and(List.of(Filters.gte("longProperty", 11),Filters.lt("longProperty",21))), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean.getId(), result.get(0).getId());
		assertEquals(1, result.size());

		result = collection.find(Filters.and(List.of(Filters.gt("longProperty", 11),Filters.lte("longProperty",21))), new SearchOrder("MyAtt1", 1), null, null, 0).collect(Collectors.toList());
		assertEquals(bean2.getId(), result.get(0).getId());
		assertEquals(1, result.size());
	}

	@Test
	public void testFindBySpecialFilters() throws Exception {
		// Bean collection
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());
		Bean bean1 = new Bean(VALUE1);
		bean1.setNested(new Bean());
		beanCollection.save(bean1);

		// Special field _class
		Bean actualBean = beanCollection.find(Filters.regex("_class", "Bean", true), null, null, null, 0).findFirst().get();
		assertEquals(bean1, actualBean);

		// Special field _class nested
		beanCollection.find(Filters.regex("nested._class", "Bean", true), null, null, null, 0).findFirst().get();
		assertEquals(bean1, actualBean);

		// Special field id
		actualBean = beanCollection.find(Filters.equals("id", bean1.getId()), null, null, null, 0).findFirst().get();
		assertEquals(bean1, actualBean);

		// Special field id nested
		actualBean = beanCollection.find(Filters.equals("nested.id", bean1.getNested().getId()), null, null, null, 0).findFirst().get();
		assertEquals(bean1, actualBean);
	}

	@Test
	public void testRemove() throws Exception {
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());

		beanCollection.save(new Bean(VALUE1));

		beanCollection.remove(Filters.equals(PROPERTY1, VALUE1));

		assertNull(beanCollection.find(Filters.equals(PROPERTY1, VALUE1), new SearchOrder("property", 1), null, null, 0)
				.findFirst().orElse(null));
	}

	@Test
	public void testSerializers() throws Exception {
		Collection<Bean> beanCollection = collectionFactory.getCollection(COLLECTION, Bean.class);
		beanCollection.remove(Filters.empty());

		Bean bean1 = new Bean(VALUE1);
		// JSR353
		JsonObject json = Json.createObjectBuilder().add("test", "value").build();
		bean1.setJsonObject(json);
		// org.json
		JSONObject jsonOrgObject = new JSONObject();
		jsonOrgObject.put("key", "value");
		bean1.setJsonOrgObject(jsonOrgObject);
		// map with dots in keys
		DottedKeyMap<String, String> map = new DottedKeyMap<String, String>();
		map.put("key.with.dots", "value");
		bean1.setMap(map);

		beanCollection.save(bean1);

		Bean actualBean = beanCollection.find(Filters.id(bean1.getId()), null, null, null, 0).findFirst().get();
		assertEquals(json, actualBean.getJsonObject());
		// JSONObject doesn't implement equals()
		assertEquals("value", actualBean.getJsonOrgObject().get("key"));
		assertEquals("value", actualBean.getMap().get("key.with.dots"));
	}

}
