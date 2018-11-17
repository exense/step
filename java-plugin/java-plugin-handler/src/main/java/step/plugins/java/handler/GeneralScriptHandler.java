package step.plugins.java.handler;

import step.functions.Input;
import step.functions.Output;
import step.functions.execution.AbstractFunctionHandler;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandler extends AbstractFunctionHandler {

	@Override
	public Output<?> handle(Input<?> input) throws Exception {
		pushRemoteApplicationContext(ScriptHandler.LIBRARIES_FILE, input.getProperties());
		
		String scriptLanguage = input.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		Class<?> handlerClass = scriptLanguage.equals("java")?JavaJarHandler.class:ScriptHandler.class;
		return delegate(handlerClass.getName(), input);		
	}

}
