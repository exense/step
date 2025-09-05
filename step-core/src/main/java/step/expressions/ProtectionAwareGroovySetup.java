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

import groovy.lang.ExpandoMetaClass;
import groovy.lang.Closure;
import groovy.lang.GString;
import org.codehaus.groovy.runtime.InvokerHelper;

public class ProtectionAwareGroovySetup {

    public static void setupProtectionAwareOperations() {
        ExpandoMetaClass.enableGlobally();

        // Override GString's plus method to handle ProtectedBinding
        ExpandoMetaClass gstringEmc = new ExpandoMetaClass(GString.class, false, true);
        gstringEmc.registerInstanceMethod("plus", new Closure<Object>(null) {
            @Override
            public Object call(Object arg) {
                GString self = (GString) getDelegate();
                return protectionAwareGStringPlus(self, arg);
            }

            @Override
            public Object call(Object... args) {
                GString self = (GString) getDelegate();
                if (args.length > 0) {
                    return protectionAwareGStringPlus(self, args[0]);
                }
                return self;
            }

            @Override
            public int getMaximumNumberOfParameters() {
                return 1;
            }

            @Override
            public Class[] getParameterTypes() {
                return new Class[]{Object.class};
            }
        });
        gstringEmc.initialize();
        InvokerHelper.getMetaRegistry().setMetaClass(GString.class, gstringEmc);

        ExpandoMetaClass stringEmc = new ExpandoMetaClass(String.class, false, true);

        // Use the correct Closure methods: call() instead of doCall()
        stringEmc.registerInstanceMethod("plus", new Closure<Object>(null) {
            @Override
            public Object call(Object arg) {
                String self = (String) getDelegate();
                return protectionAwarePlus(self, arg);
            }

            @Override
            public Object call(Object... args) {
                String self = (String) getDelegate();
                if (args.length > 0) {
                    return protectionAwarePlus(self, args[0]);
                }
                return self;
            }

            @Override
            public int getMaximumNumberOfParameters() {
                return 1;
            }

            @Override
            public Class[] getParameterTypes() {
                return new Class[]{Object.class};
            }
        });

        stringEmc.initialize();
        InvokerHelper.getMetaRegistry().setMetaClass(String.class, stringEmc);
    }

    /**
     * Handle GString plus ProtectedBinding concatenation
     * @param gstring the GString left operand
     * @param right the right operand
     * @return concatenation result
     */
    public static Object protectionAwareGStringPlus(GString gstring, Object right) {
        if (right instanceof ProtectedBinding) {
            ProtectedBinding pb = (ProtectedBinding) right;
            ProtectionContext context = ProtectionContext.get();
            if (context != null && context.canAccessProtectedValue()) {
                // First convert GString to ProtectedBinding if it contains protected values
                Object leftProcessed = handleGStringWithProtectedBindings(gstring);
                if (leftProcessed instanceof ProtectedBinding) {
                    ProtectedBinding leftPb = (ProtectedBinding) leftProcessed;
                    String result = leftPb.value.toString() + pb.value.toString();
                    String resultObfuscated = leftPb.obfuscatedValue + pb.obfuscatedValue;
                    return new ProtectedBinding(result, "gstring+", resultObfuscated);
                } else {
                    // GString doesn't contain protected values, just concatenate
                    String result = leftProcessed.toString() + pb.value.toString();
                    String resultObfuscated = leftProcessed.toString() + pb.obfuscatedValue;
                    return new ProtectedBinding(result, "gstring+", resultObfuscated);
                }
            } else {
                throw new ProtectedPropertyException("The property " + pb.key + " is protected");
            }
        }
        // Default behavior - convert GString to string first if it contains protected values
        Object leftProcessed = handleGStringWithProtectedBindings(gstring);
        return leftProcessed.toString() + String.valueOf(right);
    }

    /**
     * Handle GString interpolation that contains ProtectedBinding objects
     * @param gstring the GString to process
     * @return ProtectedBinding if any values are protected, otherwise the GString result
     */
    public static Object handleGStringWithProtectedBindings(GString gstring) {
        Object[] values = gstring.getValues();
        boolean hasProtectedBinding = false;
        
        for (Object value : values) {
            if (value instanceof ProtectedBinding) {
                hasProtectedBinding = true;
                break;
            }
        }
        
        if (hasProtectedBinding) {
            ProtectionContext context = ProtectionContext.get();
            if (context != null && context.canAccessProtectedValue()) {
                // Build both real and obfuscated strings
                StringBuilder realResult = new StringBuilder();
                StringBuilder obfuscatedResult = new StringBuilder();
                String[] strings = gstring.getStrings();
                
                for (int i = 0; i < values.length; i++) {
                    realResult.append(strings[i]);
                    obfuscatedResult.append(strings[i]);
                    
                    Object value = values[i];
                    if (value instanceof ProtectedBinding) {
                        ProtectedBinding pb = (ProtectedBinding) value;
                        realResult.append(pb.value.toString());
                        obfuscatedResult.append(pb.obfuscatedValue);
                    } else {
                        String stringValue = String.valueOf(value);
                        realResult.append(stringValue);
                        obfuscatedResult.append(stringValue);
                    }
                }
                // Append final string part
                if (strings.length > values.length) {
                    realResult.append(strings[strings.length - 1]);
                    obfuscatedResult.append(strings[strings.length - 1]);
                }
                
                return new ProtectedBinding(realResult.toString(), "gstring", obfuscatedResult.toString());
            } else {
                throw new ProtectedPropertyException("GString contains protected bindings");
            }
        }
        
        // No protected bindings, use default GString behavior
        return gstring.toString();
    }

    /**
     * this method is required to handle strings concatenations including protected bindings when the left argument is not a protected binding
     * for cases where the lerft argument is a protected cases the invocation goes through ProtectedBinding.invokeMethod
     * @param left the left string of the concatenation
     * @param right the right part of the concatenation
     * @return result of the concatenation (+ operator for Strings)
     */
    public static Object protectionAwarePlus(String left, Object right) {
        if (right instanceof ProtectedBinding) {
            ProtectedBinding pb = (ProtectedBinding) right;
            ProtectionContext context = ProtectionContext.get();
            if (context != null && context.canAccessProtectedValue()) {
                String result = left + pb.value.toString();
                String resultObfuscated = left + pb.obfuscatedValue;
                return new ProtectedBinding(result, left + "+", resultObfuscated);
            } else {
              throw new ProtectedPropertyException("The property " + pb.key + " is protected");
            }
        }
        // Default behavior
        return left + String.valueOf(right);
    }
}