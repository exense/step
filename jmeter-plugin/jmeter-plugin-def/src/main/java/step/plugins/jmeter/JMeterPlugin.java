package step.plugins.jmeter;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(prio=100)
public class JMeterPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		registerWebapp(context,"/jmeterplugin/");
		
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new JMeterFunctionType(context.getConfiguration()));
		
		super.executionControllerStart(context);
	}
	
	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("jmeterPlugin");
		webPlugin.getScripts().add("jmeterplugin/js/jmeter.js");
		return webPlugin;
	}

}
