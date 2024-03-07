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

import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;

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
		this.librariesFile = validateFileValue(librariesFile);
	}

	@EntityReference(type=EntityManager.resources)
	public DynamicValue<String> getErrorHandlerFile() {
		return errorHandlerFile;
	}

	/**
	 * @param errorHandlerFile the path to the script to be executed when the function returns an error
	 */
	public void setErrorHandlerFile(DynamicValue<String> errorHandlerFile) {
		this.errorHandlerFile = validateFileValue(errorHandlerFile);
	}

	// This performs some sanity check on values which are supposed to be files (or directories).
	// Given the nature of DynamicValues, this is not 100% bulletproof, but it should at least
	// catch blatant mistakes.
	static DynamicValue<String> validateFileValue(DynamicValue<String> fileValue) {
		if (fileValue == null || fileValue.isDynamic()) {
			return fileValue;
		}
		String filename = fileValue.getValue();
		if (filename == null || filename.isBlank()) {
			// better not mess with this, hope the user knows what he's doing.
			return fileValue;
		}
		File f = new File(filename);
		if (!f.exists()) {
			throw new RuntimeException(new FileNotFoundException(filename));
		}
		return fileValue;
	}


}
