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
package step.plugins.adaptergrid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.FileResolver;
import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.accessor.FunctionCRUDAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.routing.FunctionRouter;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.Grid;
import step.grid.client.GridClient;
import step.grid.client.GridClientConfiguration;
import step.grid.client.GridClientImpl;

@Plugin
public class GridPlugin extends AbstractPlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(GridPlugin.class);

	private Grid grid;
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
		
		grid = new Grid(gridPort, tokenTTL);
		grid.start();
		
		GridClientConfiguration gridClientConfiguration = buildGridClientConfiguration(configuration);
		client = new GridClientImpl(gridClientConfiguration, grid, grid);

		editorRegistry = new FunctionEditorRegistry();
		functionTypeRegistry = new FunctionTypeRegistryImpl(new FileResolver(context.getAttachmentManager()), grid);

		functionAccessor = new FunctionAccessorImpl(context.getMongoClientSession());
		functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
		
		functionExecutionService = new FunctionExecutionServiceImpl(client, functionAccessor, functionTypeRegistry, context.getDynamicBeanResolver());
		
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		functionRouter = new FunctionRouter(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);

		context.put(Grid.class, grid);
				
		context.put(FunctionAccessor.class, functionAccessor);
		context.put(FunctionManager.class, functionManager);
		context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		
		context.put(FunctionEditorRegistry.class, editorRegistry);
		context.put(FunctionExecutionService.class, functionExecutionService);
		context.put(FunctionRouter.class, functionRouter);
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
		context.getServiceRegistrationCallback().registerService(FunctionServices.class);
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
	public void executionStart(ExecutionContext context) {
		// Bindings needed for the execution
		boolean isolatedExecution = context.getExecutionParameters().isIsolatedExecution();
		if(isolatedExecution) {
			FunctionAccessor functionAccessor = new InMemoryFunctionAccessorImpl();
			FunctionExecutionService functionExecutionService = new FunctionExecutionServiceImpl(client, functionAccessor, functionTypeRegistry, context.getDynamicBeanResolver());
			DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
			FunctionRouter functionRouter = new FunctionRouter(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);
			
			context.put(FunctionAccessor.class, functionAccessor);
			context.put(FunctionExecutionService.class, functionExecutionService);
			context.put(FunctionRouter.class.getName(), functionRouter);
		} else {
			context.put(FunctionAccessor.class, functionAccessor);
			context.put(FunctionExecutionService.class, functionExecutionService);
			context.put(FunctionRouter.class.getName(), functionRouter);
		}
		super.executionStart(context);
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
