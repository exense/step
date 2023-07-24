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
package step.plugins.jmeter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ch.exense.commons.app.Configuration;
import step.functions.type.AbstractFunctionType;
import step.grid.filemanager.FileVersionId;

public class JMeterFunctionType extends AbstractFunctionType<JMeterFunction> {

	private FileVersionId handlerJar;
	protected final Configuration configuration;
	
	public JMeterFunctionType(Configuration configuration) {
		super();
		this.configuration = configuration;
	}

	@Override
	public void init() {
		super.init();
		handlerJar = registerResource(getClass().getClassLoader(), "jmeter-plugin-handler.jar", false);
	}

	@Override
	public String getHandlerChain(JMeterFunction function) {
		return "step.plugins.jmeter.JMeterHandler";
	}

	@Override
	public FileVersionId getHandlerPackage(JMeterFunction function) {
		return handlerJar;
	}

	@Override
	public Map<String, String> getHandlerProperties(JMeterFunction function) {
		Map<String, String> props = new HashMap<>();
		registerFile(function.getJmeterTestplan(), "$jmeter.testplan.file", props);
		
		String home = configuration.getProperty("plugins.jmeter.home");
		if(home!=null) {
			File homeFile = new File(home);
			registerFile(homeFile, "$jmeter.libraries", props);
			return props;			
		} else {
			throw new RuntimeException("Property 'plugins.jmeter.home' in step.properties isn't set. Please set it to path of the home folder of JMeter");
		}
	}

	@Override
	public JMeterFunction newFunction() {
		return new JMeterFunction();
	}

}
