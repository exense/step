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

import org.jongo.MongoCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.FunctionClient;
import step.functions.FunctionExecutionService;
import step.functions.FunctionRepository;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.routing.FunctionRouter;
import step.grid.Grid;
import step.grid.client.GridClient;

@Plugin
public class GridPlugin extends AbstractPlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(GridPlugin.class);

	public static final String GRID_KEY = "Grid_Instance";
	
	public static final String GRIDCLIENT_KEY = "GridClient_Instance";
	
	public static final String FUNCTIONCLIENT_KEY = "FunctionClient_Instance";
	
	private Grid grid;
	
	private FunctionClient functionClient;
	
	private FunctionRepositoryImpl functionRepository;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		
		Integer gridPort = Configuration.getInstance().getPropertyAsInteger("grid.port",8081);
		Integer tokenTTL = Configuration.getInstance().getPropertyAsInteger("grid.ttl",60000);
		
		grid = new Grid(gridPort, tokenTTL);
		grid.start();
		
		GridClient client = new GridClient(grid, grid);

		MongoCollection functionCollection = context.getMongoClientSession().getJongoCollection("functions");	
		
		FunctionEditorRegistry editorRegistry = new FunctionEditorRegistry();
		context.put(FunctionEditorRegistry.class.getName(), editorRegistry);

		functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		functionClient = new FunctionClient(context.getAttachmentManager(), context.getConfiguration(), context.getDynamicBeanResolver(), client, functionRepository);
		
		context.put(GRID_KEY, grid);
		context.put(GRIDCLIENT_KEY, client);
		context.put(FUNCTIONCLIENT_KEY, functionClient);
				
		context.put(FunctionExecutionService.class.getName(), functionClient);
		context.put(FunctionRepository.class.getName(), functionRepository);
		
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		context.put(FunctionRouter.class.getName(), new FunctionRouter(functionClient, functionClient, dynamicJsonObjectResolver));
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
		context.getServiceRegistrationCallback().registerService(FunctionRepositoryServices.class);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		context.put(FunctionExecutionService.class.getName(), functionClient);
		context.put(FunctionRepository.class.getName(), functionRepository);
		
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		context.put(FunctionRouter.class.getName(), new FunctionRouter(functionClient, functionClient, dynamicJsonObjectResolver));
		super.executionStart(context);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		Object o = context.get(GRIDCLIENT_KEY);
		if(o!=null) {
			((GridClient)o).close();
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
