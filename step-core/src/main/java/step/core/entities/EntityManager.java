package step.core.entities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityManager  {
	
	public final static String executions = "executions";
	public final static String plans = "plans";
	public final static String reports = "reports";
	public final static String tasks = "tasks";
	public final static String users = "users";
	
	private Map<String, Entity<?,?>> entities = new ConcurrentHashMap<String, Entity<?,?>>();
	
	public EntityManager register(Entity<?,?> entity) {
		entities.put(entity.getName(), entity);
		return this;
	}
	
	public Entity<?,?> getEntityByName(String entityName) {
		return entities.get(entityName);
	}	
	

}
