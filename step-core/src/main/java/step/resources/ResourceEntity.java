package step.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.DependencyTreeVisitorHook;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;
import step.core.entities.EntityManager;

import java.util.Optional;

public class ResourceEntity extends Entity<Resource, Accessor<Resource>> {

	private final Logger logger = LoggerFactory.getLogger(ResourceEntity.class);

	public ResourceEntity(Accessor<Resource> accessor, EntityManager entityManager) {
		super(EntityConstants.resources, accessor, Resource.class);
		entityManager.addDependencyTreeVisitorHook(new DependencyTreeVisitorHook() {
			
			@Override
			public void onVisitEntity(Object t, EntityTreeVisitorContext visitorContext) {
				if(t instanceof Resource) {
					//We always need to export the revision id set as current revision id of a resource
					String revisionId = ((Resource) t).getCurrentRevisionId().toString();
					visitorContext.visitEntity(EntityConstants.resourceRevisions, revisionId);
				}
			}
		});
	}

	@Override
	public String resolveAtomicReference(Object atomicReferene, EntityTreeVisitorContext visitorContext) {
		if(atomicReferene != null) {
			if(atomicReferene instanceof String) {
				String revisionId = resolveRevisionId(atomicReferene);
				Optional.ofNullable(revisionId).ifPresent(rv -> visitorContext.visitEntity(EntityConstants.resourceRevisions, rv));
				return resolveResourceId(atomicReferene);
			} else if (atomicReferene instanceof DynamicValue<?>) {
				DynamicValue<?> dynamicValue = (DynamicValue<?>) atomicReferene;
				try {
					Object value = dynamicValue.get();
					Optional.ofNullable(resolveRevisionId(value)).ifPresent(rv -> visitorContext.visitEntity(EntityConstants.resourceRevisions, rv));
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
				newValue = newPathForResourceId(atomicReference, resolvedEntityId, visitorContext);
			} else if (atomicReference instanceof DynamicValue<?>) {
				String newPathForResourceId = newPathForResourceId(((DynamicValue<?>) atomicReference).get(),
						resolvedEntityId, visitorContext);
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
			if(FileResolver.isResource(path)) {
				return FileResolver.resolveResourceId(path);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private String resolveRevisionId(Object valueToResolve) {
		if(valueToResolve instanceof String) {
			String path = (String) valueToResolve;
			if(FileResolver.isResourceRevision(path)) {
				return FileResolver.resolveRevisionId(path);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	private String newPathForResourceId(Object valueToResolve, String newEntityId, EntityTreeVisitorContext visitorContext) {
		if(valueToResolve instanceof String) {
			String path = (String) valueToResolve;
			if (FileResolver.isResourceRevision(path)) {
				//Get the new revision ID
				String revisionId = visitorContext.getVisitor().onResolvedEntityId(EntityConstants.resourceRevisions, FileResolver.resolveRevisionId(path));
				return FileResolver.createPathForResourceAndRevisionId(newEntityId, revisionId);
			} else if(FileResolver.isResource(path)) {
				return FileResolver.createPathForResourceId(newEntityId);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

}
