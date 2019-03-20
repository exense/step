package step.plugins.node;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(prio=10)
public class NodePlugin extends AbstractPlugin {

	protected GlobalContext context;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		this.context = context;
		
		registerWebapp(context, "/node/");
		
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new NodeFunctionType());
		
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
