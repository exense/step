package step.plugins.adaptergrid;

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.grid.Grid;
import step.grid.client.GridClient;

@Plugin
public class AdapterClientPlugin extends AbstractPlugin {

	public static final String ADAPTER_GRID_KEY = "AdapterGrid_Instance";
	
	public static final String ADAPTER_CLIENT_KEY = "AdapterClient_Instance";
	
	public static final String KEYWORD_REPOSITORY_KEY = "KeywordRepository_Instance";
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		
		Integer gridPort = Configuration.getInstance().getPropertyAsInteger("grid.port");
		
		Grid grid = new Grid(gridPort);
		grid.start();
		
		GridClient client = new GridClient(grid);

		context.put(ADAPTER_GRID_KEY, grid);
		context.put(ADAPTER_CLIENT_KEY, client);
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		Object o = context.get(ADAPTER_CLIENT_KEY);
		if(o!=null) {
			((GridClient)o).close();
		}
	}
}
