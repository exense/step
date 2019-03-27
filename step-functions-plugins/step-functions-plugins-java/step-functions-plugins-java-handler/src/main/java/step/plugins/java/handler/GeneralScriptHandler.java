package step.plugins.java.handler;

import javax.json.JsonObject;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandler extends JsonBasedFunctionHandler {

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		// Using the forked to branch in order no to have the ClassLoader of java-plugin-handler.jar as parent.
		// the project java-plugin-handler.jar has many dependencies that might conflict with the dependencies of the 
		// keyword. One of these dependencies is guava for example.
		pushLocalApplicationContext(FORKED_BRANCH, getCurrentContext().getClassLoader(), "step-api-keyword.jar");
		
		pushRemoteApplicationContext(FORKED_BRANCH, ScriptHandler.PLUGIN_LIBRARIES_FILE, input.getProperties());
		
		pushRemoteApplicationContext(FORKED_BRANCH, ScriptHandler.LIBRARIES_FILE, input.getProperties());
		
		String scriptLanguage = input.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		Class<?> handlerClass = scriptLanguage.equals("java")?JavaJarHandler.class:ScriptHandler.class;
		return delegate(handlerClass.getName(), input);		
	}

}
