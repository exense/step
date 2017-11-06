package step.plugins.java;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.FunctionClient;
import step.plugins.adaptergrid.GridPlugin;

@Plugin(prio=10)
public class JavaPlugin extends AbstractPlugin {
	
	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("javaPlugin");
		webPlugin.getScripts().add("javaplugin/js/script.js");
		return webPlugin;
	}

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		registerWebapp(context,"/javaplugin/");
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		functionClient.registerFunctionType(new GeneralScriptFunctionType());
		super.executionControllerStart(context);
	}

}
