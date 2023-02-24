package step.plugins.maven;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Mojo(name = "upload-keywords-package")
public class UploadKeywordsPackageMojo extends AbstractStepPluginMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File fileToUpload = getPackagedTarget();
		HttpPost request = prepareKeywordsUploadRequest(fileToUpload);
		try {
			String stringResponse = httpClient.execute(request, new BasicHttpClientResponseHandler());
			ResourceUploadResponseDto response = objectMapper.readValue(stringResponse, ResourceUploadResponseDto.class);
			if (response.getResource() == null || response.getResource().getId() == null) {
				throw new MojoExecutionException("Unable to upload keywords package to step. Resource id is null.");
			}
			getLog().info("Keyword package uploaded: " + response.getResource().getId());
		} catch (IOException e) {
			logAndThrow("Unable to upload keywords package to step", e);
		}
	}

	private HttpPost prepareKeywordsUploadRequest(File fileToUpload) throws MojoExecutionException {
		HttpPost post = null;
		try {
			post = new HttpPost(
					new URIBuilder(prepareUploadServiceUri())
							.addParameter("type", "functions")
							.addParameter("duplicateCheck", "false")
							.addParameter("directory", "false")
							.build()
			);

			final FileBody fileBody = new FileBody(fileToUpload, ContentType.DEFAULT_BINARY);
			final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addPart("file", fileBody);

			post.setEntity(builder.build());

			if (getAuthToken() != null && !getAuthToken().isEmpty()) {
				post.addHeader("Authorization", "Bearer " + getAuthToken());
			}
		} catch (URISyntaxException e) {
			logAndThrow("Unable to upload keywords package to step", e);
		}
		return post;
	}

	private File getPackagedTarget() {
		Artifact artifact = project.getArtifact();
		getLog().info("Artifact to be uploaded to step: "
				+ artifact.getGroupId() + ":"
				+ artifact.getArtifactId() + ":"
				+ artifact.getClassifier() + ":"
				+ artifact.getVersion()
		);
		return artifact.getFile();
	}

	protected String prepareUploadServiceUri() {
		String fullUri = getUrl();
		if (!getUrl().endsWith("/")) {
			fullUri += "/";
		}
		fullUri += "rest/resources/content";
		return fullUri;
	}
}
