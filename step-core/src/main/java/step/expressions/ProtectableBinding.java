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

public class ProtectableBinding extends GroovyObjectSupport {
    public static final String OBFUSCATE_MARKER = "***";
    public final boolean isProtected;
    public final Object value;
    public final String key;
    public final String obfuscatedValue;


    public ProtectableBinding(boolean isProtected, Object value, String key) {
        this(isProtected, value, key, obfuscate(key));
    }

    private static String obfuscate(String hint) {
        return OBFUSCATE_MARKER + hint + OBFUSCATE_MARKER;
    }

    public ProtectableBinding(boolean isProtected, Object value, String key, String obfuscatedValue) {
        this.isProtected = isProtected;
        this.value = value;;
        this.key = key;
        this.obfuscatedValue = obfuscatedValue;
    }

    @Override
    public Object getProperty(String propertyName) {
        if (isProtected) {
            ProtectionContext context = ProtectionContext.get();
            if (context != null && context.canAccessProtectedValue()) {
                Object propertyValue = InvokerHelper.getProperty(value, propertyName);
                String newKey = key + "." + propertyName;
                return new ProtectableBinding(true, propertyValue, newKey, obfuscate(newKey));
            } else {
                throw new ProtectedPropertyException("The property " + propertyName + " of " + key + " is protected");
            }
        }
        // Not protected - delegate to actual object
        return InvokerHelper.getProperty(value, propertyName);
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        if (isProtected) {
            ProtectionContext context = ProtectionContext.get();
            if (context == null || !context.canAccessProtectedValue()) {
                // Cannot set a property of a protected object if access is not granted
                throw new ProtectedPropertyException("The property " + propertyName + " of " + key + " is protected");
            }
        }
        // Not protected or has access - delegate to actual object
        InvokerHelper.setProperty(value, propertyName, newValue);
    }

    @Override
    public Object invokeMethod(String methodName, Object args) {
        if (isProtected) {
            ProtectionContext context = ProtectionContext.get();
            if (context != null && context.canAccessProtectedValue()) {
                // Special handling for String plus method to use our custom logic
                if ("plus".equals(methodName) && value instanceof String) {
                    Object newValue = InvokerHelper.invokeMethod(value, methodName, args);
                    Object newObfuscatedValue = InvokerHelper.invokeMethod(obfuscatedValue, methodName, args);
                    return new ProtectableBinding(true, newValue, "key concatenation", newObfuscatedValue.toString());
                } else {
                    Object o = InvokerHelper.invokeMethod(value, methodName, args);
                    String newKey = key + "." + methodName;
                    return new ProtectableBinding(true, o, newKey);
                }
            } else {
                throw new ProtectedPropertyException("Cannot invoke method '" + methodName + "' on the protected variable '" + key + "'");
            }
        }
        // Not protected - delegate to actual object
        return InvokerHelper.invokeMethod(value, methodName, args);
    }

    @Override
    public String toString() {
        if (isProtected) {
            ProtectionContext context = ProtectionContext.get();
            if (context == null || !context.canAccessProtectedValue()) {
                return obfuscatedValue;
            }
        }
        //Default for all other cases return the value toString
        return value.toString();
    }
}
