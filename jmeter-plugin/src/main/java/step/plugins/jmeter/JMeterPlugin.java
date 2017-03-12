package step.plugins.jmeter;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.FunctionClient;
import step.plugins.adaptergrid.GridPlugin;

@Plugin(prio=100)
public class JMeterPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		registerWebapp(context,"/jmeterplugin/");
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		functionClient.registerFunctionType(new JMeterFunctionType());
		
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
