package step.core.entities;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectPredicate;

public class EntityDependencyTreeVisitor {

	private static final Logger logger = LoggerFactory.getLogger(EntityDependencyTreeVisitor.class);
	private final EntityManager entityManager;
	private final ObjectPredicate objectPredicate;
	// Declared as static for performance reasons. In the current implementation, this class gets instantiated quite often
	// TODO declare it as non-static to avoid potential leaks
	private static final Map<Class<?>, BeanInfo> beanInfoCache = new ConcurrentHashMap<>();

	public EntityDependencyTreeVisitor(EntityManager entityManager, ObjectPredicate objectPredicate) {
		super();
		this.entityManager = entityManager;
		this.objectPredicate = objectPredicate;
	}

	public void visitEntityDependencyTree(String entityName, String entityId, EntityTreeVisitor visitor,
			boolean recursive) {
		EntityTreeVisitorContext context = new EntityTreeVisitorContext(objectPredicate, recursive, visitor);
		visitEntity(entityName, entityId, context);
	}

	public void visitSingleObject(Object object, EntityTreeVisitor visitor) {
		EntityTreeVisitorContext context = new EntityTreeVisitorContext(objectPredicate, false, visitor);
		resolveEntityDependencies(object, context);
	}

	public class EntityTreeVisitorContext {

		private final boolean recursive;
		private final ObjectPredicate objectPredicate;
		private final EntityTreeVisitor visitor;
		private final Map<String, Object> stack = new HashMap<>();

		public EntityTreeVisitorContext(ObjectPredicate objectPredicate, boolean recursive, EntityTreeVisitor visitor) {
			super();
			this.objectPredicate = objectPredicate;
			this.recursive = recursive;
			this.visitor = visitor;
		}

		public ObjectPredicate getObjectPredicate() {
			return objectPredicate;
		}

		public void visitEntity(String entityName, String entityId) {
			if (recursive) {
				EntityDependencyTreeVisitor.this.visitEntity(entityName, entityId, this);
			}
		}
		
		public String resolvedEntityId(String entityName, String entityId) {
			return visitor.onResolvedEntityId(entityName, entityId);
		}

		protected EntityTreeVisitor getVisitor() {
			return visitor;
		}

		public boolean isRecursive() {
			return recursive;
		}

		protected Map<String, Object> getStack() {
			return stack;
		}
	}

	public interface EntityTreeVisitor {

		void onWarning(String warningMessage);

		void onResolvedEntity(String entityName, String entityId, Object entity);

		String onResolvedEntityId(String entityName, String entityId);

	}

	private void visitEntity(String entityName, String entityId, EntityTreeVisitorContext context) {
		Entity<?, ?> entityType = entityManager.getEntityByName(entityName);
		if (entityType == null) {
			String error = "Entities of type '" + entityName + "' are not supported";
			logger.error(error);
			throw new RuntimeException(error);
		}
		EntityTreeVisitor visitor = context.getVisitor();
		Accessor<?> accessor = entityType.getAccessor();
		AbstractIdentifiableObject entity = accessor.get(entityId);
		Map<String, Object> stack = context.getStack();
		// avoid infinite recursions
		if (!stack.containsKey(entityId)) {
			stack.put(entityId, entity);
			if (entity == null) {
				String warning = "Referenced entity with id '" + entityId + "' and type '" + entityName
						+ "' is missing";
				logger.warn(warning);
				visitor.onWarning(warning);
			} else {
				visitor.onResolvedEntity(entityName, entityId, entity);
				resolveEntityDependencies(entity, context);
			}
			stack.remove(entityId);
		}
	}

