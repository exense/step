package step.functions.packages;

import java.util.Map;
import java.util.function.BiConsumer;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.entities.Entity;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;
import step.core.entities.EntityManager;
import step.core.entities.ResolveReferencesHook;
import step.functions.Function;

public class FunctionPackageEntity extends Entity<FunctionPackage,FunctionPackageAccessor> {

	public static final String FUNCTION_PACKAGE_ID = "functionPackageId";

	public static final String entityName = "functionPackage";
	
	public FunctionPackageEntity(String name, FunctionPackageAccessor accessor, GlobalContext context) {
		super(name, accessor, FunctionPackage.class);

		//Add hooks for function entity
		context.getEntityManager().getEntityByName(EntityManager.functions).addReferencesHook(functionReferencesHook(context.getEntityManager()));
		context.getEntityManager().getEntityByName(EntityManager.functions).addUpdateReferencesHook(functionUpdateReferencesHook());
	}

	private static ResolveReferencesHook functionReferencesHook(EntityManager em) {
		return new ResolveReferencesHook() {
			@Override
			public void accept(Object o, EntityTreeVisitorContext context) {
				if (o instanceof Function) {
					Function f = (Function) o;
					String id = (String) f.getCustomField(FUNCTION_PACKAGE_ID);
					if (id != null) {
						context.visitEntity(FunctionPackageEntity.entityName, id);
					}
				}
			}
		};
	}

	private static BiConsumer<Object, Map<String, String>> functionUpdateReferencesHook() {
		return new BiConsumer<Object, Map<String, String>>() {
			@Override
			public void accept(Object o,Map<String, String> references) {
				if (o instanceof Function) {
					Function f = (Function) o;

					String origRefId = (String) f.getCustomField(FUNCTION_PACKAGE_ID);
					if (origRefId != null && ObjectId.isValid(origRefId)) {
						String newRefId;
						//get new ref
						if (references.containsKey(origRefId)) {
							newRefId = references.get(origRefId);
						} else {
							throw new RuntimeException("No reference found for "+origRefId);
						}
						f.addCustomField(FUNCTION_PACKAGE_ID, newRefId);
					}
				}
			}
		};
	}
}
