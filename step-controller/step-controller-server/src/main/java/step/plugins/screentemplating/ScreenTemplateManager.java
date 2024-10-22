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
package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;

import step.commons.activation.Activator;
import step.core.objectenricher.ObjectPredicate;

import javax.script.SimpleBindings;

import static step.commons.activation.Activator.evaluateActivationExpression;

public class ScreenTemplateManager {

	protected final List<ScreenTemplateChangeListener> listeners = new ArrayList<>();
	
	protected ScreenInputAccessor screenInputAccessor;
	protected String defaultScriptEngine;

	public ScreenTemplateManager(ScreenInputAccessor screenInputAccessor, Configuration configuration) {
		super();
		this.screenInputAccessor = screenInputAccessor;
		this.defaultScriptEngine = configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE);
	}

	public List<Input> getInputsForScreen(String screenId, Map<String,Object> contextBindings, ObjectPredicate objectPredicate) {
		Stream<ScreenInput> stream = screenInputAccessor.getScreenInputsByScreenId(screenId).stream();
		if(objectPredicate != null) {
			stream = stream.filter(objectPredicate);
		}
		List<Input> screenInputs = stream.map(i->i.getInput()).collect(Collectors.toList());
		
		List<Input> result = new ArrayList<>();
		List<Input> inputs =  Activator.findAllMatches(contextBindings, screenInputs, defaultScriptEngine);
		for(Input input:inputs) {
			List<Option> options = input.getOptions();
			List<Option> activeOptions = null;
			if(options!=null) {
				activeOptions = Activator.findAllMatches(contextBindings, options, defaultScriptEngine);
			}
			Input clone = new Input(input.getType(), input.getId(), input.getLabel(), input.getDescription(), activeOptions);
			clone.setCustomUIComponents(input.getCustomUIComponents());
			clone.setSearchMapperService(input.getSearchMapperService());
			clone.setDefaultValue(input.getDefaultValue());
			result.add(clone);
		}
		
		return result;
	}
	
	public void moveInput(String inputId, int offset) {
		ScreenInput screenInput = screenInputAccessor.get(new ObjectId(inputId));
		
		List<ScreenInput> screenInputs = screenInputAccessor.getScreenInputsByScreenId(screenInput.getScreenId());
		
		int indexOf = screenInputs.indexOf(screenInput);
		int newIndex = indexOf+offset;
		
		if(newIndex>=0 && newIndex<screenInputs.size()) {
			Collections.swap(screenInputs, indexOf, indexOf+offset);
			for(int i=0;i<screenInputs.size();i++) {
				ScreenInput input = screenInputs.get(i);
				input.setPosition(i);
				screenInputAccessor.save(input);
			}
		}
	}
	
	public void registerListener(ScreenTemplateChangeListener listener) {
		listeners.add(listener);
	}
	
	public void notifyChange() {
		listeners.forEach(l->l.onChange());
	}

	public List<ScreenInput> getScreenInputsForScreen(String screenId, Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		Stream<ScreenInput> stream = screenInputAccessor.getScreenInputsByScreenId(screenId).stream();
		if(objectPredicate != null) {
			stream = stream.filter(objectPredicate);
		}
		List<ScreenInput> screenInputs = stream.collect(Collectors.toList());
		Map<String, String> defaultValuesAsBindings = screenInputs.stream().map(ScreenInput::getInput).filter(i -> i.getDefaultValue() != null)
				.filter(i -> !contextBindings.containsKey(i.getId())).collect(Collectors.toMap(Input::getId, Input::getDefaultValue));
		contextBindings.putAll(defaultValuesAsBindings);

		List<ScreenInput> result = new ArrayList<>();
		List<ScreenInput> activatedScreeninputs =  findAllMatches(contextBindings, screenInputs, defaultScriptEngine);
		for(ScreenInput screenInput:activatedScreeninputs) {
			//We keep the same logic as legacy methods to create a clone, to avoid any unexpected update in DB
			//The client still needs to be able to map by ID with the source screen inputs in DB
			ScreenInput screenInputClone = new ScreenInput();
			screenInputClone.setId(screenInput.getId());
			screenInputClone.setScreenId(screenInput.getScreenId());
			screenInputClone.setImmutable(screenInput.getImmutable());
			screenInputClone.setPosition(screenInput.getPosition());
			screenInputClone.setAttributes(screenInput.getAttributes());
			screenInputClone.setCustomFields(screenInput.getCustomFields());
			Input input = screenInput.getInput();
			List<Option> options = input.getOptions();
			List<Option> activeOptions = null;
			if(options!=null) {
				activeOptions = Activator.findAllMatches(contextBindings, options, defaultScriptEngine);
			}
			Input clone = new Input(input.getType(), input.getId(), input.getLabel(), input.getDescription(), activeOptions);
			clone.setCustomUIComponents(input.getCustomUIComponents());
			clone.setSearchMapperService(input.getSearchMapperService());
			clone.setDefaultValue(input.getDefaultValue());
			screenInputClone.setInput(clone);
			result.add(screenInputClone);
		}

		return result;
	}

	private List<ScreenInput> findAllMatches(Map<String, Object> bindings, List<ScreenInput> screenInputs, String defaultScriptEngine) {
		List<ScreenInput> result = new ArrayList<>();
		for(ScreenInput screenInput:screenInputs) {
			Boolean expressionResult = evaluateActivationExpression(bindings!=null?new SimpleBindings(bindings):null, screenInput.getInput().getActivationExpression(), defaultScriptEngine);

			if(expressionResult) {
				result.add(screenInput);
			}
		}
		return result;
	}
}
