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
package step.expressions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import step.commons.dynamicbeans.DynamicAttribute;
import step.core.execution.ExecutionContext;

public class DynamicBeanResolver {

	public static <T> T resolveDynamicAttributes(T bean) {
		T result = (T) cloneBean(bean);
		ExpressionHandler expressionHandler = new ExpressionHandler();
		Map<String, Object> bindings = ExecutionContext.getCurrentContext().getVariablesManager().getAllVariables();
		resolveDynamicAttributes(result, expressionHandler, bindings);
		return result;
	}
	
	public static <T> T resolveDynamicAttributes(T bean, Map<String, Object> bindings) {
		T result = (T) cloneBean(bean);
		ExpressionHandler expressionHandler = new ExpressionHandler();
		resolveDynamicAttributes(result, expressionHandler, bindings);
		return result;
	}
	
	private static void resolveDynamicAttributes(Object o, ExpressionHandler expressionHandler, Map<String, Object> bindings) {
		if(o!=null) {
			Class<?> clazz = o.getClass();
			do {
				for(Field field:clazz.getDeclaredFields()) {
					DynamicAttribute command = field.getAnnotation(DynamicAttribute.class);
					if(command!=null) {
						try {
							field.setAccessible(true);
							Object object = field.get(o);
							if(object instanceof String) {
								String string = (String) object;
								String newString = expressionHandler.evaluate(string, bindings);
								field.set(o, newString);
							} else {	
								resolveDynamicAttributes(object, expressionHandler, bindings);
							}
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
		}
	}
	
	private static <T> T cloneBean(T in) {
		try {
			@SuppressWarnings("unchecked")
			T out = (T) in.getClass().newInstance();
			Class<?> clazz = in.getClass();
			do {
				for(Field field:clazz.getDeclaredFields()) {
					if(!Modifier.isStatic(field.getModifiers())) {
						field.setAccessible(true);
						Object object = field.get(in);
						if(object instanceof String) {
							String string = (String) object;
							field.set(out, new String(string));
						} else {
							field.set(out, object);
						}
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
			
			return out;
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}
}
