package step.plugins.functions.types;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.FunctionClient;
import step.plugins.adaptergrid.GridPlugin;

@Plugin(prio=10)
public class BaseFunctionTypesPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);		
		functionClient.registerFunctionType(new CompositeFunctionType());
	}

}
