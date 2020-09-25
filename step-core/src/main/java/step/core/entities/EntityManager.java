/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.entities;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.FileResolver;
import step.core.AbstractContext;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectPredicate;
import step.resources.ResourceManager;

public class EntityManager  {
	
	private static Logger logger = LoggerFactory.getLogger(EntityManager.class);
	
	public final static String executions = "executions";
	public final static String plans = "plans";
	public static final String functions = "functions";
	public final static String reports = "reports";
	public final static String tasks = "tasks";
	public final static String users = "users";
	public final static String resources = "resources";
	public final static String resourceRevisions = "resourceRevisions";
	public final static String recursive = "recursive";
	
	private Map<String, Entity<?,?>> entities = new ConcurrentHashMap<String, Entity<?,?>>();
	private Map<Class<?>, Entity<?,?>> entitiesByClass = new ConcurrentHashMap<Class<?>, Entity<?,?>>();
	private AbstractStepContext context;
	Map<Class<?>,BeanInfo> beanInfoCache = new ConcurrentHashMap<>();
	
	public EntityManager(AbstractStepContext context) {
		this.context = context;
	}

	public EntityManager register(Entity<?,?> entity) {
		entities.put(entity.getName(), entity);
		entitiesByClass.put(entity.getEntityClass(), entity);
		return this;
	}
	
	public Entity<?,?> getEntityByName(String entityName) {
		return entities.get(entityName);
	}
	
	public void getEntitiesReferences(String entityType, ObjectPredicate objectPredicate, boolean recursively, EntityReferencesMap refs) {
		Entity<?, ?> entity = getEntityByName(entityType);
		if (entity == null ) {
			throw new RuntimeException("Entity of type " + entityType + " is not supported");
		}
		entity.getAccessor().getAll().forEachRemaining(a -> {
			if (entity.isByPassObjectPredicate() || objectPredicate.test(a)) {
				refs.addElementTo(entityType, a.getId().toHexString());
				if (recursively) {
					getAllEntities(entityType, a.getId().toHexString(), refs);	
				}
			}
		});
	}

	public void getAllEntities (String entityName, String id, EntityReferencesMap references) {
		Entity<?, ?> entity = getEntityByName(entityName);
		if (entity == null) {
			logger.error("Entities of type '" + entityName + "' are not supported");
			throw new RuntimeException("Entities of type '" + entityName + "' are not supported");
		}
		CRUDAccessor<?> accessor = entity.getAccessor();
		AbstractIdentifiableObject a = accessor.get(id);
		if (a == null ) {
			logger.warn("Referenced entity with id '" + id + "' and type '" + entityName + "' is missing");
			references.addReferenceNotFoundWarning("Referenced entity with id '" + id + "' and type '" + entityName + "' is missing");
		} else {
			resolveReferences(a, references);
			entity.getReferencesHook().forEach(h->h.accept(a,references));
		}
	}
	
