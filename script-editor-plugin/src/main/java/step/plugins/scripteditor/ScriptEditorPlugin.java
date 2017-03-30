package step.plugins.scripteditor;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.plugins.functions.types.GeneralScriptFunction;

@Plugin(prio=10)
public class ScriptEditorPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		
		registerWebapp(context, "/scripteditor/");
		
		context.getServiceRegistrationCallback().registerService(ScriptEditorServices.class);
		
		FunctionEditor editor = new FunctionEditor() {
			
			@Override
			public String getEditorPath(Function function) {
				return "root/scripteditor/"+function.getId().toString();
			}

			@Override
			public boolean isValidForFunction(Function function) {
				return (function instanceof GeneralScriptFunction) && !((GeneralScriptFunction)function).getScriptLanguage().get().equals("java");
			}
		};
		
		context.get(FunctionEditorRegistry.class).register(editor);
		
		super.executionControllerStart(context);
	}


	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("scriptEditor");
		webPlugin.getScripts().add("scripteditor/js/controllers/scriptEditor.js");
		webPlugin.getScripts().add("scripteditor/bower_components/ace-builds/src-min-noconflict/ace.js");
		return webPlugin;
	}

	
}
