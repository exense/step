package step.plugins.maven;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignore because the actual response contains much more fields than we need in maven-plugin
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceUploadResponseDto {

	private ResourceDto resource;

	public ResourceDto getResource() {
		return resource;
	}

	public void setResource(ResourceDto resource) {
		this.resource = resource;
	}

	// ignore because the actual response contains much more fields than we need in maven-plugin
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ResourceDto {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
