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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.AbstractStepContext;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.resources.ResourceManager;

public class JMeterFunctionType extends AbstractFunctionType<JMeterFunction> {

	private static final Logger log = LoggerFactory.getLogger(JMeterFunctionType.class);
	public static final String JMETER_HOME_CONFIG_PROPERTY = "plugins.jmeter.home";
	public static final String MISSING_JMETER_HOME_MESSAGE_PROPERTY = "plugins.jmeter.home.missing.message";

	protected final Configuration configuration;

	public JMeterFunctionType(Configuration configuration) {
		super();
		this.configuration = configuration;
	}

	@Override
	public void init() {
		super.init();
		handlerPackageVersion = registerResource(getClass().getClassLoader(), "jmeter-plugin-handler.jar", false, false);
	}

	@Override
	public String getHandlerChain(JMeterFunction function) {
		return "step.plugins.jmeter.JMeterHandler";
	}

	@Override
	public HandlerProperties getHandlerProperties(JMeterFunction function, AbstractStepContext executionContext) {
		String home = configuration.getProperty(JMETER_HOME_CONFIG_PROPERTY);
		if (home != null) {
			Map<String, String> props = new HashMap<>();
			File homeFile = new File(home);
			FileVersion jmeterLibFileVersion = registerFile(homeFile, "$jmeter.libraries", props, true);
			FileVersion jmeterTestplanFileVersion = registerFile(function.getJmeterTestplan(), "$jmeter.testplan.file", props, true, executionContext);
			return new HandlerProperties(props) {
				@Override
				public void close() throws Exception {
					super.close();
					releaseFile(jmeterLibFileVersion);
					releaseFile(jmeterTestplanFileVersion);
				}
			};
		} else {
			throw new RuntimeException(
					configuration.getProperty(MISSING_JMETER_HOME_MESSAGE_PROPERTY,
							"Property 'plugins.jmeter.home' in step.properties isn't set. Please set it to path of the home folder of JMeter")
			);
		}
	}

	@Override
	public JMeterFunction newFunction() {
		return new JMeterFunction();
	}

	@Override
	public void deleteFunction(JMeterFunction function) throws FunctionTypeException {
		// if the function is managed by keyword package, we can delete linked resources (these resources aren't reused anywhere)
		if (function.isManaged()) {
			// TODO: if the function is managed by keyword package, we can delete linked resources (these resources aren't reused anywhere)
			String jmeterTestplanResourceId = function.getJmeterTestplan().getValue();
			if (jmeterTestplanResourceId != null && !jmeterTestplanResourceId.isEmpty()) {
				ResourceManager resourceManager = getResourceManager(null);
				if (resourceManager != null) {
					resourceManager.deleteResource(jmeterTestplanResourceId);
				} else {
					log.warn("Unable to cleanup the jmeter testplan resource for function " + function.getId().toString() + ". Resource manager is not available");
				}
			}
		}
		super.deleteFunction(function);
	}
}
