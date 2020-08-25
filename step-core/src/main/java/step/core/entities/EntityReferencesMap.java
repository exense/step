package step.core.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityReferencesMap {
	
	private Map<String, List<String>> references;
	Set<String> refNotFoundWarnings = new HashSet<String>();

	public EntityReferencesMap() {
		super();
		references = new HashMap<String, List<String>>();
	}
	
	public boolean addElementTo(String entityType, String entitiyId) {
		if (!references.containsKey(entityType)) {
			references.put(entityType, new LinkedList<String>());
		}
		if (!references.get(entityType).contains(entitiyId)) {
			references.get(entityType).add(entitiyId);
			return true;
		}
		return false;
	}
	
	public void addReferenceNotFoundWarning(String warning) {
		refNotFoundWarnings.add(warning);
	}
	
	public Set<String> getRefNotFoundWarnings(){
		return refNotFoundWarnings;
	}
	
	public List<String> getTypes() {
		return references.keySet().stream().sorted(new EntityTypeComparator()).collect(Collectors.toList());
	}
	
	public List<String> getReferencesByType(String entityType) {
		List<String> list = references.get(entityType);
		if (list != null && list.size() > 0) {
			Collections.reverse(list);
		}
		return list;
	}
	
	protected class EntityTypeComparator implements java.util.Comparator<String> {
		
		private Map<String,Integer> entityWeight;
		
		public EntityTypeComparator() {
			entityWeight = new HashMap<String,Integer>();
			entityWeight.put(EntityManager.resourceRevisions, 1);
			entityWeight.put(EntityManager.resources, 2);
			entityWeight.put(EntityManager.functions, 3);
			entityWeight.put(EntityManager.plans, 4);
		}

		@Override
		public int compare(String x, String y) {
			return Integer.compare(entityWeight.getOrDefault(x,100), entityWeight.getOrDefault(y, 100));
		}
		
	}



}
