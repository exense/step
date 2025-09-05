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
import org.codehaus.groovy.runtime.InvokerHelper;

public class ProtectionAwareGroovySetup {

    public static void setupProtectionAwareOperations() {
        ExpandoMetaClass.enableGlobally();

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

    public static Object protectionAwarePlus(String left, Object right) {
        if (right instanceof ProtectedBinding) {
            ProtectedBinding pb = (ProtectedBinding) right;
            ProtectionContext context = ProtectionContext.get();
            if (context != null && context.canAccessProtectedValue()) {
                String result = left + pb.value.toString();
                return new ProtectedBinding(result, left + pb.obfuscatedValue);
            } else {
              throw new ProtectedPropertyException("The property " + pb.key + " is protected");
            }
        }
        // Default behavior
        return left + String.valueOf(right);
    }
}