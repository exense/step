package step.functions.packages;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.*;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.functions.manager.FunctionManager;
import step.functions.packages.handlers.JavaFunctionPackageHandler;
import step.functions.packages.handlers.RepositoryArtifactFunctionPackageHandler;
import step.functions.plugin.FunctionControllerPlugin;
import step.plugins.java.GeneralScriptFunctionControllerPlugin;
import step.plugins.screentemplating.*;
import step.resources.ResourceManager;
import step.resources.ResourceManagerControllerPlugin;

import java.io.IOException;
import java.util.List;

@Plugin(dependencies= {ObjectHookControllerPlugin.class, ResourceManagerControllerPlugin.class, FunctionControllerPlugin.class, ScreenTemplatePlugin.class, GeneralScriptFunctionControllerPlugin.class})
public class FunctionPackagePlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPackagePlugin.class);
	private FunctionPackageManager packageManager;
	private FunctionManager functionManager;
	private FunctionPackageAccessor packageAccessor;

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		FileResolver fileResolver = context.getFileResolver();
		ResourceManager resourceManager = context.getResourceManager();

		packageAccessor = new FunctionPackageAccessorImpl(
				context.getCollectionFactory().getCollection("functionPackage", FunctionPackage.class));

		Configuration configuration = context.getConfiguration();
		functionManager = context.get(FunctionManager.class);
		ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
		packageManager = new FunctionPackageManager(packageAccessor, functionManager, resourceManager, fileResolver, configuration, objectHookRegistry);
		packageManager.registerFunctionPackageHandler(new JavaFunctionPackageHandler(fileResolver, configuration));
		packageManager.registerFunctionPackageHandler(new RepositoryArtifactFunctionPackageHandler(resourceManager, fileResolver, configuration));

		packageManager.start();

		context.put(FunctionPackageManager.class, packageManager);

		Collection<FunctionPackage> functionPackageCollection = context.getCollectionFactory().getCollection("functionPackage", FunctionPackage.class);
		Table<FunctionPackage> collection = new Table<>(functionPackageCollection, "kw-read", true);
		context.get(TableRegistry.class).register("functionPackage", collection);

		context.getServiceRegistrationCallback().registerService(FunctionPackageServices.class);

		context.getEntityManager().register(new FunctionPackageEntity(FunctionPackageEntity.entityName, packageAccessor, context));

		//registerWebapp(context, "/functionpackages/");
	}



	@Override
	public void initializeData(GlobalContext context) throws Exception {

		Configuration configuration = context.getConfiguration();
		String embeddedPackageFolder = configuration.getProperty("plugins.FunctionPackagePlugin.embeddedpackages.folder");
		if(embeddedPackageFolder != null) {
			EmbeddedFunctionPackageImporter embeddedFunctionPackageImporter = new EmbeddedFunctionPackageImporter(packageAccessor, packageManager);
			embeddedFunctionPackageImporter.importEmbeddedFunctionPackages(embeddedPackageFolder);
		}
	}

	@Override
	public void serverStop(GlobalContext context) {
		try {
			packageManager.close();
		} catch (IOException e) {
			logger.error("Error while closing package manager", e);
		}
	}

	@Override
	public AbstractWebPlugin getWebPlugin() {
		Ng2WebPlugin webPlugin = new Ng2WebPlugin();
		webPlugin.setName("functionPackages");
		webPlugin.setEntryPoint("default");
		return webPlugin;
	}
}
