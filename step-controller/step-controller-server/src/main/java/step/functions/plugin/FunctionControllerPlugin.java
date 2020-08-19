/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.functions.plugin;

import java.util.List;

import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.Configuration;
import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.controller.grid.GridPlugin;
import step.controller.grid.services.FunctionServices;
import step.core.GlobalContext;
import step.core.accessors.FunctionAccessorImpl;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.entities.Entity;
import step.core.imports.GenericDBImporter;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeConfiguration;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ScreenTemplatePlugin.class, GridPlugin.class, ResourceManagerControllerPlugin.class})
public class FunctionControllerPlugin extends AbstractControllerPlugin {

	private static final String COLLECTION_FUNCTIONS = "functions";
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		Configuration configuration = context.getConfiguration();
		
		GridClient gridClient = context.require(GridClient.class);
		FileResolver fileResolver = context.require(FileResolver.class);
		
		FunctionEditorRegistry editorRegistry = new FunctionEditorRegistry();
		
		FunctionTypeConfiguration functionTypeConfiguration = new FunctionTypeConfiguration();
		functionTypeConfiguration.setFileResolverCacheConcurrencyLevel(configuration.getPropertyAsInteger("functions.fileresolver.cache.concurrencylevel", 4));
		functionTypeConfiguration.setFileResolverCacheMaximumsize(configuration.getPropertyAsInteger("functions.fileresolver.cache.maximumsize", 1000));
		functionTypeConfiguration.setFileResolverCacheExpireAfter(configuration.getPropertyAsInteger("functions.fileresolver.cache.expireafter.ms", 500));
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(fileResolver, gridClient, functionTypeConfiguration);

		FunctionAccessor functionAccessor = new FunctionAccessorImpl(context.getMongoClientSession());
		FunctionManager functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
		FunctionExecutionService functionExecutionService = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
		
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		FunctionRouter functionRouter = new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);

		context.put(FunctionAccessor.class, functionAccessor);
		context.getEntityManager().register(new Entity<Function, FunctionAccessorImpl>(
				COLLECTION_FUNCTIONS, (FunctionAccessorImpl) functionAccessor, Function.class, 
				new GenericDBImporter<Function,FunctionAccessorImpl>(context)));
		context.put(FunctionManager.class, functionManager);
		context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		
		context.put(FunctionEditorRegistry.class, editorRegistry);
		context.put(FunctionExecutionService.class, functionExecutionService);
		context.put(FunctionRouter.class, functionRouter);
		
		context.getServiceRegistrationCallback().registerService(FunctionServices.class);
		
		CollectionRegistry collectionRegistry = context.get(CollectionRegistry.class);
		MongoDatabase mongoDatabase = context.getMongoClientSession().getMongoDatabase();
		collectionRegistry.register(COLLECTION_FUNCTIONS, new Collection<Function>(mongoDatabase, COLLECTION_FUNCTIONS, Function.class, true));
	}
	
	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputsIfNecessary(context);
	}

	protected void createScreenInputsIfNecessary(GlobalContext context) {
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> functionTableInputs = screenInputAccessor.getScreenInputsByScreenId("functionTable");
		functionTableInputs.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				input.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'function'\"/> <function-link function_=\"stBean\" st-options=\"stOptions\" />");
				screenInputAccessor.save(i);
			}
		});
	}
}
