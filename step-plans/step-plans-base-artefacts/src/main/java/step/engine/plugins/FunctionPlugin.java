/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.engine.plugins;

import step.attachments.FileResolver;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.functions.accessor.CachedFunctionAccessor;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.accessor.LayeredFunctionAccessor;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.Grid;
import step.grid.client.GridClient;
import step.grid.client.MockedGridClientImpl;

@Plugin(dependencies= {})
public class FunctionPlugin extends AbstractExecutionEnginePlugin {

	private FunctionAccessor functionAccessor;
	private Grid grid;
	private GridClient gridClient;
	private FunctionTypeRegistry functionTypeRegistry;
	private FunctionExecutionService functionExecutionService;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		FileResolver fileResolver = context.getFileResolver();

		gridClient = context.inheritFromParentOrComputeIfAbsent(parentContext, GridClient.class ,k->new MockedGridClientImpl());
		if(parentContext != null) {
			grid = parentContext.get(Grid.class);
		}

		functionAccessor = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionAccessor.class, k->new InMemoryFunctionAccessorImpl());
		functionTypeRegistry = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionTypeRegistry.class, k->new FunctionTypeRegistryImpl(fileResolver, gridClient));
		
		functionExecutionService = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionExecutionService.class, k->{
			try {
				return new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
			ExecutionContext context) {
		// Use a layered function accessor to isolate the local context from the parent one
		// This allows temporary persistence of function for the duration of the execution
		LayeredFunctionAccessor layeredFunctionAccessor = new LayeredFunctionAccessor();
		// Use a cached accessor for performance reasons
		layeredFunctionAccessor.pushAccessor(new CachedFunctionAccessor(functionAccessor));
		layeredFunctionAccessor.pushAccessor(new InMemoryFunctionAccessorImpl());

		FunctionManagerImpl functionManager = new FunctionManagerImpl(layeredFunctionAccessor, functionTypeRegistry);
		context.put(FunctionAccessor.class, layeredFunctionAccessor);
		context.put(FunctionManager.class, functionManager);
		context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		context.put(FunctionExecutionService.class, functionExecutionService);

		if (grid != null) {
			// Some controls or plans might require the grid to
			// get the agent list for instance. Thus adding it to the context
			// if available
			context.put(Grid.class, grid);
		}

		if(gridClient != null) {
			context.put(GridClient.class, gridClient);
		}
	}
}
