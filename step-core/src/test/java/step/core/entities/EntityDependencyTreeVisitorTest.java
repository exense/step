package step.core.entities;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bson.types.ObjectId;
import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.InMemoryAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitor;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;

public class EntityDependencyTreeVisitorTest {

	private static final String CUSTOM_REFERENCE = "customReference";
	private static final String ENTITY_TYPE2 = "entityType2";
	private static final String ENTITY_TYPE1 = "entityType1";

	@Test
	public void testVisit() {
		EntityManager entityManager = new EntityManager();

		EntityType2 entity2 = new EntityType2();
		InMemoryAccessor<EntityType2> accessor2 = new InMemoryAccessor<EntityType2>();
		accessor2.save(entity2);

		InMemoryAccessor<EntityType1> accessor1 = new InMemoryAccessor<EntityType1>();
		EntityType1 entity1 = new EntityType1(entity2.getId().toString());
		accessor1.save(entity1);
		
		Entity<EntityType1, InMemoryAccessor<EntityType1>> entityType1 = new Entity<>(
				ENTITY_TYPE1, accessor1, EntityType1.class).addReferencesHook(new ResolveReferencesHook() {
					@Override
					public void accept(Object t, EntityTreeVisitorContext context) {
						context.visitEntity(ENTITY_TYPE2, entity2.getId().toString());
					}
				});
		
		entityManager.register(entityType1);
		entityManager
				.register(new Entity<EntityType2, Accessor<EntityType2>>(ENTITY_TYPE2, accessor2, EntityType2.class) {

					@Override
					public String resolveAtomicReference(Object atomicReference,
							EntityTreeVisitorContext visitorContext) {
						return atomicReference != null && atomicReference.equals(CUSTOM_REFERENCE)
								? entity2.getId().toString()
								: null;
					}

				});
		EntityDependencyTreeVisitor visitor = new EntityDependencyTreeVisitor(entityManager);
		
		List<String> entityIds = new ArrayList<>();
		visitor.visitEntityDependencyTree(ENTITY_TYPE1, entity1.getId().toString(), new EntityTreeVisitor() {
			
			@Override
			public void onWarning(String warningMessage) {
				
			}
			
			@Override
			public void onResolvedEntity(String entityName, String entityId, Object entity) {
				entityIds.add(entityId);
			}

			@Override
			public void onResolvedEntityId(String entityName, String resolvedEntityId,
					Consumer<String> replaceIdCallback) {
				
			}
		}, true);
		
		assertEquals(9, entityIds.size());
		
	}
	
	@Test
	public void testUpdateEntityId() {
		EntityManager entityManager = new EntityManager();

		EntityType2 entity2 = new EntityType2();
		InMemoryAccessor<EntityType2> accessor2 = new InMemoryAccessor<EntityType2>();
		accessor2.save(entity2);

		InMemoryAccessor<EntityType1> accessor1 = new InMemoryAccessor<EntityType1>();
		EntityType1 entity1 = new EntityType1(entity2.getId().toString());
		accessor1.save(entity1);
		
		Entity<EntityType1, InMemoryAccessor<EntityType1>> entityType1 = new Entity<>(ENTITY_TYPE1, accessor1, EntityType1.class).addReferencesHook(new ResolveReferencesHook() {
			@Override
			public void accept(Object t, EntityTreeVisitorContext context) {
				context.visitEntity(ENTITY_TYPE2, entity2.getId().toString());
			}
		});
		entityManager.register(entityType1);
		
		entityManager.register(new Entity<EntityType2, Accessor<EntityType2>>(ENTITY_TYPE2, accessor2, EntityType2.class) {

			@Override
			public String resolveAtomicReference(Object atomicReference, EntityTreeVisitorContext visitorContext) {
				return entity2.getId().toString();
			}

			@Override
			public Object updateAtomicReference(Object atomicReference, String newEntityId,
					EntityTreeVisitorContext visitorContext) {
				if(atomicReference != null && atomicReference.equals(CUSTOM_REFERENCE)) {
					return entity2.getId().toString();
				} else {
					return null;
				}
			}
			
		});
		
		EntityDependencyTreeVisitor visitor = new EntityDependencyTreeVisitor(entityManager);
		
		ObjectId newId = new ObjectId();
		String newIdString = newId.toHexString();
		List<String> entityIds = new ArrayList<>();
		
		visitor.visitSingleObject(entity1, new EntityTreeVisitor() {
			
			@Override
			public void onWarning(String warningMessage) {
				
			}
			
			@Override
			public void onResolvedEntity(String entityName, String entityId, Object entity) {
				entityIds.add(entityId);
			}

			@Override
			public void onResolvedEntityId(String entityName, String resolvedEntityId,
					Consumer<String> replaceIdCallback) {
				replaceIdCallback.accept(newIdString);
			}
		});
		
		assertEquals(0, entityIds.size());
		assertEquals(newIdString, entity1.getReferenceAsString());
		assertEquals(newId, entity1.getReferenceAsObjectId());
		assertEquals(null, entity1.getNullRefrence());
		assertEquals(List.of(newIdString), entity1.getReferenceList());
		assertEquals(newIdString, entity1.getReferenceObject().getRef());
		assertEquals(newIdString, entity1.getReferenceAsDynamicValue().get());
		assertEquals(newIdString, entity1.getInfiniteRecursion());
		assertEquals(entity2.getId().toString(), entity1.getCustomReference());
	}
	
