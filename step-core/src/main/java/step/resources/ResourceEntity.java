package step.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.DependencyTreeVisitorHook;
import step.core.entities.Entity;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;
import step.core.entities.EntityManager;

public class ResourceEntity extends Entity<Resource, Accessor<Resource>> {

	private final Logger logger = LoggerFactory.getLogger(ResourceEntity.class);

	private final FileResolver fileResolver;
	
	public ResourceEntity(Accessor<Resource> accessor, ResourceManager resourceManager, FileResolver fileResolver, EntityManager entityManager) {
		super(EntityManager.resources, accessor, Resource.class);
		this.fileResolver = fileResolver;
		entityManager.addDependencyTreeVisitorHook(new DependencyTreeVisitorHook() {
			
			@Override
			public void onVisitEntity(Object t, EntityTreeVisitorContext visitorContext) {
				if(visitorContext.isRecursive() && t instanceof Resource) {
					String revisionId = resourceManager.getResourceRevisionByResourceId(((Resource) t).getId().toString()).getId().toHexString();
					visitorContext.visitEntity(EntityManager.resourceRevisions, revisionId);
				}
			}
		});
	}

	@Override
	public String resolveAtomicReference(Object atomicReferene, EntityTreeVisitorContext visitorContext) {
		if(atomicReferene != null) {
			if(atomicReferene instanceof String) {
				return resolveResourceId(atomicReferene);
			} else if (atomicReferene instanceof DynamicValue<?>) {
				DynamicValue<?> dynamicValue = (DynamicValue<?>) atomicReferene;
				try {
					Object value = dynamicValue.get();
					return resolveResourceId(value);
				} catch (Exception e) {
					String warningMessage = "Unable to resolve resource referenced by dynamic expression '" + dynamicValue.getExpression() + "'.";
					if (logger.isDebugEnabled()) {
						logger.debug(warningMessage, e);
					} else {
						logger.warn(warningMessage + " Enable debug to get full error message.");
					}
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Object updateAtomicReference(Object atomicReference, String resolvedEntityId,
			EntityTreeVisitorContext visitorContext) {
		Object newValue = null;
		if (atomicReference != null) {
			if (atomicReference instanceof String) {
				newValue = newPathForResourceId(atomicReference, resolvedEntityId);
			} else if (atomicReference instanceof DynamicValue<?>) {
				String newPathForResourceId = newPathForResourceId(((DynamicValue<?>) atomicReference).get(),
						resolvedEntityId);
				if (newPathForResourceId != null) {
					newValue = new DynamicValue<>(newPathForResourceId);
				}
			}
		}
		return newValue;
	}

	private String resolveResourceId(Object valueToResolve) {
		if(valueToResolve instanceof String) {
			String path = (String) valueToResolve;
			if(fileResolver.isResource(path)) {
				return fileResolver.resolveResourceId(path);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	private String newPathForResourceId(Object valueToResolve, String newEntityId) {
		if(valueToResolve instanceof String) {
			String path = (String) valueToResolve;
			if(fileResolver.isResource(path)) {
				return fileResolver.createPathForResourceId(newEntityId);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

}
