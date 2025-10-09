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

/**
 * A Groovy object wrapper that provides protected access to values with obfuscation capabilities.
 * This class extends {@link GroovyObjectSupport} to provide controlled access to wrapped objects
 * through a protection context mechanism.
 *
 * <p>The class maintains both the original value and an obfuscated representation, allowing
 * secure handling of sensitive data while still providing necessary functionality when
 * proper access permissions are granted through a {@link ProtectionContext}.</p>
 *
 * <p>Access to the wrapped value is controlled by the current {@link ProtectionContext}.
 * Operations on protected values are only allowed when the context grants access via
 * {@code canAccessProtectedValue()}.</p>
 *
 * @see GroovyObjectSupport
 * @see ProtectionContext
 * @see ProtectedVariable
 */
public class GroovyProtectedBinding extends GroovyObjectSupport {

    /**
     * Marker string used to obfuscate sensitive values in string representations.
     * The obfuscation format is: {@code OBFUSCATE_MARKER + hint + OBFUSCATE_MARKER}
     */
    public static final String OBFUSCATE_MARKER = "***";

    /**
     * The actual wrapped value that is protected by this binding.
     */
    protected final Object value;

    /**
     * A descriptive key or identifier for this protected value, used in error messages
     * and for generating nested keys when accessing properties or methods.
     */
    protected final String key;

    /**
     * The obfuscated representation of the value, used when access is not granted
     * or for secure display purposes.
     */
    protected final String obfuscatedValue;

    /**
     * Creates a new protected binding with the specified key and value.
     * The obfuscated value is automatically generated using the key as a hint.
     *
     * @param key a descriptive identifier for the protected value
     * @param value the actual value to be protected
     */
    public GroovyProtectedBinding(String key, Object value) {
        this(key, value, obfuscate(key));
    }

    /**
     * Creates a new protected binding from an existing {@link ProtectedVariable}.
     * This constructor copies all properties from the provided protected variable.
     *
     * @param protectedVariable the protected variable to wrap in this binding
     */
    public GroovyProtectedBinding(ProtectedVariable protectedVariable) {
        this(protectedVariable.key, protectedVariable.value, protectedVariable.obfuscatedValue);
    }

    /**
     * Creates an obfuscated string representation using the provided hint.
     * The resulting string follows the format: {@code ***hint***}
     *
     * @param hint the hint text to be surrounded by obfuscation markers
     * @return an obfuscated string in the format {@code ***hint***}
     */
    public static String obfuscate(String hint) {
        return OBFUSCATE_MARKER + hint + OBFUSCATE_MARKER;
    }

    /**
     * Private constructor for internal use that allows explicit specification
     * of all binding properties.
     *
     * @param key the descriptive identifier for the protected value
     * @param value the actual value to be protected
     * @param obfuscatedValue the pre-computed obfuscated representation
     */
    private GroovyProtectedBinding(String key, Object value, String obfuscatedValue) {
        this.value = value;;
        this.key = key;
        this.obfuscatedValue = obfuscatedValue;
    }

    /**
     * Retrieves a property from the wrapped value if access is granted by the protection context.
     * When access is granted, returns a new {@code GroovyProtectedBinding} wrapping the property value.
     *
     * <p>The returned binding will have a key that combines the current key with the property name,
     * separated by a dot (e.g., "originalKey.propertyName").</p>
     *
     * @param propertyName the name of the property to access
     * @return a new {@code GroovyProtectedBinding} wrapping the property value
     * @throws ProtectedPropertyException if access is not granted by the current protection context
     * @see ProtectionContext#canAccessProtectedValue()
     */
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

    /**
     * Sets a property on the wrapped value if access is granted by the protection context.
     * This operation directly modifies the wrapped object's property.
     *
     * @param propertyName the name of the property to set
     * @param newValue the new value to assign to the property
     * @throws ProtectedPropertyException if access is not granted by the current protection context
     * @see ProtectionContext#canAccessProtectedValue()
     */
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

    /**
     * Invokes a method on the wrapped value if access is granted by the protection context.
     *
     * <p>Special handling is provided for {@code String} values:</p>
     * <ul>
     *   <li>Only the {@code plus} method is allowed on protected String values</li>
     *   <li>The {@code plus} method delegates to the string representation of this binding</li>
     *   <li>All other methods on String values throw a {@code ProtectedPropertyException}</li>
     * </ul>
     *
     * <p>For non-String values, the method is invoked normally and the result is wrapped
     * in a new {@code GroovyProtectedBinding} with an updated key.</p>
     *
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method
     * @return the result of the method invocation, potentially wrapped in a new binding
     * @throws ProtectedPropertyException if access is not granted or if an unsupported
     *         method is called on a protected String value
     * @see ProtectionContext#canAccessProtectedValue()
     */
    @Override
    public Object invokeMethod(String methodName, Object args) {
        ProtectionContext context = ProtectionContext.get();
        if (context != null && context.canAccessProtectedValue()) {
            // Special handling for String, only plus method is allowed and use custom logic
            if (value instanceof String){
                if ("plus".equals(methodName) && value instanceof String) {
                    return InvokerHelper.invokeMethod(getTokenizedString(context), methodName, args);
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

    /**
     * Returns a string representation of the wrapped value if access is granted.
     * The actual string returned is determined by the protection context's tokenizer,
     * which may return either the actual value or a tokenized representation.
     *
     * @return a string representation of the wrapped value as determined by the protection context
     * @throws ProtectedPropertyException if access is not granted by the current protection context
     * @see ProtectionContext#canAccessProtectedValue()
     * @see ProtectionContext#tokenizer()
     */
    @Override
    public String toString() {
        ProtectionContext context = ProtectionContext.get();
        if (context == null || !context.canAccessProtectedValue()) {
            throw new ProtectedPropertyException("Cannot invoke method 'toString' on the protected variable '" + key + "'");
        } else {
            return getTokenizedString(context);
        }
    }

    private String getTokenizedString(ProtectionContext context) {
        return context.tokenizer().tokenFor(String.valueOf(value), obfuscatedValue);
    }

    /**
     * Returns the obfuscated string representation of this binding for debugging purposes.
     * This method is intended for use with custom IntelliJ IDEA renderers configuration during debugging sessions.
     *
     * <p>Unlike {@link #toString()}, this method specifically returns the obfuscated value
     * rather than delegating to the protection context's tokenizer.</p>
     *
     * @return the obfuscated string representation of the wrapped value
     * @throws ProtectedPropertyException if access is not granted by the current protection context
     * @see #toString()
     */
    public String toObfuscatedString() {
        ProtectionContext context = ProtectionContext.get();
        if (context == null || !context.canAccessProtectedValue()) {
            throw new ProtectedPropertyException("Cannot invoke method 'toString' on the protected variable '" + key + "'");
        } else {
            return obfuscatedValue;
        }
    }
}