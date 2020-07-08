package step.core.entities;

import java.util.function.BiConsumer;

public class ResolveReferencesHook implements BiConsumer<Object, EntityReferencesMap> {

	protected EntityManager entityManager;
	
	
	
	public ResolveReferencesHook(EntityManager entityManager) {
		super();
		this.entityManager = entityManager;
	}



	@Override
	public void accept(Object t, EntityReferencesMap u) {
		throw new RuntimeException("Not implemented");
		
	}

}
