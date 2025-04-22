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
package step.encryption;

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
import step.parameter.ParameterScope;

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

    public static <T extends EncryptedTrackedObject> T maskProtectedValue(T obj) {
        if(obj != null && isProtected(obj) & !RESET_VALUE.equals(obj.getValue().getValue())) {
            obj.setValue(new DynamicValue<>(PROTECTED_VALUE));
        }
        return obj;
    }

    public T save(T newObj, T sourceObj, String modificationUser) {
        if (isProtected(newObj) && newObj.getValue() != null && newObj.getValue().isDynamic()) {
            throw new EncryptedValueManagerException("Protected entity (" + getEntityNameForLogging() + ") do not support values with dynamic expression.");
        }

        ParameterScope scope = newObj.getScope();
        if(scope != null && scope.equals(ParameterScope.GLOBAL) && newObj.getScopeEntity() != null) {
            throw new EncryptedValueManagerException("Scope entity cannot be set for " + getEntityNameForLogging() + "s with GLOBAL scope.");
        }

        if(sourceObj != null && isProtected(sourceObj)) {
            // protected value should not be changed
            newObj.setProtectedValue(true);
            // if the protected mask is set as value, reuse source value (i.e. value hasn't been changed)
            DynamicValue<String> newValue = newObj.getValue();
            if(newValue != null && !newValue.isDynamic() && newValue.get().equals(PROTECTED_VALUE)) {
                newObj.setValue(sourceObj.getValue());
            }
        }

        try {
            newObj = this.encryptValueIfEncryptionManagerAvailable(newObj);
        } catch (EncryptionManagerException e) {
            throw new EncryptedValueManagerException("Error while encrypting " + getEntityNameForLogging() + " value", e);
        }

        Date lastModificationDate = new Date();
        if (sourceObj == null) {
            newObj.setCreationDate(lastModificationDate);
            newObj.setCreationUser(modificationUser);
        }
        newObj.setLastModificationDate(lastModificationDate);
        newObj.setLastModificationUser(modificationUser);

        return getAccessor().save(newObj);
    }

    protected abstract Accessor<T> getAccessor();

    public static <T extends EncryptedTrackedObject> boolean isProtected(T p) {
        return p.getProtectedValue() != null && p.getProtectedValue();
    }

    public T encryptValueIfEncryptionManagerAvailable(T obj) throws EncryptionManagerException {
        if(encryptionManager != null) {
            if(isProtected(obj)) {
                DynamicValue<String> value = obj.getValue();
                if(value != null && value.get() != null) {
                    obj.setValue(null);
                    String encryptedValue = encryptionManager.encrypt(value.get());
                    obj.setEncryptedValue(encryptedValue);
                }
            }
        }
        return obj;
    }

    public Map<String, String> getAllValues(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
        return getAllObjects(contextBindings, objectPredicate).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue().get()));
    }

    public Map<String, T> getAllObjects(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
        Map<String, T> result = new HashMap<>();
        Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;

        Map<String, List<T>> objectsMap = new HashMap<>();
        getAccessor().getAll().forEachRemaining(p->{
            if(objectPredicate==null || objectPredicate.test(p)) {
                List<T> objects = objectsMap.get(p.getKey());
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
            List<T> objects = objectsMap.get(key);
            T bestMatch = Activator.findBestMatch(bindings, objects, defaultScriptEngine);
            if(bestMatch!=null) {
                result.put(key, bestMatch);
            }
        }
        resolveAllValues(result, contextBindings);
        return result;
    }

    private void resolveAllValues(Map<String, T> allObjects, Map<String, Object> contextBindings) {
        List<String> unresolvedKeys = new ArrayList<>(allObjects.keySet());
        List<String> resolvedKeys = new ArrayList<>();
        HashMap<String, Object> bindings = new HashMap<>(contextBindings);
        int unresolvedCountBeforeIteration;
        do {
            unresolvedCountBeforeIteration = unresolvedKeys.size();
            unresolvedKeys.forEach(k -> {
                T obj = allObjects.get(k);
                Boolean protectedValue = obj.getProtectedValue();
                boolean isProtected = isProtected(obj);
                DynamicValue<String> value = obj.getValue();
                if (!isProtected && value != null) {
                    try {
                        if (value.isDynamic()) {
                            dynamicBeanResolver.evaluate(obj, bindings);
                        }
                        String resolvedValue = obj.getValue().get(); //throw an error if evaluation failed
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

    public void encryptAll() {
        getAccessor().getAll().forEachRemaining(p->{
            if(isProtected(p)) {
                logger.info("Encrypting " + getEntityNameForLogging() + " " + p);
                try {
                    T encrypted = encryptValueIfEncryptionManagerAvailable(p);
                    getAccessor().save(encrypted);
                } catch (EncryptionManagerException e) {
                    logger.error("Error while encrypting " + getEntityNameForLogging() + " " + p.getKey());
                }
            }
        });
    }

    public void resetAllProtectedValues() {
        getAccessor().getAll().forEachRemaining(p->{
            if(isProtected(p)) {
                logger.info("Resetting " + getEntityNameForLogging() + " " + p);
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

    protected abstract String getEntityNameForLogging();

}
