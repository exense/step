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

public class GroovyProtectedBinding extends GroovyObjectSupport {
    public static final String OBFUSCATE_MARKER = "***";
    protected final Object value;
    protected final String key;
    protected final String obfuscatedValue;


    public GroovyProtectedBinding(String key, Object value) {
        this(key, value, obfuscate(key));
    }

    public GroovyProtectedBinding(ProtectedVariable value) {
        this(value.key, value.value, value.obfuscatedValue);
    }

    public static String obfuscate(String hint) {
        return OBFUSCATE_MARKER + hint + OBFUSCATE_MARKER;
    }

    private GroovyProtectedBinding(String key, Object value, String obfuscatedValue) {
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
            return new GroovyProtectedBinding(newKey, propertyValue, obfuscate(newKey));
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
        } else {
            InvokerHelper.setProperty(value, propertyName, newValue);
        }
    }

    @Override
    public Object invokeMethod(String methodName, Object args) {
        ProtectionContext context = ProtectionContext.get();
        if (context != null && context.canAccessProtectedValue()) {
            // Special handling for String, only plus method is allowed and use custom logic
            if (value instanceof String){
                if ("plus".equals(methodName) && value instanceof String) {
                    return InvokerHelper.invokeMethod(this.toString(), methodName, args);
                    //Object newObfuscatedValue = InvokerHelper.invokeMethod(obfuscatedValue, methodName, transformArgs(args, true));
                    //return new GroovyProtectedBinding(key + "+", newValue, newObfuscatedValue.toString());
                } else {
                    throw new ProtectedPropertyException("Method '" + methodName + "' is not allowed on protected variables. It was invoked on '" + key + "'.");
                }
            } else {
                Object o = InvokerHelper.invokeMethod(value, methodName, args);
                String newKey = key + "." + methodName;
                return new GroovyProtectedBinding(newKey, o);
            }
        } else {
            throw new ProtectedPropertyException("Cannot invoke method '" + methodName + "' on the protected variable '" + key + "'");
        }
    }

    @Override
    public String toString() {
        ProtectionContext context = ProtectionContext.get();
        if (context == null || !context.canAccessProtectedValue()) {
            throw new ProtectedPropertyException("Cannot invoke method 'toString' on the protected variable '" + key + "'");
        } else {
            return context.tokenizer().tokenFor(String.valueOf(value), obfuscatedValue);
        }
    }

    //Set custom intellij rendered for debugging
    public String toObfuscatedString() {
        ProtectionContext context = ProtectionContext.get();
        if (context == null || !context.canAccessProtectedValue()) {
            throw new ProtectedPropertyException("Cannot invoke method 'toString' on the protected variable '" + key + "'");
        } else {
            return obfuscatedValue;
        }
    }
}