	public static class EntityType1 extends AbstractOrganizableObject {
		
		private String referenceAsString;
		private ObjectId referenceAsObjectId;
		private String nullRefrence;
		private List<String> referenceList;
		private ReferenceObject referenceObject;
		private List<ReferenceObject> recursiveListOfReference;
		private DynamicValue<String> referenceAsDynamicValue;
		private String infiniteRecursion;
		private String customReference;
		
		public EntityType1(String entityRef) {
			super();
			referenceAsString = entityRef;
			referenceAsObjectId = new ObjectId(entityRef);
			nullRefrence = null;
			referenceList = List.of(entityRef);
			referenceObject = new ReferenceObject(entityRef);
			recursiveListOfReference = List.of(new ReferenceObject(entityRef));
			referenceAsDynamicValue = new DynamicValue<String>(entityRef);
			infiniteRecursion = this.getId().toString();
			customReference = CUSTOM_REFERENCE;
		}

		@EntityReference(type = ENTITY_TYPE2)
		public String getReferenceAsString() {
			return referenceAsString;
		}
		
		@EntityReference(type = ENTITY_TYPE2)
		public ObjectId getReferenceAsObjectId() {
			return referenceAsObjectId;
		}
		
		@EntityReference(type = ENTITY_TYPE2)
		public String getNullRefrence() {
			return nullRefrence;
		}
		
		@EntityReference(type = ENTITY_TYPE2)
		public List<String> getReferenceList() {
			return referenceList;
		}
		
		@EntityReference(type = "recursive")
		public ReferenceObject getReferenceObject() {
			return referenceObject;
		}
		
		@EntityReference(type = "recursive")
		public List<ReferenceObject> getRecursiveListOfReference() {
			return recursiveListOfReference;
		}
		
		@EntityReference(type = ENTITY_TYPE2)
		public DynamicValue<String> getReferenceAsDynamicValue() {
			return referenceAsDynamicValue;
		}
		
		@EntityReference(type = ENTITY_TYPE1)
		public String getInfiniteRecursion() {
			return infiniteRecursion;
		}

		@EntityReference(type = ENTITY_TYPE2)
		public String getCustomReference() {
			return customReference;
		}

		public void setReferenceAsString(String referenceAsString) {
			this.referenceAsString = referenceAsString;
		}

		public void setReferenceAsObjectId(ObjectId referenceAsObjectId) {
			this.referenceAsObjectId = referenceAsObjectId;
		}

		public void setNullRefrence(String nullRefrence) {
			this.nullRefrence = nullRefrence;
		}

		public void setReferenceList(List<String> referenceList) {
			this.referenceList = referenceList;
		}

		public void setReferenceObject(ReferenceObject referenceObject) {
			this.referenceObject = referenceObject;
		}

		public void setRecursiveListOfReference(List<ReferenceObject> recursiveListOfReference) {
			this.recursiveListOfReference = recursiveListOfReference;
		}

		public void setReferenceAsDynamicValue(DynamicValue<String> referenceAsDynamicValue) {
			this.referenceAsDynamicValue = referenceAsDynamicValue;
		}

		public void setInfiniteRecursion(String infiniteRecursion) {
			this.infiniteRecursion = infiniteRecursion;
		}

		public void setCustomReference(String customReference) {
			this.customReference = customReference;
		}
	}
	
	public static class ReferenceObject {
		
		private String entityRef;
		
		public ReferenceObject(String entityRef) {
			super();
			this.entityRef = entityRef;
		}

		@EntityReference(type = ENTITY_TYPE2)
		public String getRef() {
			return entityRef;
		}

		public void setRef(String entityRef) {
			this.entityRef = entityRef;
		}
	}

	public static class EntityType2 extends AbstractOrganizableObject {
		
	}
}
