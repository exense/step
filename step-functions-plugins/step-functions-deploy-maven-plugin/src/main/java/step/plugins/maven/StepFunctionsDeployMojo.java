package step.plugins.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Mojo(name = "step-functions-deploy")
public class StepFunctionsDeployMojo extends AbstractMojo {

	@Parameter(property = "step-functions-deploy.url", required = true)
	private String url;

	@Parameter(property = "step-functions-deploy.group-id", required = true)
	private String groupId;

	@Parameter(property = "step-functions-deploy.artifact-id", required = true)
	private String artifactId;

	@Parameter(property = "step-functions-deploy.artifact-version", required = true)
	private String artifactVersion;

	@Parameter(property = "step-functions-deploy.description", required = false, defaultValue = "")
	private String description;

	@Parameter(property = "step-functions-deploy.user-id", required = false, defaultValue = "admin")
	private String userId;

	@Parameter(property = "step-functions-deploy.custom-parameters", required = false)
	private Map<String, String> customParameters;

	// TODO: security token?

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private final ObjectMapper objectMapper = new ObjectMapper();


	public void execute() throws MojoExecutionException {
		try {
			String fullUri = prepareFullUri();
			getLog().info("Sending the package to " + fullUri);

			ExecutionParametersDto bodyDto = new ExecutionParametersDto();
			bodyDto.setMode("RUN");
			bodyDto.setUserID(userId);
			bodyDto.setDescription(description);
			bodyDto.setCustomParameters(customParameters);

			RepositoryObjectReferenceDto repoRef = new RepositoryObjectReferenceDto();
			repoRef.setRepositoryID("Artifact");

			HashMap<String, String> repoParams = new HashMap<>();
			repoParams.put("groupId", groupId);
			repoParams.put("artifactId", artifactId);
			repoParams.put("version", artifactVersion);

			repoRef.setRepositoryParameters(repoParams);

			bodyDto.setRepositoryObject(repoRef);

			String bodyString = objectMapper.writeValueAsString(bodyDto);
			getLog().info("Request body: " + bodyString);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(fullUri))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(bodyString))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new MojoExecutionException("Bad response status from step: " + response.statusCode());
			}

			getLog().info("Execution has been registered in step: " + response.body());

		} catch (URISyntaxException | IOException | InterruptedException e) {
			getLog().error("Unable to deploy the package to step", e);
			throw new MojoExecutionException("Unable to deploy the package to step", e);
		}
	}

	private String prepareFullUri() {
		String fullUri = url;
		if(!url.endsWith("/")){
			fullUri += "/";
		}
		fullUri += "rest/executions/start";
		return fullUri;
	}
}