	@SuppressWarnings("unchecked")
	private void resolveEntityDependencies(Object entity, EntityTreeVisitorContext context) {
		if (entity != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving dependencies for object " + entity);
			}
			EntityTreeVisitor visitor = context.getVisitor();
			BeanInfo beanInfo = getBeanInfo(entity.getClass(), visitor);

			entityManager.getDependencyTreeVisitorHooks().forEach(h ->  {
				try {
					h.onVisitEntity(entity, context);
				} catch (Exception e) {
					visitor.onWarning("TreeVisitorHook failed for entitiy " + entity.getClass() + ", error: " + e.getMessage());
				}
				
			});

			for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
				Method method = descriptor.getReadMethod();
				if (method != null && method.isAnnotationPresent(EntityReference.class)) {
					EntityReference entityReferenceAnnotation = method.getAnnotation(EntityReference.class);
					String entityType = entityReferenceAnnotation.type();
					Object value = null;
					try {
						value = method.invoke(entity);
					} catch (IllegalAccessException | InvocationTargetException e) {
						visitor.onWarning("IllegalAccessException failed for method " + method.getName());
					}
					if (entityType.equals(EntityManager.recursive)) {
						// No actual references, but need to process the field recursively
						if (value instanceof Collection) {
							Collection<?> c = (Collection<?>) value;
							c.forEach(o -> resolveEntityDependencies(o, context));
						} else {
							resolveEntityDependencies(value, context);
						}
					} else {
						if (value instanceof Collection) {
							Collection<?> c = (Collection<?>) value;

							AtomicBoolean listUpdated = new AtomicBoolean();
							ArrayList<Object> newList = new ArrayList<>();
							c.forEach(atomicReference -> {
								// Resolve the entity id of the atomic reference
								String resolvedEntityId = resolveEntityIdAndVisitResolvedEntity(entityType,
										atomicReference, context);
								// Update the atomic reference if needed
								Object newEntityId = updateAtomicReferenceIfNeeded(entityType, resolvedEntityId,
										atomicReference, context);
								if (newEntityId != null) {
									listUpdated.set(true);
								}
								// Add the new atomic reference to the list
								newList.add(newEntityId != null ? newEntityId : resolvedEntityId);
							});

							// Update the collection if changes were made
							if (listUpdated.get()) {
								// Create a new instance of the same type
								Collection<Object> copy;
								try {
									copy = c.getClass().getConstructor(Collection.class).newInstance(newList);
								} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
										| InvocationTargetException | NoSuchMethodException | SecurityException e) {
									if (value instanceof List) {
										// Faileover scenario for immutable lists
										copy = newList;
									} else {
										throw new RuntimeException("Unable to create copy of the collection "
												+ descriptor.getDisplayName() + " of type " + c.getClass().getName(),
												e);
									}
								}
								// Write the new collection to the field
								invokeWriteMethod(entity, descriptor, copy);
							}
						} else {
							// Resolve the entity id of the atomic reference
							String resolvedEntityId = resolveEntityIdAndVisitResolvedEntity(entityType, value, context);
							// Update the atomic reference if needed
							Object newEntityId = updateAtomicReferenceIfNeeded(entityType, resolvedEntityId, value,
									context);
							if (newEntityId != null) {
								// Write the new atomic reference to the field
								invokeWriteMethod(entity, descriptor, newEntityId);
							}
						}
					}
				}
			}
		}
	}

	private BeanInfo getBeanInfo(Class<?> clazz, EntityTreeVisitor visitor) {
		return beanInfoCache.computeIfAbsent(clazz, c -> {
			try {
				return Introspector.getBeanInfo(c, Object.class);
			} catch (IntrospectionException e) {
				visitor.onWarning("Introspection failed for class " + c.getName());
				return null;
			}
		});
	}

	private void invokeWriteMethod(Object object, PropertyDescriptor descriptor, Object value) {
		try {
			Method writeMethod = descriptor.getWriteMethod();
			if (writeMethod != null) {
				writeMethod.invoke(object, value);
			} else {
				throw new RuntimeException("No write method found for propery " + descriptor.getDisplayName());
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(
					"Error while setting property " + descriptor.getDisplayName() + " of object " + object.toString(),
					e);
		}
	}

	private String resolveEntityIdAndVisitResolvedEntity(String entityName, Object atomicReference,
			EntityTreeVisitorContext visitorContext) {
		// First try to resolve the entity id using the custom method of the entity
		String resolvedEntityId = entityManager.getEntityByName(entityName).resolveAtomicReference(atomicReference,
				visitorContext);

		// Call the generic method if the custom method returned null,
		if (resolvedEntityId == null) {
			resolvedEntityId = resolveAtomicReference(atomicReference);
		}

		// Visit the resolved entity
		if (resolvedEntityId != null && visitorContext.isRecursive()) {
			visitEntity(entityName, resolvedEntityId, visitorContext);
		}

		return resolvedEntityId;
	}

	private String resolveAtomicReference(Object atomicReference) {
		String resolvedEntityId = null;
		if (atomicReference instanceof DynamicValue) {
			DynamicValue<?> dynamicValue = (DynamicValue<?>) atomicReference;
			if (!dynamicValue.isDynamic()) {
				Object dValue = dynamicValue.get();
				resolvedEntityId = (dValue != null) ? dValue.toString() : null;
			} else {
				logger.warn(
						"Reference using dynamic expression found and cannot be resolved during export. Expression: "
								+ dynamicValue.getExpression());
			}
		} else if (atomicReference instanceof String) {
			resolvedEntityId = (String) atomicReference;
		} else if (atomicReference instanceof ObjectId) {
			resolvedEntityId = ((ObjectId) atomicReference).toHexString();
		}

		if (resolvedEntityId != null && ObjectId.isValid(resolvedEntityId)) {
			return resolvedEntityId;
		} else {
			return null;
		}
	}

	private Object updateAtomicReferenceIfNeeded(String entityName, String resolvedEntityId, Object atomicReference,
			EntityTreeVisitorContext visitorContext) {
		// Call the onResolvedEntityId hook
		String newEntityId = visitorContext.getVisitor().onResolvedEntityId(entityName, resolvedEntityId);
		if (newEntityId != null) {
			// The update entity Id callback has been called. Update the reference
			// accordingly
			Entity<?, ?> entity = entityManager.getEntityByName(entityName);
			// First try to update the atomic reference with the custom method of the target
			// entity type
			Object newAtomicReference = entity.updateAtomicReference(atomicReference, newEntityId, visitorContext);
			// Then call the generic method if the custom method of the entity type returned
			// null
			return newAtomicReference != null ? newAtomicReference
					: updateAtomicReference(atomicReference, newEntityId);
		} else {
			return null;
		}
	}

	private Object updateAtomicReference(Object atomicReference, String newEntityId) {
		Object newValue = null;
		if (atomicReference instanceof DynamicValue && !((DynamicValue<?>) atomicReference).isDynamic()) {
			newValue = new DynamicValue<>(newEntityId);
		} else if (atomicReference instanceof String) {
			newValue = newEntityId;
		} else if (atomicReference instanceof ObjectId) {
			newValue = new ObjectId(newEntityId);
		}
		return newValue;
	}
}
