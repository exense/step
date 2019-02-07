package step.resources;

import java.util.List;

@SuppressWarnings("serial")
public class SimilarResourceExistingException extends Exception {

	protected Resource resource;
	protected List<Resource> similarResources;

	public SimilarResourceExistingException(Resource resource, List<Resource> similarResources) {
		super();
		this.resource = resource;
		this.similarResources = similarResources;
	}

	public List<Resource> getSimilarResources() {
		return similarResources;
	}

	public Resource getResource() {
		return resource;
	}
}
