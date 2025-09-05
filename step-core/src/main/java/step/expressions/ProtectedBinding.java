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
package step.expressions;

import groovy.lang.GroovyObjectSupport;
import org.codehaus.groovy.runtime.InvokerHelper;

public class ProtectedBinding extends GroovyObjectSupport {
    public static final String OBFUSCATE_MARKER = "***";
    public final Object value;
    public final String key;
    public final String obfuscatedValue;


    public ProtectedBinding(Object value, String key) {
        this(value, key, obfuscate(key));
    }

    private static String obfuscate(String hint) {
        return OBFUSCATE_MARKER + hint + OBFUSCATE_MARKER;
    }

    public ProtectedBinding(Object value, String key, String obfuscatedValue) {
        this.value = value;;
        this.key = key;
        this.obfuscatedValue = obfuscatedValue;
    }

    @Override
    public Object getProperty(String propertyName) {
        ProtectionContext context = ProtectionContext.get();
        if (context != null && context.canAccessProtectedValue()) {
            Object propertyValue = InvokerHelper.getProperty(value, propertyName);
            String newKey = key + "." + propertyName;
            return new ProtectedBinding(propertyValue, newKey, obfuscate(newKey));
        } else {
            throw new ProtectedPropertyException("The property " + propertyName + " of " + key + " is protected");
        }
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        ProtectionContext context = ProtectionContext.get();
        if (context == null || !context.canAccessProtectedValue()) {
            // Cannot set a property of a protected object if access is not granted
            throw new ProtectedPropertyException("The property " + propertyName + " of " + key + " is protected");
        }
    }

    @Override
    public Object invokeMethod(String methodName, Object args) {
        ProtectionContext context = ProtectionContext.get();
        if (context != null && context.canAccessProtectedValue()) {
            // Special handling for String plus method to use our custom logic
            if ("plus".equals(methodName) && value instanceof String) {
                Object newValue = InvokerHelper.invokeMethod(value, methodName, args);
                Object newObfuscatedValue = InvokerHelper.invokeMethod(obfuscatedValue, methodName, args);
                return new ProtectedBinding(newValue, key+"+", newObfuscatedValue.toString());
            } else {
                Object o = InvokerHelper.invokeMethod(value, methodName, args);
                String newKey = key + "." + methodName;
                return new ProtectedBinding(o, newKey);
            }
        } else {
            throw new ProtectedPropertyException("Cannot invoke method '" + methodName + "' on the protected variable '" + key + "'");
        }
    }

    @Override
    public String toString() {
        ProtectionContext context = ProtectionContext.get();
        if (context == null || !context.canAccessProtectedValue()) {
            return obfuscatedValue; //TODO should we rather throw an exception here?
        } else {
            return value.toString();
        }
    }
}
