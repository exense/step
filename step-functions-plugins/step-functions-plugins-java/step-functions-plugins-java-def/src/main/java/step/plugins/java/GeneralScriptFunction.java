package step.plugins.java;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;

/**
 * This class encapsulates the configuration parameters of functions (aka Keywords)
 * of type "Script"
 *
 */
public class GeneralScriptFunction extends Function {

	DynamicValue<String> scriptFile = new DynamicValue<>("");
	
	DynamicValue<String> scriptLanguage = new DynamicValue<>("");
	
	DynamicValue<String> librariesFile = new DynamicValue<>("");
	
	DynamicValue<String> errorHandlerFile = new DynamicValue<>("");
	
	public DynamicValue<String> getScriptFile() {
		return scriptFile;
	}

	/**
	 * @param scriptFile the path to the script file (.js, .groovy, .jar, depending on the script language)
	 */
	public void setScriptFile(DynamicValue<String> scriptFile) {
		this.scriptFile = scriptFile;
	}

	public DynamicValue<String> getScriptLanguage() {
		return scriptLanguage;
	}

	/**
	 * @param scriptLanguage the script language of this function. Per default the following language are supported: javascript, groovy, java 
	 */	
	public void setScriptLanguage(DynamicValue<String> scriptLanguage) {
		this.scriptLanguage = scriptLanguage;
	}

	public DynamicValue<String> getLibrariesFile() {
		return librariesFile;
	}

	/**
	 * @param librariesFile the path to the libraries to be used for the function. This can be a single jar or a folder containing
	 * a list of jars
	 */
	public void setLibrariesFile(DynamicValue<String> librariesFile) {
		this.librariesFile = librariesFile;
	}

	public DynamicValue<String> getErrorHandlerFile() {
		return errorHandlerFile;
	}

	/**
	 * @param errorHandlerFile the path to the script to be executed when the function returns an error
	 */
	public void setErrorHandlerFile(DynamicValue<String> errorHandlerFile) {
		this.errorHandlerFile = errorHandlerFile;
	}
}
