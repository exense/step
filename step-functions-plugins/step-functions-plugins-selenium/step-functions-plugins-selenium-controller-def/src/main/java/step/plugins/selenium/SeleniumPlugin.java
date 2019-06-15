package step.plugins.selenium;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.plugin.GridPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {GridPlugin.class})
public class SeleniumPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		registerWebapp(context,"/seleniumplugin/");
		
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new SeleniumFunctionType(context.getConfiguration()));
		
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
