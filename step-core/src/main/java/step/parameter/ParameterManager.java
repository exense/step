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

import java.util.*;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.commons.activation.Activator;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.exceptions.PluginCriticalException;

public class ParameterManager {
	
	public static final String RESET_VALUE = "####change me####";
	public static final String PROTECTED_VALUE = "******";

	private static Logger logger = LoggerFactory.getLogger(ParameterManager.class);
	
	private Accessor<Parameter> parameterAccessor;
	private EncryptionManager encryptionManager;

	private String defaultScriptEngine;
	private final DynamicBeanResolver dynamicBeanResolver;

	public ParameterManager(Accessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, Configuration configuration, DynamicBeanResolver dynamicBeanResolver) {
		this(parameterAccessor, encryptionManager, configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE), dynamicBeanResolver);
	}

	public ParameterManager(Accessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, String defaultScriptEngine, DynamicBeanResolver dynamicBeanResolver) {
		this.parameterAccessor = parameterAccessor;
		this.encryptionManager = encryptionManager;
		this.defaultScriptEngine = defaultScriptEngine;
		this.dynamicBeanResolver = dynamicBeanResolver;
	}

	public static ParameterManager copy(ParameterManager from, Accessor<Parameter> parameterAccessor){
		return new ParameterManager(parameterAccessor, from.encryptionManager, from.defaultScriptEngine, from.dynamicBeanResolver);
	}

	public Parameter save(Parameter newParameter, Parameter sourceParameter, String modificationUser) {
		if (isProtected(newParameter) && newParameter.getValue() != null && newParameter.getValue().isDynamic()) {
			throw new ParameterManagerException("Protected parameters do not support values with dynamic expression.");
		}

		ParameterScope scope = newParameter.getScope();
		if(scope != null && scope.equals(ParameterScope.GLOBAL) && newParameter.getScopeEntity() != null) {
			throw new ParameterManagerException("Scope entity cannot be set for parameters with GLOBAL scope.");
		}

		if(sourceParameter != null && isProtected(sourceParameter)) {
			// protected value should not be changed
			newParameter.setProtectedValue(true);
			// if the protected mask is set as value, reuse source value (i.e. value hasn't been changed)
			DynamicValue<String> newParameterValue = newParameter.getValue();
			if(newParameterValue != null && !newParameterValue.isDynamic() && newParameterValue.get().equals(PROTECTED_VALUE)) {
				newParameter.setValue(sourceParameter.getValue());
			}
		}

		try {
			newParameter = this.encryptParameterValueIfEncryptionManagerAvailable(newParameter);
		} catch (EncryptionManagerException e) {
			throw new ParameterManagerException("Error while encrypting parameter value", e);
		}

		Date lastModificationDate = new Date();
		newParameter.setLastModificationDate(lastModificationDate);
		newParameter.setLastModificationUser(modificationUser);

		return parameterAccessor.save(newParameter);
	}

	public Map<String, String> getAllParameterValues(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		return getAllParameters(contextBindings, objectPredicate).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value.get()));
	}
	
	public Map<String, Parameter> getAllParameters(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		Map<String, Parameter> result = new HashMap<>();
		Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;

		Map<String, List<Parameter>> parameterMap = new HashMap<String, List<Parameter>>();
		parameterAccessor.getAll().forEachRemaining(p->{
			if(objectPredicate==null || objectPredicate.test(p)) {
				List<Parameter> parameters = parameterMap.get(p.key);
				if(parameters==null) {
					parameters = new ArrayList<>();
					parameterMap.put(p.key, parameters);
				}
				parameters.add(p);
				try {
					Activator.compileActivationExpression(p, defaultScriptEngine);
				} catch (ScriptException e) {
					logger.error("Error while compiling activation expression of parameter "+p, e);
				}
			}
		});
		
		
		for(String key:parameterMap.keySet()) {
			List<Parameter> parameters = parameterMap.get(key);
			Parameter bestMatch = Activator.findBestMatch(bindings, parameters, defaultScriptEngine);
			if(bestMatch!=null) {
				result.put(key, bestMatch);
			}
		}
		resolveAllParameters(result, contextBindings);
		return result;
	}

	private void resolveAllParameters(Map<String, Parameter> allParameters, Map<String, Object> contextBindings) {
		List<String> unresolvedParamKeys = new ArrayList<>(allParameters.keySet());
		List<String> resolvedParamKeys = new ArrayList<>();
		HashMap<String, Object> bindings = new HashMap<>(contextBindings);
		int unresolvedCountBeforeIteration;
		do {
			unresolvedCountBeforeIteration = unresolvedParamKeys.size();
			unresolvedParamKeys.forEach(k -> {
				Parameter parameter = allParameters.get(k);
				Boolean protectedValue = parameter.getProtectedValue();
				boolean isProtected = parameter.getProtectedValue() != null && parameter.getProtectedValue();
				DynamicValue<String> parameterValue = parameter.getValue();
				if (!isProtected && parameterValue != null) {
					try {
						if (parameterValue.isDynamic()) {
							dynamicBeanResolver.evaluate(parameter, bindings);
						}
						String resolvedValue = parameter.value.get(); //throw an error if evaluation failed
						bindings.put(k, resolvedValue);
						resolvedParamKeys.add(k);
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("Could not (yet) resolve parameter dynamic value " + parameter);
						}
					}
				} else {
					//value is not set or parameter is protected, resolution is skipped
					resolvedParamKeys.add(k);
					if (logger.isDebugEnabled()) {
						logger.debug("Following parameters won't be resolved (null or protected value) " + parameter);
					}
				}
			});
			unresolvedParamKeys.removeAll(resolvedParamKeys);
		} while (!unresolvedParamKeys.isEmpty() && unresolvedParamKeys.size() < unresolvedCountBeforeIteration);
		if (!unresolvedParamKeys.isEmpty()) {
			throw new PluginCriticalException("Error while resolving parameters, following parameters could not be resolved: " + unresolvedParamKeys);
		}
	}
	
	public void encryptAllParameters() {
		parameterAccessor.getAll().forEachRemaining(p->{
			if(isProtected(p)) {
				logger.info("Encrypting parameter "+p);
				try {
					Parameter encryptedParameter = encryptParameterValueIfEncryptionManagerAvailable(p);
					parameterAccessor.save(encryptedParameter);
				} catch (EncryptionManagerException e) {
					logger.error("Error while encrypting parameter "+p.getKey());
				}
			}
		});
	}
	
	public void resetAllProtectedParameters() {
		parameterAccessor.getAll().forEachRemaining(p->{
			if(isProtected(p)) {
				logger.info("Resetting parameter "+p);
				p.setValue(new DynamicValue<>(RESET_VALUE));
				p.setEncryptedValue(null);
				parameterAccessor.save(p);
			}
		});
	}

	private boolean isProtected(Parameter p) {
		return p.getProtectedValue() != null && p.getProtectedValue();
	}
	
	public Parameter encryptParameterValueIfEncryptionManagerAvailable(Parameter parameter) throws EncryptionManagerException {
		if(encryptionManager != null) {
			if(isProtected(parameter)) {
				DynamicValue<String> value = parameter.getValue();
				if(value != null && value.get() != null) {
					parameter.setValue(null);
					String encryptedValue = encryptionManager.encrypt(value.get());
					parameter.setEncryptedValue(encryptedValue);
				}
			}
		}
		return parameter;
	}

	public String getDefaultScriptEngine() {
		return defaultScriptEngine;
	}

	public Accessor<Parameter> getParameterAccessor() {
		return parameterAccessor;
	}

	public EncryptionManager getEncryptionManager() {
		return encryptionManager;
	}
}
