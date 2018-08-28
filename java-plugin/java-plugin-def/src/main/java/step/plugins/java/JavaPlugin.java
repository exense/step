package step.plugins.java;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.type.FunctionTypeRegistry;

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
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType());
		super.executionControllerStart(context);
	}

}
