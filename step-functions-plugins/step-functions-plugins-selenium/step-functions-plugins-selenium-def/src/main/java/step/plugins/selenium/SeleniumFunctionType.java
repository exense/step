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
package step.plugins.selenium;

import java.io.File;
import java.util.Map;

import ch.exense.commons.app.Configuration;
import step.core.dynamicbeans.DynamicValue;
import step.functions.type.SetupFunctionException;
import step.plugins.java.AbstractScriptFunctionType;

public class SeleniumFunctionType extends AbstractScriptFunctionType<SeleniumFunction> {

	public SeleniumFunctionType(Configuration configuration) {
		super(configuration);
	}

	@Override
	public Map<String, String> getHandlerProperties(SeleniumFunction function) {
		String seleniumVersion = function.getSeleniumVersion();
		
		String propertyName = "plugins.selenium.libs."+seleniumVersion;
		String seleniumLibPath = configuration.getProperty(propertyName);
		if(seleniumLibPath==null) {
			throw new RuntimeException("Property '"+propertyName+"' in step.properties isn't set. Please set it to path of the installation folder of selenium");
		}
		
		File seleniumLibFile = new File(seleniumLibPath);
		if(!seleniumLibFile.exists()) {
			throw new RuntimeException("The path to the selenium installation doesn't exist: "+seleniumLibFile.getAbsolutePath());
		}
		
		function.setLibrariesFile(new DynamicValue<String>(seleniumLibPath));
		return super.getHandlerProperties(function);
	}

	@Override
	public void setupFunction(SeleniumFunction function) throws SetupFunctionException {
		if(function.getScriptLanguage().get().equals("javascript")) {
			setupScriptFile(function,"kw_selenium.js");
		}
	}

	@Override
	public SeleniumFunction newFunction() {
		SeleniumFunction function = new SeleniumFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
