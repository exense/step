/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.screentemplating;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import com.fasterxml.jackson.core.type.TypeReference;

import step.commons.activation.Activator;
import step.commons.conf.Configuration;
import step.commons.conf.FileRepository;
import step.commons.conf.FileRepository.FileRepositoryCallback;
import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ScreenTemplatePlugin extends AbstractPlugin {

	public static final String SCREEN_TEMPLATE_KEY = "ScreenTemplatePlugin_Instance";
	
	FileRepository<Map<String, List<Input>>> repo;
	
	Map<String, List<Input>> screenTemplates;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		String config = Configuration.getInstance().getProperty("screentemplate.config");
		if(config==null) {
			URL url = this.getClass().getClassLoader().getResource("ScreenTemplates.js");
			if(url!=null) {
				config = url.getFile();
			}
		}
		
		if(config!=null) {
			repo = new FileRepository<Map<String, List<Input>>>(new File(config), new TypeReference<Map<String, List<Input>>>() {}, new FileRepositoryCallback<Map<String, List<Input>>>() {
				@Override
				public void onLoad(Map<String, List<Input>> screens) throws ScriptException {
					for(String screenId:screens.keySet()) {
						List<Input> inputs = screens.get(screenId);
						Activator.compileActivationExpressions(inputs);
						for(Input input:inputs) {
							if(input.getOptions()!=null) {
								Activator.compileActivationExpressions(input.getOptions());
							}
						}	
					}
					screenTemplates = screens;
					
				}} );
			
			context.put(SCREEN_TEMPLATE_KEY, this);
			context.getServiceRegistrationCallback().registerService(ScreenTemplateService.class);
		} else {
			
		}
	}
	
	public List<Input> getInputsForScreen(String screenId, Map<String,Object> contextBindings) {
		assertInitialized();

		List<Input> result = new ArrayList<>();
		List<Input> inputs =  Activator.findAllMatches(contextBindings, screenTemplates.get(screenId));
		for(Input input:inputs) {
			List<Option> options = input.getOptions();
			List<Option> activeOptions = null;
			if(options!=null) {
				activeOptions = Activator.findAllMatches(contextBindings, options);
			}
			result.add(new Input(input.getType(), input.getId(), input.getLabel(), activeOptions));
		}
		
		return result;
	}
	
	private void assertInitialized() {
		if(screenTemplates==null) {
			throw new RuntimeException("Service not initialized");
		}
	}

}
