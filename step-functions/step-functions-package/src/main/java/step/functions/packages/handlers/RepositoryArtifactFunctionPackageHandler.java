package step.functions.packages.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.functions.Function;
import step.functions.packages.FunctionPackage;
import step.resources.Resource;
import step.resources.ResourceManager;

public class RepositoryArtifactFunctionPackageHandler extends JavaFunctionPackageHandler {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryArtifactFunctionPackageHandler.class);
	
	private ResourceManager resourceManager;
	private RepositorySystem repositorySystem;
	private List<RemoteRepository> repositories;
	private LocalRepository localRepository;

	public RepositoryArtifactFunctionPackageHandler(ResourceManager resourceManager, FileResolver fileResolver, Configuration config) {
		super(fileResolver, config);

		this.resourceManager = resourceManager;
		repositorySystem = newRepositorySystem();
		localRepository = new LocalRepository(
				config.getProperty("plugins.FunctionPackagePlugin.maven.localrepository", "maven"));

		parseConfigurationAndCreateRepositoryList(config);
	}

	private void parseConfigurationAndCreateRepositoryList(Configuration config) {
		Map<String, Map<String, String>> repositoriesProperties = new HashMap<>();
		config.getPropertyNames().forEach(p -> {
			String key = p.toString();
			if (key.startsWith("plugins.FunctionPackagePlugin.maven.repository.")) {
				String repositoryId = key.replace("plugins.FunctionPackagePlugin.maven.repository.", "")
						.split("\\.")[0];
				Map<String, String> repositoryProperties = repositoriesProperties.computeIfAbsent(repositoryId,
						k -> new HashMap<>());
				String propertyKey = key.replace("plugins.FunctionPackagePlugin.maven.repository." + repositoryId + ".",
						"");
				repositoryProperties.put(propertyKey, config.getProperty(key));
			}
		});

		repositories = new ArrayList<>();
		repositoriesProperties.entrySet().forEach(e -> {
			Map<String, String> properties = e.getValue();
			String url = properties.get("url");
			String username = properties.get("username");
			String password = properties.get("password");

			RemoteRepository.Builder builder = new RemoteRepository.Builder(e.getKey(), "default", url);
			if (username != null) {
				Authentication authentication = new AuthenticationBuilder().addUsername(username).addPassword(password)
						.build();
				builder.setAuthentication(authentication);
			}
			
			String proxyType = properties.get("proxy.type");
			if(proxyType!=null) {
				String proxyHost = properties.get("proxy.host");
				int proxyPort = Integer.parseInt(properties.get("proxy.port"));
				String proxyUsername = properties.get("proxy.username");
				String proxyPassword = properties.get("proxy.password");
				Authentication authentication = null;
				if(proxyUsername !=null) {
					authentication = new AuthenticationBuilder().addUsername(proxyUsername).addPassword(proxyPassword)
							.build();
				}
				Proxy proxy = new Proxy(proxyType, proxyHost, proxyPort, authentication);
				builder.setProxy(proxy);
			}
			
			RemoteRepository remoteRepository = builder.build();
			repositories.add(remoteRepository);

		});
	}

	@Override
	public boolean isValidForPackage(FunctionPackage functionPackag) {
		return functionPackag.getPackageLocation().contains("<dependency>");
	}

	public List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview) throws Exception {
		String packageLocation = functionPackage.getPackageLocation();
		XmlMapper mapper = new XmlMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Dependency dependency = mapper.readValue(packageLocation, Dependency.class);
		Artifact artifact = new DefaultArtifact(dependency.groupId, dependency.artifactId, dependency.classifier, "jar", dependency.version);
		File artifactFile = getArtifactByAether(artifact);
		
		// TODO we're changing the package location to the path of the resolved file. 
		// This works because the function package gets saved by the FunctionPackageManager aftewards.
		// However this isn't a clean way of doing this. Change the API to allow changes to the FunctionPackage
		if(preview) {
			functionPackage.setPackageLocation(artifactFile.getAbsolutePath());
		} else {
			// if it's not a preview, we save the temporary file as resource
			Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, new FileInputStream(artifactFile), artifactFile.getName(), false, null);
			functionPackage.setPackageLocation(FileResolver.RESOURCE_PREFIX+resource.getId().toString());
			functionPackage.addAttribute(AbstractOrganizableObject.VERSION, artifact.getVersion());
		}
		List<Function> functions = super.buildFunctions(functionPackage, preview);
		return functions;
	}

	protected static class Dependency {
		
	    private String groupId;
	    private String artifactId;
	    private String version;
	    private String classifier;
	    
		public String getGroupId() {
			return groupId;
		}
		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}
		public String getArtifactId() {
			return artifactId;
		}
		public void setArtifactId(String artifactId) {
			this.artifactId = artifactId;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public String getClassifier() {
			return classifier;
		}
		public void setClassifier(String classifier) {
			this.classifier = classifier;
		}
	}

	private RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		return locator.getService(RepositorySystem.class);
	}

	private RepositorySystemSession newSession() {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
		return session;
	}

	private File getArtifactByAether(final Artifact artifact) throws IOException, ArtifactResolutionException {
		RepositorySystemSession session = newSession();
		ArtifactRequest artifactRequest = new ArtifactRequest();
		artifactRequest.setArtifact(artifact);
		artifactRequest.setRepositories(repositories);
		File result;

		ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
		Artifact resolvedArtifact = artifactResult.getArtifact();
		if (resolvedArtifact != null) {
			result = resolvedArtifact.getFile();
		} else {
			artifactResult.getExceptions().forEach(e->logger.error("Error while resolving artifact "+artifact.toString(), e));
			throw new RuntimeException("The resolution of the artifact failed. See the logs for more details");
		}
		
		return result;
	}
}
