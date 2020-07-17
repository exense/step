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

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.Configuration;
import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.entities.Entity;
import step.core.execution.ExecutionContext;
import step.core.imports.GenericDBImporter;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.accessor.FunctionCRUDAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.execution.ConfigurableTokenLifecycleStrategy;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.services.FunctionServices;
import step.functions.services.GridServices;
import step.functions.type.FunctionTypeConfiguration;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.Grid;
import step.grid.GridImpl;
import step.grid.GridImpl.GridImplConfig;
import step.grid.client.GridClient;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.client.TokenLifecycleStrategy;
import step.grid.io.AgentErrorCode;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ResourceManagerControllerPlugin.class})
public class GridPlugin extends AbstractControllerPlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(GridPlugin.class);
	
	private static final String functionEntityName = "functions";

	private GridImpl grid;
	private GridClient client;
	
	private FunctionEditorRegistry editorRegistry;
	private FunctionTypeRegistry functionTypeRegistry;

	private FunctionCRUDAccessor functionAccessor;
	private FunctionManager functionManager;
	
	private FunctionExecutionService functionExecutionService;
	private FunctionRouter functionRouter;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		Configuration configuration = context.getConfiguration();
		
		Integer gridPort = configuration.getPropertyAsInteger("grid.port",8081);
		Integer tokenTTL = configuration.getPropertyAsInteger("grid.ttl",60000);
		
		String fileManagerPath = configuration.getProperty("grid.filemanager.path", "filemanager");
		
		GridImplConfig gridConfig = new GridImplConfig();
		gridConfig.setFileLastModificationCacheConcurrencyLevel(configuration.getPropertyAsInteger("grid.filemanager.cache.concurrencylevel", 4));
		gridConfig.setFileLastModificationCacheMaximumsize(configuration.getPropertyAsInteger("grid.filemanager.cache.maximumsize", 1000));
		gridConfig.setFileLastModificationCacheExpireAfter(configuration.getPropertyAsInteger("grid.filemanager.cache.expireafter.ms", 500));
		gridConfig.setTtl(tokenTTL);
		
		gridConfig.setTokenAffinityEvaluatorClass(configuration.getProperty("grid.tokens.affinityevaluator.classname"));
		Map<String, String> tokenAffinityEvaluatorProperties = configuration.getPropertyNames().stream().filter(p->(p instanceof String && p.toString().startsWith("grid.tokens.affinityevaluator")))
			.collect(Collectors.toMap(p->p.toString().replace("grid.tokens.affinityevaluator.", ""), p->configuration.getProperty(p.toString())));
		gridConfig.setTokenAffinityEvaluatorProperties(tokenAffinityEvaluatorProperties);
		
		grid = new GridImpl(new File(fileManagerPath), gridPort, gridConfig);
		grid.start();
		
		TokenLifecycleStrategy tokenLifecycleStrategy = getTokenLifecycleStrategy(configuration);
		
		GridClientConfiguration gridClientConfiguration = buildGridClientConfiguration(configuration);
		client = new LocalGridClientImpl(gridClientConfiguration, tokenLifecycleStrategy, grid);

		editorRegistry = new FunctionEditorRegistry();
		
		FunctionTypeConfiguration functionTypeConfiguration = new FunctionTypeConfiguration();
		functionTypeConfiguration.setFileResolverCacheConcurrencyLevel(configuration.getPropertyAsInteger("functions.fileresolver.cache.concurrencylevel", 4));
		functionTypeConfiguration.setFileResolverCacheMaximumsize(configuration.getPropertyAsInteger("functions.fileresolver.cache.maximumsize", 1000));
		functionTypeConfiguration.setFileResolverCacheExpireAfter(configuration.getPropertyAsInteger("functions.fileresolver.cache.expireafter.ms", 500));
		functionTypeRegistry = new FunctionTypeRegistryImpl(context.get(FileResolver.class), client, functionTypeConfiguration);

		functionAccessor = new FunctionAccessorImpl(context.getMongoClientSession());
		functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
		
		functionExecutionService = new FunctionExecutionServiceImpl(client, functionTypeRegistry, context.getDynamicBeanResolver());
		
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		functionRouter = new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);

		context.put(TokenLifecycleStrategy.class, tokenLifecycleStrategy);
		context.put(Grid.class, grid);
		context.put(GridImpl.class, grid);
		context.put(GridClient.class, client);
				
		context.put(FunctionAccessor.class, functionAccessor);
		context.getEntityManager().register(new Entity<Function, FunctionAccessorImpl>(
				GridPlugin.functionEntityName, (FunctionAccessorImpl) functionAccessor, Function.class, 
				new GenericDBImporter<Function,FunctionAccessorImpl>(context)));
		context.put(FunctionManager.class, functionManager);
		context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		
		context.put(FunctionEditorRegistry.class, editorRegistry);
		context.put(FunctionExecutionService.class, functionExecutionService);
		context.put(FunctionRouter.class, functionRouter);
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
		context.getServiceRegistrationCallback().registerService(FunctionServices.class);
		
		CollectionRegistry collectionRegistry = context.get(CollectionRegistry.class);
		MongoDatabase mongoDatabase = context.getMongoClientSession().getMongoDatabase();
		collectionRegistry.register("functions", new Collection(mongoDatabase, "functions", Function.class, true));
	}

	protected ConfigurableTokenLifecycleStrategy getTokenLifecycleStrategy(Configuration configuration) {
		return new ConfigurableTokenLifecycleStrategy(
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokenreleaseerror", true),
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokenreservationerror", true),
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokencallerror", true),
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.agenterror", true),
				Arrays.asList(configuration.getProperty("grid.client.token.lifecycle.remove.on.agenterrors", AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED.toString()).split(",")).stream().map(v->AgentErrorCode.valueOf(v)).collect(Collectors.toSet()));
	}

	protected GridClientConfiguration buildGridClientConfiguration(Configuration configuration) {
		GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
		gridClientConfiguration.setNoMatchExistsTimeout(configuration.getPropertyAsLong("grid.client.token.selection.nomatch.timeout.ms", gridClientConfiguration.getNoMatchExistsTimeout()));
		gridClientConfiguration.setMatchExistsTimeout(configuration.getPropertyAsLong("grid.client.token.selection.matchexist.timeout.ms", gridClientConfiguration.getMatchExistsTimeout()));
		gridClientConfiguration.setReadTimeoutOffset(configuration.getPropertyAsInteger("grid.client.token.call.readtimeout.offset.ms", gridClientConfiguration.getReadTimeoutOffset()));
		gridClientConfiguration.setReserveSessionTimeout(configuration.getPropertyAsInteger("grid.client.token.reserve.timeout.ms", gridClientConfiguration.getReserveSessionTimeout()));
		gridClientConfiguration.setReleaseSessionTimeout(configuration.getPropertyAsInteger("grid.client.token.release.timeout.ms", gridClientConfiguration.getReleaseSessionTimeout()));
		return gridClientConfiguration;
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new AbstractExecutionEnginePlugin() {
			@Override
			public void beforePlanImport(ExecutionContext context) {
				// Bindings needed for the execution
				boolean isolatedExecution = context.getExecutionParameters().isIsolatedExecution();
				if(isolatedExecution) {
					FunctionAccessor functionAccessor = new InMemoryFunctionAccessorImpl();
					FunctionExecutionService functionExecutionService;
					try {
						functionExecutionService = new FunctionExecutionServiceImpl(client, functionTypeRegistry, context.getDynamicBeanResolver());
					} catch (FunctionExecutionServiceException e) {
						throw new RuntimeException("Error while creating function execution service", e);
					}
					DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
					FunctionRouter functionRouter = new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);
					
					context.put(FunctionAccessor.class, functionAccessor);
					context.put(FunctionExecutionService.class, functionExecutionService);
					context.put(FunctionRouter.class, functionRouter);
				} else {
					context.put(FunctionAccessor.class, functionAccessor);
					context.put(FunctionExecutionService.class, functionExecutionService);
					context.put(FunctionRouter.class, functionRouter);
				}
			}
		};
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		if(client!=null) {
			client.close();
		}
		if(grid!=null) {
			try {
				grid.stop();
			} catch (Exception e) {
				logger.error("Error while stopping the grid server",e);
			}
		}
	}
}
