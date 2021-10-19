package step.functions.packages;

import step.core.GlobalContext;
import step.core.entities.Entity;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;
import step.core.entities.EntityManager;
import step.core.entities.DependencyTreeVisitorHook;
import step.functions.Function;

public class FunctionPackageEntity extends Entity<FunctionPackage,FunctionPackageAccessor> {

	public static final String FUNCTION_PACKAGE_ID = "functionPackageId";
	public static final String entityName = "functionPackage";
	
	public FunctionPackageEntity(String name, FunctionPackageAccessor accessor, GlobalContext context) {
		super(name, accessor, FunctionPackage.class);

		//Add hooks for function entity
		EntityManager entityManager = context.getEntityManager();
		Entity<?, ?> functionEntity = entityManager.getEntityByName(EntityManager.functions);
		functionEntity.addDependencyTreeVisitorHook(functionReferencesHook(entityManager));
	}

	private static DependencyTreeVisitorHook functionReferencesHook(EntityManager em) {
		return new DependencyTreeVisitorHook() {
			@Override
			public void onVisitEntity(Object o, EntityTreeVisitorContext context) {
				if (o instanceof Function) {
					Function f = (Function) o;
					String id = (String) f.getCustomField(FUNCTION_PACKAGE_ID);
					if (id != null) {
						if(context.isRecursive()) {
							context.visitEntity(entityName, id);
						}
						
						String newEntityId = context.resolvedEntityId(entityName, id);
						if(newEntityId != null) {
							f.addCustomField(FUNCTION_PACKAGE_ID, newEntityId);
						}
					}
				}
			}
		};
	}
}
