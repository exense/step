/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.java;

import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.automation.packages.model.AutomationPackageContextual;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

/**
 * This class encapsulates the configuration parameters of functions (aka Keywords)
 * of type "Script"
 *
 */
public class GeneralScriptFunction extends Function implements AutomationPackageContextual<GeneralScriptFunction> {

	DynamicValue<String> scriptFile = new DynamicValue<>("");
	
	DynamicValue<String> scriptLanguage = new DynamicValue<>("");
	
	DynamicValue<String> librariesFile = new DynamicValue<>("");
	
	DynamicValue<String> errorHandlerFile = new DynamicValue<>("");
	
	@EntityReference(type=EntityManager.resources)
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

	@EntityReference(type=EntityManager.resources)
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

	@EntityReference(type=EntityManager.resources)
	public DynamicValue<String> getErrorHandlerFile() {
		return errorHandlerFile;
	}

	/**
	 * @param errorHandlerFile the path to the script to be executed when the function returns an error
	 */
	public void setErrorHandlerFile(DynamicValue<String> errorHandlerFile) {
		this.errorHandlerFile = errorHandlerFile;
	}

	@Override
	public GeneralScriptFunction applyAutomationPackageContext(AutomationPackageContext context) {
		if (getScriptFile().get() == null || getScriptFile().get().isEmpty()) {
			AutomationPackage ap = context.getAutomationPackage();
			if (ap != null && ap.getAutomationPackageResource() != null && !ap.getAutomationPackageResource().isEmpty()) {
				setScriptFile(new DynamicValue<>(ap.getAutomationPackageResource()));
			}
			if (ap != null && ap.getKeywordLibraryResource() != null && !ap.getKeywordLibraryResource().isEmpty()) {
				setLibrariesFile(new DynamicValue<>(ap.getAutomationPackageResource()));
			}
		}
		return this;
	}
}
