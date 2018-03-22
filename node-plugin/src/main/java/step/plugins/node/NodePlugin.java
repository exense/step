package step.plugins.node;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.FunctionClient;
import step.plugins.adaptergrid.GridPlugin;

@Plugin(prio=10)
public class NodePlugin extends AbstractPlugin {

	protected GlobalContext context;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		this.context = context;
		
		registerWebapp(context, "/node/");
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		functionClient.registerFunctionType(new NodeFunctionType());
		
		context.put(NodePlugin.class.getName(), this);
		
		super.executionControllerStart(context);
	}


	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("NodePlugin");
		webPlugin.getScripts().add("node/js/controllers/node.js");		
		return webPlugin;
	}	
}