	private void resolveReferences(Object object, EntityReferencesMap references) {
		if(object!=null) {
			Class<?> clazz = object.getClass();
			try {
				BeanInfo beanInfo = beanInfoCache.get(clazz);
				if(beanInfo==null) {
					beanInfo = Introspector.getBeanInfo(clazz, Object.class);
					beanInfoCache.put(clazz, beanInfo);
				}
				
				for(PropertyDescriptor descriptor:beanInfo.getPropertyDescriptors()) {
					Method method = descriptor.getReadMethod();
					if(method!=null) {
						if(method.isAnnotationPresent(EntityReference.class)) {
							EntityReference eRef = method.getAnnotation(EntityReference.class);
							String entityType = eRef.type();
							Object value = method.invoke(object);
							if (entityType.equals(recursive)) {
								//No actual references, but need to process the field recursively
								if (value instanceof Collection) {
									Collection<?> c = (Collection<?>) value;
									c.forEach(o->resolveReferences(o, references));
								} else {
									resolveReferences(value, references);
								}
							}
							else {	
								if (value instanceof Collection) {
									Collection<?> c = (Collection<?>) value;
									c.forEach(r->resolveReference(r, references, entityType, r.getClass()));
								} else {
									resolveReference(value, references, entityType, method.getReturnType());
								}
							}
						}						
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Export failed, unabled to get references by reflections",e);
			}			
		}
	}
	
	private void resolveReference(Object value, EntityReferencesMap references, String entityType, Class<?> type) {
		FileResolver fileResolver = context.getFileResolver();
		String refId = null;
		if (type.equals(DynamicValue.class) && value!=null) {
			DynamicValue<?> dynamicValue = (DynamicValue<?>) value;
			if (!dynamicValue.isDynamic()) {
				Object dValue = dynamicValue.get();
				refId = (dValue != null) ? dValue.toString() : null;
				if (entityType.equals(resources)) {
					refId = fileResolver.resolveResourceId(refId);
				}
			} else {
				logger.warn("Reference using dynamic expression found and cannot be resolved during export. Expression: "
						+ dynamicValue.getExpression());
			}
		} else if (type.equals(String.class)) {
			refId = (String) value;
		} else if (type.equals(ObjectId.class)) {
			refId = ((ObjectId) value).toHexString();
		}
		if (refId != null && ObjectId.isValid(refId)) {
			boolean added = references.addElementTo(entityType, refId);
			//hack for resource revisions (no explicit references for now)
			if (entityType.equals(resources)) {
				String revisionId = context.getResourceManager().getResourceRevisionByResourceId(refId).getId().toHexString();
				references.addElementTo(EntityManager.resourceRevisions, revisionId);
			}
			//avoid circular references issue
			if (added) { 
				getAllEntities(entityType, refId, references);
			}
		}
	}
	
	public Entity<?,?> getEntitiesByClass(Class<?> c) {
		Entity<?,?> result = entitiesByClass.get(c);
		while (result == null && !c.equals(Object.class)) {
			c = c.getSuperclass();
			result = entitiesByClass.get(c);
		}
		return result;
		
	}

	public void updateReferences(Object object, Map<String, String> references) {
		Entity<?,?> entity = getEntitiesByClass(object.getClass());
		if (entity != null) {
			entity.getUpdateReferencesHook().forEach(r->r.accept(object, references));
		}
		if(object!=null) {
			Class<?> clazz = object.getClass();
			try {
				BeanInfo beanInfo = beanInfoCache.get(clazz);
				if(beanInfo==null) {
					beanInfo = Introspector.getBeanInfo(clazz, Object.class);
					beanInfoCache.put(clazz, beanInfo);
				}
				for(PropertyDescriptor descriptor:beanInfo.getPropertyDescriptors()) {
					Method method = descriptor.getReadMethod();
					if(method!=null) {
						if(method.isAnnotationPresent(EntityReference.class)) {
							EntityReference eRef = method.getAnnotation(EntityReference.class);
							String entityType = eRef.type();
							Object value = method.invoke(object);
							if (entityType.equals(recursive)) {
								//No actual references, but need to process the field recursively
								if (value != null) {
									if (value instanceof Collection) {
										Collection<?> c = (Collection<?>) value;
										c.forEach(o -> updateReferences(o, references));
									} else {
										updateReferences(value, references);
									}
								} else {
									logger.warn("Skipping import of reference with null value");
								}
							}
							else {	
								Object newValue = getNewValue(value, method.getReturnType(), references, entityType);							
								if (newValue != null) {
									Method writeMethod = descriptor.getWriteMethod();
									if(writeMethod!=null) {
										descriptor.getWriteMethod().invoke(object, newValue);
									} else {
										//throw new RuntimeException("Unable to clone object "+o.toString()+". No setter found for "+descriptor);
									}
								}
							}
						}
					}
				}
			} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException e) {
				throw new RuntimeException("Import failed, unabled to updates references by reflections",e);
			}
		}
	}
	
	private Object getNewValue_(Object value, Class<?> returnType, Map<String, String> references, String entityType) {
		FileResolver fileResolver = context.getFileResolver();
		Object newValue = null;
		String origRefId = null;
		String newRefId = null;
		//Get original id
		if (returnType.equals(DynamicValue.class) && value != null &&
				!((DynamicValue<?>) value).isDynamic() && ((DynamicValue<?>) value).get() != null) {
			origRefId = ((DynamicValue<?>) value).get().toString();
		} else if (returnType.equals(String.class)) {
			origRefId = (String) value;
		} else if (returnType.equals(ObjectId.class)) {
			origRefId = ((ObjectId) value).toHexString();
		}
		if (entityType.equals(resources)) {
			origRefId = fileResolver.resolveResourceId(origRefId);
		}
		if (origRefId == null || !ObjectId.isValid(origRefId)) {
			return null;
		}
		//get or create new ref
		if (references.containsKey(origRefId)) {
			newRefId = references.get(origRefId);
		} else {
			newRefId = new ObjectId().toHexString();
			references.put(origRefId, newRefId);
		}
		//build new value
		if (entityType.equals(resources)) {
			newRefId = FileResolver.RESOURCE_PREFIX + references.get(origRefId);
		}
		if (returnType.equals(DynamicValue.class) && !((DynamicValue<?>) value).isDynamic()) {
			newValue = new DynamicValue<String> (newRefId);
		} else if (returnType.equals(String.class)) {
			newValue = newRefId;
		} else if (returnType.equals(ObjectId.class)) {
			newValue = new ObjectId(newRefId);
		}
		return newValue;
	}
	
	private Object getNewValue(Object value, Class<?> returnType, Map<String, String> references, String entityType) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (value instanceof Collection) {
			Collection<Object> c = (Collection<Object>) value;
			Collection<Object> newCol = c.getClass().getConstructor().newInstance();
			c.forEach(e-> {
				newCol.add(getNewValue_(e,e.getClass(),references, entityType));
			});
			return newCol;
		} else {
			return getNewValue_(value,returnType,references, entityType);
		}
		
	}
	
}
