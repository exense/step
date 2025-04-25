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
package step.core;

import step.commons.activation.ActivableObject;
import step.core.accessors.AbstractTrackedObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.EnricheableObject;
import step.parameter.Parameter;
import step.parameter.ParameterScope;

public abstract class EncryptedTrackedObject extends AbstractTrackedObject implements ActivableObject, EnricheableObject, ValueWithKey {

    public static final String PARAMETER_PROTECTED_VALUE_FIELD = "protectedValue";
    public static final String PARAMETER_VALUE_FIELD = "value";

    protected Boolean protectedValue = false;
    /**
     * When running with an encryption manager, the value of protected
     * {@link Parameter}s is encrypted and the encrypted value is stored into this
     * field
     */
    protected String encryptedValue;
    protected DynamicValue<String> value;
    protected String key;

    public EncryptedTrackedObject() {
        super();
    }

    public DynamicValue<String> getValue() {
        return value;
    }

    public void setValue(DynamicValue<String> value) {
        this.value = value;
    }

    public Boolean getProtectedValue() {
        return protectedValue;
    }

    public void setProtectedValue(Boolean protectedValue) {
        this.protectedValue = protectedValue;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public abstract String getScopeEntity();
}
