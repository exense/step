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
import step.core.EncryptedTrackedObject;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.objectenricher.ObjectValidator;

import java.util.*;

public abstract class AbstractEncryptedValuesManager<T extends EncryptedTrackedObject, V> {

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

    public T save(T newObj, T sourceObj, String modificationUser, ObjectValidator objectValidator) {
        validateBeforeSave(newObj, objectValidator);

        if (sourceObj != null && isProtected(sourceObj)) {
            // protected value should not be changed
            newObj.setProtectedValue(true);
            // if the protected mask is set as value, reuse source value (i.e. value hasn't been changed)
            if (getValue(newObj) != null && !isDynamicValue(newObj) && getStringValue(newObj).equals(PROTECTED_VALUE)) {
                setValue(newObj, getValue(sourceObj));
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

    protected void validateBeforeSave(T newObj, ObjectValidator objectValidator) {
        if (objectValidator != null) {
            objectValidator.validateOnSave(newObj);
        }
    }

    protected abstract Accessor<T> getAccessor();

    public static <T extends EncryptedTrackedObject> boolean isProtected(T p) {
        return p.getProtectedValue() != null && p.getProtectedValue();
    }

    public T encryptValueIfEncryptionManagerAvailable(T obj) throws EncryptionManagerException {
        if(encryptionManager != null) {
            if(isProtected(obj)) {
                String value = getStringValue(obj);
                if(value != null) {
                    setValue(obj, null);
                    String encryptedValue = encryptionManager.encrypt(value);
                    obj.setEncryptedValue(encryptedValue);
                }
            }
        }
        return obj;
    }

    protected abstract boolean isDynamicValue(T obj);

    protected abstract String getStringValue(T obj);

    protected abstract void setValue(T obj, V value);

    protected abstract V getValue(T obj);

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
                setValue(p, getResetValue());
                p.setEncryptedValue(null);
                getAccessor().save(p);
            }
        });
    }

    public abstract V getResetValue();

    public EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public String getDefaultScriptEngine() {
        return defaultScriptEngine;
    }

    public abstract String getEntityNameForLogging();

}
