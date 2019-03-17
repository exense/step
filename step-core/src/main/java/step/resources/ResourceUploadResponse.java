package step.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ResourceUploadResponse {
	
	protected Resource resource;
	protected List<Resource> similarResources;

	public ResourceUploadResponse() {
		super();
	}

	public ResourceUploadResponse(Resource resource, List<Resource> similarResources) {
		super();
		this.resource = resource;
		this.similarResources = similarResources;
	}
	
	@JsonIgnore
	public String getResourceId() {
		return resource.getId().toString();
	}

	public Resource getResource() {
		return resource;
	}
	
	public void setResource(Resource resource) {
		this.resource = resource;
	}
	
	public List<Resource> getSimilarResources() {
		return similarResources;
	}

	public void setSimilarResources(List<Resource> similarResources) {
		this.similarResources = similarResources;
	}
}