package step.plugins.adaptergrid;

import org.jongo.MongoCollection;

import com.mongodb.DBCollection;
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
		
		Integer gridPort = Configuration.getInstance().getPropertyAsInteger("grid.port");
		
		Grid grid = new Grid(gridPort);
		grid.start();
		
		GridClient client = new GridClient(grid);

		MongoClient mongoClient = context.getMongoClient();
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(mongoClient, "functions");
		MongoCollection functionConfigurationCollection = MongoDBAccessorHelper.getCollection(mongoClient, "functionConfigurations");		
		
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection, functionConfigurationCollection);
		
		FunctionClient functionClient = new FunctionClient(client, functionRepository);
		
		context.put(GRID_KEY, grid);
		context.put(GRIDCLIENT_KEY, client);
		context.put(FUNCTIONCLIENT_KEY, functionClient);
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		Object o = context.get(GRIDCLIENT_KEY);
		if(o!=null) {
			((GridClient)o).close();
		}
	}
}
