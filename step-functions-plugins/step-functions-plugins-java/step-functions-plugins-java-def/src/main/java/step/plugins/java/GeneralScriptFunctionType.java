package step.plugins.java;

import ch.exense.commons.app.Configuration;
import step.functions.type.SetupFunctionException;

public class GeneralScriptFunctionType extends AbstractScriptFunctionType<GeneralScriptFunction> {
		
	public GeneralScriptFunctionType(Configuration configuration) {
		super(configuration);
	}

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
		function.getScriptLanguage().setValue("java");
		return function;
	}
}
