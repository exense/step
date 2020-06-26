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
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.dynamicbeans.DynamicValue;

public class EntityManager  {
	
	private static Logger logger = LoggerFactory.getLogger(EntityManager.class);
	
	public final static String executions = "executions";
	public final static String plans = "plans";
	public static final String functions = "functions";
	public final static String reports = "reports";
	public final static String tasks = "tasks";
	public final static String users = "users";
	public final static String resources = "resources";
	public final static String recursive = "recursive";
	
	private Map<String, Entity<?,?>> entities = new ConcurrentHashMap<String, Entity<?,?>>();
	private AbstractContext context;
	Map<Class<?>,BeanInfo> beanInfoCache = new ConcurrentHashMap<>();
	
	public EntityManager(AbstractContext context) {
		this.context = context;
	}

	public EntityManager register(Entity<?,?> entity) {
		entities.put(entity.getName(), entity);
		return this;
	}
	
	public Entity<?,?> getEntityByName(String entityName) {
		return entities.get(entityName);
	}

	public void getAllEntities (String entityName, String id, EntityReferencesMap references) {
		Entity<?, ?> entityByName = getEntityByName(entityName);
		if (entityByName == null) {
			logger.error("Entities of type '" + entityName + "' are not supported");
			throw new RuntimeException("Entities of type '" + entityName + "' are not supported");
		}
		CRUDAccessor<?> accessor = entityByName.getAccessor();
		AbstractIdentifiableObject a = accessor.get(id);
		if (a == null ) {
			logger.error("Entities of type '" + entityName + "' are not supported");
			throw new RuntimeException("Entity with id '" + id + "' could not be found in entities of type '" + entityName + "'");
		}
		resolveReferences(a, references);
	}
	
	private void resolveReferences(Object object, EntityReferencesMap references) {
		FileResolver fileResolver = context.get(FileResolver.class);
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
								String refId = null;
								if (method.getReturnType().equals(DynamicValue.class) && value!=null) {
									Object dValue = ((DynamicValue<?>) value).get();
									refId = (dValue!=null) ? dValue.toString() : null;
									if (entityType.equals(resources)) {
										refId = fileResolver.resolveResourceId(refId);
									}
								} else if (method.getReturnType().equals(String.class)) {
									refId = (String) value;
								}
								if (refId != null && ObjectId.isValid(refId)) {
									boolean added = references.addElementTo(entityType, refId);
									//avoid circular references issue
									if (added) { 
										getAllEntities(entityType, refId, references);
									}
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

	public void updateReferences(Object object, Map<String, String> references) {
		
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
									c.forEach(o->updateReferences(o, references));
								} else {
									updateReferences(value, references);
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
			} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Import failed, unabled to updates references by reflections",e);
			}
		}
	}
	
	private Object getNewValue(Object value, Class<?> returnType, Map<String, String> references, String entityType) {
		FileResolver fileResolver = context.get(FileResolver.class);
		Object newValue = null;
		String origRefId = null;
		String newRefId = null;
		//Get original id
		if (returnType.equals(DynamicValue.class) && value != null && ((DynamicValue<?>) value).get() != null) {
			origRefId = ((DynamicValue<?>) value).get().toString();
			if (entityType.equals(resources)) {
				origRefId = fileResolver.resolveResourceId(origRefId);
			}
		} else if (returnType.equals(String.class)) {
			origRefId = (String) value;
		}
		if (origRefId == null || !ObjectId.isValid(origRefId)) {
			return null;
		}
		//get new ref
		if (references.containsKey(origRefId)) {
			newRefId = references.get(origRefId);
		} else {
			newRefId = new ObjectId().toHexString();
			references.put(origRefId, newRefId);
		}
		//build new value
		if (returnType.equals(DynamicValue.class)) {
			if (entityType.equals(resources)) {
				newRefId = FileResolver.RESOURCE_PREFIX + references.get(origRefId);
			}
			newValue = new DynamicValue<String> (newRefId);
		} else if (returnType.equals(String.class)) {
			newValue = newRefId;
		}
		return newValue;
	}
	
}
