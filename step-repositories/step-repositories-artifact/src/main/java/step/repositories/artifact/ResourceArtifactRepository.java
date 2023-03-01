package step.repositories.artifact;

import step.core.plans.PlanAccessor;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionFileHandle;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ResourceArtifactRepository extends AbstractArtifactRepository {

	protected static final String PARAM_RESOURCE_ID = "resourceId";
	protected static final String PARAM_LIB_RESOURCE_ID = "libResourceId";

	private final ResourceManager resourceManager;

	public ResourceArtifactRepository(PlanAccessor planAccessor, ResourceManager resourceManager) {
		super(Set.of(PARAM_RESOURCE_ID), planAccessor); // artifact_id = resource_id
		this.resourceManager = resourceManager;
	}

	@Override
	protected File getLibraries(Map<String, String> repositoryParameters) {
	    String resourceId = repositoryParameters.get(PARAM_LIB_RESOURCE_ID);
		if(resourceId != null){
			return getResourceFile(resourceId);
		} else {
			return null;
		}
	}

	@Override
	protected File getArtifact(Map<String, String> repositoryParameters) {
		String resourceId = getMandatoryRepositoryParameter(repositoryParameters, PARAM_RESOURCE_ID);
		return getResourceFile(resourceId);
	}

	@Override
	protected String resolveArtifactName(Map<String, String> repositoryParameters) {
		return AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_RESOURCE_ID);
	}

	private File getResourceFile(String resourceId) {
		ResourceRevisionFileHandle resourceContent = resourceManager.getResourceFile(resourceId);
		if(resourceContent == null){
			throw new RuntimeException("Resource not found by id: " + resourceId);
		}
		return resourceContent.getResourceFile();
	}
}
