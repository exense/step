/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.dynamicbeans;

import java.lang.reflect.Field;
import java.util.Map;

public class DynamicBeanResolver {
	
	DynamicValueResolver valueResolver;

	public DynamicBeanResolver(DynamicValueResolver valueResolver) {
		super();
		this.valueResolver = valueResolver;
	}

	public void evaluate(Object o, Map<String, Object> bindings) {
		if(o!=null) {
			Class<?> clazz = o.getClass();
			do {
				for(Field field:clazz.getDeclaredFields()) {
					if(field.getType().equals(DynamicValue.class)) {
						DynamicValue<?> dynamicValue;
						try {
							field.setAccessible(true);
							dynamicValue = (DynamicValue<?>) field.get(o);
							valueResolver.evaluate(dynamicValue, bindings);
							evaluate(dynamicValue.get(), bindings);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							
						}
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
		}
	}
}
