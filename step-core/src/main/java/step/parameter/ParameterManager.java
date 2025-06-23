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
package step.parameter;

import ch.exense.commons.app.Configuration;
import step.commons.activation.Activator;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.encryption.EncryptionManager;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectValidator;
import step.core.plugins.exceptions.PluginCriticalException;
import step.encryption.AbstractEncryptedValuesManager;
import step.encryption.EncryptedValueManagerException;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterManager extends AbstractEncryptedValuesManager<Parameter, DynamicValue<String>> {

	protected final DynamicBeanResolver dynamicBeanResolver;
	private final Accessor<Parameter> parameterAccessor;

	public ParameterManager(Accessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, Configuration configuration, DynamicBeanResolver dynamicBeanResolver) {
		this(parameterAccessor, encryptionManager, configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE), dynamicBeanResolver);
	}

	public ParameterManager(Accessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, String defaultScriptEngine, DynamicBeanResolver dynamicBeanResolver) {
		super(encryptionManager, defaultScriptEngine);
		this.parameterAccessor = parameterAccessor;
		this.dynamicBeanResolver = dynamicBeanResolver;
	}

	public static ParameterManager copy(ParameterManager from, Accessor<Parameter> parameterAccessor){
		return new ParameterManager(parameterAccessor, from.encryptionManager, from.defaultScriptEngine, from.dynamicBeanResolver);
	}

	@Override
	protected Accessor<Parameter> getAccessor() {
		return parameterAccessor;
	}

	@Override
	protected boolean isDynamicValue(Parameter obj) {
		return obj.getValue() != null && obj.getValue().isDynamic();
	}

	@Override
	protected String getStringValue(Parameter obj) {
		return obj.getValue() == null ? null : obj.getValue().get();
	}

	@Override
	protected void setValue(Parameter obj, DynamicValue<String> value) {
		obj.setValue(value);
	}

	@Override
	protected DynamicValue<String> getValue(Parameter obj) {
		return obj.getValue();
	}

	@Override
	public DynamicValue<String> getResetValue() {
		return new DynamicValue<>(RESET_VALUE);
	}

	@Override
	public String getEntityNameForLogging() {
		return "parameter";
	}

	public Accessor<Parameter> getParameterAccessor() {
		return getAccessor();
	}

	@Override
	protected void validateBeforeSave(Parameter newObj, ObjectValidator objectValidator) {
		super.validateBeforeSave(newObj, objectValidator);

		ParameterScope scope = newObj.getScope();
		if(scope != null && scope.equals(ParameterScope.GLOBAL) && newObj.getScopeEntity() != null) {
			throw new EncryptedValueManagerException("Scope entity cannot be set for " + getEntityNameForLogging() + "s with GLOBAL scope.");
		}

		if (isProtected(newObj) && newObj.getValue() != null && newObj.getValue().isDynamic()) {
			throw new EncryptedValueManagerException("Protected entity (" + getEntityNameForLogging() + ") do not support values with dynamic expression.");
		}

	}

	public Map<String, String> getAllValues(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		return getAllObjects(contextBindings, objectPredicate).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue().get()));
	}

	public Map<String, Parameter> getAllObjects(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		Map<String, Parameter> result = new HashMap<>();
		Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;

		Map<String, List<Parameter>> objectsMap = new HashMap<>();
		getAccessor().getAll().forEachRemaining(p->{
			if(objectPredicate==null || objectPredicate.test(p)) {
				List<Parameter> objects = objectsMap.get(p.getKey());
				if(objects==null) {
					objects = new ArrayList<>();
					objectsMap.put(p.getKey(), objects);
				}
				objects.add(p);
				try {
					Activator.compileActivationExpression(p, defaultScriptEngine);
				} catch (ScriptException e) {
					logger.error("Error while compiling activation expression of  " + getEntityNameForLogging() + " " + p, e);
				}
			}
		});


		for(String key:objectsMap.keySet()) {
			List<Parameter> objects = objectsMap.get(key);
			Parameter bestMatch = Activator.findBestMatch(bindings, objects, defaultScriptEngine);
			if(bestMatch!=null) {
				result.put(key, bestMatch);
			}
		}
		resolveAllValues(result, contextBindings);
		return result;
	}

	private void resolveAllValues(Map<String, Parameter> allObjects, Map<String, Object> contextBindings) {
		List<String> unresolvedKeys = new ArrayList<>(allObjects.keySet());
		List<String> resolvedKeys = new ArrayList<>();
		HashMap<String, Object> bindings = new HashMap<>(contextBindings);
		int unresolvedCountBeforeIteration;
		do {
			unresolvedCountBeforeIteration = unresolvedKeys.size();
			unresolvedKeys.forEach(k -> {
				Parameter obj = allObjects.get(k);
				Boolean protectedValue = obj.getProtectedValue();
				boolean isProtected = isProtected(obj);
				if (!isProtected && getValue(obj) != null) {
					try {
						if (isDynamicValue(obj) && dynamicBeanResolver != null) {
							dynamicBeanResolver.evaluate(obj, bindings);
						}
						String resolvedValue = getStringValue(obj); //throw an error if evaluation failed
						bindings.put(k, resolvedValue);
						resolvedKeys.add(k);
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("Could not (yet) resolve " + getEntityNameForLogging() + " dynamic value " + obj);
						}
					}
				} else {
					//value is not set or is protected, resolution is skipped
					resolvedKeys.add(k);
					if (logger.isDebugEnabled()) {
						logger.debug("Following won't be resolved (null or protected value) " + obj);
					}
				}
			});
			unresolvedKeys.removeAll(resolvedKeys);
		} while (!unresolvedKeys.isEmpty() && unresolvedKeys.size() < unresolvedCountBeforeIteration);
		if (!unresolvedKeys.isEmpty()) {
			throw new PluginCriticalException("Error while resolving " + getEntityNameForLogging() + "s, following " + getEntityNameForLogging() + " s could not be resolved: " + unresolvedKeys);
		}
	}


	public static Parameter maskProtectedValue(Parameter obj) {
		if(obj != null && isProtected(obj) & !RESET_VALUE.equals(obj.getValue().getValue())) {
			obj.setValue(new DynamicValue<>(PROTECTED_VALUE));
		}
		return obj;
	}
}
