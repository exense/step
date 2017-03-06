package step.plugins.selenium;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.FunctionClient;
import step.plugins.adaptergrid.GridPlugin;

@Plugin(prio=100)
public class SeleniumPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		registerWebapp(context,"/seleniumplugin/");
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		functionClient.registerFunctionType(new SeleniumFunctionType());
		
		super.executionControllerStart(context);
	}
	
	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("seleniumPlugin");
		webPlugin.getScripts().add("seleniumplugin/js/selenium.js");
		return webPlugin;
	}

}
