package step.plugins.java;

import step.functions.type.SetupFunctionException;

public class GeneralScriptFunctionType extends AbstractScriptFunctionType<GeneralScriptFunction> {
		
	@Override
	public void setupFunction(GeneralScriptFunction function) throws SetupFunctionException {
		String language = getScriptLanguage(function);
		if(language.equals("java")) {
			// No specific setup for java at the moment
		} else {
			String template = null;
			if(language.equals("javascript")) {
				template = "custom_script.js";
			} else if(language.equals("groovy")) {
				template = "custom_script.groovy";
			}			
			setupScriptFile(function, template);
		}
	}

	@Override
	public GeneralScriptFunction newFunction() {
		GeneralScriptFunction function = new GeneralScriptFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
