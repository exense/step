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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.commons.activation.Activator;
import step.core.EncryptedTrackedObject;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.exceptions.PluginCriticalException;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractEncryptedValuesManager<T extends EncryptedTrackedObject> {

    protected static Logger logger = LoggerFactory.getLogger(AbstractEncryptedValuesManager.class);

    public static final String RESET_VALUE = "####change me####";
    public static final String PROTECTED_VALUE = "******";

    protected final DynamicBeanResolver dynamicBeanResolver;
    protected final EncryptionManager encryptionManager;
    protected final String defaultScriptEngine;

    public AbstractEncryptedValuesManager(EncryptionManager encryptionManager, String defaultScriptEngine, DynamicBeanResolver dynamicBeanResolver) {
        this.encryptionManager = encryptionManager;
        this.defaultScriptEngine = defaultScriptEngine;
        this.dynamicBeanResolver = dynamicBeanResolver;
    }

    public static <T extends EncryptedTrackedObject> T maskProtectedValue(T parameter) {
        if(parameter != null && isProtected(parameter) & !RESET_VALUE.equals(parameter.getValue().getValue())) {
            parameter.setValue(new DynamicValue<>(PROTECTED_VALUE));
        }
        return parameter;
    }

    public T save(T newParameter, T sourceParameter, String modificationUser) {
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
        if (sourceParameter == null) {
            newParameter.setCreationDate(lastModificationDate);
            newParameter.setCreationUser(modificationUser);
        }
        newParameter.setLastModificationDate(lastModificationDate);
        newParameter.setLastModificationUser(modificationUser);

        return getAccessor().save(newParameter);
    }

    protected abstract Accessor<T> getAccessor();

    public static <T extends EncryptedTrackedObject> boolean isProtected(T p) {
        return p.getProtectedValue() != null && p.getProtectedValue();
    }

    public T encryptParameterValueIfEncryptionManagerAvailable(T parameter) throws EncryptionManagerException {
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

    public Map<String, String> getAllParameterValues(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
        return getAllParameters(contextBindings, objectPredicate).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue().get()));
    }

    public Map<String, T> getAllParameters(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
        Map<String, T> result = new HashMap<>();
        Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;

        Map<String, List<T>> parameterMap = new HashMap<>();
        getAccessor().getAll().forEachRemaining(p->{
            if(objectPredicate==null || objectPredicate.test(p)) {
                List<T> parameters = parameterMap.get(p.getKey());
                if(parameters==null) {
                    parameters = new ArrayList<>();
                    parameterMap.put(p.getKey(), parameters);
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
            List<T> parameters = parameterMap.get(key);
            T bestMatch = Activator.findBestMatch(bindings, parameters, defaultScriptEngine);
            if(bestMatch!=null) {
                result.put(key, bestMatch);
            }
        }
        resolveAllParameters(result, contextBindings);
        return result;
    }

    private void resolveAllParameters(Map<String, T> allParameters, Map<String, Object> contextBindings) {
        List<String> unresolvedParamKeys = new ArrayList<>(allParameters.keySet());
        List<String> resolvedParamKeys = new ArrayList<>();
        HashMap<String, Object> bindings = new HashMap<>(contextBindings);
        int unresolvedCountBeforeIteration;
        do {
            unresolvedCountBeforeIteration = unresolvedParamKeys.size();
            unresolvedParamKeys.forEach(k -> {
                T parameter = allParameters.get(k);
                Boolean protectedValue = parameter.getProtectedValue();
                boolean isProtected = isProtected(parameter);
                DynamicValue<String> parameterValue = parameter.getValue();
                if (!isProtected && parameterValue != null) {
                    try {
                        if (parameterValue.isDynamic()) {
                            dynamicBeanResolver.evaluate(parameter, bindings);
                        }
                        String resolvedValue = parameter.getValue().get(); //throw an error if evaluation failed
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
        getAccessor().getAll().forEachRemaining(p->{
            if(isProtected(p)) {
                logger.info("Encrypting parameter "+p);
                try {
                    T encryptedParameter = encryptParameterValueIfEncryptionManagerAvailable(p);
                    getAccessor().save(encryptedParameter);
                } catch (EncryptionManagerException e) {
                    logger.error("Error while encrypting parameter "+p.getKey());
                }
            }
        });
    }

    public void resetAllProtectedParameters() {
        getAccessor().getAll().forEachRemaining(p->{
            if(isProtected(p)) {
                logger.info("Resetting parameter "+p);
                p.setValue(new DynamicValue<>(RESET_VALUE));
                p.setEncryptedValue(null);
                getAccessor().save(p);
            }
        });
    }

    public EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public String getDefaultScriptEngine() {
        return defaultScriptEngine;
    }

}
