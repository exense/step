package step.functions.packages;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.manager.FunctionManagerImpl;
import step.functions.packages.handlers.JavaFunctionPackageHandler;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.MockedGridClientImpl;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

public class EmbeddedFunctionPackageImporterTest {

	private FunctionAccessor functionAccessor;

	@Test
	public void test() {
		Configuration configuration = new Configuration();
		ResourceManager resourceManager = new LocalResourceManagerImpl();
		FileResolver fileResolver = new FileResolver(resourceManager);

		functionAccessor = new InMemoryFunctionAccessorImpl();
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(fileResolver, new MockedGridClientImpl(), new ObjectHookRegistry());
		functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType(configuration));
		FunctionManagerImpl functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
		
		FunctionPackageAccessor functionPackageAccessor = new InMemoryFunctionPackageAccessorImpl();
		FunctionPackageManager functionPackageManager = new FunctionPackageManager(functionPackageAccessor,
				functionManager, resourceManager, fileResolver, configuration, new ObjectHookRegistry());
		functionPackageManager.registerFunctionPackageHandler(new JavaFunctionPackageHandler(fileResolver, configuration));
		functionPackageManager.registerAttributeResolver("attribute1", v -> "value1");
		
		EmbeddedFunctionPackageImporter embeddedFunctionPackageImporter = new EmbeddedFunctionPackageImporter(functionPackageAccessor, functionPackageManager);
		File folder = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "");
		List<String> ids = embeddedFunctionPackageImporter.importEmbeddedFunctionPackages(folder.getAbsolutePath());
		assertEquals(2, ids.size());

		FunctionPackage functionPackage = functionPackageManager.getFunctionPackage(ids.get(0));
		boolean isLocalPackage = functionPackage.getPackageLocation().contains("local");
		assertPackage(functionPackage, isLocalPackage);

		functionPackage = functionPackageManager.getFunctionPackage(ids.get(1));
		isLocalPackage = functionPackage.getPackageLocation().contains("local");
		assertPackage(functionPackage, isLocalPackage);


		List<String> ids2 = embeddedFunctionPackageImporter.importEmbeddedFunctionPackages(folder.getAbsolutePath());
		// assert that the function package has been updated and thus the id kept
		assertEquals(ids, ids2);
	}

	private void assertPackage(FunctionPackage functionPackage, boolean isLocal) {
		// Assert that the function package contains the attributes defined in the meta file
		assertEquals("value1", functionPackage.getAttribute("attribute1"));
		assertEquals("value2", functionPackage.getAttribute("attribute2"));
		assertEquals("Äöüßêï", functionPackage.getAttribute("attributeI18n"));

		List<ObjectId> functionIDs = functionPackage.getFunctions();
		Assert.assertEquals(5, functionIDs.size());
		functionIDs.forEach(f->{
			GeneralScriptFunction function = (GeneralScriptFunction) functionAccessor.get(f);
			//Routing criteria are direclty defined for the keyword MyKeywordWithRoutingCriteria that takes precedence over the package local setting
			boolean localExpected = (isLocal && !"MyKeywordWithRoutingCriteria".equals(function.getAttribute(AbstractOrganizableObject.NAME))) ||
					(!isLocal && "MyKeywordWithRoutingToController".equals(function.getAttribute(AbstractOrganizableObject.NAME)));
			assertEquals(localExpected, function.isExecuteLocally());
		});
	}

}
