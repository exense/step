package step.functions.packages;

import java.util.Map;
import java.util.function.BiConsumer;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.entities.EntityReferencesMap;
import step.core.entities.ResolveReferencesHook;
import step.core.objectenricher.ObjectPredicate;
import step.functions.Function;

public class FunctionPackageEntity extends Entity<FunctionPackage,FunctionPackageAccessor> {

	public static final String entityName = "functionPackage";
	
	public FunctionPackageEntity(String name, FunctionPackageAccessor accessor, GlobalContext context) {
		super(name, accessor, FunctionPackage.class);
		this.getReferencesHook().add(functionPackageReferencesHook(context.getEntityManager()));
		this.getUpdateReferencesHook().add(functionPackageUpdateReferencesHook());

		//Add hooks for function entity
		context.getEntityManager().getEntityByName(EntityManager.functions).getReferencesHook().add(functionReferencesHook(context.getEntityManager()));
		context.getEntityManager().getEntityByName(EntityManager.functions).getUpdateReferencesHook().add(functionUpdateReferencesHook());
	}

	private static ResolveReferencesHook functionPackageReferencesHook(EntityManager em) {
		return new ResolveReferencesHook(em) {
			@Override
			public void accept(Object o, ObjectPredicate objectPredicate, EntityReferencesMap references) {
				if (o instanceof FunctionPackage) {
					FunctionPackage fp = (FunctionPackage) o;
					String id = (String) fp.getCustomField("resourceId");
					if (id != null) {
						boolean added = references.addElementTo(EntityManager.resources, id);
						if (added) {
							em.getAllEntities(EntityManager.resources, id, objectPredicate, references);
						}
					}
				}
			}
		};
	}

	private static BiConsumer<Object, Map<String, String>> functionPackageUpdateReferencesHook() {
		return new BiConsumer<Object, Map<String, String>>() {
			@Override
			public void accept(Object o,Map<String, String> references) {
				if (o instanceof FunctionPackage) {
					FunctionPackage fp = (FunctionPackage) o;

					String origRefId = (String) fp.getCustomField("resourceId");
					if (origRefId!=null) {
						String newRefId;
						//get new ref
						if (references.containsKey(origRefId)) {
							newRefId = references.get(origRefId);
						} else {
							newRefId = new ObjectId().toHexString();
							references.put(origRefId, newRefId);
						}
						fp.addCustomField("resourceId", newRefId);
					}
				}
			}
		};
	}

	private static ResolveReferencesHook functionReferencesHook(EntityManager em) {
		return new ResolveReferencesHook(em) {
			@Override
			public void accept(Object o, ObjectPredicate objectPredicate, EntityReferencesMap references) {
				if (o instanceof Function) {
					Function f = (Function) o;
					String id = (String) f.getCustomField("functionPackageId");
					if (id != null) {
						boolean added = references.addElementTo(FunctionPackageEntity.entityName, id);
						if (added) {
							em.getAllEntities(FunctionPackageEntity.entityName, id, objectPredicate, references);
						}
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

					String origRefId = (String) f.getCustomField("functionPackageId");
					if (origRefId != null && ObjectId.isValid(origRefId)) {
						String newRefId;
						//get new ref
						if (references.containsKey(origRefId)) {
							newRefId = references.get(origRefId);
						} else {
							newRefId = new ObjectId().toHexString();
							references.put(origRefId, newRefId);
						}
						f.addCustomField("functionPackageId", newRefId);
					}
				}
			}
		};
	}
}
