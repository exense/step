package step.core.entities;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;

public class EntityManager  {
	
	public final static String executions = "executions";
	public final static String plans = "plans";
	public static final String functions = "functions";
	public final static String reports = "reports";
	public final static String tasks = "tasks";
	public final static String users = "users";
	public final static String resources = "resources";
	
	private Map<String, Entity<?,?>> entities = new ConcurrentHashMap<String, Entity<?,?>>();
	
	public EntityManager register(Entity<?,?> entity) {
		entities.put(entity.getName(), entity);
		return this;
	}
	
	public Entity<?,?> getEntityByName(String entityName) {
		return entities.get(entityName);
	}

	public void getAllEntities (String entityName, String id, EntityReferencesMap references) {
		Entity<?, ?> entityByName = getEntityByName(entityName);
		CRUDAccessor<?> accessor = entityByName.getAccessor();
		AbstractIdentifiableObject a = accessor.get(id);
		if (a == null ) {
			throw new RuntimeException("Entity with id '" + id + "' could not be found in entities of type '" + entityName + "'");
		}
		if (entityName.equals(plans)) {
			AbstractArtefact root = ((Plan) a).getRoot();
			getArtefactReferences(root, references);
		}
	}
	
	private void getArtefactReferences(AbstractArtefact artefact, EntityReferencesMap references) {
		for (Field field : artefact.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(EntityReference.class)) {
				EntityReference eRef = field.getAnnotation(EntityReference.class);
				String entityType = eRef.type();
				try {
					field.setAccessible(true);
					String refId = (String) field.get(artefact);
					boolean added = references.addElementTo(entityType, refId);
					//avoid circular references issue
					if (added) { 
						getAllEntities(entityType, refId, references);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Export failed, unabled to get references by reflections",e);
				}
			}
		}
		artefact.getChildren().forEach(c->getArtefactReferences(c,references));
	}
	

}
