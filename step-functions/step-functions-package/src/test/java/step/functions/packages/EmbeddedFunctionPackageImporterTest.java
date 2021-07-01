package step.functions.packages;

import java.io.File;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import step.attachments.FileResolver;
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

	@Test
	public void test() {
		Configuration configuration = new Configuration();
		ResourceManager resourceManager = new LocalResourceManagerImpl();
		FileResolver fileResolver = new FileResolver(resourceManager);

		FunctionAccessor functionAccessor = new InMemoryFunctionAccessorImpl();
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(fileResolver, new MockedGridClientImpl());
		functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType(configuration));
		FunctionManagerImpl functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
		
		FunctionPackageAccessor functionPackageAccessor = new InMemoryFunctionPackageAccessorImpl();
		FunctionPackageManager functionPackageManager = new FunctionPackageManager(functionPackageAccessor, functionManager, resourceManager, fileResolver, configuration);
		functionPackageManager.registerFunctionPackageHandler(new JavaFunctionPackageHandler(fileResolver, configuration));
		
		EmbeddedFunctionPackageImporter embeddedFunctionPackageImporter = new EmbeddedFunctionPackageImporter(functionManager, functionPackageAccessor, functionPackageManager);
		File folder = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "");
		List<String> ids = embeddedFunctionPackageImporter.importEmbeddedFunctionPackages(folder.getAbsolutePath());
		Assert.assertEquals(1, ids.size());
		String packageId = ids.get(0);
		FunctionPackage functionPackage = functionPackageManager.getFunctionPackage(packageId);
		List<ObjectId> functionIDs = functionPackage.getFunctions();
		Assert.assertEquals(2, functionIDs.size());
		functionIDs.forEach(f->{
			GeneralScriptFunction function = (GeneralScriptFunction) functionAccessor.get(f);
			Assert.assertEquals(true, function.isExecuteLocally());
		});

	}

}
