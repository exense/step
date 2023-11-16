package step.functions.packages.handlers;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.functions.packages.FunctionPackage;
import step.resources.LocalResourceManagerImpl;
import step.resources.Resource;

public class RepositoryArtifactFunctionPackageHandlerTest {

	private static final String TEMP_MAVEN_PATH = "temp";

	@After
	public void after() {
		FileHelper.deleteFolder(new File(TEMP_MAVEN_PATH));
	}
	
	@Test
	public void test() throws Exception {
		Configuration config = newConfigurationWithMavenCentral();
		
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		RepositoryArtifactFunctionPackageHandler handler = new RepositoryArtifactFunctionPackageHandler(resourceManager, new FileResolver(resourceManager), config);
		FunctionPackage functionPackage = new FunctionPackage();
		functionPackage.setPackageLocation(
				  "<dependency>"
				+ "<groupId>ch.exense.step</groupId>"
				+ "<artifactId>step-functions-plugins-java-handler-test</artifactId>"
				+ "<version>3.12.2</version>"
				+ "<scope>test</scope>"
				+ "</dependency>");
		boolean isValid = handler.isValidForPackage(functionPackage);
		Assert.assertTrue(isValid);
		List<Function> functions = handler.buildFunctions(functionPackage, true, null);
		Assert.assertEquals(2, functions.size());
		
		functionPackage.setPackageLocation(
				  "<dependency>"
				+ "<groupId>ch.exense.step</groupId>"
				+ "<artifactId>step-functions-plugins-java-handler-test</artifactId>"
				+ "<version>3.12.2</version>"
				+ "<scope>test</scope>"
				+ "</dependency>");
		functions = handler.buildFunctions(functionPackage, false, null);
		Assert.assertEquals(2, functions.size());
		
		functionPackage.setPackageLocation(
				  "<dependency>"
				+ "<groupId>invalid.group.id</groupId>"
				+ "<artifactId>invalid</artifactId>"
				+ "<version>0.0.0</version>"
				+ "</dependency>");
		Exception actualException = null;
		try {
			functions = handler.buildFunctions(functionPackage, false, null);
		} catch (Exception e) {
			actualException = e;
		}
		//not always getting the same error back:
		String expected = "Could not transfer artifact invalid.group.id:invalid:jar:0.0.0 from/to other (https://dummy):";
		Assert.assertTrue(actualException.getMessage().contains(expected));
	}
	
	@Test
	public void testWithObjectEnrichment() throws Exception {
		Configuration config = newConfigurationWithMavenCentral();
		
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		FileResolver fileResolver = new FileResolver(resourceManager);
		RepositoryArtifactFunctionPackageHandler handler = new RepositoryArtifactFunctionPackageHandler(resourceManager, fileResolver, config);
		FunctionPackage functionPackage = new FunctionPackage();
		functionPackage.setPackageLocation(
				  "<dependency>"
				+ "<groupId>ch.exense.step</groupId>"
				+ "<artifactId>step-functions-plugins-java-handler-test</artifactId>"
				+ "<version>3.12.2</version>"
				+ "<scope>test</scope>"
				+ "</dependency>");

		List<Function> functions = handler.buildFunctions(functionPackage, false, new ObjectEnricher() {
			
			@Override
			public void accept(EnricheableObject t) {
				t.addAttribute("attribute1", "attributeValue1");
			}
			
			@Override
			public Map<String, String> getAdditionalAttributes() {
				return null;
			}
		});
		Assert.assertEquals(2, functions.size());
		
		Resource resource = resourceManager.getResource(fileResolver.resolveResourceId(functionPackage.getPackageLocation()));
		Assert.assertEquals("attributeValue1", resource.getAttribute("attribute1"));
	}

	private Configuration newConfigurationWithMavenCentral() {
		Configuration config = new Configuration();
		config.putProperty("plugins.FunctionPackagePlugin.maven.localrepository",TEMP_MAVEN_PATH);
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.central.url", "https://repo1.maven.org/maven2/");
		
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.other.url", "https://dummy");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.other.proxy.type", "http");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.other.proxy.host", "http://myProxy");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.other.proxy.port", "8080");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.other.proxy.username", "user1");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.other.proxy.password", "pwd1");
		return config;
	}
	
	@Test
	public void testAuthentication() throws Exception {
		Configuration config = new Configuration();
		config.putProperty("plugins.FunctionPackagePlugin.maven.localrepository",TEMP_MAVEN_PATH);
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.central.url", "https://nexus-enterprise.exense.ch/repository/staging-maven/");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.central.username", "nexus-staging");
		config.putProperty("plugins.FunctionPackagePlugin.maven.repository.central.password", "100%STAGINGUser");
		
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		RepositoryArtifactFunctionPackageHandler handler = new RepositoryArtifactFunctionPackageHandler(resourceManager, new FileResolver(resourceManager), config);
		FunctionPackage functionPackage = new FunctionPackage();
		functionPackage.setPackageLocation("<dependency><groupId>donotdelete</groupId><artifactId>step-functions-package-test-artefact</artifactId><version>1.0.0</version></dependency>");
		boolean isValid = handler.isValidForPackage(functionPackage);
		Assert.assertTrue(isValid);
		List<Function> functions = handler.buildFunctions(functionPackage, true, null);
		Assert.assertEquals(6, functions.size());
	}

}
