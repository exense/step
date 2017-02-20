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

import com.mongodb.MongoClient;

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.accessors.MongoDBAccessorHelper;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.FunctionClient;
import step.grid.Grid;
import step.grid.client.GridClient;

@Plugin
public class GridPlugin extends AbstractPlugin {

	public static final String GRID_KEY = "Grid_Instance";
	
	public static final String GRIDCLIENT_KEY = "GridClient_Instance";
	
	public static final String FUNCTIONCLIENT_KEY = "FunctionClient_Instance";
	
	public static final String KEYWORD_REPOSITORY_KEY = "KeywordRepository_Instance";
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		
		Integer gridPort = Configuration.getInstance().getPropertyAsInteger("grid.port",8081);
		
		Grid grid = new Grid(gridPort);
		grid.start();
		
		GridClient client = new GridClient(grid);
		
		Integer gridCallTimeout = Configuration.getInstance().getPropertyAsInteger("grid.calltimeout.default");
		if(gridCallTimeout!=null) {
			client.setCallTimeout(gridCallTimeout);
		}

		MongoClient mongoClient = context.getMongoClient();
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(mongoClient, "functions");	
		
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		FunctionClient functionClient = new FunctionClient(client, functionRepository);
		
		context.put(GRID_KEY, grid);
		context.put(GRIDCLIENT_KEY, client);
		context.put(FUNCTIONCLIENT_KEY, functionClient);
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
		context.getServiceRegistrationCallback().registerService(FunctionRepositoryServices.class);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		Object o = context.get(GRIDCLIENT_KEY);
		if(o!=null) {
			((GridClient)o).close();
		}
	}
}
